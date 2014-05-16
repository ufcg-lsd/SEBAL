package org.fogbowcloud.sebal;

import java.util.Arrays;

import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.Satellite;
import org.fogbowcloud.sebal.parsers.EarthSunDistance;

public class SEBAL {

	private EarthSunDistance earthSunDistance;
	
	public SEBAL() throws Exception {
		earthSunDistance = new EarthSunDistance();
	}
	
	double deltaE(double Rn, double H, double G) {
		return Rn - H - G;
	}
	
	double Rn(double alpha, double RSDown, double RLDown, 
			double RLUp, double epsilonZero) {
		return (1. - alpha) * RSDown + RLDown - RLUp - (1 - epsilonZero) * RLDown;
	}
	
	double LLambda(double LLambdaMin, double LLambdaMax, double DN) {
		return LLambdaMin + ((LLambdaMax - LLambdaMin) / 255.) * DN;
	}
	
	double rho(double LLambda, double d, double ESUN, double cosTheta) {
		return (Math.PI * LLambda * Math.pow(d, 2)) / (ESUN * cosTheta);
	}

	double alphaToa(double rho1, double rho2, double rho3, double rho4, 
			double rho5, double rho7) {
		return 0.298221 * rho1 + 0.270098 * rho2 + 0.230997 * rho3 + 0.155051 * rho4 
				+ 0.033085 * rho5 + 0.012548 * rho7;
	}
	
	double tauSW(double z) {
		return 0.75 + 2 * 0.00001 * z;
	}
	
	double alpha(double alphaToa, double tauSW) {
		double alphaP = 0.03;
		return (alphaToa - alphaP) / Math.pow(tauSW, 2);
	}
	
	double RSDown(double cosTheta, double d, double tauSW) {
		double GSC = 1367;
		return (GSC * cosTheta * tauSW) / Math.pow(d, 2);
	}
	
	double NDVI(double rho3, double rho4) {
		return (rho4 - rho3) / (rho4 + rho3);
	}
	
	double SAVI(double rho3, double rho4) {
		double L = 0.1;
		return (1 + L) * (rho4 - rho3) / (L + rho4 + rho3);
	}
	
	double EVI(double rho1, double rho3, double rho4) {
		double G = 2.5;
		double C1 = 6;
		double C2 = 7.5;
		double L = 1;
		return G * ((rho4 - rho3) / (rho4 + C1 * rho3 - C2 * rho1 + L));
	}
	
	double IAF(double SAVI) {
		if (SAVI == 0.69) {
			return 6.;
		}
		if (SAVI < 0.1) {
			return 0;
		}
		return -1 * (Math.log((0.69 - SAVI) / 0.59) / 0.91); 
	}
	
	double epsilonNB(double IAF) {
		if (IAF >= 3.) {
			return 0.98;
		}
		return 0.97 + 0.0033 * IAF;
	}
	
	double epsilonZero(double IAF) {
		if (IAF >= 3.) {
			return 0.98;
		}
		return 0.95 + 0.01 * IAF;
	}
	
	double TS(double K2, double epsilonNB, double K1, double LLambda6) {
		return K2 / Math.log((epsilonNB * K1 / LLambda6) + 1);
	}
	
	static final double sigma = 5.67 * Math.pow(10, -8);
	
	double RLUp(double epsilonZero, double TS) {
		return epsilonZero * sigma * Math.pow(TS, 4);
	}
	
	double RLDown(double epsilonA, double TA) {
		return epsilonA * sigma * Math.pow(TA + 273.15, 4);
	}
	
	double epsilonA(double tauSW) {
		return 0.85 * Math.pow(-1 * Math.log(tauSW), 0.09);
	}
	
	double G(double TS, double alpha, double NDVI, double Rn) {
		if (NDVI < 0) {
			return Rn * 0.5;
		}
		return ((TS - 273.15)/alpha * (0.0038 * alpha + 0.0074 * Math.pow(alpha, 2)) * (1 - 0.98 * Math.pow(NDVI, 4))) * Rn;
	}
	
	static final double rho = 1.15;
	static final double cp = 1004;
	
	double H(double Ta1, double Ta2, double rah) {
		return rho * cp * (Ta1 - Ta2) / rah;
	}
	
