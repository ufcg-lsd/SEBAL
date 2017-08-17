########################################################################################
#                                                                                      #
#                         EU BRAZIL Cloud Connect                                      #
#                                                                                      #
#                                                                                      #
########################################################################################

options(echo = TRUE)
rm(list = ls())

library(R.utils)
library(raster)
library(rgdal)
library(maptools)
library(ncdf4)
library(sp)
args <- commandArgs(trailingOnly = TRUE)
WD <- args[1]
setwd(WD) # Working Directory

tmpdir_name <- paste(c("", "tmp", "rasterTmp"), collapse = "/")
# changing raster tmpdir
rasterOptions(
    tmpdir = tmpdir_name
)
removeTmpFiles(h = 0)

load("d_sun_earth.RData")             # Ler dist?ncia realtiva Sol a Terra
dados <- read.csv("dados.csv", sep = ";", stringsAsFactors = FALSE) # Data

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
######################### Reading sensor parameters#####################################

get_file_informations <- function(dados) {
    #Images file reading
    dir <- dados$File.images[1]
    #Number of file characters
    m <- nchar(dados$File.images)
    #Image name that will be processed
    fic <- substring(dir, m[1] - 20)
    #Sensor Number
    sensors <- as.numeric(substr(fic, 3, 3))
    #WRSPR
    wrspr <- substr(fic, 4, 9)
    #Images year
    year <- as.numeric(substr(fic, 10, 13))
    #Julian Day
    julian_day <- as.numeric(substr(fic, 14, 16))
    #Reading image file
    fichs.imagens <- list.files(
        path = dir,
        pattern = "*.TIF",
        full.names = TRUE
    )
    if (sensors == 8) st <- stack(as.list(fichs.imagens[c(4:9, 2)]))
    if (sensors == 7) st <- stack(as.list(fichs.imagens[1:8]))
    if (sensors == 5) st <- stack(as.list(fichs.imagens[1:7]))

    rows <- 140
    if (sensors == 8) rows <- 180
    MTL <- read.table(dados$MTL[1],
                      skip = 0,
                      nrows = rows,
                      sep = "=",
                      quote = "''",
                      as.is = TRUE)
    sun_elevation <- as.numeric(MTL$V2[MTL$V1 == grep(
        pattern = "SUN_ELEVATION",
        MTL$V1,
        value = TRUE)
    ])
    costheta <- sin( sun_elevation * pi / 180)
    acquired_date <- as.Date(MTL$V2[MTL$V1 == grep(
        pattern = "DATE_ACQUIRED",
        MTL$V1,
        value = TRUE)]
    )
    date1970 <- as.Date("1970-01-01")
    days_since_1970 <- as.numeric(acquired_date) - as.numeric(date1970)
    tdim <- ncdim_def(
        "time",
        "days since 1970-1-1",
        days_since_1970,
        unlim = TRUE,
        create_dimvar = TRUE,
        "standard",
        "time"
    )

    return(list(
        "sensors" = sensors,
        "wrspr" = wrspr,
        "files" = fic,
        "year" = year,
        "julian_day" = julian_day,
        "raster_stack" = st,
        "costheta" = costheta,
        "tdim" = tdim,
        "mtl" = MTL
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
#From SUN ELEVATION
costheta <- returned_values$costheta
#Time image
tdim <- returned_values$tdim
#MTL File
mtl <- returned_values$mtl
rm(returned_values)

get_sensors_parameters <- function(sensors, year) {
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
    return(s_p)
}
sensor_parameters <- get_sensors_parameters(sensors, year)
proc.time()
print("DATA")

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

#Reading Fmask
fmask <- change_raster_projection(raster(dados$File.Fmask[1]), constantes)

get_elevation_raster <- function(file, bounding_box, fmask, constantes) {
    fic <- paste(file)
    elevation <- raster(fic)
    beginCluster(constantes$clusters)
    elevation <- crop(elevation, extent(bounding_box))
    endCluster()
    elevation_aux <- raster(elevation)
    res(elevation_aux) <- res(fmask)
    return(list(
        "elevation" = elevation,
        "elevation_aux" = elevation_aux
    ))
}
returned_values <- get_elevation_raster(
    "Elevation/srtm_29_14.tif",
    bounding_box,
    fmask,
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
fmask <- resample_image(fmask, elevation, "ngb", constantes)
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
    tal <- elevation
    tal[] <- 0.75 + 2 * 10 ^ -5 * elevation[]
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
    raster_reference <- images[[1]]
    if (sensors < 8){
        # Radi�ncia
        rad_data <- list()
        if (sensors == 5) r <- 7 else r <- 8
        for (i in 1:r) {
            rad_data[[i]] <- images[[i]][] *
                sensors_param$Grescale[i] +
                sensors_param$Brescale[i]
            rad_data[[i]][rad_data[[i]] < 0] <- 0
        }
        rad7 <- rad_data[[6]]

        #Reflect�ncia
        ref_data <- list()
        for (i in 1:r) {
            ref_data[[i]] <- pi *
                rad_data[[i]] *
                sun_dist ^ 2 /
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
        rad_data <- images[[7]][] * multiplicative + additive
        rad7 <- rad_data

        #Reflect�ncia
        ref_data <- list()
        for (i in 1:6) {
            ref_data[[i]] <- (images[[i]][] * 0.00002 - 0.1) / costheta
        }

        k1 <- 774.8853
        k2 <- 1321.0789
    }
    tal_data <- tal[]

    #Albedo de superf�cie
    alb_data <- ref_data[[1]] * sensors_param$wb[1] +
           ref_data[[2]] * sensors_param$wb[2] +
           ref_data[[3]] * sensors_param$wb[3] +
           ref_data[[4]] * sensors_param$wb[4] +
           ref_data[[5]] * sensors_param$wb[5] +
           ref_data[[r]] * sensors_param$wb[r]
    alb_data <- (alb_data - 0.03) / tal_data ^ 2

    #Radia��o de onda curta incidente (Rs)
    Rs_data <- (1367 * costheta * tal_data) / (sun_dist ^ 2)

    #NDVI,SAVI,LAI e EVI
    NDVI_data <- (ref_data[[4]] - ref_data[[3]]) /
                 (ref_data[[4]] + ref_data[[3]])
    EVI_data <- 2.5 * (
        (ref_data[[4]] - ref_data[[3]]) /
        (ref_data[[4]] + (6 * ref_data[[3]]) - (7.5 * ref_data[[1]]) + 1)
    )
    SAVI_data <- ( (1 + 0.05) * (ref_data[[4]] - ref_data[[3]]) ) /
        (0.05 + ref_data[[4]] + ref_data[[3]])

    LAI_data <- SAVI_data
    SAVI_subset1 <- SAVI_data > 0.687 & !is.na(SAVI_data)
    SAVI_subset2 <- SAVI_data <= 0.687 & !is.na(SAVI_data)
    SAVI_subset3 <- SAVI_data < 0.1 & !is.na(SAVI_data)
    LAI_data[SAVI_subset1] <- 6
    LAI_data[SAVI_subset2] <- -log(
                                (0.69 - SAVI_data[SAVI_subset2]) / 0.59
                            ) / 0.91
    LAI_data[SAVI_subset3] <- 0

    #Emissividade Enb
    Enb_data <- 0.97 + 0.0033 * LAI_data
    NDVI_subset_1 <- NDVI_data < 0 & !is.na(NDVI_data)
    LAI_subset_1 <- LAI_data > 2.99 & !is.na(LAI_data)
    Enb_data[NDVI_subset_1 | LAI_subset_1] <- 0.98

    #Emissividade Eo
    Eo_data <- 0.95 + 0.01 * LAI_data
    Eo_data[NDVI_subset_1 | LAI_subset_1] <- 0.98

    #Temperatura de Superf�cie em Kelvin (TS)
    TS_data <- k2 / log( (Enb_data * k1 / rad7) + 1)

    #Radia��o de onda longa emitida pela superf�cie (RLsup)
    RLsup_data <- Eo_data * 5.67 * 10 ^ -8 * TS_data ^ 4

    #Emissividade atmosf�rica (Ea)
    Ea_data <- 0.85 * ( -1 * log(tal_data)) ^ 0.09

    #Radia��o de onda longa emitida pela atmosfera (RLatm)
    RLatm_data <- Ea_data * 5.67 * 10 ^ -8 *
            (swt$V4[2] + 273.15) ^ 4

    #Saldo de radia��o Instant�nea (Rn)
    Rn_data <- Rs_data - Rs_data * alb_data + RLatm_data -
                RLsup_data - (1 - Eo_data) * RLatm_data
    Rn_subset <- Rn_data < 0 & !is.na(Rn_data)
    Rn_data[Rn_subset] <- 0

    #Fluxo de Calor no Solo (G)
    G_data <- ( (TS_data - 273.15) *
                (0.0038 + 0.0074 * alb_data) *
                (1 - 0.98 * NDVI_data ^ 4)) * Rn_data
    NDVI_subset <- NDVI_data < 0 & !is.na(NDVI_data)
    G_data[NDVI_subset] <- 0.5 * Rn_data[NDVI_subset]
    G_subset <- G_data < 0 & !is.na(G_data)
    G_data[G_subset] <- 0

    Rn <- raster_reference
    Rn[] <- Rn_data
    TS <- raster_reference
    TS[] <- TS_data
    NDVI <- raster_reference
    NDVI[] <- NDVI_data
    EVI <- raster_reference
    EVI[] <- EVI_data
    LAI <- raster_reference
    LAI[] <- LAI_data
    G <- raster_reference
    G[] <- G_data
    alb <- raster_reference
    alb[] <- alb_data
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

clear_unnecessary_cells <- function(st, fmask) {
    mask <- fmask[]
    for (i in 1:nlayers(st)) {
        m <- st[[i]][]
        m[mask > 1] <- NaN
        st[[i]][] <- m
    }
    return(st)
}
raster_stack <- clear_unnecessary_cells(raster_stack, fmask)
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

phase2 <- function(stack, swt, constantes, sun_dist, julian_day) {
    ##################Selection of reference pixels##################
    raster_Rn <- stack[[1]]
    Rn <- raster_Rn[]
    raster_TS <- stack[[2]]
    TS <- raster_TS[]
    raster_NDVI <- stack[[3]]
    NDVI <- raster_NDVI[]
    # EVI <- stack[[4]] # never used
    # LAI <- stack[[5]] # never used
    raster_G <- stack[[6]]
    G <- raster_G[]
    raster_alb <- stack[[7]]
    alb <- raster_alb[]

    #Candidates hot Pixel
    Ho <- Rn - G
    y <- Ho[NDVI > 0.15 & NDVI < 0.19]
    x <- TS[NDVI > 0.15 & NDVI < 0.19]
    TS_hot <- quantile(x[x > 273.16], 0.8, na.rm = TRUE)

    i <- 0.1
    Erro <- TRUE
    while (Erro){
        Ho_hot <- median(y[x > TS_hot - i & x < TS_hot + i], na.rm = TRUE)
        Erro <- is.na(Ho_hot)
        i <- i + 0.1
    }

    TS_Ho <- abs(TS - TS_hot) + abs(Ho - Ho_hot)
    Cand_hot <- sort(TS_Ho[])[1:20]
    ll_hot <- numeric()
    for (k in 0:length(Cand_hot)) {
        ll_hot <- c(ll_hot, which(TS_Ho[] == Cand_hot[k]))
    }

    raster_TS_Ho <- stack[[1]]
    raster_TS_Ho[] <- TS_Ho
    beginCluster(constantes$clusters)
    xy_hot <- xyFromCell(raster_TS_Ho, ll_hot)
    NDVI_hot <- extract(raster_NDVI, xy_hot, buffer = 105)
    endCluster()

    NDVI_hot_2 <- NDVI_hot[!sapply(NDVI_hot, is.null)]
    NDVI_hot_cv <- sapply(NDVI_hot_2, sd, na.rm = TRUE) /
                   sapply(NDVI_hot_2, mean, na.rm = TRUE)
    NDVI_hot_cv_min <- sort(NDVI_hot_cv)
    i_NDVI_hot_cv <- which(NDVI_hot_cv[] == NDVI_hot_cv_min[1])

    beginCluster(constantes$clusters)
    TQ_hot <- extract(raster_TS, xy_hot)
    endCluster()

    TQ_hot <- TQ_hot[i_NDVI_hot_cv[1]]
    ll_hot_f <- cbind(as.vector(xy_hot[i_NDVI_hot_cv[1], 1]),
                      as.vector(xy_hot[i_NDVI_hot_cv[1], 2]))

    #Candidates cold Pixel
    z <- TS[NDVI < 0 & TS > 273.16]
    TS_cold <- quantile(z, 0.08, na.rm = TRUE)
    TS_dif <- abs(TS - TS_cold)
    Cand_cold <- sort(TS_dif[])[1:20]
    
    ll_cold <- numeric()
    for (k in 0:length(Cand_cold)) {
        ll_cold <- c(ll_cold, which(TS_dif[] == Cand_cold[k]))
    }

    raster_TS_dif <- stack[[1]]
    raster_TS_dif[] <- TS_dif
    beginCluster(constantes$clusters)
    xy_cold <- xyFromCell(raster_TS_dif, ll_cold)
    NDVI_cold <- extract(raster_NDVI, xy_cold, buffer = 120)
    endCluster()

    NDVI_cold_2 <- NDVI_cold[!sapply(NDVI_cold, is.null)]
    NDVI_cold_cv <- sapply(NDVI_cold_2, sd, na.rm = TRUE) /
                    sapply(NDVI_cold_2, mean, na.rm = TRUE)
    NDVI_cold_cv_positive <- NDVI_cold_cv[NDVI_cold_cv > 0]
    NDVI_cold_cv_min <- sort(NDVI_cold_cv_positive)
    i_NDVI_cold <- which(NDVI_cold_cv[] == NDVI_cold_cv_min[1])

    beginCluster(constantes$clusters)
    TQ_cold <- extract(raster_TS, xy_cold)
    endCluster()

    TQ_cold <- TQ_cold[i_NDVI_cold[1]]
    ll_cold_f <- cbind(as.vector(xy_cold[i_NDVI_cold[1], 1]),
                       as.vector(xy_cold[i_NDVI_cold[1], 2]))

    #Location of reference pixels (hot and cold)
    ll_ref <- rbind(ll_hot_f[1, ], ll_cold_f[1, ])
    colnames(ll_ref) <- c("long", "lat")
    rownames(ll_ref) <- c("hot", "cold")

    #######################################################
    #Weather station data
    x <- 3 # Wind speed sensor Height (meters)
    hc <- 0.2 #Vegetation height (meters)
    Lat <-  swt$V4 #Station Latitude
    # Long <- swt$V5 #Station Longitude # never used

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
    raster_ustar <- stack[[1]]
    raster_ustar[] <- ustar
    raster_rah <- stack[[1]]
    raster_rah[] <- rah
    base_ref <- stack(raster_NDVI, raster_TS, raster_Rn, raster_G, raster_ustar, raster_rah)
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

        L_subset <- L < 0 & !is.na(L)
        psi_0_1 <- 2 * log( (1 + y_0_1 ^ 2) / 2)
        psi_0_1[L_subset] <- -5 * (0.1 / L[L_subset])

        psi_2 <- 2 * log( (1 + y_2 ^ 2) / 2)
        psi_2[L_subset] <- -5 * (2 / L[L_subset])

        psi_200 <- 2 * log( (1 + x200) / 2) + log( (1 + x200 ^ 2) / 2) -
                   2 * atan(x200) + 0.5 * pi
        psi_200[L_subset] <- -5 * (2 / L[L_subset])

        # Velocidade de fric??o para todos os pixels
        ustar <- k * u200 / (log(200 / zom) - psi_200)
        # Resist?ncia aerodin?mica para todos os pixels
        rah <- (log(2 / 0.1) - psi_2 + psi_0_1) / (ustar * k)
        raster_rah <- stack[[1]]
        raster_rah[] <- rah
        beginCluster(constantes$clusters)
        rah_hot <- extract(raster_rah, matrix(ll_ref["hot", ], 1, 2))
        endCluster()
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
    RNG <- Rn - G
    H <- constantes$rho * constantes$cp * (a + b * (TS - 273.15)) / rah
    H_subset <- H > RNG & !is.na(H)
    H[H_subset] <- RNG[H_subset]

    #Instant latent heat flux (LE)
    LE <- Rn - G - H

    #Upscalling temporal
    dr <- (1 / sun_dist) ^ 2 #Inverse square of the distance on Earth-SOL
    # Declination Solar (rad)
    sigma <- 0.409 * sin( ( (2 * pi / 365) * julian_day) - 1.39)
    phi <- (pi / 180) * Lat #Solar latitude in degrees
    #Angle Time for sunsets (rad)
    omegas <- acos( -tan(phi) * tan(sigma))
    Ra24h <- ( ( (24 * 60 / pi) * constantes$gsc * dr) * (omegas * sin(phi) *
            sin(sigma) + cos(phi) * cos(sigma) * sin(omegas))) *
            (1000000 / 86400)

    #Short wave radiation incident in 24 hours (Rs24h)
    Rs24h <- F_int * sqrt(max(swt$V7[]) - min(swt$V7[])) * Ra24h

    FL <- 110
    # Method of Bruin
    Rn24h_dB <- (1 - alb) * Rs24h - FL * Rs24h / Ra24h

    #Evapotranspiration fraction Bastiaanssen
    EF <- LE / (Rn - G)

    #Sensible heat flux 24 hours (H24h)
    H24h_dB <- (1 - EF) * Rn24h_dB

    #Latent Heat Flux 24 hours (LE24h)
    LE24h_dB <- EF * Rn24h_dB

    #Evapotranspiration 24 hours (ET24h)
    ET24h_dB <- LE24h_dB * 86400 / ( (2.501 - 0.00236 *
                    (max(swt$V7[]) + min(swt$V7[])) / 2) * 10 ^ 6)

    real_EF <- stack[[1]]
    real_EF[] <- EF
    real_ET24h_dB <- stack[[1]]
    real_ET24h_dB[] <- EF
    evapo_trans <- stack(real_EF, real_ET24h_dB)
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