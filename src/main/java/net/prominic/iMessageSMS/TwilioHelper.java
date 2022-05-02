package net.prominic.iMessageSMS;

import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;

public class TwilioHelper {
	private final String BASE_API = "https://api.twilio.com/2010-04-01";

	private String account_sid = null;
	private String auth_token = null;
	private String twilio_phone = null;

	public TwilioHelper(String account_sid, String auth_token, String twilio_phone) {
		this.account_sid = account_sid;
		this.auth_token = auth_token;
		this.twilio_phone = twilio_phone;
	}

	private String encode(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, "UTF-8");
	}

	public int send(String phoneTo, String body) {
		System.out.println("twilio.send: started");

		int res = 0;
		try {
			URL u = new URL(BASE_API + "/Accounts/"+ account_sid + "/Messages.json");
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			String data = "To=" + encode(phoneTo) + "&From=" + encode(twilio_phone) + "&Body=" + encode(body);
			byte[] out = data.getBytes("UTF-8");
			int length = out.length;
			String userCredentials = this.account_sid + ":" + this.auth_token;
			String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

			conn.setFixedLengthStreamingMode(length);
			conn.addRequestProperty("Authorization", basicAuth);
			conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

			conn.getOutputStream().write(out);

			res = conn.getResponseCode();

			conn.disconnect();

			if (res > 201) {
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuffer response = new StringBuffer();
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

				System.out.println("> response: " + response.toString());
			}

		} catch(Exception e) {
			e.printStackTrace();
		}

		System.out.println("> response code: " + String.valueOf(res));

		System.out.println("twilio.send: completed");
		return res;
	}
}