	double HQuente(double Rn, double G) {
		return Rn - G;
	}
	
	static final double k = 0.41;
	
	double uAsterisk(double ux, double zx, double d, double z0m) {
		return k * ux / Math.log((zx - d)/ z0m);
	}
	
	double u200(double uAsterisk, double d, double z0m) {
		return uAsterisk * Math.log((200 - d) / z0m) / k;
	}
	
	double uAsteriskxy(double u200, double dxy, double z0mxy) {
		return k * u200 / Math.log((200 - dxy) / z0mxy);
	}
	
	double rahxy(double uAsteriskxy, double dxy) {
		double z1 = 0.1 + dxy;
		double z2 = 2. + dxy;
		return Math.log(z1 / z2) / uAsteriskxy * k;
	}
	
	double b(double Tquente, double Tfrio, double rahquente, double Rnquente, double Gquente) {
		return ((rahquente * (Rnquente - Gquente)) / rho * cp) / (Tquente - Tfrio);
	}
	
	double a(double b, double Tfrio) {
		return -1 * b * Tfrio;
	}
	
	static final double g = -9.81;
	
	double Lxy(double uAsteriskxy, double TSxy, double Hxy) {
		return -1 * (rho * cp * Math.pow(uAsteriskxy, 3) * TSxy / k * g * Hxy);
	}
	
	double uAsteriskCorrxy(double u200, double dxy, double z0mxy, double psimxy) {
		return k * u200 / (Math.log((200 - dxy) / z0mxy) - psimxy);
	}
	
	double rahCorrxy(double dxy, double psih1xy, double psih2xy, double uAsteriskxy) {
		double z1 = 0.1 + dxy;
		double z2 = 2. + dxy;
		return (Math.log(z1 / z2) - (psih1xy - psih2xy)) / uAsteriskxy * k;
	}
	
	double ET24h(double lambda24h, double Rn24h) {
		return lambda24h * Rn24h;
	}
	
	double lambdaInst(double lambda, double ET, double Rn, double G) {
		return (lambda * ET) / (Rn - G);
	}
	
	double Rn24h(double alpha, double RSDown, double tauW24h) {
		return (1 - alpha) * RSDown - 100 * tauW24h;
	}
	
	double z0mxy(double SAVI) {
		//TODO Parametrizar os variaveis obtidas de forma empirica
		double z0mx = -5.809;
		double z0my = 5.62;
		return SAVI = z0mx + SAVI * z0my;
	}
	
	double z0m(double h) {
		//TODO Parametrizar os variaveis obtidas de forma empirica
		return h * 0.12;
	}
	
	double hc(double z0m) {
		return z0m / 0.136;
	}
	
	double d0(double hc) {
		return 2 * hc / 3;
	}
	
	double dTQuente(double rahquente, double Rnquente, double Gquente) {
		return (rahquente * (Rnquente - Gquente)) / (rho * cp);
	}
	
	double psiH1(double L) {
		double y = Math.pow((1 - (16 * (0.1 / L))), 0.25);
		return 2 * Math.log((1 + Math.pow(y, 2)) / 2);
	}
	
	double psiH2(double L) {
		double y = Math.pow((1 - (16 * (2 / L))), 0.25);
		return 2 * Math.log((1 + Math.pow(y, 2)) / 2);
	}
	
	double psim(double L) {
		double y = Math.pow((1 - (16 * (200 / L))), 0.25);
		return 2 * Math.log((1 + y) / 2) + Math.log((1 + Math.pow(y, 2)) / 2) - 2 * Math.atan(y) + 0.5;
	}
	
