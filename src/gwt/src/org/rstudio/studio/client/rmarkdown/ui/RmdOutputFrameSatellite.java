package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.RmdOutputSatellite;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;

public class RmdOutputFrameSatellite extends RmdOutputFrameBase
{
   @Inject
   public RmdOutputFrameSatellite (SatelliteManager satelliteManager)
   {
      satelliteManager_ = satelliteManager;
   }
   
   @Override
   public void closeOutputFrame(boolean forReopen) 
   {
      if (forReopen)
         SatelliteManager.callNotifyPendingReactivate(getWindowObject());
      satelliteManager_.closeSatelliteWindow(RmdOutputSatellite.NAME);
   }

   @Override
   public WindowEx getWindowObject() 
   {
      return satelliteManager_.getSatelliteWindowObject(RmdOutputSatellite.NAME);
   }

   @Override
   public void showRmdPreview(RmdPreviewParams params, boolean activate)
   {
      super.showRmdPreview(params);
      WindowEx win = getWindowObject();
      if (activate && win != null && !Desktop.isDesktop() && BrowseCap.isChrome())
      {
         satelliteManager_.forceReopenSatellite(RmdOutputSatellite.NAME, 
                                                params,
                                                activate);
      }
      else
      {
         satelliteManager_.openSatellite(RmdOutputSatellite.NAME,     
                                         params,
                                         params.getPreferredSize(),
                                         activate);   
      }
   }

   @Override
   public int getViewerType()
   {
      return RmdOutput.RMD_VIEWER_TYPE_WINDOW;
   }
   
   @Override
   public int getScrollPosition()
   {
      return getScrollPositionNative(getWindowObject());
   }
   
   @Override
   public String getAnchor()
   {
      return getAnchorNative(getWindowObject());
   }

   private final native int getScrollPositionNative(JavaScriptObject win) /*-{
      var scrollPosition;
      try {
         scrollPosition = win.getRstudioFrameScrollPosition();
      }
      catch (e) {
         // fail gracefully with top of document
      }
      if (typeof(scrollPosition) === "undefined")
         scrollPosition = 0;
      return scrollPosition;
   }-*/;
   
   private final native String getAnchorNative(JavaScriptObject win) /*-{
      var anchor;
      try { 
         anchor = win.getRstudioFrameAnchor();
      }
      catch (e) {
         // fail gracefully with no anchor
      }
      if (typeof(anchor) === "undefined")
         anchor = "";
      return anchor;
   }-*/;
   
   private final SatelliteManager satelliteManager_;
}
