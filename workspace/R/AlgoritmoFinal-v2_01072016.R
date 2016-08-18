########################################################################################
#                                                                                      #
#                         EU BRAZIL Cloud Connect                                      #
#                                                                                      #
#                                                                                      #
########################################################################################

options(echo=TRUE)
rm(list=ls())
library(raster)
library(rgdal)
library(maptools)
library(ncdf4)
library(sp)
args = commandArgs(trailingOnly=TRUE)
WD<-args[1]
#setwd("/home/ubuntu/R/") # Diretório
setwd(WD)
source("landsat.R")
dados <- read.csv("dados.csv",sep=";", stringsAsFactors=FALSE) # Data
####################################constantes##########################################
k<-0.41        #Von K?rm?n
g<-9.81        #Gravity
rho<- 1.15     #Air density
cp<- 1004      #Specific heat of air
Gsc<-0.082     #Solar constant (0.0820 MJ m-2 min-1)

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
MTL<-read.table(dados$MTL[1],skip=0,nrows=140,sep="=", quote = "''",as.is=TRUE) #Arquivo MTL
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
if (n.sensor==8) fic.st <- stack(as.list(fichs.imagens[c(2:7,10)]))
if (n.sensor==7) fic.st <- stack(as.list(fichs.imagens[1:8]))
if (n.sensor==5) fic.st <- stack(as.list(fichs.imagens[1:7]))
fic.st<-projectRaster(fic.st,crs = WGS84)            # change projection (UTM para GEO)
proc.time()

#Reading Bounding Box
fic.bounding.boxes<-paste("wrs2_asc_desc/wrs2_asc_desc.shp")
BoundingBoxes<-readShapePoly(fic.bounding.boxes,proj4string = CRS(WGS84))
BoundingBox<-BoundingBoxes[BoundingBoxes@data$WRSPR==WRSPR,]


#Reading Fmask
Fmask <- raster(dados$File.Fmask[1])
Fmask <- projectRaster(Fmask, crs = WGS84)
proc.time()

#Reading Elevation
fic.elevation<-paste("Elevation/srtm_29_14.tif")
raster.elevation <- raster(fic.elevation)
raster.elevation <- crop(raster.elevation,extent(BoundingBox))
raster.elevation.aux<-raster(raster.elevation)
res(raster.elevation.aux)<-res(Fmask)
proc.time()

# Resample images
raster.elevation<-resample(raster.elevation,raster.elevation.aux,method="ngb")
proc.time()

image.rec <- resample(fic.st,raster.elevation,method="ngb")
proc.time()

Fmask <- resample(Fmask,raster.elevation,method="ngb")
proc.time()

#Reading file Station weather
fic.sw<-dados$File.Station.Weather[1]
table.sw<-(read.csv(fic.sw,sep=";", header=FALSE, stringsAsFactors=FALSE))

#Transmissivity 
tal<-0.75+2*10^-5*raster.elevation 
proc.time()

#Processamento da Fase 1
output<-landsat()
proc.time()

output<-mask(output, BoundingBox)
proc.time()

output[Fmask>1]<-NaN
names(output)<-c("Rn","TS","NDVI","EVI","LAI","G","alb")
output.path<-paste(dados$Path.Output[1],"/",fic,".nc",sep = "")
writeRaster(output,output.path, overwrite=TRUE, format="CDF", varname= fic,varunit="daily",
            longname=fic, xname="lon",yname="lat",bylayer= TRUE, suffix="names")
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
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_alb.nc",sep="")

oldAlbValues<-ncvar_get(nc,fic)
newAlbValues<-ncvar_def("alb","daily",list(dimLonDef,dimLatDef,tdim),longname="alb",missval=NaN, prec="double")
newAlbNCDF4<-nc_create(file_output,newAlbValues)
ncvar_put(newAlbNCDF4,"alb",oldAlbValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newAlbNCDF4)
nc_close(nc)
proc.time()

#Opening old EVI NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_EVI.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New EVI file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_EVI.nc",sep="")

oldEVIValues<-ncvar_get(nc,fic)
newEVIValues<-ncvar_def("EVI","daily",list(dimLonDef,dimLatDef,tdim),longname="EVI",missval=NaN,prec="double")
newEVINCDF4<-nc_create(file_output,newEVIValues)
ncvar_put(newEVINCDF4,"EVI",oldEVIValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newEVINCDF4)
nc_close(nc)
proc.time()

#Opening old G NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_G.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New G file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_G.nc",sep="")

oldGValues<-ncvar_get(nc,fic)
newGValues<-ncvar_def("G","daily",list(dimLonDef,dimLatDef,tdim),longname="G",missval=NaN,prec="double")
newGNCDF4<-nc_create(file_output,newGValues)
ncvar_put(newGNCDF4,"G",oldGValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newGNCDF4)
nc_close(nc)
proc.time()

