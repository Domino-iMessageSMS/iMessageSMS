package net.prominic.iMessageSMS;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import net.prominic.gja_v084.Event;
import net.prominic.gja_v084.GLogger;

public class EventMessagingServiceHandler extends Event {
	public MessagingServiceHelper  	messsangingHelper	= null;
	public View 					request				= null;
	public String					forceMessageType	= null;

	public EventMessagingServiceHandler(String name, long seconds, boolean fireOnStart, GLogger logger) {
		super(name, seconds, fireOnStart, logger);
	}

	@Override
	public void run() {
		try {
			processRequest();
		} catch (NotesException e) {
			e.printStackTrace();
		}
	}

	private void processRequest() throws NotesException {
		if (messsangingHelper == null) return;

		request.refresh();

		Document doc = request.getFirstDocument();
		while (doc != null) {
			Document docNext = request.getNextDocument(doc);

			int res = 0;
			String mfa = doc.getItemValueString("Type");
			if (mfa.isEmpty()) {
				mfa = "sms";
			}
			
			String to = doc.getItemValueString("To");

			if ("sms".equals(forceMessageType) || "call".equalsIgnoreCase(forceMessageType)) {
				mfa = forceMessageType;
			}
			
			if ("whatsapp".equals(mfa)) {
				String sid = doc.getItemValueString("sid");
				String parameters = doc.getItemValueString("parameters");

				if (to.isEmpty() || sid.isEmpty()) {
					res = -1;
				}
				else {
					res = messsangingHelper.send(mfa, to, sid, parameters);
				}
			}
			else {	// sms or call
				String body = doc.getItemValueString("Body");

				if (to.isEmpty() || body.isEmpty()) {
					res = -1;
				}
				else {
					res = messsangingHelper.send(mfa, to, body);	
				}
			}

			// mark as processed
			doc.replaceItemValue("Processed", "1");
			doc.replaceItemValue("ResponseCode", res);
			doc.save();
			doc.recycle();

			doc = docNext;
		}
	}
}
