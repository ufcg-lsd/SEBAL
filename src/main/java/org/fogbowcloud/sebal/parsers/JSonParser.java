package org.fogbowcloud.sebal.parsers;

import static org.fogbowcloud.sebal.parsers.JSonParserConstants.ALPHA_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.COS_THETA_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.D_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.EPSILON_NB_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.EPSILON_ZERO_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.GEOLOC_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.G_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.HC_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.H_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.IMAGE_PIXEL_OUTPUT_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.I_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.J_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.LAMBDAE_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.LATITUDE_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.LONGITUDE_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.L_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.NDVI_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.RHO_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.RN_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.SAVI_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.TA_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.TS_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.UX_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.Z0MXY_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.ZX_TAG;
import static org.fogbowcloud.sebal.parsers.JSonParserConstants.Z_TAG;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSonParser {

	public static JSONObject parseDefaultImagePixelToJson(ImagePixel pixelQuente) {
		JSONObject obj = new JSONObject();
		JSONArray L = new JSONArray();
		if( pixelQuente.L() != null) {
			for (int i = 0; i < pixelQuente.L().length; i ++) {
				Double value = new Double(pixelQuente.L()[i]);
				L.put(value);
			}
			obj.put(L_TAG, L);
		}
		obj.put(COS_THETA_TAG, pixelQuente.cosTheta());
		obj.put(Z_TAG, pixelQuente.z());
		obj.put(TA_TAG, pixelQuente.Ta());
		obj.put(UX_TAG, pixelQuente.ux());
		obj.put(ZX_TAG, pixelQuente.zx());
		obj.put(D_TAG, pixelQuente.d());
		obj.put(HC_TAG, pixelQuente.hc());
		obj.put(IMAGE_PIXEL_OUTPUT_TAG, parseImagePixelOutputToJson(pixelQuente.output()));
		if (pixelQuente.geoLoc() != null) {
			obj.put(GEOLOC_TAG, parseGeoLocToJson(pixelQuente.geoLoc()));
		}
		return obj;
	}

	public static ImagePixel parseJsonToDefaultImagePixel(JSONObject imagePixelJson) {
		DefaultImagePixel imagePixel = new DefaultImagePixel();

//		double [] L = parseJsonToArray(imagePixelJson.getJSONArray(L_TAG));
//		imagePixel.L(L);
		imagePixel.cosTheta(imagePixelJson.getDouble(COS_THETA_TAG));
		imagePixel.z(imagePixelJson.getDouble(Z_TAG));
		imagePixel.Ta(imagePixelJson.getDouble(TA_TAG));
		imagePixel.ux(imagePixelJson.getDouble(UX_TAG));
		imagePixel.zx(imagePixelJson.getDouble(ZX_TAG));
		imagePixel.d(imagePixelJson.getDouble(D_TAG));
		imagePixel.hc(imagePixelJson.getDouble(HC_TAG));
		imagePixel.setOutput(parseJsonToImagePixelOutput(imagePixelJson.
				getJSONObject(IMAGE_PIXEL_OUTPUT_TAG)));
//		imagePixel.geoLoc(parseJsonToGeoLoc(imagePixelJson.getJSONObject(GEOLOC_TAG)));
		return imagePixel;
	}

	public static JSONObject parseImagePixelOutputToJson(ImagePixelOutput output) {
		JSONObject obj = new JSONObject();
		JSONArray rho = new JSONArray();
		if (output.getRho() != null) {
			for (Double rhoValue : output.getRho()) {
				rho.put(rhoValue);
			}
			obj.put(RHO_TAG, rho);
		}
		obj.put(NDVI_TAG, output.getNDVI());
		obj.put(TS_TAG, output.getTs());
		obj.put(SAVI_TAG, output.SAVI());
		obj.put(RN_TAG, output.Rn());
		obj.put(G_TAG, output.G());
		obj.put(H_TAG, output.getH());
		obj.put(LAMBDAE_TAG, output.getLambdaE());
		obj.put(ALPHA_TAG, output.getAlpha());
		obj.put(EPSILON_NB_TAG, output.getEpsilonNB());
		obj.put(EPSILON_ZERO_TAG, output.getEpsilonZero());
		obj.put(Z0MXY_TAG, output.getZ0mxy());
		return obj;
	}

	public static ImagePixelOutput parseJsonToImagePixelOutput(JSONObject outputJson) {
		ImagePixelOutput output = new ImagePixelOutput();
//		double[] rho = parseJsonToArray(outputJson.getJSONArray(RHO_TAG));
//		output.setRho(rho);
		output.setNDVI(outputJson.getDouble(NDVI_TAG));
		output.setTs(outputJson.getDouble(TS_TAG));
		output.setSAVI(outputJson.getDouble(SAVI_TAG));
		output.setRn(outputJson.getDouble(RN_TAG));
		output.setG(outputJson.getDouble(G_TAG));
		output.setH(outputJson.getDouble(H_TAG));
		output.setLambdaE(outputJson.getDouble(LAMBDAE_TAG));
		output.setAlpha(outputJson.getDouble(ALPHA_TAG));
		output.setEpsilonNB(outputJson.getDouble(EPSILON_NB_TAG));
		output.setEpsilonZero(outputJson.getDouble(EPSILON_ZERO_TAG));
		output.setZ0mxy(outputJson.getDouble(Z0MXY_TAG));
		return output;
	}

	private static double[] parseJsonToArray(JSONArray jsonArray) {
		List<Double> rhoList = new ArrayList<Double>();
		for (int i = 0; i < jsonArray.length(); i++) {
			rhoList.add(jsonArray.getDouble(i));
		}
		double[] rho = new double[rhoList.size()];
		for (int i = 0; i < rhoList.size(); i ++) {
			rho[i] = rhoList.get(i);
		}
		return rho;
	}

	public static JSONObject parseGeoLocToJson(GeoLoc geoLoc) {
		JSONObject obj = new JSONObject();
		obj.put(I_TAG, geoLoc.getI());
		obj.put(J_TAG, geoLoc.getJ());
		obj.put(LATITUDE_TAG, geoLoc.getLat());
		obj.put(LONGITUDE_TAG, geoLoc.getLon());
		return obj;
	}

	public static GeoLoc parseJsonToGeoLoc(JSONObject geoLocJson) throws JSONException {
		GeoLoc geoLoc = new GeoLoc();
		geoLoc.setI(geoLocJson.getInt(I_TAG));
		geoLoc.setJ(geoLocJson.getInt(J_TAG));
		geoLoc.setLat(geoLocJson.getDouble(LATITUDE_TAG));
		geoLoc.setLon(geoLocJson.getDouble(LONGITUDE_TAG));
		return geoLoc;
	}
}
