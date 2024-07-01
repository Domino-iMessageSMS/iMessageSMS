package net.prominic.iMessageSMS;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SinchHelper extends MessagingServiceHelper {
    private final String ServicePlanID;
    private final String APIToken;
    private final String AppKey;
    private final String AppSecret;

    public SinchHelper(String ServicePlanID, String APIToken, String AppKey, String AppSecret, String fromPhone) {
        super(fromPhone);
        this.ServicePlanID = ServicePlanID;
        this.APIToken = APIToken;
        this.AppKey = AppKey;
        this.AppSecret = AppSecret;
    }

    @Override
    public String getServiceName() {
        return "sinch";
    }

    protected String getAuth(String mfa) {
    	if ("call".equalsIgnoreCase(mfa)) {
            String userCredentials = AppKey + ":" + AppSecret;
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes(StandardCharsets.UTF_8));
            return basicAuth;
    	}
    	
    	//sms
        return "Bearer " + APIToken;
    }

    @Override
    protected String createDataPayload(String mfa, String to, String... args) throws UnsupportedEncodingException {
    	String body = args[0];
    	
        if ("call".equalsIgnoreCase(mfa)) {
            return String.format(
                    "{\"method\":\"ttsCallout\",\"ttsCallout\":{\"cli\":\"%s\", \"domain\": \"pstn\", \"destination\":{\"type\":\"number\",\"endpoint\":\"%s\"},\"locale\":\"en-US\",\"prompts\":\"#tts[%s]\"}}",
                    this.fromPhone, to, body);
        } else if ("whatsapp".equalsIgnoreCase(mfa)) {
        } else {
        	return String.format("{\"from\":\"%s\",\"to\":[\"%s\"],\"body\":\"%s\"}", this.fromPhone, to, body);
        }

        return "";
    }

	@Override
	protected String getContentType() {
		return "application/json; charset=UTF-8";
	}

	@Override
	protected String getEndpoint(String mfa) {
		String endpoint;
		if ("call".equalsIgnoreCase(mfa)) {
			endpoint= "https://calling.api.sinch.com/calling/v1/callouts";
		}
		else {
			endpoint = "https://sms.api.sinch.com/xms/v1/"+ServicePlanID+"/batches";
		}

		return endpoint;
	}
}