#Opening old LAI NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_LAI.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New LAI file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_LAI.nc",sep="")

oldLAIValues<-ncvar_get(nc,fic)
newLAIValues<-ncvar_def("LAI","daily",list(dimLonDef,dimLatDef,tdim),longname="LAI",missval=NaN,prec="double")
newLAINCDF4<-nc_create(file_output,newLAIValues)
ncvar_put(newLAINCDF4,"LAI",oldLAIValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newLAINCDF4)
nc_close(nc)
proc.time()

#Opening old NDVI NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_NDVI.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New NDVI file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_NDVI.nc",sep="")

oldNDVIValues<-ncvar_get(nc,fic)
newNDVIValues<-ncvar_def("NDVI","daily",list(dimLonDef,dimLatDef,tdim),longname="NDVI",missval=NaN,prec="double")
newNDVINCDF4<-nc_create(file_output,newNDVIValues)
ncvar_put(newNDVINCDF4,"NDVI",oldNDVIValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newNDVINCDF4)
nc_close(nc)
proc.time()

#Opening old Rn NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_Rn.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New Rn file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_Rn.nc",sep="")

oldRnValues<-ncvar_get(nc,fic)
newRnValues<-ncvar_def("Rn","daily",list(dimLonDef,dimLatDef,tdim),longname="Rn",missval=NaN,prec="double")
newRnNCDF4<-nc_create(file_output,newRnValues)
ncvar_put(newRnNCDF4,"Rn",oldRnValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newRnNCDF4)
nc_close(nc)
proc.time()

#Opening old TS NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_TS.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New TS file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_TS.nc",sep="")

oldTSValues<-ncvar_get(nc,fic)
newTSValues<-ncvar_def("TS","daily",list(dimLonDef,dimLatDef,tdim),longname="TS",missval=NaN,prec="double")
newTSNCDF4<-nc_create(file_output,newTSValues)
ncvar_put(newTSNCDF4,"TS",oldTSValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newTSNCDF4)
nc_close(nc)
proc.time()

########################Candidatos a Pixel quente#####################################
Rn<-output[[1]]
TS<-output[[2]]
NDVI<-output[[3]]
EVI<-output[[4]]
LAI<-output[[5]]
G<-output[[6]]
alb<-output[[7]]

Ho<-Rn-G
y<-Ho[NDVI>0.15 & NDVI<0.19]
x<-TS[NDVI>0.15 & NDVI<0.19]
TS.c.hot<-quantile(x[x>273.16],0.8)
Ho.c.hot<-median(y[x>TS.c.hot-0.5 & x<TS.c.hot+0.5], na.rm = TRUE)
TS.Ho<-abs(TS-TS.c.hot)+abs(Ho-Ho.c.hot)
Cand.hot<-sort(TS.Ho[])[1:20]
ll.hot<-numeric()
for(k in 0:length(Cand.hot)){
  ll.hot<-c(ll.hot,which(TS.Ho[]==Cand.hot[k]))
}
xy.hot <- xyFromCell(TS.Ho, ll.hot)
NDVI.hot<-extract(NDVI,xy.hot, buffer=105)
NDVI.hot.cv<-sapply(NDVI.hot, sd)/sapply(NDVI.hot, mean)
i.NDVI.hot.cv<-which(NDVI.hot.cv<0.16)
TQ.hot<-extract(TS,xy.hot[i.NDVI.hot.cv,])
ll.hot.f<-xyFromCell(TS, which(TS[]==TQ.hot[1]))
proc.time()

#Candidatos a Pixel frio
z<-TS[NDVI<0 & TS>273.16]
TS.c.cold<-quantile(z,0.08)
TS.dif<-abs(TS-TS.c.cold)
Cand.cold<-sort(TS.dif[])[1:20]
ll.cold<-numeric()
for(k in 0:length(Cand.cold)){
  ll.cold<-c(ll.cold,which(TS.dif[]==Cand.cold[k]))
}
xy.cold <- xyFromCell(TS.dif, ll.cold)
NDVI.cold<-extract(NDVI,xy.cold, buffer=210)
NDVI.cold.cv<-sapply(NDVI.cold, sd)/sapply(NDVI.cold, mean)
i.NDVI.cold<-which(NDVI.cold.cv<0.1)
TQ.cold<-extract(TS,xy.cold[i.NDVI.cold,])
ll.cold.f<-xyFromCell(TS, which(TS[]==TQ.cold[1]))
proc.time()

#Localiza??o dos pixels de refer?ncia (quente e frio)
ll_ref<-rbind(ll.hot.f[1,],ll.cold.f[1,])
colnames(ll_ref)<-c("long", "lat")
rownames(ll_ref)<-c("hot","cold")
proc.time()

