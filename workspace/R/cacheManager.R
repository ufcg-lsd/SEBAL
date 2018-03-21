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
  for(i in c(nlayers(rasterObject):1)) {
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
    if(!file.exists(paste(fileName, '.grd', sep = ''))) {
      writeRaster(object$cache[[1]], filename = fileName, overWrite = FALSE)
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
    print(tmpFilePath)
    rasterObject <- raster(paste(tmpFilePath, '.grd', sep = ''))
    
    newObject <- putObjectInCache(object, rasterObjectRef, rasterObject, FALSE)
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
getObject(CM, r2)
CM
getObject(CM, r3)
CM

mem_used()

r3 <- getObject(CM, r3)

r3[] <- r3[]*2
toupper(r3@file@name)
CM <- addObject(CM, r3)
CM

mem_used()

