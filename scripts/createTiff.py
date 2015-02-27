import numpy, os, sys
from osgeo import osr, gdal

if __name__ == '__main__':
    
    # Set file vars
    csv_file = sys.argv[1]
    tif_file_prefix = csv_file.replace(".csv","_")
    ground_heat_file = tif_file_prefix + "ground_heat_flux.tif"
    net_radiation_file = tif_file_prefix + "net_radiation.tif"
    surface_temperature_file = tif_file_prefix + "surface_temperature.tif"
    ndvi_file = tif_file_prefix + "ndvi.tif"
    savi_file = tif_file_prefix + "savi.tif"
    surface_albedo_file = tif_file_prefix + "surface_albedo.tif"
    evi_file = tif_file_prefix + "evi.tif"

    MASK_HEIGHT=2600
    MASK_WIDTH=2924

    # Create gtif
    driver = gdal.GetDriverByName("GTiff")
    dst_g = driver.Create(ground_heat_file, MASK_WIDTH, MASK_HEIGHT, 1, gdal.GDT_Float64)
    dst_rn = driver.Create(net_radiation_file, MASK_WIDTH, MASK_HEIGHT, 1, gdal.GDT_Float64)
    dst_ts = driver.Create(surface_temperature_file, MASK_WIDTH, MASK_HEIGHT, 1, gdal.GDT_Float64)
    dst_ndvi = driver.Create(ndvi_file, MASK_WIDTH, MASK_HEIGHT, 1, gdal.GDT_Float64)
    dst_savi = driver.Create(savi_file, MASK_WIDTH, MASK_HEIGHT, 1, gdal.GDT_Float64)
    dst_a = driver.Create(surface_albedo_file, MASK_WIDTH, MASK_HEIGHT, 1, gdal.GDT_Float64)
    dst_evi = driver.Create(evi_file, MASK_WIDTH, MASK_HEIGHT, 1, gdal.GDT_Float64)

    # raster = numpy.zeros( (100, 100) )
    # top left x, w-e pixel resolution, rotation, top left y, rotation, n-s pixel resolution
    x_min = -37.1785965  
    y_max = -6.974605083

    PIXEL_SIZE = 0.00027
    dst_g.SetGeoTransform( [x_min,    # 0
        PIXEL_SIZE,                    # 1
        0,                             # 2
        y_max,                         # 3
        0,                             # 4
        -PIXEL_SIZE] )
    dst_rn.SetGeoTransform( [x_min,    # 0
        PIXEL_SIZE,                    # 1
        0,                             # 2
        y_max,                         # 3
        0,                             # 4
        -PIXEL_SIZE] )
    dst_ts.SetGeoTransform( [x_min,    # 0
        PIXEL_SIZE,                    # 1
        0,                             # 2
        y_max,                         # 3
        0,                             # 4
        -PIXEL_SIZE] )
    dst_ndvi.SetGeoTransform( [x_min,    # 0
        PIXEL_SIZE,                    # 1
        0,                             # 2
        y_max,                         # 3
        0,                             # 4
        -PIXEL_SIZE] )
    dst_savi.SetGeoTransform( [x_min,    # 0
        PIXEL_SIZE,                    # 1
        0,                             # 2
        y_max,                         # 3
        0,                             # 4
        -PIXEL_SIZE] )
    dst_a.SetGeoTransform( [x_min,    # 0
        PIXEL_SIZE,                    # 1
        0,                             # 2
        y_max,                         # 3
        0,                             # 4
        -PIXEL_SIZE] )
    dst_evi.SetGeoTransform( [x_min,    # 0
        PIXEL_SIZE,                    # 1
        0,                             # 2
        y_max,                         # 3
        0,                             # 4
        -PIXEL_SIZE] )

    # set the reference info 
    srs = osr.SpatialReference()
    srs.SetWellKnownGeogCS("WGS84")
    print srs.IsGeographic()
    print srs.IsProjected()
    dst_g.SetProjection( srs.ExportToWkt() )
    dst_rn.SetProjection( srs.ExportToWkt() )
    dst_ts.SetProjection( srs.ExportToWkt() )
    dst_ndvi.SetProjection( srs.ExportToWkt() )
    dst_savi.SetProjection( srs.ExportToWkt() )
    dst_a.SetProjection( srs.ExportToWkt() )
    dst_evi.SetProjection( srs.ExportToWkt() )
    
    band_g = dst_g.GetRasterBand(1)
    band_rn = dst_rn.GetRasterBand(1)
    band_ts = dst_ts.GetRasterBand(1)
    band_ndvi = dst_ndvi.GetRasterBand(1)
    band_savi = dst_savi.GetRasterBand(1)
    band_a = dst_a.GetRasterBand(1)
    band_evi = dst_evi.GetRasterBand(1)
    
    raster_g = None
    raster_rn = None
    raster_ts = None
    raster_ndvi = None
    raster_savi = None
    raster_a = None
    raster_evi = None

    lineIdx = -1

    MAX_LAT=-7.03983
    MIN_LON=-37.176
    MIN_LAT=-7.7409
    MAX_LON=-36.3782
    first_line = True
 
    initialI = -1
    initialJ = -1

    raster_g = numpy.zeros((MASK_HEIGHT, MASK_WIDTH), dtype='complex64')
    raster_rn = numpy.zeros((MASK_HEIGHT, MASK_WIDTH), dtype='complex64')
    raster_ts = numpy.zeros((MASK_HEIGHT, MASK_WIDTH), dtype='complex64')
    raster_ndvi = numpy.zeros((MASK_HEIGHT, MASK_WIDTH), dtype='complex64')
    raster_savi = numpy.zeros((MASK_HEIGHT, MASK_WIDTH), dtype='complex64')
    raster_a = numpy.zeros((MASK_HEIGHT, MASK_WIDTH), dtype='complex64')
    raster_evi = numpy.zeros((MASK_HEIGHT, MASK_WIDTH), dtype='complex64')

    max_lat = -100
    min_lon = 0

    with open(csv_file, 'r') as f:
      for line in f:
        if (first_line):
          first_line = False
          continue

        fields = line.split(',')
        g = float(fields[4])
        rn = float(fields[5])
        ts = float(fields[6])
        ndvi = float(fields[7])
        savi = float(fields[8])
        a = float(fields[9])
        evi = float(fields[10])
        i = int(fields[0])
        j = int(fields[1])
        lat = float(fields[2])
        lon = float(fields[3])
        if (initialI < 0 and lat > MIN_LAT and lat < MAX_LAT and lon > MIN_LON and lon < MAX_LON):
           initialI = i
	   initialJ = j
           print lat
           print lon
        
        if (initialI < 0 or initialJ < 0):
           continue

        if (i - initialI < 0 or i - initialI >= MASK_WIDTH or j >= initialJ or j < initialJ - MASK_HEIGHT):
           continue
        
        if (min_lon > lon and max_lat < lat):
            min_lon = lon
            max_lat = lat

        raster_g[j - (initialJ - MASK_HEIGHT)][i - initialI] = g
        raster_rn[j - (initialJ - MASK_HEIGHT)][i - initialI] = rn
        raster_ts[j - (initialJ - MASK_HEIGHT)][i - initialI] = ts
        raster_ndvi[j - (initialJ - MASK_HEIGHT)][i - initialI] = ndvi
        raster_savi[j - (initialJ - MASK_HEIGHT)][i - initialI] = savi
        raster_a[j - (initialJ - MASK_HEIGHT)][i - initialI] = a
        raster_evi[j - (initialJ - MASK_HEIGHT)][i - initialI] = evi

    # write the band
    print "------"
    print min_lon
    print max_lat
    band_g.WriteArray(raster_g)
    band_rn.WriteArray(raster_rn)
    band_ts.WriteArray(raster_ts)
    band_ndvi.WriteArray(raster_ndvi)
    band_savi.WriteArray(raster_savi)
    band_a.WriteArray(raster_a)
    band_evi.WriteArray(raster_evi)
    # band.FlushCache()>
