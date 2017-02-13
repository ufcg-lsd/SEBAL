########################################################################################
#                                                                                      #
#                         EU BRAZIL Cloud Connect                                      #
#                                                                                      #
#                                                                                      #
########################################################################################

options(echo=TRUE)
rm(list=ls())

# for now, this will be here
install.packages("snow", repos="http://nbcgib.uesc.br/mirrors/cran/")
install.packages("R.utils", repos="http://nbcgib.uesc.br/mirrors/cran/")

library(R.utils)
library(raster)
library(rgdal)
library(maptools)
library(ncdf4)
library(sp)
args = commandArgs(trailingOnly=TRUE)
WD<-args[1]
setwd(WD) # Working Directory

# changing raster tmpdir
rasterOptions(tmpdir="/mnt/rasterTmp")

source("landsat.R")
dados <- read.csv("dados.csv",sep=";", stringsAsFactors=FALSE) # Data
####################################constantes##########################################
clusters<-7    #Number of clusters used in image processing
######################### Reading sensor parameters#####################################
p.s.TM1<- read.csv("parametros do sensor/parametrosdosensorTM1.csv"
                   ,sep=";", stringsAsFactors=FALSE)
p.s.TM2<- read.csv("parametros do sensor/parametrosdosensorTM2.csv"
                   ,sep=";", stringsAsFactors=FALSE)
p.s.ETM<-read.csv("parametros do sensor/parametrosdosensorETM.csv"
                  ,sep=";", stringsAsFactors=FALSE)
p.s.LC<- read.csv("parametros do sensor/parametrosdosensorLC.csv"
                  ,sep=";", stringsAsFactors=FALSE)
load("d_sun_earth.RData")             # Ler dist?ncia realtiva Sol a Terra

#Set projection and spatial resolution
WGS84<- "+proj=longlat +datum=WGS84 +ellps=WGS84"

######################### Image Information ######################################
fic.dir<-dados$File.images[1]                     #Images file reading
m<-nchar(dados$File.images)                       #Number of file characters
fic<-substring(fic.dir,m[1]-20)                   #Image name that will be processed
n.sensor<-as.numeric(substr(fic,3,3))             #Sensor Number
WRSPR<-substr(fic,4,9)                            #WRSPR
Ano<-as.numeric(substr(fic,10,13))                #Images year
Dia.juliano<-as.numeric(substr(fic,14,16))        #Julian Day
MTL<-read.table(dados$MTL[1],skip=0,nrows=130,sep="=", quote = "''",as.is=TRUE) #MTL File
if (n.sensor==8) MTL<-read.table(dados$MTL[1],skip=0,nrows=180,sep="=", quote = "''",as.is=TRUE)
sun_elevation<-as.numeric(MTL$V2[MTL$V1==grep(pattern ="SUN_ELEVATION",
                                              MTL$V1, value = TRUE)])
costheta<-sin(sun_elevation*pi/180)               #From SUN ELEVATION
if (n.sensor==8) p.s<-p.s.LC
if (n.sensor==7) p.s<-p.s.ETM
if (Ano < 1992 & n.sensor==5) p.s<-p.s.TM1 
if (Ano > 1992 & n.sensor==5) p.s<-p.s.TM2

#Time image
acquired_date<-as.Date(MTL$V2[MTL$V1==grep(pattern ="DATE_ACQUIRED",MTL$V1, value = TRUE)])
date1970<-as.Date("1970-01-01")
date1970<-as.numeric(date1970) * 86400000
acquired_date<-as.numeric(acquired_date) * 86400000
daysSince1970 = (acquired_date - date1970)/(24*60*60*1000)
tdim<-ncdim_def("time","days since 1970-1-1",daysSince1970,unlim=TRUE,create_dimvar=TRUE,"standard","time")

#Reading image file
fichs.imagens <- list.files(path = fic.dir, pattern ="*.TIF", full.names = TRUE)
if (n.sensor==8) fic.st <- stack(as.list(fichs.imagens[c(4:9,2)]))
if (n.sensor==7) fic.st <- stack(as.list(fichs.imagens[1:8]))
if (n.sensor==5) fic.st <- stack(as.list(fichs.imagens[1:7]))
beginCluster(clusters)
fic.st<-projectRaster(fic.st,crs = WGS84)            # change projection (UTM para GEO)
endCluster()
proc.time()

