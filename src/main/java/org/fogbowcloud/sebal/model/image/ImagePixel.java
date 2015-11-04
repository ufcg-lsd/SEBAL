package org.fogbowcloud.sebal.model.image;

public interface ImagePixel {

	Image image();
	ImagePixelOutput output();
	
	void setOutput(ImagePixelOutput output);
	
	GeoLoc geoLoc();
	// Número Digital
	int[] DN();
	
	// Radiancia
	double[] L();
	
	// Coeficiente de Radiância
	double[] Al();
	
	// Coeficiente de Radiância
	double[] Ml();
	
	// Coeficiente de Reflectância
	double[] Ap();
	
	// Coeficiente de Reflectância
	double[] Mp();
	
	// Cosseno do Angulo zenital
	double cosTheta();
	
	// Seno do Angulo de Elevação Solar
	double sinThetaSunEle();
	
	// Altitude (elevacao)
	double z();

	// Temperatura do ar
	double Ta();

	// Velocidade do vento
	double ux();

	// Nivel de medicao
	double zx();

	// Deslocamento do plano zero
	double d();
	
	// Altura da vegetacao
	double hc();
	
	// Pixel deve ser considerado no processamento
	boolean isValid();
}
