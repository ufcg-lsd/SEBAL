package org.fogbowcloud.sebal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReader;
import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReaderPlugin;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.fogbowcloud.sebal.model.image.BoundingBox;
import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.parsers.Elevation;
import org.fogbowcloud.sebal.parsers.WeatherStation;

public class SEBALHelper {
    public static Product readProduct(String mtlFileName,
            String boundingBoxFileName) throws IOException {
        File mtlFile = new File(mtlFileName);
        LandsatGeotiffReaderPlugin readerPlugin = new LandsatGeotiffReaderPlugin();
        LandsatGeotiffReader reader = new LandsatGeotiffReader(readerPlugin);
        Product product = reader.readProductNodes(mtlFile, null);
        ProductSubsetDef productSubsetDef = null;
        Product boundedProduct = product;
        if (boundingBoxFileName != null) {
            BoundingBox boundingBox = calculateBoundingBox(boundingBoxFileName,
                    product);

            productSubsetDef = new ProductSubsetDef();
            productSubsetDef.setRegion(boundingBox.getX(), boundingBox.getY(),
                    boundingBox.getW(), boundingBox.getH());
            boundedProduct = reader.readProductNodes(mtlFile, productSubsetDef);
        }
        return boundedProduct;
    }

    private static BoundingBox calculateBoundingBox(String boudingBoxFileName,
            Product product) throws IOException {
        String boundingBoxInfo = FileUtils.readFileToString(new File(
                boudingBoxFileName));
        String[] boundingBoxValues = boundingBoxInfo.split(",");

        double x0 = Double.parseDouble(boundingBoxValues[0]);
        double y0 = Double.parseDouble(boundingBoxValues[1]);
        double x1 = Double.parseDouble(boundingBoxValues[2]);
        double y1 = Double.parseDouble(boundingBoxValues[3]);

        MetadataElement metadataRoot = product.getMetadataRoot();
        double ULx = metadataRoot.getElement("L1_METADATA_FILE")
                .getElement("PRODUCT_METADATA")
                .getAttribute("CORNER_UL_PROJECTION_X_PRODUCT").getData()
                .getElemDouble();
        double ULy = metadataRoot.getElement("L1_METADATA_FILE")
                .getElement("PRODUCT_METADATA")
                .getAttribute("CORNER_UL_PROJECTION_Y_PRODUCT").getData()
                .getElemDouble();

        int offsetX = (int) ((x0 - ULx) / 30);
        int offsetY = (int) ((ULy - y0) / 30);
        int w = (int) ((x1 - x0) / 30);
        int h = (int) ((y0 - y1) / 30);
        BoundingBox boundingBox = new BoundingBox(offsetX, offsetY, w, h);
        return boundingBox;
    }

    public static Image readPixels(List<ImagePixel> pixels,
            ImagePixel pixelQuente, ImagePixel pixelFrio,
            PixelQuenteFrioChooser pixelQuenteFrioChooser) {
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        image.pixels(pixels);
        image.pixelQuente(pixelQuente);
        image.pixelFrio(pixelFrio);
        return image;
    }

    public static Image readPixels(List<ImagePixel> pixelsQuente,
            List<ImagePixel> pixelsFrio,
            PixelQuenteFrioChooser pixelQuenteFrioChooser) {
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.addAll(pixelsFrio);
        pixels.addAll(pixelsQuente);
        image.pixels(pixels);
        return image;
    }

    public static Image readPixels(Product product, int iBegin, int iFinal,
            int jBegin, int jFinal,
            PixelQuenteFrioChooser pixelQuenteFrioChooser) throws Exception {

        Locale.setDefault(Locale.ROOT);
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        Elevation elevation = new Elevation();
        WeatherStation station = new WeatherStation();

        UTC startTime = product.getStartTime();
        int day = startTime.getAsCalendar().get(Calendar.DAY_OF_YEAR);
        image.setDay(day);

        Band bandAt = product.getBandAt(0);
        bandAt.ensureRasterData();

        MetadataElement metadataRoot = product.getMetadataRoot();
        Double sunElevation = metadataRoot.getElement("L1_METADATA_FILE")
                .getElement("IMAGE_ATTRIBUTES").getAttribute("SUN_ELEVATION")
                .getData().getElemDouble();

        for (int i = iBegin; i < Math.min(iFinal, bandAt.getSceneRasterWidth()); i++) {
            for (int j = jBegin; j < Math.min(jFinal, bandAt.getSceneRasterHeight()); j++) {

                DefaultImagePixel imagePixel = new DefaultImagePixel();

                double[] LArray = new double[product.getNumBands()];
                for (int k = 0; k < product.getNumBands(); k++) {
                    double L = product.getBandAt(k).getSampleFloat(i, j);
                    LArray[k] = L;
                }
                imagePixel.L(LArray);

                PixelPos pixelPos = new PixelPos(i, j);

                imagePixel.cosTheta(Math.sin(Math.toRadians(sunElevation)));

                // System.out.println(i + " " + j);

                GeoPos geoPos = bandAt.getGeoCoding().getGeoPos(pixelPos, null);
                double latitude = Double.valueOf(String.format("%.10g%n",
                        geoPos.getLat()));
                double longitude = Double.valueOf(String.format("%.10g%n",
                        geoPos.getLon()));
                Double z = elevation.z(latitude, longitude);
                imagePixel.z(z == null ? 400 : z);

                GeoLoc geoLoc = new GeoLoc();
                geoLoc.setI(i);
                geoLoc.setJ(j);
                geoLoc.setLat(latitude);
                geoLoc.setLon(longitude);
                imagePixel.geoLoc(geoLoc);

                double Ta = station.Ta(geoPos.getLat(), geoPos.getLon(),
                        startTime.getAsDate());
                imagePixel.Ta(Ta);

                double ux = station.ux(geoPos.getLat(), geoPos.getLon(),
                        startTime.getAsDate());
                imagePixel.ux(ux);

                double zx = station.zx(geoPos.getLat(), geoPos.getLon());
                imagePixel.zx(zx);

                double d = station.d(geoPos.getLat(), geoPos.getLon());
                imagePixel.d(d);

                double hc = station.hc(geoPos.getLat(), geoPos.getLon());
                imagePixel.hc(hc);

                imagePixel.image(image);

                image.addPixel(imagePixel);

                geoPos = null;
            }
        }

        return image;
    }

}
