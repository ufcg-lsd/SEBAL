package org.fogbowcloud.sebal;

import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.model.image.HOutput;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.Satellite;
import org.fogbowcloud.sebal.parsers.EarthSunDistance;
import org.python.modules.math;

public class SEBAL {

    private EarthSunDistance earthSunDistance;
	
	private static final Logger LOGGER = Logger.getLogger(SEBAL.class);
   
    public SEBAL() throws Exception {
        earthSunDistance = new EarthSunDistance();
    }

    double deltaE(double Rn, double H, double G) {
        return Rn - H - G;
    }

    double Rn(double alpha, double RSDown, double RLDown, double RLUp,
            double epsilonZero) {
        return (1.0 - alpha) * RSDown + RLDown - RLUp - (1 - epsilonZero)
                * RLDown;
    }

    double LLambda(double LLambdaMin, double LLambdaMax, double DN) {
        return LLambdaMin + ((LLambdaMax - LLambdaMin) / 255.0) * DN;
    }

    double rho(double LLambda, double d, double ESUN, double cosTheta) {
        return (Math.PI * LLambda * Math.pow(d, 2)) / (ESUN * cosTheta);
    }

    double alphaToa(double rho1, double rho2, double rho3, double rho4,
            double rho5, double rho7) {
        return 0.298221 * rho1 + 0.270098 * rho2 + 0.230997 * rho3 + 0.155051
                * rho4 + 0.033085 * rho5 + 0.012548 * rho7;
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
        double SAVItemp = round(SAVI, 2);
        if (SAVItemp == 0.69) {
            return 6.;
        }
        if (SAVI < 0.1) {
            return 0;
        }
        Double IAF = -1. * (Math.log((0.69 - SAVI) / 0.59) / 0.91);
        return IAF;
    }

