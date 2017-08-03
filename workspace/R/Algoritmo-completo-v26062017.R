########################################################################################
#                                                                                      #
#                         EU BRAZIL Cloud Connect                                      #
#                                                                                      #
#                                                                                      #
########################################################################################

options(echo=TRUE)
rm(list=ls())

# for now, this will be here
install.packages("snow", repos="https://vps.fmvz.usp.br/CRAN/")
install.packages("R.utils", repos="https://vps.fmvz.usp.br/CRAN/")

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
k<-0.41        #Von K?rm?n
g<-9.81        #Gravity
rho<- 1.15     #Air density
cp<- 1004      #Specific heat of air
Gsc<-0.082     #Solar constant (0.0820 MJ m-2 min-1)
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
MTL<-read.table(dados$MTL[1],skip=0,nrows=140,sep="=", quote = "''",as.is=TRUE) #MTL File
fic<-substr(MTL$V2[MTL$V1==grep(pattern ="LANDSAT_SCENE_ID",MTL$V1, value = T)],3,23)
n.sensor<-as.numeric(substr(fic,3,3))             #Sensor Number
if (n.sensor==8) MTL<-read.table(dados$MTL[1],skip=0,nrows=190,sep="=", quote = "''",as.is=TRUE)
WRSPR<-substr(fic,4,9)                            #WRSPR
Ano<-as.numeric(substr(fic,10,13))                #Images year
Dia.juliano<-as.numeric(substr(fic,14,16))        #Julian Day
sun_elevation<-as.numeric(MTL$V2[MTL$V1==grep(pattern ="SUN_ELEVATION",MTL$V1, value = TRUE)])
costheta<-sin(sun_elevation*pi/180)               #From SUN ELEVATION
if (n.sensor==8) p.s<-p.s.LC
if (n.sensor==7) p.s<-p.s.ETM
if (Ano < 1992 & n.sensor==5) p.s<-p.s.TM1 
if (Ano > 1992 & n.sensor==5) p.s<-p.s.TM2

#Time image
acquired_date<-as.Date(MTL$V2[MTL$V1==grep(pattern ="DATE_ACQUIRED",MTL$V1, value = TRUE)])
daysSince1970 <- as.numeric(acquired_date)
tdim<-ncdim_def("time","days since 1970-1-1",daysSince1970,unlim=TRUE,create_dimvar=TRUE,"standard","time")

#Reading image file
fichs.imagens <- list.files(path = fic.dir, pattern ="*.TIF", full.names = TRUE)
if (n.sensor==8) fic.st <- stack(as.list(fichs.imagens[c(4:9,2)]))####Fmask
if (n.sensor==7) fic.st <- stack(as.list(fichs.imagens[1:8]))####Fmask
if (n.sensor==5) fic.st <- stack(as.list(fichs.imagens[1:7]))

###Identifier of clouds and shadows
proc.time()
n.fmask<-length(fichs.imagens)
Fmask <- raster(fichs.imagens[[n.fmask]])
if (n.sensor !=8) fic.st[Fmask!=672]<-NaN else
  fic.st[Fmask!=2720]<-NaN
endCluster()
proc.time()

beginCluster(clusters)
fic.st<-projectRaster(fic.st,crs = WGS84)            # change projection (UTM para GEO)
endCluster()
proc.time()

#Reading Bounding Box
fic.bounding.boxes<-paste("wrs2_asc_desc/wrs2_asc_desc.shp")
BoundingBoxes<-readShapePoly(fic.bounding.boxes,proj4string = CRS(WGS84))
BoundingBox<-BoundingBoxes[BoundingBoxes@data$WRSPR==WRSPR,]

#Reading Elevation
fic.elevation<-paste("Elevation/srtm_29_14.tif")
raster.elevation <- raster(fic.elevation)
beginCluster(clusters)
raster.elevation <- crop(raster.elevation,extent(BoundingBox))
endCluster()
proc.time()

raster.elevation.aux<-raster(raster.elevation)
res(raster.elevation.aux)<-res(fic.st)

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

