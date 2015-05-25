package org.fogbowcloud.sebal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

public class findSmallestSquare {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        String mtlListFile = args[0];
        double x0 = Double.MIN_VALUE;
        double x1 = Double.MAX_VALUE;
        double y0 = Double.MAX_VALUE;
        double y1 = Double.NEGATIVE_INFINITY;

        BufferedReader br = new BufferedReader(new FileReader(mtlListFile));
        String mtlFile = null;
        while ((mtlFile = br.readLine()) != null) {
            Product product = SEBALHelper.readProduct(mtlFile, null);
            MetadataElement metadataRoot = product.getMetadataRoot();
            double ULx = metadataRoot.getElement("L1_METADATA_FILE")
                    .getElement("PRODUCT_METADATA")
                    .getAttribute("CORNER_UL_PROJECTION_X_PRODUCT").getData()
                    .getElemDouble();
            double ULy = metadataRoot.getElement("L1_METADATA_FILE")
                    .getElement("PRODUCT_METADATA")
                    .getAttribute("CORNER_UL_PROJECTION_Y_PRODUCT").getData()
                    .getElemDouble();
            double LRx = metadataRoot.getElement("L1_METADATA_FILE")
                    .getElement("PRODUCT_METADATA")
                    .getAttribute("CORNER_LR_PROJECTION_X_PRODUCT").getData()
                    .getElemDouble();
            double LRy = metadataRoot.getElement("L1_METADATA_FILE")
                    .getElement("PRODUCT_METADATA")
                    .getAttribute("CORNER_LR_PROJECTION_Y_PRODUCT").getData()
                    .getElemDouble();
            if (ULx > x0) {
                x0 = ULx;
            }
            if (ULy < y0) {
                y0 = ULy;
            }
            if (LRx < x1) {
                x1 = LRx;
            }
            if (LRy > y1) {
                y1 = LRy;
            }
        }
        br.close();
        
        StringBuilder result = new StringBuilder();
        result.append(x0 + "," + y0 + "," + x1 + "," + y1);
        FileUtils.writeStringToFile(new File("boundingBox"), result.toString());
    }
}
