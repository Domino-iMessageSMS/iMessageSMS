package net.prominic.iMessageSMS;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public abstract class MessagingServiceHelper {
    private static final Logger LOGGER = Logger.getLogger(MessagingServiceHelper.class.getName());

    protected final Map<String, String> phones;

    public MessagingServiceHelper(Map<String, String> phones) {
        this.phones = phones;
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

    public String getPhone(String type, String regionCode) {
    	String key = type + "-" + regionCode;
    	String res = phones.containsKey(key) ? phones.get(key) : phones.get("default");
        return res;
    }
    
    protected String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }
    
    protected String getCountryFromPhoneNumber(String phoneNumber) {
        try {
        	PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, null);
            String regionCode = phoneUtil.getRegionCodeForNumber(number);
            if (regionCode==null) {
            	regionCode = "?";
            }
            return regionCode;
        } catch (NumberParseException e) {
            LOGGER.log(Level.SEVERE, "Error while trying to get region", e);
            return "?";
        }
    }
}
