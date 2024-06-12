package net.prominic.iMessageSMS;

import java.io.UnsupportedEncodingException;

public class SinchHelper extends MessagingServiceHelper {
    private static final String BASE_API = "https://api.sinch.com/v1";
    private final String appKey;
    private final String appSecret;

    public SinchHelper(String appKey, String appSecret, String fromPhone) {
        super(fromPhone);
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    @Override
    protected String getServiceName() {
        return "sinch";
    }

    @Override
    protected String getBaseApiUrl() {
        return BASE_API;
    }

    @Override
    protected String getAuth() {
        return "Bearer " + appSecret;
    }

    @Override
    protected String createDataPayload(String mfa, String to, String body) throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder();
        data.append("{");

        if ("call".equalsIgnoreCase(mfa)) {
            data.append("\"method\":\"tts\",")
                .append("\"message\":\"").append(body).append("\",")
                .append("\"to\":\"").append(to).append("\",")
                .append("\"from\":\"").append(fromPhone).append("\"");
        } else if ("whatsapp".equalsIgnoreCase(mfa)) {
            data.append("\"from\":\"whatsapp:").append(fromPhone).append("\",")
                .append("\"to\":\"whatsapp:").append(to).append("\",")
                .append("\"body\":\"").append(body).append("\"");
        } else {
            data.append("\"from\":\"").append(fromPhone).append("\",")
                .append("\"to\":\"").append(to).append("\",")
                .append("\"body\":\"").append(body).append("\"");
        }

        data.append("}");
        return data.toString();
    }

	@Override
	protected String getContentType() {
		return "application/json; charset=UTF-8";
	}

	@Override
	protected String getEndpoint(String mfa) {
String endpoint = getBaseApiUrl();

		return endpoint;
	}

	@Override
	protected String getAccountId() {
		// TODO Auto-generated method stub
		return null;
	}

}
