library(raster)
library(pryr)

cacheManagerConstructor <- function(tmpDirectory = '/tmp/', cacheManagerSize = 10) {
  CM <- list(
    tmpObjectsPaths = vector(), 
    tmpDirec = tmpDirectory, 
    cacheSize = max(cacheManagerSize, 2), 
    cache = list(), 
    objectNameByCache = list())
  
  class(CM) <- append(class(CM), "cacheManager")
  return(CM)
}

addObject <- function(object, rasterObject) {
  UseMethod("addObject", object)
}

putObjectInCache <- function(object, objectRef, rasterObject) {
  UseMethod("putObjectInCache", object)
}

removeLastObject <- function(object) {
  UseMethod("removeLastObject", object)
}

cacheIsFull <- function(object) {
  UseMethod("cacheIsFull", object)
}

getObject <- function(object, rasterObject, position = 1) {
  UseMethod("getObject", object)
}

updateObject <- function(object, rasterObject, position = 1) {
  UseMethod("updateObject", object)
}

cacheWriteRaster <- function(object, rasterObject, tmpFilePath) {
  UseMethod("cacheWriteRaster", object)
}


addObject.cacheManager <- function(object, rasterObject) {
  rasterObjectName <- deparse(substitute(rasterObject))
  for(i in c(nlayers(rasterObject):1)) {
    objectRef <- paste(rasterObjectName, i, sep = '-')
    
    object$tmpObjectsPaths[[objectRef]] <- paste(object$tmpDirec, objectRef, sep = '')
    
    object <- putObjectInCache(object, objectRef, rasterObject[[i]])
  }
  rm(list = c(rasterObjectName), envir = as.environment(globalenv()))
  return(object)
}

putObjectInCache.cacheManager <- function(object, objectRef, rasterObject) {
  if(cacheIsFull(object) == TRUE) {
    print("Cache is Full")
    object <- removeLastObject(object)
  }
  object$cache <- c(object$cache, as.list(rasterObject))
  object$objectNameByCache <- c(object$objectNameByCache, as.list(objectRef))
  return(object)
}

removeLastObject.cacheManager <- function(object) {
  # think about write the raster in disk with a new thread
  fileName <- object$tmpObjectsPaths[[ object$objectNameByCache[[1]] ]]
  print(paste("Writing", fileName, "::", object$cache[[1]]@file@name))
  
  cacheWriteRaster(object, object$cache[[1]], fileName)
  
  object$cache <- object$cache[c(-1)]
  object$objectNameByCache <- object$objectNameByCache[c(-1)]
  return(object)
}

updateObject.cacheManager <- function(object, rasterObject, position = 1) {
  rasterObjectName <- deparse(substitute(rasterObject))
  rasterObjectRef <- paste(rasterObjectName, position, sep = '-')
  objectPos <- match(rasterObjectRef, object$objectNameByCache)
  if(!is.na(objectPos)) {
    object$cache <- object$cache[c(-objectPos)]
    object$objectNameByCache <- object$objectNameByCache[c(-objectPos)]
  }
  
  tmpFilePath <- object$tmpObjectsPaths[[rasterObjectRef]]
  cacheWriteRaster(object, rasterObject, tmpFilePath)
  object <- putObjectInCache(object, rasterObjectRef, rasterObject)
  rm(list = c(rasterObjectName), envir = as.environment(globalenv()))
  return(object)
}

cacheWriteRaster.cacheManager <- function(object, rasterObject, tmpFilePath) {
  print(paste("writing", rasterObject@file@name, tmpFilePath, sep = ' :: '))
  tr <- blockSize(rasterObject)
  # use x to keep layer name
  r <- writeStart(rasterObject, filename=tmpFilePath, overwrite = TRUE)
  for (i in 1:tr$n) {
    v <- getValues(rasterObject, row=tr$row[i], nrows=tr$nrows[i])
    r <- writeValues(r, v, tr$row[i])
  }
  if (isTRUE(any(is.factor(rasterObject)))) {
    levels(r) <- levels(rasterObject)
  }
  #r <- setZ(r, getZ(x))
  r <- writeStop(r)
}

cacheIsFull.cacheManager <- function(object) {
  if(length(object$cache) == object$cacheSize) {
    return(TRUE)
  } else {
    return(FALSE)
  }
}

getObject.cacheManager <- function(object, rasterObject, position = 1) {
  rasterObjectRef <- paste(deparse(substitute(rasterObject)), position, sep = '-') 
  objectPos <- match(rasterObjectRef, object$objectNameByCache)
  if(!is.na(objectPos)) {
    return(object$cache[[objectPos]])
  } else {
    tmpFilePath <- object$tmpObjectsPaths[[rasterObjectRef]]
    print("Reading")
    print(tmpFilePath)
    rasterObject <- raster(paste(tmpFilePath, '.grd', sep = ''))
    
    newObject <- putObjectInCache(object, rasterObjectRef, rasterObject)
    eval.parent(substitute(object <- newObject))
    rm(rasterObject)
    return(newObject$cache[[newObject$cacheSize]])
  }
}

r1 <- raster(ncols=7000, nrows=7000)
r1[] <- 1:ncell(r1)
object.size(r1)
r2 <- raster(ncols=7000, nrows=7000)
r2[] <- (1:ncell(r2)) + 4000000
object.size(r2)
r3 <- raster(ncols=7000, nrows=7000)
r3[] <- 1:ncell(r3) + 8000000
object.size(r3)

mem_used()

CM <- cacheManagerConstructor('/tmp/', 2)

CM <- addObject(CM, r1)
CM
CM <- addObject(CM, r2)
CM
CM <- addObject(CM, r3)
CM

getObject(CM, r1)
CM
getObject(CM, r2)
CM
getObject(CM, r3)
CM
getObject(CM, r1)
CM
getObject(CM, r2)
CM
getObject(CM, r3)
CM

mem_used()

r3 <- getObject(CM, r3)

r3[] <- r3[]*2

mem_used()

CM <- updateObject(CM, r3, 1)
CM

mem_used()

