import lotus.domino.Database;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import net.prominic.gja_v084.JavaServerAddinGenesis;
import net.prominic.iMessageSMS.EventMessagingServiceHandler;
import net.prominic.iMessageSMS.MessagingServiceHelper;
import net.prominic.iMessageSMS.SinchHelper;
import net.prominic.iMessageSMS.TwilioHelper;

public class iMessageSMS extends JavaServerAddinGenesis {
	private Database m_database = null; // iMessageSMS.nsf
	private MessagingServiceHelper m_messagingServiceHelper = null;
	private View m_request = null;
	private int m_interval = 3; // seconds

	@Override
	protected String getJavaAddinVersion() {
		return "1.1.1";
	}

	@Override
	protected String getJavaAddinDate() {
		return "2024-07-01 19:30 (WhatsApp)";
	}

	@Override
	protected boolean runNotesAfterInitialize() {
		try {
			// 1. Open database
			m_database = m_session.getDatabase(null, "iMessageSMS.nsf");
			if (m_database == null || !m_database.isOpen()) {
				logMessage("(!) iMessageSMS.nsf - can't be opened");
				return false;
			}

			// 2. Config
			Document doc = getConfig();
			if (doc == null) {
				logMessage("(!) Active Config is missing in iMessageSMS.nsf");
				return false;
			}

			// 3. Initialize MessagingServiceHelper (Twilio or Sinch)
			if (!initMessageHelper(doc)) {
				return false;	
			}

			// 4. Get view and disable auto-update
			m_request = m_database.getView("($requests.unprocessed)");
			if (m_request == null) {
				logMessage("(!) Unable to open view ($requests.unprocessed)");
				return false;
			}
			m_request.setAutoUpdate(false);

			// 5. Create and add messaging event handler
			String ForceMessageType = doc.getItemValueString("ForceMessageType");
			EventMessagingServiceHandler event = new EventMessagingServiceHandler("MessagingServiceHandler", m_interval,
					false, this.m_logger);
			event.messsangingHelper = m_messagingServiceHelper;
			event.request = m_request;
			event.forceMessageType = ForceMessageType;
			eventsAdd(event);

			doc.recycle();

			return true;
		} catch (NotesException e) {
			logException(e);
			return false;
		}
	}
	
	private Document getConfig() {
		View view = null;
		Document doc = null;
		try {
			view = m_database.getView("($config)");
			doc = view.getFirstDocument();
			view.recycle();
		} catch (NotesException e) {
			e.printStackTrace();
		}
		return doc;
	}

	@Override
	protected boolean resolveMessageQueueState(String cmd) {
		boolean flag = super.resolveMessageQueueState(cmd);
		if (flag)
			return true;

		if (cmd.startsWith("sms ")) {
			sms(cmd);
		} else if (cmd.startsWith("call ")) {
			call(cmd);
		} else if (cmd.startsWith("config")) {
			config();
		} else {
			logMessage("Invalid command (use -h or help to get details)");
		}

		return true;
	}

	private boolean initMessageHelper(Document doc) {
		try {
			String provider = doc.getItemValueString("Provider");
			if ("Sinch".equalsIgnoreCase(provider)) {
				String SinchServicePlanID = doc.getItemValueString("SinchServicePlanID");
				String SinchAPIToken = doc.getItemValueString("SinchAPIToken");
				String SinchPhone = doc.getItemValueString("SinchPhone");
				String SinchAppKey = doc.getItemValueString("SinchAppKey");
				String SinchAppSecret = doc.getItemValueString("SinchAppSecret");

				if (SinchServicePlanID.isEmpty() || SinchAPIToken.isEmpty()) {
					logMessage("(!) Config missing Twilio SID/token");
					return false;
				}

				m_messagingServiceHelper = new SinchHelper(SinchServicePlanID, SinchAPIToken, SinchAppKey, SinchAppSecret,
						SinchPhone);
			} else {
				String TwilioAccount_SID = doc.getItemValueString("TwilioAccount_SID");
				String TwilioAuth_token = doc.getItemValueString("TwilioAuth_token");
				String TwilioPhone = doc.getItemValueString("TwilioPhone");

				if (TwilioAccount_SID.isEmpty() || TwilioAuth_token.isEmpty()) {
					logMessage("(!) Config missing Twilio SID/token");
					return false;
				}
				m_messagingServiceHelper = new TwilioHelper(TwilioAccount_SID, TwilioAuth_token, TwilioPhone);
			}
			return true;
		} catch (NotesException e) {
			e.printStackTrace();
		}

		return false;
	}

	private void config() {
		Document doc = getConfig();
		if (doc == null) {
			logMessage("(!) Active Config is missing in iMessageSMS.nsf");
			return;
		}
		
		if (initMessageHelper(doc)) {
			logMessage("Config updated: OK");
		} else {
			logMessage("Config updated: failed");
		}
	}

	private void sms(String cmd) {
		send(cmd, "sms");
	}

	private void call(String cmd) {
		send(cmd, "call");
	}

	private void send(String cmd, String type) {
		if (cmd.length() < type.length() + 1) {
			logMessage("Command should be longer than the type and a space");
			return;
		}

		try {
			int index = cmd.indexOf(" ", type.length() + 1);
			if (index == -1 || index == cmd.length() - 1) {
				logMessage("Command format is incorrect");
				return;
			}

			String to = cmd.substring(type.length() + 1, index).trim();
			String body = cmd.substring(index + 1).trim();

			if (to.isEmpty() || body.isEmpty()) {
				logMessage("To and Body must not be empty");
				return;
			}

			Document doc = m_database.createDocument();
			doc.replaceItemValue("Form", "Request");
			doc.replaceItemValue("Type", type);
			doc.replaceItemValue("To", to);
			doc.replaceItemValue("Body", body);
			doc.save();
			doc.recycle();

			logMessage("Request has been created");
		} catch (NotesException e) {
			logException(e);
		}
	}

	@Override
	protected void showHelpExt() {
		logMessage("   config            Refresh SID, token and phone from active config");
		logMessage("   sms <to> <body>   Send SMS");
		logMessage("   call <to> <body>  Start a call");
	}

	@Override
	protected void showInfoExt() {
		logMessage("Interval:     " + m_interval + " seconds");
		if (m_messagingServiceHelper != null) {
			logMessage("Provider: " + m_messagingServiceHelper.getServiceName());
			logMessage("Phone:    " + m_messagingServiceHelper.getFromPhone());
		} else {
			logMessage("Messanging Helper not initialized.");
		}
	}

	@Override
	protected void termBeforeAB() {
		try {
			if (m_request != null) {
				m_request.recycle();
				m_request = null;
			}

			if (m_database != null) {
				m_database.recycle();
				m_database = null;
			}
		} catch (NotesException e) {
			logException(e);
		}
	}

	private void logException(Exception e) {
		e.printStackTrace(); // You might want to use a proper logging framework here
		logMessage("Error: " + e.getMessage());
	}
}
