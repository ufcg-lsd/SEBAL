########################################################################################
#                                                                                      #
#                         EU BRAZIL Cloud Connect                                      #
#                                                                                      #
#                                                                                      #
########################################################################################

options(echo = TRUE)
rm(list = ls())
# for now, this will be here
#install.packages("snow", repos="https://vps.fmvz.usp.br/CRAN/")
#install.packages("R.utils", repos="https://vps.fmvz.usp.br/CRAN/")

library(R.utils)
library(raster)
library(rgdal)
library(maptools)
library(ncdf4)
library(sp)
library(snow)
args <- commandArgs(trailingOnly = TRUE)
#WD<-args[1]
setwd("D:/SEBAL-master/workspace/R") # Working Directory

# changing raster tmpdir
rasterOptions(tmpdir = "C:/mnt/rasterTmp")

dados <- read.csv("dados.csv", sep = ";", stringsAsFactors = FALSE) # Data
#dados <- read.csv(args[1],sep=";", stringsAsFactors=FALSE)

######################### 1. Constants ######################################
#Von K?rm?n
constantes <- list("k" = 0.41)
#Gravity
constantes <- c(constantes, "g" = 9.81)
#Air density
constantes <- c(constantes, "rho" = 1.15)
#Specific heat of air
constantes <- c(constantes, "cp" = 1004)
#Solar constant (0.0820 MJ m-2 min-1)
constantes <- c(constantes, "gsc" = 0.082)
#Number of clusters used in image processing
constantes <- c(constantes, "clusters" = 7)
#Set projection and spatial resolution
constantes <- c(constantes, "WGS84" = "+proj=longlat +datum=WGS84 +ellps=WGS84")

# Ler dist?ncia realtiva Sol a Terra
load("d_sun_earth.RData")

######################### 2. Methods ######################################

# Loads image information such as name, date
# elevation of the sun when image was taken,
# which satelite took the image,
# etc
load_image_information <- function(dados) {
  #Images file reading
  fic_dir <- dados$File.images[1]

  #MTL File
  MTL <- read.table(
    dados$MTL[1],
    skip = 0,
    nrows = 140,
    sep = "=",
    quote = "''",
    as.is = TRUE
  )

  # Gets the scene name, used to prefix the images
  file_name <-  substr(
    MTL$V2[
      MTL$V1 == grep(
        pattern = "LANDSAT_SCENE_ID",
        MTL$V1,
        value = T
      )
    ],
    3,
    23
  )

  #Sensor Number
  sensor_num <- as.numeric(substr(file_name, 3, 3))
  if (sensor_num == 8) {
    #vers�o alterada 01/08/2017
    MTL <- read.table(
      dados$MTL[1],
      skip = 0,
      nrows = -1,
      sep = "=",
      quote = "''",
      as.is = TRUE,
      fill = TRUE
    )
  }

  #WRSPR
  WRSPR <- substr(file_name, 4, 9)

  #Images year
  Ano <- as.numeric(substr(file_name, 10, 13))

  #Julian Day
  Dia_juliano <- as.numeric(substr(file_name, 14, 16))

  sun_elevation <- as.numeric(
    MTL$V2[
      MTL$V1 == grep(
        pattern = "SUN_ELEVATION",
        MTL$V1,
        value = TRUE
      )
    ]
  )

  #From SUN ELEVATION
  costheta <- sin(sun_elevation * pi / 180)

  #Reading image file
  imagens <- list.files(path = fic_dir, pattern = "*.TIF", full.names = TRUE)
  if (sensor_num == 8) {
    raster_stack <- stack(as.list(imagens[c(4:9, 2)]))
    sensor_params <- read.csv(
      "parametros do sensor/parametrosdosensorLC.csv",
      sep = ";",
      stringsAsFactors = FALSE
    )
  } else if (sensor_num == 7) {
    raster_stack <- stack(as.list(imagens[1:8]))
    sensor_params <- read.csv(
      "parametros do sensor/parametrosdosensorETM.csv",
      sep = ";",
      stringsAsFactors = FALSE
    )
  } else {
    raster_stack <- stack(as.list(imagens[1:7]))
    if (Ano < 1992) {
      sensor_params <- read.csv(
        "parametros do sensor/parametrosdosensorTM1.csv",
        sep = ";",
        stringsAsFactors = FALSE
      )
    } else if (Ano > 1992) {
      sensor_params <- read.csv(
        "parametros do sensor/parametrosdosensorTM2.csv",
        sep = ";",
        stringsAsFactors = FALSE
      )
    } else {
      print("No known behavior for year 1992")
      quit(1)
    }
  }

  #Time image
  acquired_date <- as.Date(
    MTL$V2[
      MTL$V1 == grep(
        pattern = "DATE_ACQUIRED",
        MTL$V1,
        value = TRUE
      )
    ]
  )
  daysSince1970 <- as.numeric(acquired_date)
  tdim <- ncdim_def(
    "time",
    "days since 1970-1-1",
    daysSince1970,
    unlim = TRUE,
    create_dimvar = TRUE,
    "standard",
    "time"
  )

  #Reading file Station weather
  fic_sw <- dados$File.Station.Weather[1]
  table_sw <- read.csv(
    fic_sw,
    sep = ";",
    header = FALSE,
    stringsAsFactors = FALSE
  )

  fmask <- raster(imagens[[length(imagens)]])
  return(
    list(
      "raster_stack" = raster_stack,
      "fmask" = fmask,
      "output_path" = dados$Path.Output[1],
      "file_name" = file_name,
      "sensor_num" = sensor_num,
      "WRSPR" = WRSPR,
      "sensor_params" = sensor_params,
      "Dia_juliano" = Dia_juliano,
      "station_weather_table" = table_sw,
      "tdim" = tdim,
      "costheta" = costheta,
      "MTL" = MTL
    )
  )
}

