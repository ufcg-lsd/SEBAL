library(raster)
library(pryr)

cacheManagerConstructor <- function(tmpDirectory = '/tmp/', cacheManagerSize = 10) {
  CM <- list(
    tmpObjectsPaths = vector(), 
    tmpDirec = tmpDirectory, 
    cacheSize = cacheManagerSize, 
    cache = list(), 
    objectNameByCache = list())
  
  class(CM) <- append(class(CM), "cacheManager")
  return(CM)
}

addObject <- function(object, rasterObject) {
  UseMethod("addObject", object)
}

putObjectInCache <- function(object, objectRef, rasterObject, overWrite) {
  UseMethod("putObjectInCache", object)
}

removeLastObject <- function(object, overWrite) {
  UseMethod("removeLastObject", object)
}

cacheIsFull <- function(object) {
  UseMethod("cacheIsFull", object)
}

getObject <- function(object, rasterObject, position = 1) {
  UseMethod("getObject", object)
}

addObject.cacheManager <- function(object, rasterObject) {
  rasterObjectName <- deparse(substitute(rasterObject))
  for(i in c(1:nlayers(rasterObject))) {
    objectRef <- paste(rasterObjectName, i, sep = '-')
    
    object$tmpObjectsPaths[[objectRef]] <- paste(object$tmpDirec, objectRef, sep = '')
    
    object <- putObjectInCache(object, objectRef, rasterObject[[i]], TRUE)
  }
  rm(list = c(rasterObjectName), envir = as.environment(globalenv()))
  return(object)
}

putObjectInCache.cacheManager <- function(object, objectRef, rasterObject, overWrite) {
  if(cacheIsFull(object) == TRUE) {
    object <- removeLastObject(object, overWrite)
  }
  object$cache <- c(object$cache, as.list(rasterObject))
  object$objectNameByCache <- c(object$objectNameByCache, as.list(objectRef))
  return(object)
}

removeLastObject.cacheManager <- function(object, overWrite) {
  # think about write the raster in disk with a new thread
  fileName <- object$tmpObjectsPaths[[ object$objectNameByCache[[1]] ]]
  if(overWrite == TRUE) {
    writeRaster(object$cache[[1]], filename = fileName, overwrite = TRUE)
  } else {
    if(!file.exists(paste(fileName, '.gri', sep = ''))) {
      writeRaster(object$cache[[1]], filename = fileName)
    }
  }
  
  object$cache <- object$cache[c(-1)]
  object$objectNameByCache <- object$objectNameByCache[c(-1)]
  return(object)
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
    rasterObject <- raster(tmpFilePath)
    newObject <- putObjectInCache(object, rasterObjectRef, rasterObject, FALSE)
    eval.parent(substitute(object <- newObject))
    rm(rasterObject)
    return(newObject$cache[[newObject$cacheSize]])
  }
}
