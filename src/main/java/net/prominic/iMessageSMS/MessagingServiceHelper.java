package net.prominic.iMessageSMS;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class MessagingServiceHelper {
    private static final Logger LOGGER = Logger.getLogger(MessagingServiceHelper.class.getName());

    protected final String fromPhone;

    public MessagingServiceHelper(String fromPhone) {
        this.fromPhone = fromPhone;
    }

    /*
     * used for WhatsApp
     */
    public int send(String mfa, String to, String... args) {
        LOGGER.info(String.format("%s: %s to %s", getServiceName(), mfa, to));

        try {
        	String endpoint = getEndpoint(mfa);
            String payload = createDataPayload(mfa, to, args);
            String auth = this.getAuth(mfa);
            
            return sendHTTPRequest(endpoint, payload, auth);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while creating data payload", e);
            return -1;
        }
    }

    protected int sendHTTPRequest(String endpoint, String payload, String auth) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            byte[] out = payload.getBytes(StandardCharsets.UTF_8);

            conn.setFixedLengthStreamingMode(out.length);
            conn.setRequestProperty("Authorization", auth);
            conn.setRequestProperty("Content-Type", getContentType());

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
            LOGGER.log(Level.SEVERE, "Error while sending message via " + getServiceName(), e);
            return -1;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public abstract String getServiceName();

    protected abstract String getContentType();
    
    protected abstract String getEndpoint(String mfa);

    protected abstract String getAuth(String mfa);

    protected abstract String createDataPayload(String mfa, String to, String... args) throws UnsupportedEncodingException;

    public String getFromPhone() {
        return fromPhone;
    }
    
    protected String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }
}