# Filters clouds and cloud shadows
filter_clouds <- function(raster_stack, fmask, sensor_num) {
    if (sensor_num != 8) {
        mask_filter <- 672
    } else {
        mask_filter <- 2720
    }
    v_fmask <- values(fmask)
    for (i in 1:nlayers(raster_stack)) {
        v_raster <- values(raster_stack[[i]])
        v_raster[v_fmask != mask_filter] <- NaN
        percent <- sum(is.na(v_raster) / length(v_raster))
        if (percent >= 0.99) {
            t <- paste(
                "Imagem incompatível para o processamento",
                "Mais de 99% nuvem e sombra de nuvem"
            )
            print(t)
            quit("no", 4, FALSE)
        }
        values(raster_stack[[i]]) <- v_raster
    }
    return(raster_stack)
}

# Change raster projection
project_raster <- function(raster_stack, constantes) {
    beginCluster(constantes$clusters)
    raster_stack <- projectRaster(raster_stack, crs = constantes$WGS84)
    endCluster()
}

resample_raster <- function(r1, r2, constantes) {
    beginCluster(constantes$clusters)
    tryCatch(
        r1 <- evalWithTimeout(
            resample(r1, r2, method = "ngb"),
            timeout = 3600
        ),
        TimeoutException = function(ex) {
            print("Image resample timedout. Exiting with 124 code...");
            quit("no", 124, FALSE)
        }
    )
    endCluster()
    return(r1)
}

mask_raster <- function(raster_stack, boundingbox, constantes) {
    beginCluster(constantes$clusters)
    tryCatch(
        output <- evalWithTimeout(
            mask(raster_stack, boundingbox),
            timeout = 10800
        ),
        TimeoutException = function(ex) {
            print("Output Fmask timedout. Exiting with 124 code...");
            quit("no", 124, FALSE)
        }
    )
    endCluster()
    return(output)
}

