import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.View;
import lotus.domino.NotesException;
import lotus.notes.internal.MessageQueue;
import net.prominic.gja_v20220413.JavaServerAddinGenesis;
import net.prominic.iMessageSMS.SendBlueHelper;
import net.prominic.iMessageSMS.TwilioHelper;

public class iMessageSMS extends JavaServerAddinGenesis {
	Database				m_database				= null;		// iMessageSMS.nsf
	TwilioHelper 			m_twilioHelper			= null;
	SendBlueHelper 			m_sendblueHelper		= null;

	// Instance variables
	View 					m_twilio				= null;
	View 					m_sendblue				= null;
	private int				m_interval				= 3;		// seconds
	private long			m_counter				= 0;

	@Override
	protected String getJavaAddinVersion() {
		return "0.4.3";
	}

	@Override
	protected String getJavaAddinDate() {
		return "2022-04-13 15:05";
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
			
			view.recycle();
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	protected void listen() {
		StringBuffer qBuffer = new StringBuffer(1024);

		try {
			mq = new MessageQueue();
			int messageQueueState = mq.create(this.getQName(), 0, 0);	// use like MQCreate in API
			if (messageQueueState == MessageQueue.ERR_DUPLICATE_MQ) {
				logMessage(this.getJavaAddinName() + " task is already running");
				return;
			}

			if (messageQueueState != MessageQueue.NOERROR) {
				logMessage("Unable to create the Domino message queue");
				return;
			}

			if (mq.open(this.getQName(), 0) != MessageQueue.NOERROR) {
				logMessage("Unable to open Domino message queue");
				return;
			}

			while (this.addInRunning() && messageQueueState != MessageQueue.ERR_MQ_QUITTING) {
				setAddinState("Idle");

				/* gives control to other task in non preemptive os*/
				OSPreemptOccasionally();

				// check for command from console
				messageQueueState = mq.get(qBuffer, MQ_MAX_MSGSIZE, MessageQueue.MQ_WAIT_FOR_MSG, 1000);
				if (messageQueueState == MessageQueue.ERR_MQ_QUITTING) {
					return;
				}

				// check messages
				String cmd = qBuffer.toString().trim();
				if (!cmd.isEmpty()) {
					resolveMessageQueueState(cmd);
				};

				if (this.AddInHasSecondsElapsed(m_interval)) {
					setAddinState("checking...");
					process();
				}
			}
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
		logMessage("counter      " + m_counter);
	}

	/*
	 * business logic
	 */
	private void process() throws NotesException {
		processSendBlue();
		processTwilio();
	}

	private void processTwilio() throws NotesException {
		if (m_twilioHelper == null) return;

		m_twilio.refresh();

		Document doc = m_twilio.getFirstDocument();
		while (doc != null) {
			setAddinState("sending...");

			Document docNext = m_twilio.getNextDocument(doc);

			int res = 0;
			String to = doc.getItemValueString("To");
			String body = doc.getItemValueString("Body");
			if (!(to.isEmpty() || body.isEmpty())) {
				res = m_twilioHelper.send(to, body);
			}

			// mark as processed
			doc.replaceItemValue("ProcessedTwilio", "1");
			doc.replaceItemValue("ResponseCodeTwilio", res);
			doc.save();
			doc.recycle();
			m_counter++;

			doc = docNext;
		}
	}

	private void processSendBlue() throws NotesException {
		if (m_sendblueHelper == null) return;

		m_sendblue.refresh();

		Document doc = m_sendblue.getFirstDocument();
		while (doc != null) {
			setAddinState("sending...");

			Document docNext = m_sendblue.getNextDocument(doc);

			int res = 0;
			String to = doc.getItemValueString("To");
			String body = doc.getItemValueString("Body");
			if (!(to.isEmpty() || body.isEmpty())) {
				res = m_sendblueHelper.send(to, body);
			}

			// mark as processed
			doc.replaceItemValue("ProcessedSendBlue", "1");
			doc.replaceItemValue("ResponseCodeSendBlue", res);
			doc.save();
			doc.recycle();
			m_counter++;

			doc = docNext;
		}
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