#Reading Bounding Box
fic.bounding.boxes<-paste("wrs2_asc_desc/wrs2_asc_desc.shp")
BoundingBoxes<-readShapePoly(fic.bounding.boxes,proj4string = CRS(WGS84))
BoundingBox<-BoundingBoxes[BoundingBoxes@data$WRSPR==WRSPR,]


#Reading Fmask
Fmask <- raster(dados$File.Fmask[1])
beginCluster(clusters)
Fmask <- projectRaster(Fmask, crs = WGS84)
endCluster()
proc.time()

#Reading Elevation
fic.elevation<-paste("Elevation/srtm_29_14.tif")
raster.elevation <- raster(fic.elevation)
beginCluster(clusters)
raster.elevation <- crop(raster.elevation,extent(BoundingBox))
endCluster()
proc.time()

raster.elevation.aux<-raster(raster.elevation)
res(raster.elevation.aux)<-res(Fmask)

# Resample images
beginCluster(clusters)
raster.elevation<-resample(raster.elevation,raster.elevation.aux,method="ngb")
proc.time()

# See if timeouts presented here will be the default or distinct between sites
# timeout before = 2177.062
# timeout now is 3600 (cause: Azure slowness)
image.rec <- NULL;
imageResample <- function() {
  image_resample <- resample(fic.st,raster.elevation,method="ngb")
  return(image_resample)
}

res <- NULL;
tryCatch({
  res <- evalWithTimeout({
    image.rec <- imageResample();
  }, timeout=3600);
}, TimeoutException=function(ex) {
  cat("Image resample timedout. Exiting with 124 code...\n");
  quit("no", 124, FALSE)
})

proc.time()

Fmask <- resample(Fmask,raster.elevation,method="ngb")
endCluster()
proc.time()

#Reading file Station weather
fic.sw<-dados$File.Station.Weather[1]
table.sw<-(read.csv(fic.sw,sep=";", header=FALSE, stringsAsFactors=FALSE))

#Transmissivity 
tal<-0.75+2*10^-5*raster.elevation 
proc.time()

#Processamento da Fase 1
output <- NULL;
outputLandsat <- function() {
  output <- landsat()
  return(output)
}

# timeout before = 2665.151
# timeout now is 7200 (cause: Azure slowness)
res <- NULL;
tryCatch({
  res <- evalWithTimeout({
    output <- outputLandsat();
  }, timeout=7200);
}, TimeoutException=function(ex) {
  cat("Output landsat timedout. Exiting with 124 code...\n");
  quit("no", 124, FALSE)
})

proc.time()

outputMask <- function() {
  beginCluster(clusters)
  output<-mask(output, BoundingBox)
  endCluster()
  return(output)
}

# timeout before = 1716.853
# timeout now is 10800 (cause: Azure slowness)

res <- NULL;
tryCatch({
  res <- evalWithTimeout({
    output <- outputMask();
  }, timeout=10800);
}, TimeoutException=function(ex) {
  cat("Output Fmask timedout. Exiting with 124 code...\n");
  quit("no", 124, FALSE)
})

proc.time()

outputWriteRaster <- function() {
  output[Fmask>1]<-NaN
  names(output)<-c("Rn","TS","NDVI","EVI","LAI","G","alb")
  output.path<-paste(dados$Path.Output[1],"/",fic,".nc",sep = "")
  writeRaster(output,output.path, overwrite=TRUE, format="CDF", varname= fic,varunit="daily",
            longname=fic, xname="lon",yname="lat",bylayer= TRUE, suffix="names")
}

# timeout before = 1708.507
# timeout now is 10800 (cause: Azure slowness)

res <- NULL;
tryCatch({
  res <- evalWithTimeout({
    outputWriteRaster();
  }, timeout=10800);
}, TimeoutException=function(ex) {
  cat("Output write raster timedout. Exiting with 124 code...\n");
  quit("no", 124, FALSE)
})