update_output_file <- function(band, file_name, output_path, raster_elevation, tdim) {
    #Opening old NetCDF
    var_output <- paste(output_path, "/", file_name, "_", band, ".nc", sep = "")
    nc <- nc_open(
        var_output,
        write = TRUE,
        readunlim = FALSE,
        verbose = TRUE,
        auto_GMT = FALSE,
        suppress_dimvals = FALSE
    )

    #Getting lat and lon values from old NetCDF
    oldlat <- ncvar_get(
        nc,
        "lat",
        start = 1,
        count = raster_elevation@nrows
    )
    oldlon <- ncvar_get(
        nc,
        "lon",
        start = 1,
        count = raster_elevation@ncols
    )

    #Defining latitude and longitude dimensions
    dimlatdef <- ncdim_def(
        "lat",
        "degrees",
        oldlat,
        unlim = FALSE,
        longname = "latitude"
    )
    dimlondef <- ncdim_def(
        "lon",
        "degrees",
        oldlon,
        unlim = FALSE,
        longname = "longitude"
    )

    #New alb file name
    file_output <- paste(
        output_path,
        "/",
        file_name,
        "_",
        band,
        ".nc",
        sep = ""
    )
    oldvalues <- ncvar_get(nc, file_name)
    newvalues <- ncvar_def(
        band,
        "daily",
        list(dimlondef, dimlatdef, tdim),
        longname = band,
        missval = NaN,
        prec = "double"
    )
    nc_close(nc)
    newNCDF4 <- nc_create(file_output, newvalues)
    ncvar_put(
        newNCDF4,
        band,
        oldvalues,
        start = c(1, 1, 1),
        count = c(raster_elevation@ncols, raster_elevation@nrows, 1)
    )
    nc_close(newNCDF4)
}

write_raster <- function(
    raster_stack,
    names,
    raster_elevation,
    file_name,
    output_path,
    tdim) {
    names(raster_stack) <- names
    writeRaster(
        raster_stack,
        output_path,
        overwrite = TRUE,
        format = "CDF",
        varname = file_name,
        varunit = "daily",
        longname = file_name,
        xname = "lon",
        yname = "lat",
        bylayer = TRUE,
        suffix = "names"
    )

    for (band in names(raster_stack)) {
        update_output_file(band, file_name, output_path, raster_elevation, tdim)
    }
}

# Reading Bounding Box
read_bounding_box <- function(WRSPR, constantes) {
    fic_bounding_boxes <- paste("wrs2_asc_desc/wrs2_asc_desc.shp")
    boundingboxes <- readShapePoly(
        fic_bounding_boxes,
        proj4string = CRS(constantes$WGS84)
    )
    boundingbox <- boundingboxes[boundingboxes@data$WRSPR == WRSPR, ]
    return(boundingbox)
}

#Reading Elevation
read_elevation <- function(raster_stack, boundingbox, constantes) {
    fic_elevation <- paste("Elevation/srtm_29_14.tif")
    raster_elevation <- raster(fic_elevation)

    beginCluster(constantes$clusters)
    raster_elevation <- crop(raster_elevation, extent(boundingbox))
    endCluster()

    raster_elevation_aux <- raster(raster_elevation)
    res(raster_elevation_aux) <- res(raster_stack)

    raster_elevation <- resample_raster(
        raster_elevation,
        raster_elevation_aux,
        constantes
    )
    return(raster_elevation)
}

#Transmissivity 
calc_transmissivity <- function(raster_elevation) {
    raster_transmissivity <- raster(raster_elevation)
    values(raster_transmissivity) <- 0.75 + 2 *
        10 ^ -5 * values(raster_elevation)
    return(raster_transmissivity)
}

