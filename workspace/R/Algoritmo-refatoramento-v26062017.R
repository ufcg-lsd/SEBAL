########################################################################################
#                                                                                      #
#                         EU BRAZIL Cloud Connect                                      #
#                                                                                      #
#                                                                                      #
########################################################################################

options(echo = TRUE)
rm(list = ls())

# for now, this will be here
install.packages("snow", repos = "https://vps.fmvz.usp.br/CRAN/")
install.packages("R.utils", repos = "https://vps.fmvz.usp.br/CRAN/")

library(R.utils)
library(raster)
library(rgdal)
library(maptools)
library(ncdf4)
library(sp)
args <- commandArgs(trailingOnly = TRUE)
WD <- args[1]
setwd(WD) # Working Directory

# changing raster tmpdir
rasterOptions(tmpdir = paste("", "tmp", "rasterTmp", sep = "/"))

source("landsat.R")
# Ler dist?ncia realtiva Sol a Terra
load("d_sun_earth.RData")
# Data
dados <- read.csv("dados.csv", sep = ";", stringsAsFactors=FALSE)
####################################constantes##########################################
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

get_file_informations <- function(dados) {
    #Images file reading
    dir <- dados$File.images[1]
    #MTL File
    rows <- 140
    if (sensors == 8) rows <- 190
    MTL <- read.table(
        dados$MTL[1],
        skip = 0,
        nrows = 140,
        sep = "=",
        quote = "''",
        as.is = TRUE
    )
    #Image name that will be processed
    fic <- substr(
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
    sensors <- as.numeric(substr(fic, 3, 3))
    #MTL File
    if (sensors == 8) {
        MTL <- read.table(
            dados$MTL[1],
            skip = 0,
            nrows = 190,
            sep = "=",
            quote = "''",
            as.is = TRUE
        )
    }
    #WRSPR
    wrspr <- substr(fic, 4, 9)
    #Images year
    year <- as.numeric(substr(fic, 10, 13))
    #Julian Day
    julian_day <- as.numeric(substr(fic, 14, 16))
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
    costheta <- sin( sun_elevation * pi / 180)
    #Reading image file
    fichs_imagens <- list.files(
        path = dir,
        pattern = "*.TIF",
        full.names = TRUE
    )
    if (sensors == 8) st <- stack(as.list(fichs_imagens[c(4:9, 2)]))
    if (sensors == 7) st <- stack(as.list(fichs_imagens[1:8]))
    if (sensors == 5) st <- stack(as.list(fichs_imagens[1:7]))

    acquired_date <- as.Date(
        MTL$V2[
            MTL$V1 == grep(
                pattern = "DATE_ACQUIRED",
                MTL$V1,
                value = TRUE
            )
        ]
    )
    days_since_1970 <- as.numeric(acquired_date)
    tdim <- ncdim_def(
        "time",
        "days since 1970-1-1",
        days_since_1970,
        unlim = TRUE,
        create_dimvar = TRUE,
        "standard",
        "time"
    )

    s_p <- NULL
    if (sensors == 8) s_p <- read.csv(
        "parametros do sensor/parametrosdosensorLC.csv",
        sep = ";",
        stringsAsFactors = FALSE
    )
    if (sensors == 7) s_p <- read.csv(
        "parametros do sensor/parametrosdosensorETM.csv",
        sep = ";",
        stringsAsFactors = FALSE
    )
    if (year < 1992 & sensors == 5) s_p <- read.csv(
        "parametros do sensor/parametrosdosensorTM1.csv",
        sep = ";",
        stringsAsFactors = FALSE
    )
    if (year > 1992 & sensors == 5) s_p <- read.csv(
        "parametros do sensor/parametrosdosensorTM2.csv",
        sep = ";",
        stringsAsFactors = FALSE
    )

    n_fmask <- length(fichs_imagens)
    Fmask <- raster(fichs_imagens[[n_fmask]])

    return(list(
        "sensors" = sensors,
        "wrspr" = wrspr,
        "files" = fic,
        "year" = year,
        "julian_day" = julian_day,
        "raster_stack" = st,
        "costheta" = costheta,
        "tdim" = tdim,
        "mtl" = MTL,
        "sensor_parameters" = s_p,
        "Fmask" = Fmask
    ))
}
returned_values <- get_file_informations(dados)
#Sensor Number
sensors <- returned_values$sensors
#WRSPR
wrspr <- returned_values$wrspr
#Image name that will be processed
files <- returned_values$files
#Images year
year <- returned_values$year
#Julian Day
julian_day <- returned_values$julian_day
#Reading image file
raster_stack <- returned_values$raster_stack
#Fmask
Fmask <- returned_values$Fmask
#From SUN ELEVATION
costheta <- returned_values$costheta
#Time image
tdim <- returned_values$tdim
#MTL File
mtl <- returned_values$mtl
sensor_parameters <- returned_values$sensor_parameters
rm(returned_values)
proc.time()
print("DATA")

identify_clouds <- function(rast, mask, sensors) {
    if (sensors != 8) threshold <- 672 else threshold <- 2720
    rast[mask != threshold] <- NaN
    return(rast)
}
raster_stack <- identify_clouds(raster_stack, Fmask, sensors)

change_raster_projection <- function(rast, constantes) {
    beginCluster(constantes$clusters)
    res <- projectRaster(rast, crs = constantes$WGS84)
    endCluster()
    return(res)
}
# change projection (UTM para GEO)
raster_stack <- change_raster_projection(raster_stack, constantes)
proc.time()
print("PROJECTION")

get_bounding_box <- function(constantes, wrspr) {
    boxes <- paste("wrs2_asc_desc/wrs2_asc_desc.shp")
    bounding_boxes <- readShapePoly(boxes, proj4string = CRS(constantes$WGS84))
    bounding_box <- bounding_boxes[bounding_boxes@data$WRSPR == wrspr, ]
    return(bounding_box)
}
#Reading Bounding Box
bounding_box <- get_bounding_box(constantes, wrspr)

get_elevation_raster <- function(file, bounding_box, ele_res, constantes) {
    fic <- paste(file)
    elevation <- raster(fic)
    beginCluster(constantes$clusters)
    elevation <- crop(elevation, extent(bounding_box))
    endCluster()
    elevation_aux <- raster(elevation)
    res(elevation_aux) <- ele_res
    return(list(
        "elevation" = elevation,
        "elevation_aux" = elevation_aux
    ))
}
returned_values <- get_elevation_raster(
    "Elevation/srtm_29_14.tif",
    bounding_box,
    res(raster_stack),
    constantes
)
#Reading Elevation
elevation <- returned_values$elevation
elevation_aux <- returned_values$elevation_aux
rm(returned_values)
proc.time()
print("FMASK-BB-ELEVATION")

resample_image <- function(fic1, fic2, method, constantes) {
    beginCluster(constantes$clusters)
    tryCatch({
            evalWithTimeout(
                res <- resample(fic1, fic2, "method" = method),
                timeout = 3600
            )
        }
        , TimeoutException = function(ex) {
            cat("Image resample timedout. Exiting with 124 code...\n")
            quit("no", 124, FALSE)
        }
    )
    endCluster()
    return(res)
}

# Resample images
elevation <- resample_image(elevation, elevation_aux, "ngb", constantes)
raster_stack <- resample_image(raster_stack, elevation, "ngb", constantes)
proc.time()
print("RESAMPLING")

read_station_weather_data <- function(dados) {
    sw <- dados$File.Station.Weather[1]
    table <- (read.csv(
        sw,
        sep = ";",
        header = FALSE,
        stringsAsFactors = FALSE)
    )
    return(table)
}
#Reading file Station weather
station_weather_table <- read_station_weather_data(dados) #linha 143

calc_transmissivity <- function(elevation) {
    tal <- 0.75 + 2 * 10 ^ -5 * elevation
    return(tal)
}
#Transmissivity 
transmissivity <- calc_transmissivity(elevation)
proc.time()
print("STATION-TRANSMISSIVITY")

get_satelite_information <- function(
        sensors,
        sensors_param,
        images,
        costheta,
        mtl,
        tal,
        swt,
        sun_dist
    ) {
    if (sensors < 8){
        # Radi�ncia
        rad <- list()
        if (sensors == 5) r <- 7 else r <- 8
        for (i in 1:r){
            rad[[i]] <- image.rec[[i]] *
                sensors_param$Grescale[i] +
                sensors_param$Brescale[i]
            rad[[i]][rad[[i]] < 0] <- 0
        }
        rad_ts <- rad[[6]]

        #Reflect�ncia
        ref < -list()
        for (i in 1:r){
            ref[[i]] <- pi * rad[[i]] * sun_dist ^ 2 /
                        (sensors_param$ESUN[i] * costheta)
        }

        if (sensors == 5) {
            k1 <- 607.76
            k2 <- 1260.56
        } else {
            k1 <- 666.09
            k2 <- 1282.71
        }
    } else {
        r <- 6
        multiplicative <- as.numeric(
            mtl$V2[mtl$V1 == grep(
                pattern = "RADIANCE_MULT_BAND_10 ",
                mtl$V1,
                value = TRUE)
            ]
        )
        additive <- as.numeric(
            mtl$V2[mtl$V1 == grep(
                pattern = "RADIANCE_ADD_BAND_10 ",
                mtl$V1,
                value = TRUE)
            ]
        )
        # Radi�ncia
        rad <- images[[7]] * multiplicative + additive
        rad_ts <- rad

        #Reflect�ncia
        ref <- list()
        for (i in 1:6) {
            ref[[i]] <- (images[[i]] * 0.00002 - 0.1) / costheta
        }

        k1 <- 774.8853
        k2 <- 1321.0789
    }

    #Albedo de superf�cie
    alb <- ref[[1]] * sensors_param$wb[1] +
           ref[[2]] * sensors_param$wb[2] +
           ref[[3]] * sensors_param$wb[3] +
           ref[[4]] * sensors_param$wb[4] +
           ref[[5]] * sensors_param$wb[5] +
           ref[[r]] * sensors_param$wb[r]
    alb <- (alb - 0.03) / tal ^ 2

    #Radia��o de onda curta incidente (Rs)
    Rs <- (1367 * costheta * tal) / (sun_dist ^ 2)

    #NDVI,SAVI,LAI e EVI
    NDVI <- (ref[[4]] - ref[[3]]) / (ref[[4]] + ref[[3]])
    EVI <- 2.5 * (
        (ref[[4]] - ref[[3]]) /
        (ref[[4]] + (6 * ref[[3]]) - (7.5 * ref[[1]]) + 1)
    )
    SAVI <- ( (1 + 0.05) * (ref[[4]] - ref[[3]]) ) /
        (0.05 + ref[[4]] + ref[[3]])
    LAI <- SAVI
    LAI[SAVI > 0.687] <- 6
    LAI[SAVI <= 0.687] <- -log( (0.69 - SAVI[SAVI <= 0.687]) / 0.59) / 0.91
    LAI[SAVI < 0.1] <- 0

    #Emissividade Enb
    Enb <- 0.97 + 0.0033 * LAI
    Enb[NDVI < 0 | LAI > 2.99] <- 0.98

    #Emissividade Eo
    Eo <- 0.95 + 0.01 * LAI
    Eo[NDVI < 0 | LAI > 2.99] <- 0.98

    #Temperatura de Superf�cie em Kelvin (TS)
    TS <- k2 / log( (Enb * k1 / rad_ts) + 1)

    #Radia��o de onda longa emitida pela superf�cie (RLsup)
    RLsup <- Eo * 5.67 * 10 ^ -8 * TS ^ 4

    #Emissividade atmosf�rica (Ea)
    Ea <- 0.85 * ( -1 * log(tal)) ^ 0.09

    #Radia��o de onda longa emitida pela atmosfera (RLatm)
    RLatm <- Ea * 5.67 * 10 ^ -8 *
            (swt$V7[2] + 273.15) ^ 4

    #Saldo de radia��o Instant�nea (Rn)
    Rn <- Rs - Rs * alb + RLatm - RLsup - (1 - Eo) * RLatm
    Rn[Rn < 0] <- 0

    #Fluxo de Calor no Solo (G)
    G <- ( (TS - 273.15) *
            (0.0038  + 0.0074  * alb) *
            (1 - 0.98 * NDVI ^ 4)) * Rn
    G[NDVI < 0] <- 0.5 * Rn[NDVI < 0]
    G[G < 0] <- 0

    return(stack(Rn, TS, NDVI, EVI, LAI, G, alb))
}
tryCatch({
    evalWithTimeout(
        raster_stack <- get_satelite_information(
            sensors,
            sensor_parameters,
            raster_stack,
            costheta,
            mtl,
            transmissivity,
            station_weather_table,
            d_sun_earth$dist[julian_day]
        ),
        timeout = 7200)
    }
    , TimeoutException = function(ex) {
        cat("Output landsat timedout. Exiting with 124 code...\n");
        quit("no", 124, FALSE)
    }
)
proc.time()
print("FASE-1")

apply_mask <- function(dest, source, constantes) {
    beginCluster(constantes$clusters)
    output <- mask(dest, source)
    endCluster()
    return(output)
}
tryCatch({
    evalWithTimeout(
        raster_stack <- apply_mask(raster_stack, bounding_box, constantes),
        timeout = 10800
    )}
    , TimeoutException = function(ex) {
        cat("Output Fmask timedout. Exiting with 124 code...\n");
        quit("no", 124, FALSE)
    }
)
proc.time()
print("MASK")

rm(transmissivity)
rm(elevation_aux)
rm(bounding_box)
rm(fmask)

get_latitude_and_longitude <- function(dados, fic, elevation) {
    var_output <- paste(dados$Path.Output[1], "/", fic, "_alb.nc", sep = "")
    nc <- nc_open(
        var_output,
        write = TRUE,
        readunlim = FALSE,
        verbose = TRUE,
        auto_GMT = FALSE,
        suppress_dimvals = FALSE
    )

    #Getting lat and lon values from old NetCDF
    old_lat <- ncvar_get(nc, "lat", start = 1, count = elevation@nrows)
    old_lon <- ncvar_get(nc, "lon", start = 1, count = elevation@ncols)

    #Defining latitude and longitude dimensions
    dim_lat_def <- ncdim_def("lat", "degrees", old_lat,
                            unlim = FALSE, longname = "latitude")
    dim_lon_def <- ncdim_def("lon", "degrees", old_lon,
                            unlim = FALSE, longname = "longitude")
    nc_close(nc)
    return(list(
        "dim_lat" = dim_lat_def,
        "dim_lon" = dim_lon_def
    ))
}

update_file <- function(dados, layer, fic, elevation, tdim, lat_lon) {
    #Opening old NetCDF
    var_output <- paste(
        dados$Path.Output[1],
        "/",
        fic,
        "_",
        layer,
        ".nc",
        sep = ""
    )
    nc <- nc_open(
        var_output,
        write = TRUE,
        readunlim = FALSE,
        verbose = TRUE,
        auto_GMT = FALSE,
        suppress_dimvals = FALSE
    )

    #New file name
    file_output <- var_output
    old_values <- ncvar_get(nc, fic)
    new_values <- ncvar_def(
        layer,
        "daily",
        list(
            lat_lon$dim_lon,
            lat_lon$dim_lat,
            tdim
        ),
        longname = layer,
        missval = NaN,
        prec = "double"
    )
    nc_close(nc)
    new_NCDF4 <- nc_create(file_output, new_values)
    ncvar_put(
        new_NCDF4,
        layer,
        old_values,
        start = c(1, 1, 1),
        count = c(elevation@ncols, elevation@nrows, 1)
    )
    nc_close(new_NCDF4)
}

write_raster <- function(dados, output, fic, na, elevation, tdim) {
    output_path <- paste(dados$Path.Output[1], "/", fic, ".nc", sep = "")
    names(output) <- na
    writeRaster(
        output,
        output_path,
        overwrite = TRUE,
        format = "CDF",
        varname = fic,
        varunit = "daily",
        longname = fic,
        xname = "lon",
        yname = "lat",
        bylayer = TRUE,
        suffix = "names"
    )

    lat_lon <- get_latitude_and_longitude(dados, fic, elevation)

    for (i in 1:length(na)) {
        update_file(dados, na[i], fic, elevation, tdim, lat_lon)
    }

}
write_raster(
    dados,
    raster_stack,
    files,
    c("Rn", "TS", "NDVI", "EVI", "LAI", "G", "alb"),
    elevation,
    tdim
)
proc.time()
print("WRITE-1")

get_candidate_pixel <- function(Ho, TS, NDVI, cold = FALSE) {
    if (cold) {
        x <- TS[NDVI < 0 & !is.na(Ho)]
	    x <- x[x > 273.16]
	    TS_cand <- sort(x)[round(0.5 * length(x))]
	    Ho_cand <- Ho[NDVI < 0 & TS == TS_cand & !is.na(Ho)]
    } else {
        x <- TS[NDVI > 0.15 & NDVI < 0.20]
	    x <- x[x > 273.16]
	    TS_cand <- sort(x)[round(0.95 * length(x))]
	    Ho_cand <- Ho[NDVI > 0.15 & NDVI < 0.20 & TS == TS_cand]
    }

    if (length(Ho_cand) == 1) {
	    ll_cand <- which(TS[] == TS_cand & Ho[] == Ho_cand)
	    xy_cand <- xyFromCell(TS, ll_cand)
	    ll_cand_f <- cbind(as.vector(xy_cand[1, 1]),
	                       as.vector(xy_cand[1, 2]))
	} else {
        Ho_cand_sorted <- sort(Ho_cand)
	    Ho_cand_min <- Ho_cand_sorted[round(0.25 * length(Ho_cand))]
	    Ho_cand_max <- Ho_cand_sorted[round(0.75 * length(Ho_cand))]
	    ll_cand <- which(TS[] == TS_cand & Ho[] > Ho_cand_min & Ho[] < Ho_cand_max)
	    xy_cand <- xyFromCell(TS, ll_cand)
	    NDVI_cand <- extract(NDVI, xy_cand, buffer = 105)
	    NDVI_cand_2 <- NDVI_cand[!sapply(NDVI_cand, is.null)]

        if (cold) {
            #Maximum number of neighboring pixels with $NVDI < 0$
	        t <- function(x) {
                sum(x < 0, na.rm = TRUE)
            }
	        n_neg_NDVI <- sapply(NDVI_cand_2, t)
	        i_NDVI_cand <- which.max(n_neg_NDVI)
	        ll_cand_f <- cbind(as.vector(xy_cand[i_NDVI_cand, 1]),
	                           as.vector(xy_cand[i_NDVI_cand, 2]))
        } else {
	        NDVI_cand_cv <- sapply(NDVI_cand_2, sd  , na.rm = TRUE) /
                            sapply(NDVI_cand_2, mean, na.rm = TRUE)
	        i_NDVI_cand_cv <- which.min(NDVI_cand_cv)
	        ll_cand_f <- cbind(as.vector(xy_cand[i_NDVI_cand_cv, 1]),
	                           as.vector(xy_cand[i_NDVI_cand_cv, 2]))
        }
	}
    return(ll_cand_f)
}

phase2 <- function(stack, swt, constantes, sun_dist, julian_day) {
    ##################Selection of reference pixels##################
    Rn <- stack[[1]]
    TS <- stack[[2]]
    NDVI <- stack[[3]]
    # EVI <- stack[[4]] # never used
    # LAI <- stack[[5]] # never used
    G <- stack[[6]]
    alb <- stack[[7]]

    Ho <- Rn - G

    #Candidates hot Pixel
    ll_hot_f  <- get_candidate_pixel(Ho, TS, NDVI)
    #Candidates cold Pixel
    ll_cold_f <- get_candidate_pixel(Ho, TS, NDVI, cold = TRUE)

    #Location of reference pixels (hot and cold)
    ll_ref <- rbind(ll_hot_f[1, ], ll_cold_f[1, ])
    colnames(ll_ref) <- c("long", "lat")
    rownames(ll_ref) <- c("hot", "cold")

    #######################################################
    #Weather station data
    x <- 3 # Wind speed sensor Height (meters)
    hc <- 0.2 #Vegetation height (meters)
    Lat <-  swt$V4[1] #Station Latitude
    # Long <- swt$V5[1] #Station Longitude # never used

    #Surface roughness parameters in station
    zom_est <- hc * 0.12
    azom <- -3    #Parameter for the Zom image
    bzom <- 6.47  #Parameter for the Zom image
    F_int <- 0.16  #internalization factor for Rs 24 calculation (default value)

    #friction velocity at the station (ustar_est)
    ustar_est <- constantes$k * swt$V6[2] / log(x / zom_est)

    #velocity 200 meters
    u200 <- ustar_est / constantes$k * log(200 / zom_est)

    #zom for all pixels
    zom <- exp(azom + bzom * NDVI)

    #Initial values
    # friction velocity for all pixels
    ustar <- constantes$k * u200 / log(200 / zom)
    # aerodynamic resistance for all pixels
    rah <- log(2 / 0.1) / (ustar * constantes$k)
    base_ref <- stack(NDVI, TS, Rn, G, ustar, rah)
    nbase <- c("NDVI", "TS", "Rn", "G", "ustar", "rah")
    names(base_ref) <- nbase
    beginCluster(constantes$clusters)
    value_pixels_ref <- extract(base_ref, ll_ref)
    endCluster()
    rownames(value_pixels_ref) <- c("hot", "cold")
    H_hot <- value_pixels_ref["hot", "Rn"] -
             value_pixels_ref["hot", "G"]  
    value_pixel_rah <- value_pixels_ref["hot", "rah"]

    i <- 1
    Erro <- TRUE

    #Beginning of the cycle stability
    while (Erro) {
        rah_hot_0 <- value_pixel_rah[i]
        #Hot and cold pixels      
        dt_hot <- H_hot * rah_hot_0 / (constantes$rho * constantes$cp)
        b <- dt_hot / (value_pixels_ref["hot", "TS"] -
                       value_pixels_ref["cold", "TS"])
        a <- -b * (value_pixels_ref["cold", "TS"] - 273.15)
        #All pixels
        H <- constantes$rho * constantes$cp * (a + b * (TS - 273.15)) / rah
        L <- -1 * (
            (constantes$rho * constantes$cp * ustar ^ 3 * TS) /
            (constantes$k * constantes$g * H)
        )
        y_0_1 <- (1 - 16 * 0.1 / L) ^ 0.25
        y_2 <- (1 - 16 * 2 / L) ^ 0.25
        x200 <- (1 - 16 * 200 / L) ^ 0.25

        psi_0_1 <- 2 * log( (1 + y_0_1 ^ 2) / 2)
        psi_0_1[L > 0] <- -5 * (0.1 / L[L > 0])

        psi_2 <- 2 * log( (1 + y_2 ^ 2) / 2)
        psi_2[L > 0] <- -5 * (2 / L[L > 0])

        psi_200 <- 2 * log( (1 + x200) / 2) + log( (1 + x200 ^ 2) / 2) -
                   2 * atan(x200) + 0.5 * pi
        psi_200[L > 0] <- -5 * (2 / L[L > 0])

        # Velocidade de fric??o para todos os pixels
        ustar <- k * u200 / (log(200 / zom) - psi_200)
        # Resist?ncia aerodin?mica para todos os pixels
        rah <- (log(2 / 0.1) - psi_2 + psi_0_1) / (ustar * k)
        rah_hot <- extract(rah, matrix(ll_ref["hot", ], 1, 2))
        value_pixel_rah <- c(value_pixel_rah, rah_hot)
        Erro <- (abs(1 - rah_hot_0 / rah_hot) >= 0.05)
        i <- i + 1
    }

    #End sensible heat flux (H)

    #Hot and cold pixels
    dt_hot <- H_hot * rah_hot / (constantes$rho * constantes$cp)
    b <- dt_hot /
        (value_pixels_ref["hot", "TS"] - value_pixels_ref["cold", "TS"])
    a <- -b * (value_pixels_ref["cold", "TS"] - 273.15)

    #All pixels
    H <- constantes$rho * constantes$cp * (a + b * (TS - 273.15)) / rah
    H[H > Ho] <- Ho[H > Ho]

    #Instant latent heat flux (LE)
    LE <- Ho - H

    #Upscalling temporal
    #Inverse square of the distance on Earth-SOL
    dr <- (1 / sun_dist) ^ 2
    # Declination Solar (rad)
    sigma <- 0.409 * sin( ( (2 * pi / 365) * julian_day) - 1.39)
    #Solar latitude in degrees
    phi <- (pi / 180) * Lat
    #Angle Time for sunsets (rad)
    omegas <- acos( -tan(phi) * tan(sigma))
    Ra24h <- ( ( (24 * 60 / pi) * constantes$gsc * dr) * (omegas * sin(phi) *
            sin(sigma) + cos(phi) * cos(sigma) * sin(omegas))) *
            (10000 / 864)

    #Short wave radiation incident in 24 hours (Rs24h)
    Rs24h <- F_int * sqrt(max(swt$V7[]) - min(swt$V7[])) * Ra24h

    FL <- 110
    # Method of Bruin
    Rn24h_dB <- (1 - alb) * Rs24h - FL * Rs24h / Ra24h

    #Evapotranspiration fraction Bastiaanssen
    EF <- LE / Ho

    #Sensible heat flux 24 hours (H24h)
    H24h_dB <- (1 - EF) * Rn24h_dB

    #Latent Heat Flux 24 hours (LE24h)
    LE24h_dB <- EF * Rn24h_dB

    #Evapotranspiration 24 hours (ET24h)
    ET24h_dB <- LE24h_dB * 86400 / ( (2.501 - 0.00236 *
                    (max(swt$V7[]) + min(swt$V7[])) / 2) * 10 ^ 6)

    evapo_trans <- stack(EF, ET24h_dB)
    return(evapo_trans)
}

evapo_trans <- NULL
tryCatch({
    evalWithTimeout(
        evapo_trans <- phase2(
            raster_stack,
            station_weather_table,
            constantes,
            d_sun_earth$dist[julian_day],
            julian_day
        ),
        timeout=7200
    )}
    , TimeoutException=function(ex) {
        cat("Image phase two processing timedout. Exiting with 124 code...\n");
        quit("no", 124, FALSE)
    }
)
proc.time()
print("FASE-2")
write_raster(dados, evapo_trans, files, c("EF", "ET24h"), elevation, tdim)
proc.time()
print("WRITE-2")