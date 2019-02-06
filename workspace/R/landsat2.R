landsat<-function(){
  if (n.sensor<8){
    # Radiância
    rad<-list()
    if (n.sensor==5) r<-7 else r<-8
    for(i in 1:r){
      rad[[i]]<- image.rec[[i]][]*p.s$Grescale[i]+p.s$Brescale[i]
      rad_subset <- rad[[i]] < 0 & !is.na(rad[[i]])
      rad[[i]][rad_subset]<-0
    }
    
    #Reflectância
    ref<-list()
    for(i in 1:r){
      ref[[i]]<-pi*rad[[i]]*d_sun_earth$dist[Dia.juliano]^2/
        (p.s$ESUN[i]*costheta)
    }
    
    #Albedo de superfície
    alb_temp<-ref[[1]]*p.s$wb[1]+ref[[2]]*p.s$wb[2]+ref[[3]]*p.s$wb[3]+ref[[4]]*p.s$wb[4]+
      ref[[5]]*p.s$wb[5]+ref[[r]]*p.s$wb[r]
    alb_temp<-(alb_temp-0.03)/tal[]^2
    
    #Radiação de onda curta incidente
    Rs_temp<-1367*costheta*tal[]/d_sun_earth$dist[Dia.juliano]^2 # Céu claro
    
    #NDVI,SAVI,LAI e EVI
    NDVI_temp<-(ref[[4]]-ref[[3]])/(ref[[4]]+ref[[3]])
    SAVI_temp<- ((1+0.05)*(ref[[4]]-ref[[3]]))/(0.05+ref[[4]]+ref[[3]])
    LAI_temp <- SAVI_temp
    SAVI_subset1 <- SAVI_temp > 0.687 & !is.na(SAVI_temp)
    SAVI_subset2 <- SAVI_temp <= 0.687 & !is.na(SAVI_temp)
    SAVI_subset3 <- SAVI_temp < 0.1 & !is.na(SAVI_temp)
    LAI_temp[SAVI_subset1] <- 6
    LAI_temp[SAVI_subset2] <- -log(
      (0.69 - SAVI_temp[SAVI_subset2]) / 0.59
    ) / 0.91
    LAI_temp[SAVI_subset3] <- 0
    EVI_temp<-2.5*((ref[[4]]-ref[[3]])/(ref[[4]]+(6*ref[[3]])-(7.5*ref[[1]])+1))
    
    #Emissividade Enb
    Enb_temp <- 0.97+0.0033*LAI_temp
    Enb_temp[NDVI_temp<0 | LAI_temp>2.99]<-0.98
    
    #Emissividade Eo
    Eo_temp <- 0.95+0.01*LAI_temp
    Eo_temp[NDVI_temp<0 | LAI_temp>2.99]<-0.98
    
    #Temperatura de Superfície em Kelvin (TS)
    if (n.sensor==5) k1<-607.76 else k1<-666.09   #Constante Temperatura de superfície
    if (n.sensor==7) k2<-1260.56 else k2<-1282.71 #Constante Temperatura de superfície
    if (n.sensor==5) TS_temp<-k2/log((Enb_temp*k1/rad[[6]])+1) else TS_temp<-k2/log((Enb_temp*k1/rad[[7]])+1)
    
    #Radiação de onda longa emitida pela superfície (RLsup)
    RLsup_temp<-Eo_temp*5.67*10^-8*TS_temp^4
    
    #Emissividade atmosférica (Ea)
    Ea_temp<-0.85*(-1*log(tal[]))^0.09 # Céu Claro
    
    #Radiação de onda longa emitida pela atmosfera (RLatm)
    RLatm_temp<-Ea_temp*5.67*10^-8*(table.sw$V7[hour.image.station]+273.15)^4 # Ajustar
    
    #Saldo de radiação Instantânea (Rn)
    Rn_temp<- Rs_temp-Rs_temp*alb_temp+RLatm_temp-RLsup_temp-(1-Eo_temp)*RLatm_temp
    Rn_temp[Rn_temp<0]<-0
    
    #Fluxo de Calor no Solo (G)
    G_temp_1<-(NDVI_temp>=0)*(((TS_temp-273.15)*(0.0038+0.0074*alb_temp)*(1-0.98*NDVI_temp^4))*Rn_temp)
    G_temp_2 <- (NDVI_temp<0)*(0.5*Rn_temp)
    G_temp <- G_temp_1 + G_temp_2
    G_temp[G_temp<0]<-0
    
    alb <- tal
    alb[] <- alb_temp
    Rn<-tal
    Rn[]<-Rn_temp
    TS<-tal 
    TS[]<-TS_temp
    NDVI<-tal 
    NDVI[]<-NDVI_temp
    EVI<-tal
    EVI[]<-EVI_temp
    LAI<-tal
    LAI[]<-LAI_temp
    G<-tal
    G[]<-G_temp
    
    output<-stack(Rn,TS,NDVI,EVI,LAI,G,alb)
  } else {
    # Radiância
    rad.mult<-as.numeric(MTL$V2[MTL$V1==grep(pattern ="RADIANCE_MULT_BAND_10 ",
                                             MTL$V1, value = TRUE)])
    rad.add<-as.numeric(MTL$V2[MTL$V1==grep(pattern ="RADIANCE_ADD_BAND_10 ",
                                            MTL$V1, value = TRUE)])
    rad10<- image.rec[[7]][]*rad.mult+rad.add
    
    #Reflectância
    ref<-list()
    for(i in 1:6){
      ref[[i]]<-(image.rec[[i]][]*0.00002-0.1)/costheta
    }
    
    #Albedo de superfície
    alb_temp<-ref[[1]]*p.s$wb[1]+ref[[2]]*p.s$wb[2]+ref[[3]]*p.s$wb[3]+ref[[4]]*p.s$wb[4]+
      ref[[5]]*p.s$wb[5]+ref[[6]]*p.s$wb[6]
    alb_temp<-(alb_temp-0.03)/tal[]^2
    
    #Radiação de onda curta incidente
    Rs_temp<-1367*costheta*tal[]/d_sun_earth$dist[Dia.juliano]^2 # Céu claro
    
    #NDVI,SAVI,LAI e EVI
    NDVI_temp<-(ref[[4]]-ref[[3]])/(ref[[4]]+ref[[3]])
    SAVI_temp<- ((1+0.05)*(ref[[4]]-ref[[3]]))/(0.05+ref[[4]]+ref[[3]])
    LAI_temp <- SAVI_temp
    SAVI_subset1 <- SAVI_temp > 0.687 & !is.na(SAVI_temp)
    SAVI_subset2 <- SAVI_temp <= 0.687 & !is.na(SAVI_temp)
    SAVI_subset3 <- SAVI_temp < 0.1 & !is.na(SAVI_temp)
    LAI_temp[SAVI_subset1] <- 6
    LAI_temp[SAVI_subset2] <- -log(
      (0.69 - SAVI_temp[SAVI_subset2]) / 0.59
    ) / 0.91
    LAI_temp[SAVI_subset3] <- 0
    
    
    EVI_temp<-2.5*((ref[[4]]-ref[[3]])/(ref[[4]]+(6*ref[[3]])-(7.5*ref[[1]])+1))
    
    #Emissividade Enb
    Enb_temp <- 0.97+0.0033*LAI_temp
    Enb_temp[NDVI_temp<0 | LAI_temp>2.99]<-0.98
    
    #Emissividade Eo
    Eo_temp <- 0.95+0.01*LAI_temp
    Eo_temp[NDVI_temp<0 | LAI_temp>2.99]<-0.98
    
    #Temperatura de Superfície em Kelvin (TS)
    k1<-774.8853      #Constante Temperatura de superfície
    k2<-1321.0789     #Constante Temperatura de superfície
    TS_temp<-k2/log((Enb_temp*k1/rad10)+1)
    
    #Radiação de onda longa emitida pela superfície (RLsup)
    RLsup_temp<-Eo_temp*5.67*10^-8*TS_temp^4
    
    #Emissividade atmosférica (Ea)
    Ea_temp<-0.85*(-1*log(tal[]))^0.09 # Céu Claro
    
    #Radiação de onda longa emitida pela atmosfera (RLatm)
    RLatm_temp<-Ea_temp*5.67*10^-8*(table.sw$V7[hour.image.station]+273.15)^4 # Ajustar
    
    #Saldo de radiação Instantânea (Rn)
    Rn_temp<- Rs_temp-Rs_temp*alb_temp+RLatm_temp-RLsup_temp-(1-Eo_temp)*RLatm_temp
    Rn_temp[Rn_temp<0]<-0
    
    #Fluxo de Calor no Solo (G)
    G_temp_1<-(NDVI_temp>=0)*(((TS_temp-273.15)*(0.0038+0.0074*alb_temp)*(1-0.98*NDVI_temp^4))*Rn_temp)
    G_temp_2 <- (NDVI_temp<0)*(0.5*Rn_temp)
    G_temp <- G_temp_1 + G_temp_2
    G_temp[G_temp<0]<-0
    
    alb <- tal
    alb[] <- alb_temp
    Rn<-tal
    Rn[]<-Rn_temp
    TS<-tal 
    TS[]<-TS_temp
    NDVI<-tal 
    NDVI[]<-NDVI_temp
    EVI<-tal
    EVI[]<-EVI_temp
    LAI<-tal
    LAI[]<-LAI_temp
    G<-tal
    G[]<-G_temp
    
    output<-stack(Rn,TS,NDVI,EVI,LAI,G,alb)
  } 
  return(output)
}
