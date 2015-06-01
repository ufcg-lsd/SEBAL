package org.fogbowcloud.sebal.tiff;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;

public class NetCDFHelper {

	public static void normalize(String fileName, String varName, double daysSince1970) throws IOException, InvalidRangeException {
		NetcdfFile reader = NetcdfFile.open(fileName);
		
		NetcdfFileWriter writer = NetcdfFileWriter.createNew(Version.netcdf3, fileName + ".tmp");
		List<Dimension> dims = new LinkedList<Dimension>();
		Dimension timeDim = writer.addUnlimitedDimension("time");
		dims.add(timeDim);
		for (Dimension dim : reader.getDimensions()) {
			dims.add(writer.addDimension(null, dim.getShortName(), dim.getLength()));
		}
		
		List<Dimension> timeDimList = new LinkedList<Dimension>();
		timeDimList.add(timeDim);
		
		Variable timeVar = writer.addVariable(null, "time", DataType.DOUBLE, timeDimList);
		timeVar.addAttribute(new Attribute("units", "days since 1970-1-1"));
		timeVar.addAttribute(new Attribute("long_name", "time"));
		timeVar.addAttribute(new Attribute("standard_name", "time"));
		timeVar.addAttribute(new Attribute("calendar", "standard"));
		
		for (Variable oldVar : reader.getVariables()) {
			if (oldVar.getShortName().equals("Band1") || oldVar.getShortName().equals("time")) {
				continue;
			}
			List<Dimension> tempDimList = new LinkedList<Dimension>();
			
			if (!oldVar.getDimensions().isEmpty()) {
				for (Dimension dimension : dims) {
					if (dimension.getShortName().equals(oldVar.getDimension(0).getShortName())) {
						tempDimList.add(dimension);
						break;
					}
				}
			}
			Variable newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), tempDimList);
			for (Attribute att : oldVar.getAttributes()) {
				newVar.addAttribute(att);
			}
		}
		
		Variable band1 = reader.findVariable("Band1");
		List<Dimension> band1Dimensions = band1.getDimensions();
		List<Dimension> ndviDims = new LinkedList<Dimension>();
		ndviDims.add(timeDim);
		for (Dimension band1Dim : band1Dimensions) {
			for (Dimension newDim : dims) {
				if (band1Dim.getShortName().equals(newDim.getShortName())) {
					ndviDims.add(newDim);
				}
			}
		}
		
		Variable ndvi = writer.addVariable(null, varName, DataType.DOUBLE, ndviDims);
		for (Attribute att : band1.getAttributes()) {
			ndvi.addAttribute(att);
		}
		ndvi.addAttribute(new Attribute("long_name", varName));
		
		List<Attribute> globalAttributes = reader.getGlobalAttributes();
		for (Attribute attribute : globalAttributes) {
			writer.addGroupAttribute(null, attribute);
		}
		writer.create();
		writer.flush();
		writer.close();
		
		writer = NetcdfFileWriter.openExisting(fileName + ".tmp");
		writer.write(writer.findVariable("time"), Array.factory(new double[]{daysSince1970}));
		double[][] doubleAr = (double[][])(band1.read().copyToNDJavaArray());
		
		writer.write(writer.findVariable(varName), Array.factory(new double[][][]{doubleAr}));
		for (Variable oldVar : reader.getVariables()) {
			Variable newVar = writer.findVariable(oldVar.getShortName());
			if (newVar == null) {
				continue;
			}
			writer.write(newVar, oldVar.read());
		}
		writer.flush();
		writer.close();

		reader.close();
		
		new File(fileName).delete();
		FileUtils.moveFile(new File(fileName + ".tmp"), new File(fileName));
	}
	
}
