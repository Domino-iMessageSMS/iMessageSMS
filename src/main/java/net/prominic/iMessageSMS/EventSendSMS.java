package net.prominic.iMessageSMS;

import java.util.HashMap;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import net.prominic.gja_v20220427.Event;
import net.prominic.gja_v20220427.GLogger;

public class EventSendSMS extends Event {
	TwilioHelper 			m_twilioHelper			= null;
	SendBlueHelper 			m_sendblueHelper		= null;
	View 					m_twilio				= null;
	View 					m_sendblue				= null;

	public EventSendSMS(String name, long seconds, boolean fireOnStart, HashMap<String, Object> params, GLogger logger) {
		super(name, seconds, fireOnStart, params, logger);
		
		m_twilioHelper = (TwilioHelper) params.get("twilioHelper");
		m_sendblueHelper = (SendBlueHelper) params.get("sendblueHelper");
		m_twilio = (View) params.get("twilio");
		m_sendblue = (View) params.get("sendblue");
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
			doc.recycle();

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
			doc.recycle();

			doc = docNext;
		}
	}
	
}
