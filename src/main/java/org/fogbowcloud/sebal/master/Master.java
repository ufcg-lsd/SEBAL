package org.fogbowcloud.sebal.master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.parsers.JSonParser;
import org.fogbowcloud.sebal.slave.TaskType;
import org.json.JSONObject;


public class Master extends Thread {


	private Socket csocket;
	private List<ImagePixel> pixelsQuente;
	private List<ImagePixel> pixelsFrio;

	Master(Socket csocket) {
		this.csocket = csocket;
	}

	public static void main(String args[]) 
			throws Exception {
		ServerSocket ssock = new ServerSocket(1234);
		System.out.println("Listening");
		while (true) {
			Socket sock = ssock.accept();
			System.out.println("Connected");
			new Thread(new Master(sock)).start();
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
			e.printStackTrace();
		}
	}

	public void iAmReady() {
		
	}

	List<ImagePixel> verifyPixelQuentePixelFrio() {
		return null;
	}
}
