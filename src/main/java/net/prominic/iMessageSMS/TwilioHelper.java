package net.prominic.iMessageSMS;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TwilioHelper extends MessagingServiceHelper {
    private final String accountSid;
    private final String authToken;

    public TwilioHelper(String accountSid, String authToken, String fromPhone) {
        super(fromPhone);
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

    @Override
    protected String createDataPayload(String mfa, String to, String body) throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder();

        if ("call".equalsIgnoreCase(mfa)) {
            data.append("To=").append(encode(to))
                .append("&From=").append(encode(fromPhone))
                .append("&Url=").append(encode("http://twimlets.com/message?Message=" + encode(body)));
        } else if ("whatsapp".equalsIgnoreCase(mfa)) {
            data.append("To=").append(encode("whatsapp:" + to))
                .append("&From=").append(encode("whatsapp:" + fromPhone))
                .append("&Body=").append(encode(body));
        } else {
            data.append("To=").append(encode(to))
                .append("&From=").append(encode(fromPhone))
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
        String endpoint = "https://api.twilio.com/2010-04-01/" + getAccountId() + "/" + action;
        return endpoint;
	}

}