	public void run(Satellite satellite, Image image) {
		
		double maxNDVI = 0;
		for (ImagePixel imagePixel : image.pixels()) {
			ImagePixelOutput output = processPixel(satellite, imagePixel);
			maxNDVI = Math.max(output.getNDVI(), maxNDVI);
			imagePixel.setOutput(output);
		}
		
		ImagePixelOutput pixelQuenteOutput = image.pixelQuente().output();
		
		// TODO Escolhendo a weather station mais proxima ao pixelQuente
		// TODO A altura da vegetacao deve ser parametrizada
		double h = 0.2;
		double z0m = z0m(h);
		
		double uAsterisk = uAsterisk(image.pixelQuente().ux(), 
				image.pixelQuente().zx(), image.pixelQuente().d(), z0m);
		double u200 = u200(uAsterisk, image.pixelQuente().d(), z0m);
		
		double uAsteriskxy = uAsteriskxy(u200, image.pixelQuente().d(), 
				z0mxy(image.pixelQuente().output().SAVI()));
		double rahxy = rahxy(uAsteriskxy, image.pixelQuente().d());
		
		double rahxyCorr = Double.MAX_VALUE;
//		double H = HQuente(image.pixelQuente().output().Rn(), image.pixelQuente().output().G());
		double dTQuente = dTQuente(rahxy, image.pixelQuente().output().Rn(), image.pixelQuente().output().G());
		double b = b(image.pixelQuente().Ta(), image.pixelFrio().Ta(), rahxy, 
				image.pixelQuente().output().Rn(), image.pixelQuente().output().G());
		double a =  a(b, image.pixelFrio().Ta());
		
		//TODO configuracao
		while(Math.abs(rahxyCorr - rahxy) > 0.01) {
			rahxy = rahxyCorr;
			double L = Lxy(uAsteriskxy, pixelQuenteOutput.getTs(), H);
			double uAsteriskCorrxy = uAsteriskCorrxy(u200, d0, z0m, psim(L));
			rahxyCorr = rahCorrxy(d0, psiH1(L), psiH2(L), uAsteriskCorrxy);
		}
		
		double H = H(image.pixelQuente().Ta(), image.pixelFrio().Ta(), rahxyCorr);
		
	}

	private ImagePixelOutput processPixel(Satellite satellite, ImagePixel imagePixel) {
		double[] LLambda = imagePixel.L();
		
		double[] rho = new double[7];
		for (int i = 0; i < rho.length; i++) {
			if (i == 5) {
				continue;
			}
			double rhoI = rho(LLambda[i], earthSunDistance.get(imagePixel.image().getDay()), 
					satellite.ESUN(i + 1), imagePixel.cosTheta());
			rho[i] = rhoI;
		}
		System.out.println("rho " + Arrays.toString(rho));
		
		double alphaToa = alphaToa(rho[0], rho[1], rho[2], rho[3], rho[4], rho[6]);
		System.out.println("alphaToa " + alphaToa);
		
		double tauSW = tauSW(imagePixel.z());
		System.out.println("tauSW " + tauSW);
		
		double alpha = alpha(alphaToa, tauSW);
		System.out.println("alpha " + alpha);
		
		double RSDown = RSDown(imagePixel.cosTheta(), 
				earthSunDistance.get(imagePixel.image().getDay()), tauSW);

		ImagePixelOutput output = new ImagePixelOutput();
		
		double NDVI = NDVI(rho[2], rho[3]);
		output.setNDVI(NDVI);
		System.out.println("NDVI " + NDVI);
		
		double SAVI = SAVI(rho[2], rho[3]);
		System.out.println("SAVI " + SAVI);
		output.setSAVI(SAVI);
		
		double EVI = EVI(rho[0], rho[2], rho[3]);
		System.out.println("EVI " + EVI);
		
		double IAF = IAF(SAVI);
		System.out.println("IAF " + IAF);
		
		double epsilonNB = epsilonNB(IAF);
		System.out.println("epsilonNB " + epsilonNB);
		
		double epsilonZero = epsilonZero(IAF);
		System.out.println("epsilonZero " + epsilonZero);
		
		double TS = TS(satellite.K2(), epsilonNB, satellite.K1(), LLambda[5]);
		output.setTs(TS);
		System.out.println("TS " + TS);
		
		double RLUp = RLUp(epsilonZero, TS);
		System.out.println("RLUp " + RLUp);
		
		double epsilonA = epsilonA(tauSW);
		System.out.println("epsilonA " + epsilonA);
		
		double RLDown = RLDown(epsilonA, imagePixel.Ta());
		System.out.println("RLDown " + RLDown);
		
		double Rn = Rn(alpha, RSDown, RLDown, RLUp, epsilonZero);
		System.out.println("Rn " + Rn);
		output.setRn(Rn);
		
		double G = G(TS, alpha, NDVI, Rn);
		System.out.println("G " + G);
		output.setG(G);
		
		return output;
	}
}
