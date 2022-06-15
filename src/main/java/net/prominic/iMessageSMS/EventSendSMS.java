package net.prominic.iMessageSMS;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import net.prominic.gja_v082.Event;
import net.prominic.gja_v082.GLogger;

public class EventSendSMS extends Event {
	public TwilioHelper 			twilioHelper		= null;
	public SendBlueHelper 			sendblueHelper		= null;
	public View 					twilio				= null;
	public View 					sendblue			= null;

	public EventSendSMS(String name, long seconds, boolean fireOnStart, GLogger logger) {
		super(name, seconds, fireOnStart, logger);
	}

	@Override
	public void run() {
		try {
			processSendBlue();
			processTwilio();
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

	private void processTwilio() throws NotesException {
		if (twilioHelper == null) return;

		twilio.refresh();

		Document doc = twilio.getFirstDocument();
		while (doc != null) {
			Document docNext = twilio.getNextDocument(doc);

			int res = 0;
			String to = doc.getItemValueString("To");
			String body = doc.getItemValueString("Body");
			if (!(to.isEmpty() || body.isEmpty())) {
				res = twilioHelper.send(to, body);
			}

			// mark as processed
			doc.replaceItemValue("ProcessedTwilio", "1");
			doc.replaceItemValue("ResponseCodeTwilio", res);
			doc.save();
			doc.recycle();

			doc = docNext;
		}
	}

	private void processSendBlue() throws NotesException {
		if (sendblueHelper == null) return;

		sendblue.refresh();

		Document doc = sendblue.getFirstDocument();
		while (doc != null) {
			Document docNext = sendblue.getNextDocument(doc);

			int res = 0;
			String to = doc.getItemValueString("To");
			String body = doc.getItemValueString("Body");
			if (!(to.isEmpty() || body.isEmpty())) {
				res = sendblueHelper.send(to, body);
			}

			// mark as processed
			doc.replaceItemValue("ProcessedSendBlue", "1");
			doc.replaceItemValue("ResponseCodeSendBlue", res);
			doc.save();
			doc.recycle();

			doc = docNext;
		}
	}
	
}
