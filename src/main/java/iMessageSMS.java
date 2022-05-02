import java.util.HashMap;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import net.prominic.gja_v20220427.Event;
import net.prominic.gja_v20220427.JavaServerAddinGenesis;
import net.prominic.iMessageSMS.EventSendSMS;
import net.prominic.iMessageSMS.SendBlueHelper;
import net.prominic.iMessageSMS.TwilioHelper;

public class iMessageSMS extends JavaServerAddinGenesis {
	Database				m_database				= null;		// iMessageSMS.nsf
	TwilioHelper 			m_twilioHelper 			= null;
	SendBlueHelper 			m_sendblueHelper 		= null;
	View 					m_twilio				= null;
	View 					m_sendblue				= null;
	private int				m_interval				= 3;		// seconds

	@Override
	protected String getJavaAddinVersion() {
		return "0.4.4";
	}

	@Override
	protected String getJavaAddinDate() {
		return "2022-05-02 12:35";
	}

	@Override
	protected void runNotesBeforeListen() {
		initHelpers();
	}

	private void initHelpers() {
		try {
			m_database = m_session.getDatabase(null, "iMessageSMS.nsf");
			if (m_database == null || !m_database.isOpen()) {
				logMessage("iMessageSMS.nsf" + " - not opened.");
				return;
			}

			m_twilio = m_database.getView("(Sys.UnprocessedTwilio)");
			m_twilio.setAutoUpdate(false);
			m_sendblue = m_database.getView("(Sys.UnprocessedSendBlue)");
			m_sendblue.setAutoUpdate(false);

			View view = m_database.getView("(Sys.Config)");
			Document doc = view.getFirstDocument();
			while (doc != null) {
				Document docNext = view.getNextDocument(doc);
				
				String form = doc.getItemValueString("Form");
				if (form.equalsIgnoreCase("twilio") && m_twilioHelper == null) {
					String Account_SID = doc.getItemValueString("Account_SID");
					String Auth_token = doc.getItemValueString("Auth_token");
					String Phone = doc.getItemValueString("Phone");
					m_twilioHelper = new TwilioHelper(Account_SID, Auth_token, Phone);
				}
				else if(form.equalsIgnoreCase("sendblue") && m_sendblueHelper == null) {
					String api_key = doc.getItemValueString("api_key");
					String api_secret = doc.getItemValueString("api_secret");
					m_sendblueHelper = new SendBlueHelper(api_key, api_secret);
				}

				doc.recycle();
				doc = docNext;
			}
			
			// init Event
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("twilioHelper", m_twilioHelper);
			params.put("sendblueHelper", m_sendblueHelper);
			params.put("twilio", m_twilio);
			params.put("sendblue", m_sendblue);
			Event event = new EventSendSMS("SendSMS", m_interval, false, params, this.m_logger);
			eventsAdd(event);
			
			view.recycle();
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

	protected boolean resolveMessageQueueState(String cmd) {
		boolean flag = super.resolveMessageQueueState(cmd);
		if (flag) return true;

		if (cmd.startsWith("sms ")) {
			sms(cmd);
		}
		else {
			logMessage("invalid command (use -h or help to get details)");
		}

		return true;
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

			this.logMessage("request has been created");
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

	protected void showHelpExt() {
		AddInLogMessageText("   sms <to> <body>  Send sms");		
	}

	protected void showInfoExt() {
		logMessage("interval     " + m_interval);
		logMessage("twilio       " + String.valueOf(this.m_twilioHelper != null));		
		logMessage("sendblue     " + String.valueOf(this.m_sendblueHelper != null));
	}

	protected void termBeforeAB() {
		try {
			if (m_twilio != null) {
				m_twilio.recycle();
				m_twilio = null;
			}

			if (m_sendblue != null) {
				m_sendblue.recycle();
				m_sendblue = null;
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
