package org.fogbowcloud.sebal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.parsers.JSonParser;
import org.json.JSONObject;

public class Server extends Thread {

	Socket csocket;
	Server(Socket csocket) {
		this.csocket = csocket;
	}

	public static void main(String args[]) 
			throws Exception {
		ServerSocket ssock = new ServerSocket(1234);
		System.out.println("Listening");
		while (true) {
			Socket sock = ssock.accept();
			System.out.println("Connected");
			new Thread(new Server(sock)).start();
		}
	}
	public void run() {
		try {
		    BufferedReader streamReader = new BufferedReader(
		    		new InputStreamReader(csocket.getInputStream(), "UTF-8")); 
		    StringBuilder responseStrBuilder = new StringBuilder();

		    String inputStr;
		    while ((inputStr = streamReader.readLine()) != null) {
		        responseStrBuilder.append(inputStr);
		    }
		    System.out.println(responseStrBuilder.toString());
		    JSONObject jObject = new JSONObject(responseStrBuilder.toString());
			csocket.close();
			ImagePixel imagePixel = JSonParser.parseJsonToDefaultImagePixel(jObject);
			System.out.println(imagePixel.d());
			
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

}
