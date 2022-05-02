package net.prominic.iMessageSMS;

import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;

public class SendBlueHelper {
	private final String BASE_API = "https://api.sendblue.co/api/send-message";

	private String api_key = null;
	private String api_secret = null;

	public SendBlueHelper(String api_key, String api_secret) {
		this.api_key = api_key;
		this.api_secret = api_secret;
	}

	private String encode(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, "UTF-8");
	}

	public int send(String number, String content) {
		System.out.println("sendblue.send: started");

		int res = 0;
		try {
			URL u = new URL(BASE_API);
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			String data = "number=" + encode(number) + "&content=" + encode("[sendblue javaaddin] " + content);
			byte[] out = data.getBytes("UTF-8");
			int length = out.length;

			conn.setFixedLengthStreamingMode(length);
			conn.setRequestProperty("SB-API-KEY-ID", this.api_key);
			conn.setRequestProperty("SB-API-SECRET-KEY", this.api_secret);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

			conn.getOutputStream().write(data.getBytes(), 0, data.length());

			res = conn.getResponseCode();

			conn.disconnect();
		} catch(Exception e) {
			e.printStackTrace();
		}

		System.out.println("> response code: " + String.valueOf(res));

		System.out.println("sendblue.send: completed");
		return res;
	}
}