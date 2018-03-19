cacheManager <- setClass(
  "cacheManager",
  
  slots = c(
    tmpObjectsPaths <- "vector",
    tmpDirec <- "character",
    cache <- "vector",
    cacheSize <- "numeric"
  ),
  
  prototype = c(
    tmpObjectsPaths <- vector(),
    tmpDirec <- "/tmp/",
    cache <- vector(),
    cacheSize <- 10
  ),
  
  validity = function(object) {
    if(object@cacheSize < 2) {
      return("Invalid Cache Size")
    }
    length(object@cache) <- object@cacheSize
    return(TRUE)
  }
)

setGeneric(
  name = "addObject",
  def = function(classObject, rasterObject) {
    standardGeneric("addObject")
  }
)

setMethod(
  f = "addObject",
  signature = "cacheManager",
  definition = function(cacheManager, rasterObject) {
    rasterObjectName <- deparse(substitute(rasterObject))
    for(i in c(1:nlayers(rasterObject))) {
      objectRef <- paste(rasterObjectName, i, sep = '-')
      cacheManager@tmpObjectsPaths[[objectRef]] <- paste(cacheManager@tmpDirec, objectRef, sep = '')
    }
    return(cacheManager)
  }
)



test <- cacheManager(tmpDirec = '/tmp/', cacheSize = 20)
test
r <- raster(ncols=10, nrows=10)
test <- addObject(test, r)
test
test@tmpDirec
test@tmpObjectsPaths[['r-1']]



