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

    private String getAccountId() {
        return "Accounts/" + accountSid;
    }

    @Override
    protected String getAuth(String mfa) {
        String userCredentials = accountSid + ":" + authToken;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes(StandardCharsets.UTF_8));
        
        return basicAuth;
    }

    /*
     * For WhatsApp message will only contain code, while for SMS and Call it will be a full message (including code and expire time)
     */
    protected String createDataPayload(String mfa, String to, String from, String... args) throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder();

        String message = args.length > 0 ? args[0] : "";
        String serviceSid = args.length > 1 ? args[1] : "";
        String templateSid = args.length > 2 ? args[2] : "";
        
        if ("call".equalsIgnoreCase(mfa)) {
            data.append("To=").append(encode(to))
                .append("&From=").append(encode(from))
                .append("&Url=").append(encode("http://twimlets.com/message?Message=" + encode(message)));
        } else if ("whatsapp".equalsIgnoreCase(mfa)) {
            data.append("To=").append(encode("whatsapp:" + to))
                .append("&From=").append(encode("whatsapp:" + from))
                .append("&MessagingServiceSid=").append(serviceSid)
                .append("&ContentSid=").append(templateSid)
                .append("&ContentVariables=").append(encode(String.format("{\"1\": \"%s\"}", message)));
        } else {
            data.append("To=").append(encode(to))
                .append("&From=").append(encode(from))
                .append("&Body=").append(encode(message));
        }

        return data.toString();    	
    }

	protected String getContentType() {
		return "application/x-www-form-urlencoded; charset=UTF-8";
	}

	@Override
	protected String getEndpoint(String mfa) {
        String action = "call".equalsIgnoreCase(mfa) ? "Calls.json" : "Messages.json";
        String endpoint = "https://api.twilio.com/2010-04-01/" + getAccountId() + "/" + action;
        return endpoint;
	}

}
