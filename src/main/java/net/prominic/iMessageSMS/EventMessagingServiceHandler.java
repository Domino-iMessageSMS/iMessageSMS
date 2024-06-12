package net.prominic.iMessageSMS;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import net.prominic.gja_v084.Event;
import net.prominic.gja_v084.GLogger;

public class EventMessagingServiceHandler extends Event {
	public MessagingServiceHelper  	messsangingHelper	= null;
	public View 					request				= null;

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
			String type = doc.getItemValueString("Type");
			String to = doc.getItemValueString("To");
			String body = doc.getItemValueString("Body");
			if (!(to.isEmpty() || body.isEmpty())) {
				res = messsangingHelper.send(type, to, body);
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
