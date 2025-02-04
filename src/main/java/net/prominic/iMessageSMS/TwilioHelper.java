package net.prominic.iMessageSMS;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class TwilioHelper extends MessagingServiceHelper {
    private final String accountSid;
    private final String authToken;

    public TwilioHelper(String accountSid, String authToken, Map<String, String> phones) {
        super(phones);
        this.accountSid = accountSid;
        this.authToken = authToken;
    }

    @Override
    public String getServiceName() {
        return "twilio";
    }

    @Override
    protected String getAuth(String mfa) {
        String userCredentials = accountSid + ":" + authToken;
        return "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes(StandardCharsets.UTF_8));
    }

    /*
     * For WhatsApp message will only contain code, while for SMS and Call it will be a full message (including code and expire time)
     */
    protected String createDataPayload(String mfa, String to, String from, String... args) throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder();

        if ("call".equalsIgnoreCase(mfa)) {
            String message = args.length > 0 ? args[0] : "";
            data.append("To=").append(encode(to))
                .append("&From=").append(encode(from))
                .append("&Url=").append(encode("http://twimlets.com/message?Message=" + encode(message)));
        } else if ("whatsapp".equalsIgnoreCase(mfa)) {
            String sid = args.length > 0 ? args[0] : "";
            String parameters = args.length > 1 ? args[1] : "";

            data.append("To=").append(encode("whatsapp:" + to))
                .append("&From=").append(encode("whatsapp:" + from))
                .append("&ContentSid=").append(sid)
                .append("&ContentVariables=").append(encode(parameters));
        } else {
            String body = args.length > 0 ? args[0] : "";
            data.append("To=").append(encode(to))
                .append("&From=").append(encode(from))
                .append("&Body=").append(encode(body));
        }
        
        return data.toString();    	
    }

	protected String getContentType() {
		return "application/x-www-form-urlencoded; charset=UTF-8";
	}

	@Override
	protected String getEndpoint(String mfa) {
        String action = "call".equalsIgnoreCase(mfa) ? "Calls.json" : "Messages.json";
        return String.format("https://api.twilio.com/2010-04-01/Accounts/%s/%s", accountSid, action);
	}

}
