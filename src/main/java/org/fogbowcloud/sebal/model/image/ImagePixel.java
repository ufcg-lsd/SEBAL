package org.fogbowcloud.sebal.model.image;

public interface ImagePixel {

	Image image();
	
	ImagePixelOutput output();
	
	void setOutput(ImagePixelOutput output);
	
	// Radiancia
	double[] L();
	
	// Angulo zenital
	double cosTheta();
	
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
}
