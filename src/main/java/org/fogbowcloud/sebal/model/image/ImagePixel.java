package org.fogbowcloud.sebal.model.image;

public interface ImagePixel {

	Image image();
	ImagePixelOutput output();
	
	void setOutput(ImagePixelOutput output);
	
	GeoLoc geoLoc();
	// Radiancia
	double[] L();
	
	// Cosseno do Angulo zenital
	double cosTheta();
	
	// Seno do Angulo zenital
	double sinTheta();
	
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
