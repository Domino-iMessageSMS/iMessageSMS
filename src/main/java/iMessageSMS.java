import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import net.prominic.gja_v084.JavaServerAddinGenesis;
import net.prominic.iMessageSMS.EventTwilioHandler;
import net.prominic.iMessageSMS.TwilioHelper;

public class iMessageSMS extends JavaServerAddinGenesis {
    private Database m_database = null; // iMessageSMS.nsf
    private TwilioHelper m_twilioHelper = null;
    private View m_twilio = null;
    private int m_interval = 3; // seconds

    @Override
    protected String getJavaAddinVersion() {
        return "1.0.7";
    }

    @Override
    protected String getJavaAddinDate() {
        return "2024-05-26 18:00";
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

            // 2. Initialize Twilio helper
            if (!initTwilioHelper()) return false;

            // 3. Get view and disable auto-update
            m_twilio = m_database.getView("($requests.unprocessedTwilio)");
            if (m_twilio == null) {
                logMessage("(!) Unable to open view ($requests.unprocessedTwilio)");
                return false;
            }
            m_twilio.setAutoUpdate(false);

            // 4. Create and add Twilio event handler
            EventTwilioHandler event = new EventTwilioHandler("TwilioHandler", m_interval, false, this.m_logger);
            event.twilioHelper = m_twilioHelper;
            event.twilio = m_twilio;
            eventsAdd(event);

            return true;
        } catch (NotesException e) {
            logException(e);
            return false;
        }
    }

    @Override
    protected boolean resolveMessageQueueState(String cmd) {
        boolean flag = super.resolveMessageQueueState(cmd);
        if (flag) return true;

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

    private boolean initTwilioHelper() {
        try {
            View view = m_database.getView("($config)");
            Document doc = view.getFirstDocument();
            if (doc == null) {
                logMessage("(!) Config is missing in iMessageSMS.nsf");
                return false;
            }

            String accountSid = doc.getItemValueString("Account_SID");
            String authToken = doc.getItemValueString("Auth_token");
            String phone = doc.getItemValueString("Phone");

            doc.recycle();
            view.recycle();

            if (accountSid.isEmpty() || authToken.isEmpty()) {
                logMessage("(!) Config missing SID/token");
                return false;
            }

            m_twilioHelper = new TwilioHelper(accountSid, authToken, phone);
            return true;
        } catch (NotesException e) {
            logException(e);
            return false;
        }
    }

    private void config() {
        if (initTwilioHelper()) {
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
        if (m_twilioHelper != null) {
            logMessage("Twilio phone: " + m_twilioHelper.getFromPhone());
        } else {
            logMessage("Twilio Helper not initialized.");
        }
    }

    @Override
    protected void termBeforeAB() {
        try {
            if (m_twilio != null) {
                m_twilio.recycle();
                m_twilio = null;
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
