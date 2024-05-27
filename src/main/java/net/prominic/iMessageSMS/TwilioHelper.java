package net.prominic.iMessageSMS;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TwilioHelper {
    private static final Logger LOGGER = Logger.getLogger(TwilioHelper.class.getName());
    private static final String BASE_API = "https://api.twilio.com/2010-04-01";

    private final String accountSid;
    private final String authToken;
    private final String fromPhone;

    public TwilioHelper(String accountSid, String authToken, String fromPhone) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromPhone = fromPhone;
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    public int send(String type, String phoneTo, String body) {
        LOGGER.info("twilio.send: started");

        String action = "call".equalsIgnoreCase(type) ? "Calls.json" : "Messages.json";
        String endpoint = BASE_API + "/Accounts/" + accountSid + "/" + action;

        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String data = "To=" + encode(phoneTo) + "&From=" + encode(getFromPhone());
            if ("call".equalsIgnoreCase(type)) {
            	data += "&Url=" + encode("http://twimlets.com/message?Message=" + encode(body));
            }
            else {
                data += "&Body=" + encode(body);
            }
            	
            byte[] out = data.getBytes(StandardCharsets.UTF_8);
            String userCredentials = accountSid + ":" + authToken;
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes(StandardCharsets.UTF_8));

            conn.setFixedLengthStreamingMode(out.length);
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode > 201) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    LOGGER.warning("Response: " + response.toString());
                }
            }

            return responseCode;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while sending message via Twilio", e);
            return -1;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

	public String getFromPhone() {
		return fromPhone;
	}
}
