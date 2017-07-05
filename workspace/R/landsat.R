landsat<-function(){
  if (n.sensor<8){
  # Radiância
  rad<-list()
  if (n.sensor==5) r<-7 else r<-8
  for(i in 1:r){
    rad[[i]]<- image.rec[[i]]*p.s$Grescale[i]+p.s$Brescale[i]
    rad[[i]][rad[[i]]<0]<-0
  }
  
  #Reflectância
  ref<-list()
  for(i in 1:r){
    ref[[i]]<-pi*rad[[i]]*d_sun_earth$dist[Dia.juliano]^2/
      (p.s$ESUN[i]*costheta)
  }
  
  #Albedo de superfície
  alb<-ref[[1]]*p.s$wb[1]+ref[[2]]*p.s$wb[2]+ref[[3]]*p.s$wb[3]+ref[[4]]*p.s$wb[4]+
    ref[[5]]*p.s$wb[5]+ref[[r]]*p.s$wb[r]
  alb<-(alb-0.03)/tal^2
  
  #Radiação de onda curta incidente
  Rs<-1367*costheta*tal/d_sun_earth$dist[Dia.juliano]^2 # Céu claro
  
  #NDVI,SAVI,LAI e EVI
  NDVI<-(ref[[4]]-ref[[3]])/(ref[[4]]+ref[[3]])
  SAVI<- ((1+0.05)*(ref[[4]]-ref[[3]]))/(0.05+ref[[4]]+ref[[3]])
  LAI<- SAVI
  LAI[SAVI>0.687]<-6
  LAI[SAVI<=0.687]<- -log((0.69-SAVI[SAVI<=0.687])/0.59)/0.91
  LAI[SAVI<0.1]<-0
  EVI<-2.5*((ref[[4]]-ref[[3]])/(ref[[4]]+(6*ref[[3]])-(7.5*ref[[1]])+1))
  
  #Emissividade Enb
  Enb <- 0.97+0.0033*LAI
  Enb[NDVI<0 | LAI>2.99]<-0.98
  
  #Emissividade Eo
  Eo <- 0.95+0.01*LAI
  Eo[NDVI<0 | LAI>2.99]<-0.98
  
  #Temperatura de Superfície em Kelvin (TS)
  if (n.sensor==5) k1<-607.76 else k1<-666.09   #Constante Temperatura de superfície
  if (n.sensor==5) k2<-1260.56 else k2<-1282.71 #Constante Temperatura de superfície
  if (n.sensor==5) TS<-k2/log((Enb*k1/rad[[6]])+1) else TS<-k2/log((Enb*k1/rad[[7]])+1)
  
  #Radiação de onda longa emitida pela superfície (RLsup)
  RLsup<-Eo*5.67*10^-8*TS^4
  
  #Emissividade atmosférica (Ea)
  Ea<-0.85*(-1*log(tal))^0.09 # Céu Claro
  
  #Radiação de onda longa emitida pela atmosfera (RLatm)
  RLatm<-Ea*5.67*10^-8*(table.sw$V7[2]+273.15)^4
  
  #Saldo de radiação Instantânea (Rn)
  Rn<- Rs-Rs*alb+RLatm-RLsup-(1-Eo)*RLatm
  Rn[Rn<0]<-0
  
  #Fluxo de Calor no Solo (G)
  G<-((TS-273.15)*(0.0038+0.0074*alb)*(1-0.98*NDVI^4))*Rn
  G[NDVI<0]<-0.5*Rn[NDVI<0]
  G[G<0]<-0
  
  output<-stack(Rn,TS,NDVI,EVI,LAI,G,alb)
  } else {
    # Radiância
    rad.mult<-as.numeric(MTL$V2[MTL$V1==grep(pattern ="RADIANCE_MULT_BAND_10 ",
                                             MTL$V1, value = TRUE)])
    rad.add<-as.numeric(MTL$V2[MTL$V1==grep(pattern ="RADIANCE_ADD_BAND_10 ",
                                            MTL$V1, value = TRUE)])
    rad10<- image.rec[[7]]*rad.mult+rad.add
    
    #Reflectância
    ref<-list()
    for(i in 1:6){
      ref[[i]]<-(image.rec[[i]]*0.00002-0.1)/costheta
    }
    
    #Albedo de superfície
    alb<-ref[[1]]*p.s$wb[1]+ref[[2]]*p.s$wb[2]+ref[[3]]*p.s$wb[3]+ref[[4]]*p.s$wb[4]+
      ref[[5]]*p.s$wb[5]+ref[[6]]*p.s$wb[6]
    alb<-(alb-0.03)/tal^2
    
    #Radiação de onda curta incidente
    Rs<-1367*costheta*tal/d_sun_earth$dist[Dia.juliano]^2 # Céu claro
    
    #NDVI,SAVI,LAI e EVI
    NDVI<-(ref[[4]]-ref[[3]])/(ref[[4]]+ref[[3]])
    SAVI<- ((1+0.05)*(ref[[4]]-ref[[3]]))/(0.05+ref[[4]]+ref[[3]])
    LAI<- SAVI
    LAI[SAVI>0.687]<-6
    LAI[SAVI<=0.687]<- -log((0.69-SAVI[SAVI<=0.687])/0.59)/0.91
    LAI[SAVI<0.1]<-0
    EVI<-2.5*((ref[[4]]-ref[[3]])/(ref[[4]]+(6*ref[[3]])-(7.5*ref[[1]])+1))
    
    #Emissividade Enb
    Enb <- 0.97+0.0033*LAI
    Enb[NDVI<0 | LAI>2.99]<-0.98
    
    #Emissividade Eo
    Eo <- 0.95+0.01*LAI
    Eo[NDVI<0 | LAI>2.99]<-0.98
    
    #Temperatura de Superfície em Kelvin (TS)
    k1<-774.8853      #Constante Temperatura de superfície
    k2<-1321.0789     #Constante Temperatura de superfície
    TS<-k2/log((Enb*k1/rad10)+1)
    
    #Radiação de onda longa emitida pela superfície (RLsup)
    RLsup<-Eo*5.67*10^-8*TS^4
    
    #Emissividade atmosférica (Ea)
    Ea<-0.85*(-1*log(tal))^0.09 # Céu Claro
    
    #Radiação de onda longa emitida pela atmosfera (RLatm)
    RLatm<-Ea*5.67*10^-8*(table.sw$V7[2]+273.15)^4
    
    #Saldo de radiação Instantânea (Rn)
    Rn<- Rs-Rs*alb+RLatm-RLsup-(1-Eo)*RLatm
    Rn[Rn<0]<-0
    
    #Fluxo de Calor no Solo (G)
    G<-((TS-273.15)*(0.0038+0.0074*alb)*(1-0.98*NDVI^4))*Rn
    G[NDVI<0]<-0.5*Rn[NDVI<0]
    G[G<0]<-0
    
    output<-stack(Rn,TS,NDVI,EVI,LAI,G,alb)
    } 
  return(output)
}