output.path<-paste(dados$Path.Output[1],"/",fic,".nc",sep = "")
outputWriteRaster <- function() {
  names(output)<-c("Rn","TS","NDVI","EVI","LAI","G","alb")
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

phase2 <- function() {
	########################Selection of reference pixels###################################
	Rn<-output[[1]]
	TS<-output[[2]]
	NDVI<-output[[3]]
	EVI<-output[[4]]
	LAI<-output[[5]]
	G<-output[[6]]
	alb<-output[[7]]
	
	#Candidates hot Pixel
	HO<-Rn-G
	x<-TS[NDVI>0.15 & NDVI<0.20]
	x<-x[x>273.16]
	TS.c.hot<-sort(x)[round(0.95*length(x))]
	HO.c.hot<-HO[NDVI>0.15 & NDVI<0.20 & TS==TS.c.hot]
	if (length(HO.c.hot)==1){
	  ll.hot<-which(TS[]==TS.c.hot & HO[]==HO.c.hot)
	  xy.hot <- xyFromCell(TS, ll.hot)
	  ll.hot.f<-cbind(as.vector(xy.hot[1,1]),
	                  as.vector(xy.hot[1,2]))
	  }else{
	HO.c.hot.min<-sort(HO.c.hot)[round(0.25*length(HO.c.hot))]
	HO.c.hot.max<-sort(HO.c.hot)[round(0.75*length(HO.c.hot))]
	ll.hot<-which(TS[]==TS.c.hot & HO[]>HO.c.hot.min & HO[]<HO.c.hot.max)
	xy.hot <- xyFromCell(TS, ll.hot)
	NDVI.hot<-extract(NDVI,xy.hot, buffer=105)
	NDVI.hot.2<-NDVI.hot[!sapply(NDVI.hot, is.null)]
	NDVI.hot.cv <- sapply(NDVI.hot.2,sd, na.rm=TRUE)/sapply(NDVI.hot.2, mean, na.rm=TRUE)
	i.NDVI.hot.cv<-which.min(NDVI.hot.cv)
	ll.hot.f<-cbind(as.vector(xy.hot[i.NDVI.hot.cv,1]),
	                as.vector(xy.hot[i.NDVI.hot.cv,2]))
	}
	
	#Candidatos a Pixel frio
	
	x<-TS[NDVI<0 & !is.na(HO)]
	x<-x[x>273.16]
	TS.c.cold<-sort(x)[round(0.5*length(x))]
	HO.c.cold<-HO[NDVI<0 & TS==TS.c.cold & !is.na(HO)]
	if (length(HO.c.cold)==1){
	  ll.cold<-which(TS[]==TS.c.cold & HO[]==HO.c.cold)
	  xy.cold <- xyFromCell(TS, ll.cold)
	  ll.cold.f<-cbind(as.vector(xy.cold[1,1]),
	                  as.vector(xy.cold[1,2]))
	}else{
	HO.c.cold.min<-sort(HO.c.cold)[round(0.25*length(HO.c.cold))]
	HO.c.cold.max<-sort(HO.c.cold)[round(0.75*length(HO.c.cold))]
	ll.cold<-which(TS[]==TS.c.cold & HO[]>HO.c.cold.min & HO[]<HO.c.cold.max)
	xy.cold <- xyFromCell(TS, ll.cold)
	NDVI.cold<-extract(NDVI,xy.cold, buffer=105)
	NDVI.cold.2<-NDVI.cold[!sapply(NDVI.cold, is.null)]
	
	#Maximum number of neighboring pixels with $NVDI < 0$
	t<-function(x){ sum(x<0,na.rm = TRUE)}
	n.neg.NDVI<-sapply(NDVI.cold.2,t)
	i.NDVI.cold<-which.max(n.neg.NDVI)
	ll.cold.f<-cbind(as.vector(xy.cold[i.NDVI.cold,1]),
	                 as.vector(xy.cold[i.NDVI.cold,2]))
	}
	endCluster()
	
	#Location of reference pixels (hot and cold)
	ll_ref<-rbind(ll.hot.f[1,],ll.cold.f[1,])
	colnames(ll_ref)<-c("long", "lat")
	rownames(ll_ref)<-c("hot","cold")
	proc.time()
	
	####################################################################################
	
	#Weather station data
	x<-3 # Wind speed sensor Height (meters)
	hc<-0.2 #Vegetation height (meters)
	Lat<-  table.sw$V4[1]  #Station Latitude
	Long<- table.sw$V5[1] #Station Longitude
	
	#Surface roughness parameters in station
	zom.est<-hc*0.12
	azom<- -3    #Parameter for the Zom image
	bzom<- 6.47  #Parameter for the Zom image
	F_int<-0.16  #internalization factor for Rs 24 calculation (default value)
	proc.time()
	
	#friction velocity at the station (ustar.est)
	ustar.est<-k*table.sw$V6[2]/log((x)/zom.est)
	
	#velocity 200 meters
	u200<-ustar.est/k*log(200/zom.est)
	
	#zom for all pixels
	zom<-exp(azom+bzom*NDVI)
	
	#Initial values
	ustar<-k*u200/(log(200/zom))         # friction velocity for all pixels
	rah<-(log(2/0.1))/(ustar*k)          # aerodynamic resistance for all pixels
	base_ref<-stack(NDVI,TS,Rn,G,ustar,rah)
	nbase<-c("NDVI","TS","Rn","G")
	names(base_ref)<-c(nbase,"ustar","rah")
	value.pixels.ref<-extract(base_ref,ll_ref)
	rownames(value.pixels.ref)<-c("hot","cold")
	H.hot<-value.pixels.ref["hot","Rn"]-value.pixels.ref["hot","G"]  
	value.pixel.rah<-value.pixels.ref["hot","rah"]
	
	i<-1
	Erro<-TRUE
	proc.time()
	
	#Beginning of the cycle stability
	while(Erro){
	  rah.hot.0<-value.pixel.rah[i]
	  #Hot and cold pixels      
	  dt.hot<-H.hot*rah.hot.0/(rho*cp)                  
	  b<-dt.hot/(value.pixels.ref["hot","TS"]-value.pixels.ref["cold","TS"]) 
	  a<- -b*(value.pixels.ref["cold","TS"]-273.15)                          
	  #All pixels
	  H<-rho*cp*(a+b*(TS-273.15))/rah                                   
	  L<- -1*((rho*cp*ustar^3*TS)/(k*g*H))                              
	  y_0.1<-(1-16*0.1/L)^0.25
	  y_2<-(1-16*2/L)^0.25                                              
	  x200<-(1-16*200/L)^0.25                                           
	  psi_0.1<-2*log((1+y_0.1^2)/2)                                     
	  psi_0.1[L>0]<--5*(0.1/L[L>0])
	  psi_2<-2*log((1+y_2^2)/2)
	  psi_2[L>0]<--5*(2/L[L>0])
	  psi_200<-2*log((1+x200)/2)+log((1+x200^2)/2)-2*atan(x200)+0.5*pi  
	  psi_200[L>0]<--5*(2/L[L>0])
	  ustar<-k*u200/(log(200/zom)-psi_200)              # Velocidade de fric??o para todos os pixels
	  rah<-(log(2/0.1)-psi_2+psi_0.1)/(ustar*k)          # Resist?ncia aerodin?mica para todos os pixels
	  rah.hot<-extract(rah,matrix(ll_ref["hot",],1,2))
	  value.pixel.rah<-c(value.pixel.rah,rah.hot)
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
	H<-rho*cp*(a+b*(TS-273.15))/rah
	H[H>(Rn-G)]<-(Rn-G)[H>(Rn-G)]
	proc.time()
	
	#Instant latent heat flux (LE)
	LE<-Rn-G-H
	
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
	Rn24h_dB<-(1-alb)*Rs24h-FL*Rs24h/Ra24h         # Method of Bruin
	
	#Evapotranspiration fraction Bastiaanssen
	EF<-LE/(Rn-G)
	
	#Sensible heat flux 24 hours (H24h)
	H24h_dB<-(1-EF)*Rn24h_dB
	
	#Latent Heat Flux 24 hours (LE24h)
	LE24h_dB<-EF*Rn24h_dB
	
	#Evapotranspiration 24 hours (ET24h)
	ET24h_dB<-LE24h_dB*86400/((2.501-0.00236*
	                             (max(table.sw$V7[])+min(table.sw$V7[]))/2)*10^6)
	proc.time()
	
	output.evapo<-stack(EF,ET24h_dB)
	names(output.evapo)<-c('EF','ET24h')
	writeRaster(output.evapo,output.path, overwrite=TRUE, format="CDF", varname= fic,varunit="daily",
	            longname=fic, xname="lon",yname="lat",bylayer= TRUE, suffix="names")
	proc.time()
	
	#Opening old EF NetCDF
	var_output<-paste(dados$Path.Output[1],"/",fic,"_EF.nc",sep="")
	nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)
	
	#New EF file name
	file_output<-paste(dados$Path.Output[1],"/",fic,"_EF.nc",sep="")
	oldEFValues<-ncvar_get(nc,fic)
	newEFValues<-ncvar_def("EF","daily",list(dimLonDef,dimLatDef,tdim),longname="EF",missval=NaN,prec="double")
	nc_close(nc)
	newEFNCDF4<-nc_create(file_output,newEFValues)
	ncvar_put(newEFNCDF4,"EF",oldEFValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
	nc_close(newEFNCDF4)
	proc.time()
	
	#Opening old ET24h NetCDF
	var_output<-paste(dados$Path.Output[1],"/",fic,"_ET24h.nc",sep="")
	nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)
	
	#New ET24h file name
	file_output<-paste(dados$Path.Output[1],"/",fic,"_ET24h.nc",sep="")
	oldET24hValues<-ncvar_get(nc,fic)
	newET24hValues<-ncvar_def("ET24h","daily",list(dimLonDef,dimLatDef,tdim),longname="ET24h",missval=NaN,prec="double")
	nc_close(nc)
	newET24hNCDF4<-nc_create(file_output,newET24hValues)
	ncvar_put(newET24hNCDF4,"ET24h",oldET24hValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
	nc_close(newET24hNCDF4)
	proc.time()
}

tryCatch({
  res <- evalWithTimeout({
    phase2();
  }, timeout=5400);
}, TimeoutException=function(ex) {
  cat("Image phase two processing timedout. Exiting with 124 code...\n");
  quit("no", 124, FALSE)
})