phase1 <- function(
    raster_stack,
    raster_transmissivity,
    sensor_num,
    sensor_params,
    sun_earth_dist,
    station_weather_table,
    costheta,
    MTL,
    constantes
    ) {
    if (sensor_num < 8){
        if (sensor_num == 5) {
            r <- 7
            #Constante Temperatura de superf�cie
            k1 <- 607.76
            k2 <- 1260.56
        } else {
            r <- 8
            #Constante Temperatura de superf�cie
            k1 <- 666.09
            k2 <- 1282.71
        }

        # Radi�ncia
        v_rad <- list()
        for (i in 1:r){
            v_rad[[i]] <- values(image.rec[[i]]) *
                sensor_params$Grescale[i] +
                sensor_params$Brescale[i]
            v_rad[[i]][v_rad < 0] <- 0
        }

        ts_rad <- v_rad[[r - 1]]

        #Reflect�ncia
        v_ref <- list()
        for (i in 1:r) {
            v_ref[[i]] < (-pi * v_rad[[i]] * sun_earth_dist ^ 2) /
                (sensor_params$ESUN[i] * costheta)
        }
    } else {
        r <- 6
        #Constante Temperatura de superf�cie
        k1 <- 774.8853
        k2 <- 1321.0789

        # Radi�ncia
        rad_mult <- as.numeric(
            MTL$V2[
                MTL$V1 == grep(
                    pattern = "RADIANCE_MULT_BAND_10",
                    MTL$V1,
                    value = TRUE
                )
            ]
        )
        rad_add <- as.numeric(
            MTL$V2[
                MTL$V1 == grep(
                    pattern = "RADIANCE_ADD_BAND_10",
                    MTL$V1,
                    value = TRUE
                )
            ]
        )
        v_rad10 <- values(raster_stack[[7]]) * rad_mult + rad_add

        ts_rad <- v_rad10

        #Reflect�ncia
        v_ref <- list()
        for (i in 1:6){
            v_ref[[i]] <- (values(raster_stack[[i]]) * 0.00002 - 0.1) / costheta
        }
    }

    #Albedo de superf�cie
    v_alb <- v_ref[[1]] * sensor_params$wb[1] +
                v_ref[[2]] * sensor_params$wb[2] +
                v_ref[[3]] * sensor_params$wb[3] +
                v_ref[[4]] * sensor_params$wb[4] +
                v_ref[[5]] * sensor_params$wb[5] +
                v_ref[[r]] * sensor_params$wb[r]
    v_alb <- (v_alb - 0.03) / values(raster_transmissivity) ^ 2

    #Radia��o de onda curta incidente
    # C�u claro
    v_rs <- 1367 * costheta * values(raster_transmissivity) / sun_earth_dist ^ 2

    #NDVI,SAVI,LAI e EVI
    v_ndvi <- (v_ref[[4]] - v_ref[[3]]) / (v_ref[[4]] + v_ref[[3]])
    v_savi <- ( (1 + 0.05) * (v_ref[[4]] - v_ref[[3]])) /
                (0.05 + v_ref[[4]] + v_ref[[3]])
    v_lai <- v_savi
    v_lai[v_savi > 0.687] <- 6
    v_lai[v_savi <= 0.687] <- -log(
        (0.69 - v_savi[v_savi <= 0.687]) / 0.59
    ) / 0.91
    v_lai[v_savi < 0.1] <- 0
    v_evi <- 2.5 * ( (v_ref[[4]] - v_ref[[3]]) /
            (v_ref[[4]] + (6 * v_ref[[3]]) - (7.5 * v_ref[[1]]) + 1))

    #Emissividade Enb
    v_enb <- 0.97 + 0.0033 * v_lai
    v_enb[v_ndvi < 0 | v_lai > 2.99] <- 0.98

    #Emissividade Eo
    v_eo <- 0.95 + 0.01 * v_lai
    v_eo[v_ndvi < 0 | v_lai > 2.99] <- 0.98

    #Temperatura de Superf�cie em Kelvin (TS)
    v_ts <- k2 / log( (v_enb * k1 / ts_rad) + 1)

    #Radia��o de onda longa emitida pela superf�cie (RLsup)
    v_rlsup <- v_eo * 5.67 * 10 ^ -8 * v_ts ^ 4

    #Emissividade atmosf�rica (Ea)
    # C�u Claro
    v_ea <- 0.85 * (-1 * log(values(raster_transmissivity))) ^ 0.09

    #Radia��o de onda longa emitida pela atmosfera (RLatm)
    v_rlatm <- v_ea * 5.67 * 10 ^ -8 *
                (station_weather_table$V7[2] + 273.15) ^ 4

    #Saldo de radia��o Instant�nea (Rn)
    v_rn <- v_rs - v_rs * v_alb + v_rlatm - v_rlsup - (1 - v_eo) * v_rlatm
    v_rn[v_rn < 0] <- 0

    #Fluxo de Calor no Solo (G)
    v_g_1 <- (n_ndvi >= 0) * (
        (
            (v_ts - 273.15) *
            (0.0038 + 0.0074 * v_alb) *
            (1 - 0.98 * v_ndvi ^ 4)
        ) * v_rn
    )
    v_g_2 <- (v_ndvi < 0) * (0.5 * v_rn)
    v_g <- v_g_1 + v_g_2
    v_g[v_g < 0] <- 0

    alb <- raster(raster_transmissivity)
    values(alb) <- v_alb
    Rn <- raster(raster_transmissivity)
    values(Rn) <- v_rn
    TS <- raster(raster_transmissivity) 
    values(TS) <- v_ts
    NDVI <- raster(raster_transmissivity) 
    values(NDVI) <- v_ndvi
    EVI <- raster(raster_transmissivity)
    values(EVI) <- v_evi
    LAI <- raster(raster_transmissivity)
    values(LAI) <- v_lai
    G <- raster(raster_transmissivity)
    values(G) <- v_g

    return(stack(Rn, TS, NDVI, EVI, LAI, G, alb))
}

