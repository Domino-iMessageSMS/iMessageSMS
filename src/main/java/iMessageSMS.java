import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import net.prominic.gja_v082.JavaServerAddinGenesis;
import net.prominic.iMessageSMS.EventSendSMS;
import net.prominic.iMessageSMS.TwilioHelper;

public class iMessageSMS extends JavaServerAddinGenesis {
	Database m_database = null; // iMessageSMS.nsf
	TwilioHelper m_twilioHelper = null;
	View m_twilio = null;
	private int m_interval = 3; // seconds

	@Override
	protected String getJavaAddinVersion() {
		return "0.6.0";
	}

	@Override
	protected String getJavaAddinDate() {
		return "2023-02-21 19:00";
	}

	@Override
	protected void runNotesBeforeListen() {
		initHelpers();
	}

	private void initHelpers() {
		try {
			// 1. database
			m_database = m_session.getDatabase(null, "iMessageSMS.nsf");
			if (m_database == null || !m_database.isOpen()) {
				logMessage("(!) iMessageSMS.nsf - can't be opened");
				return;
			}

			// 2. create twilio rest helper
			initTwilioHelper();
			if (m_twilioHelper==null) return;

			m_twilio = m_database.getView("(Sys.UnprocessedTwilio)");
			m_twilio.setAutoUpdate(false);

			// 4. create send sms job
			EventSendSMS event = new EventSendSMS("SendSMS", m_interval, false, this.m_logger);
			event.twilioHelper = m_twilioHelper;
			event.twilio = m_twilio;
			eventsAdd(event);
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

	protected boolean resolveMessageQueueState(String cmd) {
		boolean flag = super.resolveMessageQueueState(cmd);
		if (flag)
			return true;

		if (cmd.startsWith("sms ")) {
			sms(cmd);
		} else if ("cmd".startsWith("config")) {
			config();
		} else {
			logMessage("invalid command (use -h or help to get details)");
		}

		return true;
	}

	private void initTwilioHelper() {
		try {
			View view = m_database.getView("(Sys.Config)");
			Document doc = view.getFirstDocument();
			if (doc == null) {
				logMessage("(!) Config is missing in iMessageSMS.nsf");
				return;
			}

			String Account_SID = doc.getItemValueString("Account_SID");
			String Auth_token = doc.getItemValueString("Auth_token");
			String Phone = doc.getItemValueString("Phone");

			doc.recycle();
			view.recycle();

			if (Account_SID.isEmpty() || Auth_token.isEmpty()) {
				logMessage("(!) Config missing SID/token");
				return;
			}

			if (m_twilioHelper == null) {
				m_twilioHelper = new TwilioHelper(Account_SID, Auth_token, Phone);
			} else {
				m_twilioHelper.setAccount_sid(Account_SID);
				m_twilioHelper.setAuth_token(Auth_token);
				m_twilioHelper.setPhone(Phone);
			}
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

	private void config() {
		initTwilioHelper();
	}

	private void sms(String cmd) {
		if (cmd.length() < 10) {
			this.logMessage("command should be longer than 10 characters");
			return;
		}

		int index1 = cmd.indexOf(" ", 4);
		String to = cmd.substring(4, index1);
		String body = cmd.substring(index1 + 1);

		try {
			Document doc = m_database.createDocument();
			doc.replaceItemValue("Form", "Request");
			doc.replaceItemValue("To", to);
			doc.replaceItemValue("Body", body);
			doc.save();

			doc.recycle();

			logMessage("request has been created");
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

	protected void showHelpExt() {
		logMessage("   config           Refresh SID, token and phone from active config");
		logMessage("   sms <to> <body>  Send sms");
	}

	protected void showInfoExt() {
		logMessage("interval     " + m_interval);
		logMessage("twilio phone " + m_twilioHelper.getPhone());
	}

	protected void termBeforeAB() {
		try {
			if (m_twilio != null) {
				m_twilio.recycle();
				m_twilio = null;
			}

			if (this.m_database != null) {
				this.m_database.recycle();
				this.m_database = null;
			}
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

}
