package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.RmdOutputSatellite;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;

import com.google.inject.Inject;

public class RmdOutputFrameSatellite extends RmdOutputFrameBase
{
   @Inject
   public RmdOutputFrameSatellite (SatelliteManager satelliteManager)
   {
      satelliteManager_ = satelliteManager;
   }
   
   @Override
   public void closeOutputFrame() 
   {
      satelliteManager_.closeSatelliteWindow(RmdOutputSatellite.NAME);
   }

   @Override
   public WindowEx getWindowObject() 
   {
      return satelliteManager_.getSatelliteWindowObject(RmdOutputSatellite.NAME);
   }

   @Override
   public void showRmdPreview(RmdPreviewParams params)
   {
      super.showRmdPreview(params);
      WindowEx win = getWindowObject();
      if (win != null && !Desktop.isDesktop() && BrowseCap.isChrome())
      {
         satelliteManager_.forceReopenSatellite(RmdOutputSatellite.NAME, 
                                                params);
      }
      else
      {
         satelliteManager_.openSatellite(RmdOutputSatellite.NAME,     
                                         params,
                                         params.getPreferredSize());   
      }
   }

   @Override
   public int getViewerType()
   {
      return RmdOutput.RMD_VIEWER_TYPE_WINDOW;
   }
   
   private final SatelliteManager satelliteManager_;
}