proc.time()

#Opening old alb NetCDF
var_output<-paste(dados$Path.Output,"/",fic,"_alb.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)
proc.time()

#Getting lat and lon values from old NetCDF
oldLat<-ncvar_get(nc,"lat",start=1,count=raster.elevation@nrows)
oldLon<-ncvar_get(nc,"lon",start=1,count=raster.elevation@ncols)

#Defining latitude and longitude dimensions
dimLatDef<-ncdim_def("lat","degrees",oldLat,unlim=FALSE,longname="latitude")
dimLonDef<-ncdim_def("lon","degrees",oldLon,unlim=FALSE,longname="longitude")
proc.time()

#New alb file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_alb.nc",sep="")
oldAlbValues<-ncvar_get(nc,fic)
newAlbValues<-ncvar_def("alb","daily",list(dimLonDef,dimLatDef,tdim),longname="alb",missval=NaN, prec="double")
nc_close(nc)
newAlbNCDF4<-nc_create(file_output,newAlbValues)
ncvar_put(newAlbNCDF4,"alb",oldAlbValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newAlbNCDF4)
proc.time()

#Opening old EVI NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_EVI.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New EVI file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_EVI.nc",sep="")
oldEVIValues<-ncvar_get(nc,fic)
newEVIValues<-ncvar_def("EVI","daily",list(dimLonDef,dimLatDef,tdim),longname="EVI",missval=NaN,prec="double")
nc_close(nc)
newEVINCDF4<-nc_create(file_output,newEVIValues)
ncvar_put(newEVINCDF4,"EVI",oldEVIValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newEVINCDF4)
proc.time()

#Opening old G NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_G.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New G file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_G.nc",sep="")
oldGValues<-ncvar_get(nc,fic)
newGValues<-ncvar_def("G","daily",list(dimLonDef,dimLatDef,tdim),longname="G",missval=NaN,prec="double")
nc_close(nc)
newGNCDF4<-nc_create(file_output,newGValues)
ncvar_put(newGNCDF4,"G",oldGValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newGNCDF4)
proc.time()

#Opening old LAI NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_LAI.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New LAI file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_LAI.nc",sep="")
oldLAIValues<-ncvar_get(nc,fic)
newLAIValues<-ncvar_def("LAI","daily",list(dimLonDef,dimLatDef,tdim),longname="LAI",missval=NaN,prec="double")
nc_close(nc)
newLAINCDF4<-nc_create(file_output,newLAIValues)
ncvar_put(newLAINCDF4,"LAI",oldLAIValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newLAINCDF4)
proc.time()

#Opening old NDVI NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_NDVI.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New NDVI file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_NDVI.nc",sep="")
oldNDVIValues<-ncvar_get(nc,fic)
newNDVIValues<-ncvar_def("NDVI","daily",list(dimLonDef,dimLatDef,tdim),longname="NDVI",missval=NaN,prec="double")
nc_close(nc)
newNDVINCDF4<-nc_create(file_output,newNDVIValues)
ncvar_put(newNDVINCDF4,"NDVI",oldNDVIValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newNDVINCDF4)
proc.time()

#Opening old Rn NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_Rn.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New Rn file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_Rn.nc",sep="")
oldRnValues<-ncvar_get(nc,fic)
newRnValues<-ncvar_def("Rn","daily",list(dimLonDef,dimLatDef,tdim),longname="Rn",missval=NaN,prec="double")
nc_close(nc)
newRnNCDF4<-nc_create(file_output,newRnValues)
ncvar_put(newRnNCDF4,"Rn",oldRnValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newRnNCDF4)
proc.time()

#Opening old TS NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_TS.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New TS file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_TS.nc",sep="")
oldTSValues<-ncvar_get(nc,fic)
newTSValues<-ncvar_def("TS","daily",list(dimLonDef,dimLatDef,tdim),longname="TS",missval=NaN,prec="double")
nc_close(nc)
newTSNCDF4<-nc_create(file_output,newTSValues)
ncvar_put(newTSNCDF4,"TS",oldTSValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newTSNCDF4)
proc.time()