####################################################################################

#Dados da esta??o
x<-3 # Altura do sensor de velocidade do vento (metros)
hc<-0.2 #Altura da vegeta??o (metros)
Lat<-  -7.14  #Latitude da esta??o
Long<- -36.75 #Longitude da esta??o

#Par?metros de rugosidade da superf?cie na Esta??o
zom.est<-hc*0.12
azom<- -3    #Par?metro para o Zom imagem
bzom<- 6.47  #Par?metro para o Zom imagem
F_int<-0.16  #Fator de interioriza??o para calculo do Rs 24h(valor default)
proc.time()

#Velocidade de fric??o na esta??o (ustar.est)
ustar.est<-k*table.sw$V9[2]/log((x)/zom.est)

#Velocidade 200 metros
u200<-ustar.est/k*log(200/zom.est)

#zom para todos os pixels
zom<-exp(azom+bzom*NDVI)

#Valores iniciais
ustar<-k*u200/(log(200/zom))         # Velocidade de fric??o para todos os pixels
rah<-(log(2/0.1))/(ustar*k)          # Resist?ncia aerodin?mica para todos os pixels
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

#in?cio do ciclo de estabilidade
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

#Fluxo de calor sens?vel final (H)

#Hot and cold pixels
dt.hot<-H.hot*rah.hot/(rho*cp)                  
b<-dt.hot/(value.pixels.ref["hot","TS"]-value.pixels.ref["cold","TS"]) 
a<- -b*(value.pixels.ref["cold","TS"]-273.15)                          
proc.time()

#All pixels
H<-rho*cp*(a+b*(TS-273.15))/rah
H[H>(Rn-G)]<-(Rn-G)[H>(Rn-G)]
proc.time()

#Fluxo de calor latente instant?neo (LE)
LE<-Rn-G-H

#Upscalling temporal
dr<-(1/d_sun_earth$dist[Dia.juliano])^2 #Inverso do quadrado da dist?ncia relativa Terra-SOL
sigma<-0.409*sin(((2*pi/365)*Dia.juliano)-1.39) # Declina??o Solar (rad)
phi<-(pi/180)*Lat #Latitude solar em graus
omegas<-acos(-tan(phi)*tan(sigma)) #?ngulo hor?rio de por-do-sol (rad)
Ra24h<-(((24*60/pi)*Gsc*dr)*(omegas*sin(phi)*
                               sin(sigma)+cos(phi)*cos(sigma)*sin(omegas)))*(1000000/86400)
proc.time()

#Radia??o de onda curta incidente em 24 horas (Rs24h)
Rs24h<-F_int*sqrt(max(table.sw$V4[])-min(table.sw$V4[]))*Ra24h

FL<-110                                
Rn24h_dB<-(1-alb)*Rs24h-FL*Rs24h/Ra24h         # M?todo de Bruin

#Fra??o evapotranspirativa Bastiaanssen
EF<-LE/(Rn-G)

#Fluxo de calor sens?vel de 24 horas (H24h)
H24h_dB<-(1-EF)*Rn24h_dB

#Fluxo de calor Latente de 24 horas (LE24h)
LE24h_dB<-EF*Rn24h_dB

#Evapotranpira??o de 24 horas (ET24h)
ET24h_dB<-LE24h_dB*86400/((2.501-0.00236*
                             (max(table.sw$V4[])+min(table.sw$V4[]))/2)*10^6)
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
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_EF.nc",sep="")

oldEFValues<-ncvar_get(nc,fic)
newEFValues<-ncvar_def("EF","daily",list(dimLonDef,dimLatDef,tdim),longname="EF",missval=NaN,prec="double")
newEFNCDF4<-nc_create(file_output,newEFValues)
ncvar_put(newEFNCDF4,"EF",oldEFValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newEFNCDF4)
nc_close(nc)
proc.time()

#Opening old ET24h NetCDF
var_output<-paste(dados$Path.Output[1],"/",fic,"_ET24h.nc",sep="")
nc<-nc_open(var_output, write=TRUE,readunlim=FALSE,verbose=TRUE,auto_GMT=FALSE,suppress_dimvals=FALSE)

#New ET24h file name
file_output<-paste(dados$Path.Output[1],"/",fic,"_new_ET24h.nc",sep="")

oldET24hValues<-ncvar_get(nc,fic)
newET24hValues<-ncvar_def("ET24h","daily",list(dimLonDef,dimLatDef,tdim),longname="ET24h",missval=NaN,prec="double")
newET24hNCDF4<-nc_create(file_output,newET24hValues)
ncvar_put(newET24hNCDF4,"ET24h",oldET24hValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
nc_close(newET24hNCDF4)
nc_close(nc)
proc.time()
