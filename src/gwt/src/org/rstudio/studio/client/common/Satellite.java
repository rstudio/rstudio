package org.rstudio.studio.client.common;

public class Satellite
{
   public static native void initialize() /*-{
      $wnd.isRStudioSatellite = true;
   }-*/;
   
   public static native boolean isSatellite() /*-{
      return !!$wnd.isRStudioSatellite;
   }-*/;
   
}
