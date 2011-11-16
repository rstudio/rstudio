package org.rstudio.studio.client.common.satellite;

import com.google.gwt.core.client.JavaScriptObject;

public class SatelliteWindowPrefs extends JavaScriptObject
{
   protected SatelliteWindowPrefs()
   { 
   }
   
   public static final SatelliteWindowPrefs createEmpty()
   {
      return create(0,0);
   }
    
   public native static final SatelliteWindowPrefs create(int width, 
                                                          int height) /*-{
      var prefs = new Object();
      prefs.width = width;
      prefs.height = height;
      return prefs;
   }-*/;
   
   
   public final boolean isEmpty()
   {
      return getWidth() == 0;
   }
   
   public native final int getWidth() /*-{
      return this.width;
   }-*/;
   
   public native final int getHeight() /*-{
      return this.height;
   }-*/;
   
   public static native boolean areEqual(SatelliteWindowPrefs a, 
                                         SatelliteWindowPrefs b) /*-{
      if (a === null ^ b === null)
         return false;
      if (a === null)
         return true;
      return a.width === b.width && a.height === b.height; 
   }-*/;

}
