package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.image.Image;

public class TestPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {
    
    @Override
    public void choosePixelsQuenteFrio() {
      DefaultImagePixel pixelQuenteLocal = new DefaultImagePixel();
      pixelQuenteLocal.ux(4.388);
      pixelQuenteLocal.zx(6.);
      pixelQuenteLocal.hc(4.);
      pixelQuenteLocal.d(4.0* (2./3.));
      
      ImagePixelOutput outputQuente = new ImagePixelOutput();
      outputQuente.setG(89.352632);
      outputQuente.setRn(449.55188);
      outputQuente.setSAVI(0.148563);
      outputQuente.setTs(35.8928340 + 273.15);
      pixelQuenteLocal.setOutput(outputQuente);
      
      DefaultImagePixel pixelFrioLocal = new DefaultImagePixel();
      ImagePixelOutput outputFrio = new ImagePixelOutput();
      outputFrio.setTs(26.4577440 + 273.15);
      pixelFrioLocal.setOutput(outputFrio);
      
      this.pixelQuente = pixelQuenteLocal;
      this.pixelFrio = pixelFrioLocal;
    }

	@Override
	public void selectPixelsQuenteFrioCandidates(Image image) {
		// TODO Auto-generated method stub
		
	}
}