    public double round(double value, int places) {
        if (places < 0)
            throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.DOWN);
        return bd.doubleValue();
    }

    double epsilonNB(double IAF) {
        if (IAF >= 3.) {
            return 0.98;
        }
        Double epsilonNB = 0.97 + 0.0033 * IAF;
        return epsilonNB;
    }

    double epsilonZero(double IAF) {
        if (IAF >= 3.) {
            return 0.98;
        }
        return 0.95 + 0.01 * IAF;
    }

    double TS(double K2, double epsilonNB, double K1, double LLambda6) {

        Double ts = K2 / Math.log((epsilonNB * K1 / LLambda6) + 1);
        return ts;
    }

    static final double sigma = 5.67 * Math.pow(10, -8);

    double RLUp(double epsilonZero, double TS) {
        return epsilonZero * sigma * Math.pow(TS, 4);
    }

    double RLDown(double epsilonA, double TA) {
        return epsilonA * sigma * Math.pow(TA + 273.15, 4);
    }

    double epsilonA(double tauSW) {
        return 0.85 * (Math.pow(-1 * Math.log(tauSW), 0.09));
    }

    double G(double TS, double alpha, double NDVI, double Rn) {
        if (NDVI < 0) {
            return Rn * 0.5;
        }
        return ((TS - 273.15) / alpha
                * (0.0038 * alpha + 0.0074 * Math.pow(alpha, 2)) * (1 - 0.98 * Math
                .pow(NDVI, 4))) * Rn;
        // return Math.abs((TS - 273.15)*(0.0038 + 0.0074 *
        // alpha)*(1-0.98*Math.pow(NDVI, 4))) * Rn;
    }

    static final double rho = 1.15;
    static final double cp = 1004;

    double H(double a, double b, double Ts, double rah) {
        double H = (rho * cp) * (a + b * Ts) / rah;
        return H;
    }

    double HQuente(double Rn, double G) {
        return Rn - G;
    }

    static final double k = 0.41;

    double uAsterisk(double ux, double zx, double d, double z0m) {
        return k * ux / Math.log((zx - d) / z0m);
    }

    double u200(double uAsterisk, double d, double z0m) {
        return (uAsterisk * Math.log((200 - d) / z0m)) / k;
    }

    double uAsteriskxy(double u200, double dxy, double z0mxy) {
        return (k * u200) / Math.log((200 - dxy) / z0mxy);
    }

    double rahxy(double uAsteriskxy) {
        double z1 = 0.1;
        double z2 = 2.0;
        return Math.log(z2 / z1) / (uAsteriskxy * k);
    }

    double b(double Tquente, double Tfrio, double rahquente, double Rnquente,
            double Gquente) {
        return ((rahquente * (Rnquente - Gquente)) / rho * cp)
                / (Tquente - Tfrio);
    }

    double a(double b, double Tfrio) {
        return -1 * b * Tfrio;
    }

    static final double g = 9.81;

    double Lxy(double uAsteriskxy, double TSxy, double Hxy) {
        double L = -1 * (rho * cp * Math.pow(uAsteriskxy, 3) * TSxy)
                / (k * g * Hxy);
        return L;
    }

    double uAsteriskCorrxy(double u200, double dxy, double z0mxy, double psimxy) {
        return (k * u200) / (Math.log((200 - dxy) / z0mxy) - psimxy);
    }

    double rahCorrxy(double psih1xy, double psih2xy, double uAsteriskxy) {
        double z1 = 0.1;
        double z2 = 2.;
        return (Math.log(z2 / z1) - psih2xy + psih1xy) / (uAsteriskxy * k);
    }

    double ET24h(double le24h) {
        return (le24h * 86.4) / 2450.;
    }

    double lambdaInst(double lambda, double ET, double Rn, double G) {
        return (lambda * ET) / (Rn - G);
    }

    double Rn24h(double alpha, double tauW24h) {
        return Rsolar() * (1 - alpha) - 110 * tauW24h;
    }

    double tau24h() {
        return Rsolar() / Ra2();
    }

    double Rsolar() {
        return 259.42;
    }

    double LE24h(double frEvapo, double Rn24h) {
        return frEvapo * Rn24h;
    }

    double z0mxy(double SAVI) {
        // TODO Parametrizar os variaveis obtidas de forma empirica
        double z0mx = -5.809;
        double z0my = 5.62;
        return Math.exp(z0mx + SAVI * z0my);
    }

    double z0m(double h) {
        // TODO Parametrizar os variaveis obtidas de forma empirica
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
        double psiH1 = 0;
        if (L == 0) {
            psiH1 = 0;
        } else if (L > 0) {
            psiH1 = -5 * (0.1 / L);
        } else {
            double y = Math.pow((1 - (16 * (0.1 / L))), 0.25);
            psiH1 = 2 * Math.log((1 + Math.pow(y, 2)) / 2);
        }
        return psiH1;
    }

    double psiH2(double L) {
        double psiH2 = 0;
        if (L == 0) {
            psiH2 = 0;
        } else if (L > 0) {
            psiH2 = -5 * (2. / L);
        } else {
            double y = Math.pow((1 - (16 * (2 / L))), 0.25);
            psiH2 = 2 * Math.log((1 + Math.pow(y, 2)) / 2);
        }
        return psiH2;
    }

    double psim(double L) {
        double psim = 0;
        if (L > 0) {
            psim = -5 * (200 / L);
        } else if (L == 0) {
            psim = 0;
        } else {
            double y = Math.pow((1 - (16 * (200 / L))), 0.25);
            psim = 2 * Math.log((1 + y) / 2)
                    + Math.log((1 + Math.pow(y, 2)) / 2) - 2 * Math.atan(y)
                    + 0.5 * Math.PI;
        }
        return psim;
    }

	public Image processPixelQuentePixelFrio(Image image, Satellite satellite,
			List<BoundingBoxVertice> boundingBoxVertices, int maskWidth, int maskHeight, boolean cloudDetection) {

		LOGGER.info("Processing pixels...");
		LOGGER.debug("pixels size=" + image.pixels().size());
		
		LinkedList<Double> waterPixelsTS = new LinkedList<Double>();
		LinkedList<Double> landPixelsTS = new LinkedList<Double>();
		
	    long now = System.currentTimeMillis();
		for (ImagePixel imagePixel : image.pixels()) {
			if (pixelIsInsideBoundingBox(imagePixel, boundingBoxVertices) && imagePixel.isValid()) {
				ImagePixelOutput output = processPixel(satellite, imagePixel);
				imagePixel.setOutput(output);
				
				if (output.getClearSkyWater()) {
					waterPixelsTS.add(output.getTs());
				} 
				
				if (output.getClearSkyLand()) {
					landPixelsTS.add(output.getTs());
				}
			} else {
				LOGGER.debug("(" + imagePixel.geoLoc().getLon() + ", "
						+ imagePixel.geoLoc().getLat()
						+ ") is out of the bounding box or is a invalid pixel.");
				imagePixel.setOutput(new ImagePixelOutput());
			}
		}
		
		LOGGER.debug("Proccessing pixels execution time = " + (System.currentTimeMillis() - now));
		now = System.currentTimeMillis();
		
		if (!cloudDetection || (waterPixelsTS.isEmpty() && landPixelsTS.isEmpty())) {
			image.choosePixelsQuenteFrio();
			return image;
		}
		
		//calculating TS percentils
		double highWaterPercentil = Double.NaN;
		if (!waterPixelsTS.isEmpty()) {
			Collections.sort(waterPixelsTS);
			int k = getPercentilPos(82.5, waterPixelsTS.size());
			highWaterPercentil = waterPixelsTS.get(k);
		}
		
		double lowLandPercentil = Double.NaN;
		double highLandPercentil = Double.NaN;
		if (!landPixelsTS.isEmpty()){
			Collections.sort(landPixelsTS);
			int k = getPercentilPos(17.5, landPixelsTS.size());
			lowLandPercentil = landPixelsTS.get(k);
			k = getPercentilPos(82.5, landPixelsTS.size());
			highLandPercentil = landPixelsTS.get(k);
		}
		
		//calculating lCloudProb and wCloudProb
		LOGGER.debug("Calculating probabilities...");
		LinkedList<Double> lClearSkyCloudProbs = new LinkedList<Double>();

		for (ImagePixel imagePixel : image.pixels()) {
			if (pixelIsInsideBoundingBox(imagePixel, boundingBoxVertices)) {
				calcProbalities(satellite, imagePixel, highWaterPercentil, lowLandPercentil, highLandPercentil);		
				
				if (imagePixel.output().getClearSkyLand()) {
					lClearSkyCloudProbs.add(imagePixel.output().getLCloudProb());
				}				
			}
		}
		
		double clearSkyLandCloudProbPercentil = Double.NaN;
		if (!lClearSkyCloudProbs.isEmpty()) {
			Collections.sort(lClearSkyCloudProbs);
			int k = getPercentilPos(82.5, lClearSkyCloudProbs.size());
			clearSkyLandCloudProbPercentil = lClearSkyCloudProbs.get(k);
		}
		
		// cloud detection
		int numberOfCloudPixels = 0;
		LOGGER.debug("Detecting cloud...");
		
		for (ImagePixel imagePixel : image.pixels()) {
			if (pixelIsInsideBoundingBox(imagePixel, boundingBoxVertices)) {
				if (isCloudPixel(imagePixel, clearSkyLandCloudProbPercentil, lowLandPercentil)
						|| isSnowPixel(satellite, imagePixel)) {
					LOGGER.debug("(" + imagePixel.geoLoc().getLon() + ", "
							+ imagePixel.geoLoc().getLat() + ") is a cloud or snow pixel.");
					ImagePixelOutput output = new ImagePixelOutput();
					output.setIsCloud(true);					
					imagePixel.setOutput(output);
					numberOfCloudPixels++;
				}
			}
		}
		
		LOGGER.debug("Cloud detection execution time = " + (System.currentTimeMillis() - now));
		LOGGER.debug("Number of cloud pixels = " + numberOfCloudPixels);
		
		image.choosePixelsQuenteFrio();
		return image;
	}

	private void calcProbalities(Satellite satellite, ImagePixel imagePixel,
			double TWater, double lTLow, double lTHigh) {
		
		ImagePixelOutput output = imagePixel.output();
		double wCloudProb = Double.NaN;
		try {
			double wTemperatureProb = (TWater - output.getTs()) / 4;
			double brightnessProb = Math.min(output.getRho()[4], 0.11) / 0.11;
			wCloudProb = wTemperatureProb * brightnessProb;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        imagePixel.output().setWCloudProb(wCloudProb);
        
        double lCloudProb = Double.NaN;
		try {
			double lTemperatureProb = (lTHigh + 4 - output.getTs()) / (lTHigh + 4 - (lTLow - 4));

			double modifiedNDSI = output.getNDSI();
			if (modifiedNDSI < 0) {
				modifiedNDSI = 0;
			}

			double modifiedNDVI = output.getNDVI();
			if (modifiedNDVI < 0) {
				modifiedNDVI = 0;
			}

			double whiteness = calcWhiteness(output.getRho()[0], output.getRho()[1],
					output.getRho()[2]);
			double variabilityProb = 1 - Math.max(Math.abs(modifiedNDVI),
					Math.max(Math.abs(modifiedNDSI), whiteness));
			lCloudProb = lTemperatureProb * variabilityProb;
		} catch (Exception e) {
			e.printStackTrace();
		}
        imagePixel.output().setLCloudProb(lCloudProb);
		
	}

	private int getPercentilPos(double percentil, int n) {
		return (int) Math.round((percentil * n / 100));
	}

	private boolean pixelIsInsideBoundingBox(ImagePixel imagePixel,
			List<BoundingBoxVertice> boundingBoxVertices) {
    	if (boundingBoxVertices.size() < 3) {
    		return true;
    	}
    	
    	double[] xpoints = new double[boundingBoxVertices.size()];
    	double[] ypoints = new double[boundingBoxVertices.size()];

    	for (int i = 0; i < boundingBoxVertices.size(); i++) {
			xpoints[i] = boundingBoxVertices.get(i).getLon();
			ypoints[i]= boundingBoxVertices.get(i).getLat();	
		}
    	
    	Path2D path = new Path2D.Double();
    	path.moveTo(xpoints[0], ypoints[0]);
    	for(int i = 1; i < xpoints.length; ++i) {
    	   path.lineTo(xpoints[i], ypoints[i]);
    	}
    	path.closePath();
   	
    	return path.contains(imagePixel.geoLoc().getLon(), imagePixel.geoLoc().getLat());
	}

	public Image pixelHProcess(List<ImagePixel> pixels, ImagePixel pixelQuente,
            ImagePixelOutput pixelQuenteOutput,
            ImagePixelOutput pixelFrioOutput, Image image) {

        if (pixelQuente != null || pixelQuenteOutput != null
                || pixelFrioOutput != null) {
            double z0m = z0m(pixelQuente.hc());
            double uAsterisk = uAsterisk(pixelQuente.ux(), pixelQuente.zx(),
                    pixelQuente.d(), z0m);
            double u200 = u200(uAsterisk, pixelQuente.d(), z0m);
            double d0 = pixelQuente.d();
            double Hcal = HQuente(pixelQuenteOutput.Rn(), pixelQuenteOutput.G());
            double z0mxy = z0mxy(pixelQuenteOutput.SAVI());
            double uAsteriskxy = uAsteriskxy(u200, d0, z0mxy);
            double rahxy = rahxy(uAsteriskxy);
            double uAsteriskCorrxy = uAsteriskxy;
            double rahxyCorr = Double.MAX_VALUE;

            // TODO configuracao

            List<HOutput> hOutput = hOutput(rahxyCorr, rahxy, uAsteriskCorrxy,
                    Hcal, u200, pixelQuenteOutput, pixelFrioOutput, d0, z0mxy);
            Image updatedImage = hPixelProces(pixels, hOutput, image);
            return updatedImage;
        }
        LOGGER.debug("(pixelQuente != null || pixelQuenteOutput != null || pixelFrioOutput != null) was false");
        return image;
    }

    private Image hPixelProces(List<ImagePixel> pixels,
            List<HOutput> listHOutput, Image image) {
        if (!pixels.isEmpty()) {
            for (int i = 0; i < pixels.size(); i++) {
                ImagePixel imagePixel = pixels.get(i);
                ImagePixelOutput output = imagePixel.output();
                List<HOutput> hOutPixel = iterH(imagePixel, listHOutput);
                output.sethOuts(hOutPixel);
                output.setH(hOutPixel.get(hOutPixel.size() - 1).getH());
                double lambdaE = lambdaE(output);
                output.setLambdaE(lambdaE);
                output.sethOuts(hOutPixel);
                imagePixel.setOutput(output);
                output.setRn24h(Rn24h(output.getAlpha(), tau24h()));
                output.setFrEvapo(frEvapo(lambdaE, output.Rn(), output.G()));
                output.setLambda24h(LE24h(output.getFrEvapo(),
                        output.getRn24h()));
                output.setEvapo24h(ET24h(output.getLambda24h()));
                output.setTau24h(tau24h());
                pixels.remove(i);
                pixels.add(i, imagePixel);
            }
        }
        image.pixels(pixels);
        return image;
    }

    public List<HOutput> hOutput(double rahxyCorr, double rahxy,
            double uAsteriskCorrxy, double Hcal, double u200,
            ImagePixelOutput pixelQuenteOutput,
            ImagePixelOutput pixelFrioOutput, double d0, double z0mxy) {

        double H = Hcal;
        List<HOutput> listH = new ArrayList<HOutput>();
        while (Math.abs((1. - (rahxyCorr / rahxy)) * 100.0) > 0.01) {
            HOutput hOutput = new HOutput();
            // System.out.println(i);
            // double dT = (Hcal * rahxy)/(rho * cp);
            double L = Lxy(uAsteriskCorrxy, pixelQuenteOutput.getTs(), H);
            double psim = psim(L);
            uAsteriskCorrxy = uAsteriskCorrxy(u200, d0, z0mxy, psim);
            double psiH1 = psiH1(L);
            double psiH2 = psiH2(L);
            double dTQuente = dTQuente(rahxy, pixelQuenteOutput.Rn(),
                    pixelQuenteOutput.G());
            double bQuente = b(pixelQuenteOutput, pixelFrioOutput, dTQuente);
            double aQuente = a(bQuente, pixelFrioOutput.getTs() - 273.15);
            H = H(aQuente, bQuente, (pixelQuenteOutput.getTs() - 273.15), rahxy);
            rahxyCorr = rahxy;
            rahxy = rahCorrxy(psiH1, psiH2, uAsteriskCorrxy);
            // double ta1 = bQuente * (pixelQuenteOutput.getTs() - 273.15);
            // double ta2 = aQuente * -1;

            hOutput.setA(aQuente);
            hOutput.setB(bQuente);
            hOutput.setH(H);
            listH.add(hOutput);
        }

        return listH;
    }

    public List<HOutput> hOutputPixel(double uAsterisk, double rahxy,
            double u200, ImagePixelOutput pixelQuenteOutput,
            ImagePixelOutput pixelFrioOutput, double d0, double z0mxy,
            List<HOutput> listHOutput) {

        double H;
        List<HOutput> listH = new ArrayList<HOutput>();
        double uAsteriskCorrxy = uAsterisk;
        double rahCorrxy = rahxy;
        for (HOutput hOut : listHOutput) {

            HOutput hOutput = new HOutput();
            // System.out.println(i);
            // double dT = (Hcal * rahxy)/(rho * cp);
            double aQuente = hOut.getA();
            double bQuente = hOut.getB();
            H = H(aQuente, bQuente, (pixelQuenteOutput.getTs() - 273.15),
                    rahCorrxy);
            double L = Lxy(uAsteriskCorrxy, pixelQuenteOutput.getTs(), H);
            double psim = psim(L);
            uAsteriskCorrxy = uAsteriskCorrxy(u200, d0, z0mxy, psim);
            double psiH1 = psiH1(L);
            double psiH2 = psiH2(L);
            rahCorrxy = rahCorrxy(psiH1, psiH2, uAsteriskCorrxy);
            // double dTQuente = dTQuente(rahCorrxy, pixelQuenteOutput.Rn(),
            // pixelQuenteOutput.G());

            hOutput.setA(aQuente);
            hOutput.setB(bQuente);
            hOutput.setH(H);
            hOutput.setRah(rahCorrxy);
            hOutput.setuAsterisk(uAsteriskCorrxy);
            hOutput.setL(L);
            listH.add(hOutput);
        }

        return listH;
    }

    public List<HOutput> iterH(ImagePixel imagePixel, List<HOutput> listHOutput) {
        double z0m = z0m(imagePixel.hc());
        double uAsterisk = uAsterisk(imagePixel.ux(), imagePixel.zx(),
                imagePixel.d(), z0m);
        double u200 = u200(uAsterisk, imagePixel.d(), z0m);
        double d0 = imagePixel.d();

        ImagePixelOutput imagePixelOutput = imagePixel.output();

        double z0mxy = z0mxy(imagePixelOutput.SAVI());
        double uAsteriskxy = uAsteriskxy(u200, d0, z0mxy);
        double rahxy = rahxy(uAsteriskxy);
        imagePixel.output().setZ0mxy(z0mxy);

        // TODO configuracao

        List<HOutput> hOutput = hOutputPixel(uAsterisk, rahxy, u200,
                imagePixelOutput, imagePixelOutput, d0, z0mxy, listHOutput);
        return hOutput;
    }

    double lambdaE(ImagePixelOutput output) {
        return output.Rn() - output.G() - output.getH();
    }

    private double b(ImagePixelOutput pixelQuenteOutput,
            ImagePixelOutput pixelFrioOutput, double dTQuente) {
        return dTQuente / (pixelQuenteOutput.getTs() - pixelFrioOutput.getTs());
    }

    public ImagePixelOutput processPixel(Satellite satellite,
            ImagePixel imagePixel) {
        ImagePixelOutput output = new ImagePixelOutput();
        
        double[] LLambda = imagePixel.L();

        double[] rho = calcRho(satellite, imagePixel);
        
        // System.out.println("rho " + Arrays.toString(rho));
        output.setRho(rho);
        double alphaToa = alphaToa(rho[0], rho[1], rho[2], rho[3], rho[4],
                rho[6]);
        output.setAlphaToa(alphaToa);
        // System.out.println("alphaToa " + alphaToa);

        double tauSW = tauSW(imagePixel.z());
        output.setTauSW(tauSW);
        // System.out.println("tauSW " + tauSW);

        double alpha = alpha(alphaToa, tauSW);
        output.setAlpha(alpha);
        // System.out.println("alpha " + alpha);

        double RSDown = RSDown(imagePixel.cosTheta(),
                earthSunDistance.get(imagePixel.image().getDay()), tauSW);
        output.setRSDown(RSDown);

        double NDVI = NDVI(rho[2], rho[3]);
        output.setNDVI(NDVI);
        // System.out.println("NDVI " + NDVI);

        double SAVI = SAVI(rho[2], rho[3]);
        // System.out.println("SAVI " + SAVI);
        output.setSAVI(SAVI);

        double EVI = EVI(rho[0], rho[2], rho[3]);
        // System.out.println("EVI " + EVI);
        output.setEVI(EVI);

        double IAF = IAF(SAVI);
        output.setIAF(IAF);
        // System.out.println("IAF " + IAF);

        double epsilonNB = epsilonNB(IAF);
        output.setEpsilonNB(epsilonNB);
        // System.out.)println("epsilonNB " + epsilonNB);

        double epsilonZero = epsilonZero(IAF);
        output.setEpsilonZero(epsilonZero);
        // System.out.println("epsilonZero " + epsilonZero);

        double TS = TS(satellite.K2(), epsilonNB, satellite.K1(), LLambda[5]);
        output.setTs(TS);
        // System.out.println("TS " + TS);

        double RLUp = RLUp(epsilonZero, TS);
        output.setRLUp(RLUp);
        // System.out.println("RLUp " + RLUp);

        double epsilonA = epsilonA(tauSW);
        output.setEpsilonA(epsilonA);
        // System.out.println("epsilonA " + epsilonA);

        double RLDown = RLDown(epsilonA, imagePixel.Ta());
        output.setRLDown(RLDown);
        // System.out.println("RLDown " + RLDown);

        double Rn = Rn(alpha, RSDown, RLDown, RLUp, epsilonZero);
        // System.out.println("Rn " + Rn);
        output.setRn(Rn);

        double G = G(TS, alpha, NDVI, Rn);
        // System.out.println("G " + G);
        output.setG(G);
        
        double NDSI = NDSI(rho[1], rho[4]);
        output.setNDSI(NDSI);
        
        boolean waterTest = cloudDetectionWaterTest(NDVI, rho[3]);
        output.setWaterTest(waterTest); 
        
        // Potential cloud layer - pass one
        boolean basicTest = cloudDetectionBasicTest(output.getNDVI(), output.getTs(), output.getNDSI(), rho[6]);
        boolean whitenessTest = cloudDetectionWhitenessTest(rho[0], rho[1], rho[2]);
        boolean hotTest = cloudDetectionHotTest(rho[0], rho[2]);
        boolean b4b5Test = cloudDetectionB4B5Test(rho[3], rho[4]);
        
        
        //potential cloud pixel
        boolean PCP = basicTest && whitenessTest && hotTest && b4b5Test;
        
        output.setPCP(PCP);
        return output;
    }
    
	public boolean isCloudPixel(ImagePixel imagePixel, double clearSkyLandCloudProbPercentil, double lTLow) {
		ImagePixelOutput output = imagePixel.output();

		double landThreshold = clearSkyLandCloudProbPercentil + 0.2;
		boolean PCL = (output.getPCP() && output.getWaterTest() && output.getWCloudProb() > 0.5)
				|| (output.getPCP() && !output.getWaterTest() && output.getLCloudProb() > landThreshold)
				|| (output.getLCloudProb() > 0.99 && output.getWaterTest())
				|| (output.getTs() < lTLow - 35);

		return PCL;
	}
	
	private boolean isSnowPixel(Satellite satellite, ImagePixel imagePixel) {
		double[] rho = calcRho(satellite, imagePixel);
		ImagePixelOutput output = imagePixel.output();
		return output.getNDSI() > 0.15 && output.getTs() < 3.8 && rho[3] > 0.11 && rho[1] > 0.1;
	}
	
	private double[] calcRho(Satellite satellite, ImagePixel imagePixel) {
		double[] rho = new double[7];
		double[] LLambda = imagePixel.L();
		
		for (int i = 0; i < rho.length; i++) {
            if (i == 5) {
                continue;
            }
            double rhoI = rho(LLambda[i],
                    earthSunDistance.get(imagePixel.image().getDay()),
                    satellite.ESUN(i + 1), imagePixel.cosTheta());
            rho[i] = rhoI;
        }
		return rho;
	}

    private boolean cloudDetectionWaterTest(double NDVI, double rho4) {
		return (NDVI < 0.01 && rho4 < 0.11) || (NDVI < 0.1 && rho4 < 0.05);
	}

	private boolean cloudDetectionB4B5Test(double rho4, double rho5) {
		return rho4/rho5 > 0.75;
	}

	private boolean cloudDetectionHotTest(double rho1, double rho3) {
		return (rho1 - 0.5 * rho3 - 0.08) > 0;
	}

	private boolean cloudDetectionWhitenessTest(double rho1, double rho2, double rho3) {
		return calcWhiteness(rho1, rho2, rho3) < 0.7;
	}

	private double calcWhiteness(double rho1, double rho2, double rho3) {
		double meanVIS = (rho1 + rho2 + rho3) / 3;
		return absoluteDiffVIS(rho1, meanVIS) + absoluteDiffVIS(rho2, meanVIS)
				+ absoluteDiffVIS(rho3, meanVIS);
	}

	private double absoluteDiffVIS(double rho1, double meanVIS) {
		return Math.abs((rho1 - meanVIS) / meanVIS);
	}

	private boolean cloudDetectionBasicTest(double NDVI, double TS, double NDSI, double rho7) {
		return rho7 > 0.03 && TS < 300.15 && NDVI < 0.8 && NDSI < 0.8;
	}

	private double NDSI(double rho2, double rho5) {
		return (rho2 - rho5) / (rho2 + rho5);
	}

	private double Gsc() {
        return 0.082;
    }

    // private double dr() {
    // return 1+0.33*math.cos((2*Math.PI/365)*DDA());
    // }

    private double omegaR() {
        return Math.acos((-1 * Math.tan(phi1())) * Math.tan(delta()));
    }

    private double phi1() {
        return Math.PI / 180 * phi2();
    }

    private double delta() {
        return 0.409 * Math.sin(((2 * Math.PI / 365) * DDA()) - 1.39);
    }

    private double DDA() {
        return 135;
    }

    private double phi2() {
        return -7.379;
    }

    private double Ra1() {
        return ((24 * 60 / Math.PI) * Gsc() * DDA())
                * (omegaR() * Math.sin(phi1()) * math.sin(delta()) + Math
                        .cos(phi1()) * math.cos(delta()) * math.sin(omegaR()));
    }

    private double Ra2() {
        return Ra1() / 11.57;
    }

    private double frEvapo(double lambdaE, double Rn, double G) {
        return lambdaE / (Rn - G);
    }
}