phase2 <- function(raster_stack, station_weather_table, constantes) {
	v_rn <- values(raster_stack[[1]])
	Rn <- raster_stack[[1]]
	v_ts <- values(raster_stack[[2]])
	TS <- raster_stack[[2]]
	v_ndvi <- values(raster_stack[[3]])
	NDVI <- raster_stack[[3]]
	v_evi <- values(raster_stack[[4]])
	EVI <- raster_stack[[4]]
	v_lai <- values(raster_stack[[5]])
	LAI <- raster_stack[[5]]
	v_g <- values(raster_stack[[6]])
	G <- raster_stack[[6]]
	v_alb <- values(raster_stack[[7]])
	alb <- raster_stack[[7]]
	
	########################Selection of reference pixels###################################
	#Candidates hot Pixel
	v_ho <- v_rn - v_g # Era raster - leio so o vetor. HO <- VETOR
	x <- v_ts[
        (v_nvdi > 0.15 & !is.na(v_ndvi) &
        (v_ndvi < 0.20 & !is.na(v_ndvi)))
    ] # Retorna Vetor
	x <- x[x > 273.16]
	TS_c_hot <- sort(x)[round(0.95 * length(x))] # Retorna um valor
	HO_c_hot <- v_ho[
        (v_ndvi > 0.15 & !is.na(v_ndvi)) &
        (v_ndvi < 0.20 & !is.na(v_ndvi)) &
        v_ts == TS_c_hot
    ] # Retorna um valor
	if (length(HO_c_hot) == 1) {
        ll_hot <- which(v_ts == TS_c_hot & v_ho == HO_c_hot)
	    xy_hot <- xyFromCell(TS, ll_hot)
	    ll_hot_f <- cbind(as.vector(xy_hot[1, 1]),
	                      as.vector(xy_hot[1, 2]))
	} else {
	    HO_c_hot_min <- sort(HO_c_hot)[round(0.25 * length(HO_c_hot))]
	    HO_c_hot_max <- sort(HO_c_hot)[round(0.75 * length(HO_c_hot))]
	    ll_hot <- which(v_ts == TS_c_hot & v_ho > HO_c_hot_min & v_ho < HO_c_hot_max)
	    xy_hot <- xyFromCell(TS, ll_hot)
	    NDVI_hot <- extract(NDVI, xy_hot, buffer = 105)
	    NDVI_hot_2 <- NDVI_hot[!sapply(NDVI_hot, is.null)]
	    NDVI_hot_cv <- sapply(NDVI_hot_2, sd, na.rm=TRUE) / 
                       sapply(NDVI_hot_2, mean, na.rm=TRUE)
	    i_NDVI_hot_cv <- which.min(NDVI_hot_cv)
	    ll_hot_f <- cbind(as.vector(xy_hot[i_NDVI_hot_cv, 1]),
	                      as.vector(xy_hot[i_NDVI_hot_cv, 2]))
	}
	
	#Candidatos a Pixel frio
	x <- v_ts[(v_ndvi < 0 & !is.na(v_ndvi))  & !is.na(v_ho)]
	x <- x[x > 273.16]
	TS_c_cold <- sort(x)[round(0.5 * length(x))]
	HO_c_cold <- v_ho[(v_ndvi < 0 & !is.na(v_ndvi)) & v_ts == TS_c_cold & !is.na(v_ho)]
	if (length(HO_c_cold) == 1){
	  ll_cold <- which(v_ts == TS_c_cold & v_ho == HO_c_cold)
	  xy_cold <- xyFromCell(TS, ll_cold)
	  ll_cold_f <- cbind(as.vector(xy_cold[1, 1]),
	                     as.vector(xy_cold[1, 2]))
	} else {
	    HO_c_cold_min <- sort(HO_c_cold)[round(0.25 * length(HO_c_cold))]
	    HO_c_cold_max <- sort(HO_c_cold)[round(0.75 * length(HO_c_cold))]
	    ll_cold <- which(v_ts == TS_c_cold & (v_ho > HO_c_cold_min &
                        !is.na(v_ho)) & (v_ho < HO_c_cold_max & !is.na(v_ho)))
	    xy_cold <- xyFromCell(TS, ll_cold)
	    NDVI_cold <- extract(NDVI, xy_cold, buffer = 105)
	    NDVI_cold_2 <- NDVI_cold[!sapply(NDVI_cold, is.null)]
	
	    #Maximum number of neighboring pixels with $NVDI < 0$
	    t <- function(x) { sum(x < 0, na.rm = TRUE) }
	    n_neg_NDVI <- sapply(NDVI_cold_2, t)
	    i_NDVI_cold <- which.max(n_neg_NDVI)
	    ll_cold_f <- cbind(as.vector(xy_cold[i_NDVI_cold, 1]),
	                       as.vector(xy_cold[i_NDVI_cold, 2]))
	}
	
	#Location of reference pixels (hot and cold)
	ll_ref <- rbind(ll_hot_f[1, ], ll_cold_f[1, ])
	colnames(ll_ref) <- c("long", "lat")
	rownames(ll_ref) <- c("hot", "cold")
	
	####################################################################################
	
	#Weather station data
	x <- 3 # Wind speed sensor Height (meters)
	hc <- 0.2 #Vegetation height (meters)
	Lat<-  station_weather_table$V4[1] #Station Latitude
	# Long<- station_weather_table$V5[1] #Station Longitude
	
	#Surface roughness parameters in station
	zom_est <- hc * 0.12
	azom <- -3    #Parameter for the Zom image
	bzom <- 6.47  #Parameter for the Zom image
	F_int <- 0.16  #internalization factor for Rs 24 calculation (default value)
	
	#friction velocity at the station (ustar.est)
	ustar_est <- constantes$k * station_weather_table$V6[2] / 
                    log( (x) / zom_est)
	
	#velocity 200 meters
	u200 <- ustar_est / constantes$k * log(200 / zom_est)
	
	#zom for all pixels
	zom <- exp(azom + bzom * v_ndvi) #rASTER  MUDEI PARA VETOR
	
	#Initial values
    # friction velocity for all pixels #RASTER - VETOR 
	ustar <- raster(NDVI)
	values(ustar) <- constantes$k * u200 / (log(200 / zom))
    v_ustar <- values(ustar)
    # aerodynamic resistance for all pixels #RASTER - VETOR
	rah <- raster(NDVI)
	values(rah) <- (log(2 / 0.1)) / (v_ustar * constantes$k)
    v_rah <- values(rah)

	base_ref <- stack(NDVI, TS, Rn, G, ustar, rah) # RASTER
	nbase <- c("NDVI", "TS", "Rn", "G", "ustar", "rah")
	names(base_ref) <- nbase
	value_pixels_ref <- extract(base_ref, ll_ref)
	rownames(value_pixels_ref) <- c("hot", "cold")
	H_hot <- value_pixels_ref["hot", "Rn"] - value_pixels_ref["hot", "G"]  
	value_pixel_rah <- value_pixels_ref["hot", "rah"]
	
	i <- 1
	Erro<-TRUE
	
	#Beginning of the cycle stability
	while(Erro) {
	    rah_hot_0 <- value_pixel_rah[i] #VALOR
	    #Hot and cold pixels      
	    dt_hot <- H_hot * rah_hot.0 / (constantes$rho * constantes$cp) #VALOR                  
	    b <- dt_hot / (value_pixels_ref["hot","TS"] - value_pixels_ref["cold","TS"])  #VALOR
	    a <- -b * (value_pixels_ref["cold","TS"] - 273.15) #VALOR                          
	    #All pixels
	    H <- constantes$rho * constantes$cp * (a + b * (v_ts - 273.15)) / v_rah   # RASTER   - vetor                               
	    L <- -1 * (
            (constantes$rho * constantes$cp * v_ustar ^ 3 * v_ts) /
            (constantes$k * constantes$g * H)
        ) # RASTER - VETOR                       
	    y_0_1 <- (1 - 16 * 0.1 / L) ^ 0.25 # RASTER - VETOR 
	    y_2 <- (1 - 16 * 2 / L) ^ 0.25      # RASTER                                        
	    x200 <- (1 - 16 * 200 / L) ^ 0.25    # RASTER                                       
	    
        psi_0.1 <- 2 * log((1 + y_0_1 ^ 2) / 2)  # RASTER                                    
	    psi_0.1[L > 0 & !is.na(L)] <- -5 * (0.1 / L[L > 0 & !is.na(L)]) # RASTER # mUDAR A LOGICA 
	    
        psi_2<-2*log((1+y_2^2)/2)      # RASTER
	    psi_2[L>0 &!is.na(L) ]<--5*(2/L[L>0 &!is.na(L)])      # RASTER
	    
        psi_200<-2*log((1+x200)/2)+log((1+x200^2)/2)-2*atan(x200)+0.5*pi  # RASTER
	    psi_200[L>0 &!is.na(L) ]<--5*(2/L[(L>0 &!is.na(L))]) # RASTER
	    
        ustar<-k*u200/(log(200/zom)-psi_200)  # RASTER            # Velocidade de fric??o para todos os pixels
	    rah<-NDVI
	    rah[]<-(log(2/0.1)-psi_2+psi_0.1)/(ustar*k)  # RASTER       # Resist?ncia aerodin?mica para todos os pixels
	    rah.hot<-extract(rah,matrix(ll_ref["hot",],1,2)) #VALOR
	    value.pixel.rah<-c(value.pixel.rah,rah.hot)  #VALOR
	    Erro<-(abs(1-rah.hot.0/rah.hot)>=0.05)
	    i<-i+1
	}
	proc.time()
	
	#End sensible heat flux (H)
	
	#Hot and cold pixels
	dt.hot<-H.hot*rah.hot/(rho*cp)                  
	b<-dt.hot/(value.pixels.ref["hot","TS"]-value.pixels.ref["cold","TS"]) 
	a<- -b*(value.pixels.ref["cold","TS"]-273.15)                          
	proc.time()
	
	#All pixels
	
	H<-rho*cp*(a+b*(TS[]-273.15))/rah[] #Vetor 
	H[(H>(Rn[]-G[]) &!is.na(H))]<-(Rn[]-G[])[(H>(Rn[]-G[]) &!is.na(H))] # Vetor
	proc.time()

	#Instant latent heat flux (LE)
	LE<-Rn[]-G[]-H
	
	#Upscalling temporal
	dr<-(1/d_sun_earth$dist[Dia.juliano])^2 #Inverse square of the distance on Earth-SOL
	sigma<-0.409*sin(((2*pi/365)*Dia.juliano)-1.39) # Declination Solar (rad)
	phi<-(pi/180)*Lat #Solar latitude in degrees
	omegas<-acos(-tan(phi)*tan(sigma)) #Angle Time for sunsets (rad)
	Ra24h<-(((24*60/pi)*Gsc*dr)*(omegas*sin(phi)*
	        sin(sigma)+cos(phi)*cos(sigma)*sin(omegas)))*(1000000/86400)
	proc.time()
	
	#Short wave radiation incident in 24 hours (Rs24h)
	Rs24h<-F_int*sqrt(max(table.sw$V7[])-min(table.sw$V7[]))*Ra24h
	
	FL<-110                                
	Rn24h_dB<-(1-alb[])*Rs24h-FL*Rs24h/Ra24h         # Method of Bruin #VETOR
	
	#Evapotranspiration fraction Bastiaanssen
	EF<-NDVI
	EF[]<-LE/(Rn[]-G[])
	
	#Sensible heat flux 24 hours (H24h)
	H24h_dB<-(1-EF[])*Rn24h_dB
	
	#Latent Heat Flux 24 hours (LE24h)
	LE24h_dB<-EF[]*Rn24h_dB
	
	#Evapotranspiration 24 hours (ET24h)
	ET24h_dB<-NDVI
	ET24h_dB[]<-LE24h_dB*86400/((2.501-0.00236* (max(table.sw$V7[])+min(table.sw$V7[]))/2)*10^6)

    return(stack(EF, ET24h_dB))
}

