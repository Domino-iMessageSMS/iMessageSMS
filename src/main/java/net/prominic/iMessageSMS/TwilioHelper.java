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

	private String m_account_sid = null;
	private String m_auth_token = null;
	private String m_phone = null;

	public TwilioHelper(String account_sid, String auth_token, String twilio_phone) {
		this.setAccount_sid(account_sid);
		this.setAuth_token(auth_token);
		this.setPhone(twilio_phone);
	}

	private String encode(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, "UTF-8");
	}

	public int send(String phoneTo, String body) {
		System.out.println("twilio.send: started");

		int res = 0;
		try {
			URL u = new URL(BASE_API + "/Accounts/"+ getAccount_sid() + "/Messages.json");
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			String data = "To=" + encode(phoneTo) + "&From=" + encode(getPhone()) + "&Body=" + encode(body);
			byte[] out = data.getBytes("UTF-8");
			int length = out.length;
			String userCredentials = this.getAccount_sid() + ":" + this.getAuth_token();
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

	public String getPhone() {
		return m_phone;
	}

	public void setPhone(String phone) {
		this.m_phone = phone;
	}

	public String getAuth_token() {
		return m_auth_token;
	}

	public void setAuth_token(String auth_token) {
		this.m_auth_token = auth_token;
	}

	public String getAccount_sid() {
		return m_account_sid;
	}

	public void setAccount_sid(String account_sid) {
		this.m_account_sid = account_sid;
	}
}