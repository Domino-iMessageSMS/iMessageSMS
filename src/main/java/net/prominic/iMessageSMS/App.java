package net.prominic.iMessageSMS;

import java.util.Arrays;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.notes.addins.JavaServerAddin;
import lotus.notes.internal.MessageQueue;

public class App extends JavaServerAddin {
	// Constants
	private final String		JADDIN_NAME				= "iMessageSMS";
	private final String		JADDIN_VERSION			= "0.3.5 (message configuration)";
	private final String		JADDIN_DATE				= "2021-03-17 16:30";

	// MessageQueue Constants
	public static final int MQ_MAX_MSGSIZE = 1024;
	// this is already defined (should be = 1):
	public static final int	MQ_WAIT_FOR_MSG = MessageQueue.MQ_WAIT_FOR_MSG;

	TwilioHelper 			m_twilioHelper			= null;
	SendBlueHelper 			m_sendblueHelper		= null;

	// Instance variables
	MessageQueue 			mq						= null;
	Session 				m_session				= null;
	Database 				m_database				= null;
	View 					m_twilio				= null;
	View 					m_sendblue				= null;
	private String[] 		args 					= null;
	private int 			dominoTaskID			= 0;
	private int				m_interval				= 3;		// second
	private int 			m_logState				= 1;		// 0 - debug, 1 - events, 2 - warnings, 3 errors

	// constructor if parameters are provided
	public App(String[] args) {
		this.args = args;
	}

	public App() {}

	/* the runNotes method, which is the main loop of the Addin */
	@Override
	public void runNotes () {
		setAddinState("Initializing");

		this.setName(JADDIN_NAME);

		// Create the status line showed in 'Show Task' console command
		this.dominoTaskID = createAddinStatusLine(this.JADDIN_NAME);

		try {
			m_session = NotesFactory.createSession();
			m_database = m_session.getDatabase("", "iMessageSMS.nsf");
			m_twilio = m_database.getView("(Sys.UnprocessedTwilio)");
			m_twilio.setAutoUpdate(false);
			m_sendblue = m_database.getView("(Sys.UnprocessedSendBlue)");
			m_sendblue.setAutoUpdate(false);

			logMessage("version      " + this.JADDIN_VERSION);
			logMessage("date         " + this.JADDIN_DATE);
			logMessage("parameters   " + Arrays.toString(this.args));
			logMessage("interval     " + m_interval);
			logMessage("log level    " + m_logState);

			createHelpers();

			runLoop();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void createHelpers() throws NotesException {
		View view = m_database.getView("(Sys.Config)");
		Document doc = view.getFirstDocument();
		while (doc != null) {
			String form = doc.getItemValueString("Form");
			if (form.equalsIgnoreCase("twilio") && m_twilioHelper == null) {
				String Account_SID = doc.getItemValueString("Account_SID");
				String Auth_token = doc.getItemValueString("Auth_token");
				String Phone = doc.getItemValueString("Phone");
				m_twilioHelper = new TwilioHelper(Account_SID, Auth_token, Phone);
				this.logMessage("Twilio is loaded");
			}
			else if(form.equalsIgnoreCase("sendblue") && m_sendblueHelper == null) {
				String api_key = doc.getItemValueString("api_key");
				String api_secret = doc.getItemValueString("api_secret");
				m_sendblueHelper = new SendBlueHelper(api_key, api_secret);
				this.logMessage("SendBlue is loaded");
			}

			doc = view.getNextDocument(doc);
		}
	}

	@SuppressWarnings("deprecation")
	private void runLoop() {
		StringBuffer qBuffer = new StringBuffer(1024);
		String qName = MSG_Q_PREFIX + JADDIN_NAME.toUpperCase();

		mq = new MessageQueue();
		int messageQueueState = mq.create(qName, 0, 0);	// use like MQCreate in API
		if (messageQueueState == MessageQueue.ERR_DUPLICATE_MQ) {
			logMessage(this.JADDIN_NAME + " task is already running");
			return;
		}

		if (messageQueueState != MessageQueue.NOERROR) {
			logMessage("Unable to create the Domino message queue");
			return;
		}

		if (mq.open(qName, 0) != MessageQueue.NOERROR) {
			logMessage("Unable to open Domino message queue");
			return;
		}

		while (this.addInRunning() && messageQueueState != MessageQueue.ERR_MQ_QUITTING) {
			/* gives control to other task in non preemptive os*/
			OSPreemptOccasionally();

			// check for command from console
			messageQueueState = mq.get(qBuffer, MQ_MAX_MSGSIZE, MessageQueue.MQ_WAIT_FOR_MSG, 1000);

			setAddinState("Idle");

			if (this.AddInHasSecondsElapsed(m_interval)) {
				try {
					setAddinState("processing...");
					process();
				} catch (NotesException e) {
					this.stopAddin();
					e.printStackTrace();
				}
			}
		}
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

			doc = docNext;
		}
	}

	private void processSendBlue() throws NotesException {
		if (m_sendblueHelper == null) return;

		m_sendblue.refresh();

		Document doc = m_sendblue.getFirstDocument();
		while (doc != null) {
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

			doc = docNext;
		}
	}

	/**
	 * Create the Domino task status line which is shown in <code>"show tasks"</code> command.
	 *
	 * Note: This method is also called by the JAddinThread and the user add-in
	 *
	 * @param	name	Name of task
	 * @return	Domino task ID
	 */
	public final int createAddinStatusLine(String name) {
		return (AddInCreateStatusLine(name));
	}

	/**
	 * Delete the Domino task status line.
	 *
	 * Note: This method is also called by the JAddinThread and the user add-in
	 *
	 * @param	id	Domino task id
	 */
	public final void deleteAddinStatusLine(int id) {
		if (id != 0)
			AddInDeleteStatusLine(id);
	}

	/**
	 * Set the text of the add-in which is shown in command <code>"show tasks"</code>.
	 *
	 * @param	text	Text to be set
	 */
	private final void setAddinState(String text) {

		if (this.dominoTaskID == 0)
			return;

		AddInSetStatusLine(this.dominoTaskID, text);
	}

	/**
	 * Write a log message to the Domino console. The message string will be prefixed with the add-in name
	 * followed by a column, e.g. <code>"AddinName: xxxxxxxx"</code>
	 *
	 * @param	message		Message to be displayed
	 */
	private final void logMessage(String message) {
		AddInLogMessageText(this.JADDIN_NAME + ": " + message, 0);
	}

	@Override
	public void termThread() {
		terminate();

		super.termThread();
	}

	/**
	 * Terminate all variables
	 */
	private void terminate() {
		try {
			deleteAddinStatusLine(dominoTaskID);

			if (m_twilio != null) {
				m_twilio.recycle();
				m_twilio = null;
			}

			if (m_sendblue != null) {
				m_sendblue.recycle();
				m_sendblue = null;
			}

			if (m_database != null) {
				m_database.recycle();
				m_database = null;
			}

			if (m_session != null) {
				m_session.recycle();
				m_session = null;
			}

			if (this.mq != null) {
				this.mq.close(0);
				this.mq = null;
			}

			logMessage("UNLOADED (OK) " + this.JADDIN_VERSION);
		} catch (NotesException e) {
			logMessage("UNLOADED (FAILED) " + this.JADDIN_VERSION);
		}
	}
}
