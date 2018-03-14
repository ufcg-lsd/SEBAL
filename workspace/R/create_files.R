library(ncdf4)

create_alb_file <- function(dados, fic) {
    #Opening old alb NetCDF
    var_output <- paste(dados$Path.Output, "/", fic, "_alb.nc", sep = "")
    nc <- nc_open(var_output, write = TRUE, readunlim = FALSE,
                  verbose = TRUE, auto_GMT = FALSE, suppress_dimvals = FALSE)

    #Getting lat and lon values from old NetCDF
    old_lat < -ncvar_get(nc, "lat", start = 1, count = raster.elevation@nrows)
    old_lon <- ncvar_get(nc, "lon", start = 1, count = raster.elevation@ncols)

    #Defining latitude and longitude dimensions
    dimLatDef<-ncdim_def("lat","degrees",old_lat,unlim=FALSE,longname="latitude")
    dimLonDef<-ncdim_def("lon","degrees",old_lon,unlim=FALSE,longname="longitude")

    #New alb file name
    file_output<-paste(dados$Path.Output[1],"/",fic,"_alb.nc",sep="")
    oldAlbValues<-ncvar_get(nc,fic)
    newAlbValues<-ncvar_def("alb","daily",list(dimLonDef,dimLatDef,tdim),longname="alb",missval=NaN, prec="double")
    nc_close(nc)
    newAlbNCDF4<-nc_create(file_output,newAlbValues)
    ncvar_put(newAlbNCDF4,"alb",oldAlbValues,start=c(1,1,1),count=c(raster.elevation@ncols,raster.elevation@nrows,1))
    nc_close(newAlbNCDF4)
}

create_evi_file <- function(dados, fic) {
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
}

create_g_file < function(dados, fic) {
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
}

create_lai_file <- function(dados, fic) {
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
}

create_ndvi_file <- function(dados, fic) {
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
}

create_rn_file <- function(dados, fic) {
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
}

create_ts_file <- function(dados, fic) {
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
}