######################### 3. Main ######################################

returned_values <- load_image_information(dados)
raster_stack <- returned_values$raster_stack
fmask <- returned_values$fmask
output_path <- returned_values$output_path
file_name <- returned_values$file_name
sensor_num <- returned_values$sensor_num
WRSPR <- returned_values$WRSPR
sensor_params <- returned_values$sensor_params
Dia_juliano <- returned_values$Dia_juliano
station_weather_table <- returned_values$station_weather_table
tdim <- returned_values$tdim
costheta <- returned_values$costheta
MTL <- returned_values$MTL
rm(returned_values)
proc.time()

raster_stack <- filter_clouds(
    raster_stack,
    fmask,
    sensor_num
)
proc.time()

# change projection (UTM para GEO)
raster_stack <- project_raster(raster_stack, constantes)
proc.time()

boundingbox <- read_bounding_box(WRSPR, constantes)
proc.time()

raster_elevation <- read_elevation(raster_stack, boundingbox, constantes)
proc.time()

raster_stack <- resample_raster(raster_stack, raster_elevation, constantes)
proc.time()

raster_transmissivity <- calc_transmissivity(raster_elevation)
proc.time()

tryCatch(
    raster_stack <- evalWithTimeout(
        phase1(
            raster_stack,
            raster_transmissivity,
            sensor_num,
            sensor_params,
            d_sun_earth$dist[Dia_juliano],
            station_weather_table,
            costheta,
            MTL,
            constantes
        ),
        timeout = 7200
    ),
    TimeoutException = function(ex) {
        cat("Output landsat timedout. Exiting with 124 code...");
        quit("no", 124, FALSE)
    }
)
proc.time()

raster_stack <- mask_raster(raster_stack, boundingbox, constantes)
proc.time()

names <- c(
    "Rn",
    "TS",
    "NDVI",
    "EVI",
    "LAI",
    "G",
    "alb"
)
write_raster(
    raster_stack,
    names,
    raster_elevation,
    file_name,
    output_path,
    tdim
)
proc.time()