package org.rstudio.studio.client.common.debugging.model;

import org.rstudio.studio.client.workbench.views.environment.events.LineData;

public class DebugState extends LineData
{
   protected DebugState() {}
   
   public final native boolean isTopLevelDebug() /*-{
      return this.top_level_debug;
   }-*/;

   public final native String getDebugFile() /*-{
      return this.debug_file;
   }-*/;
   
   public final native int getDebugStep() /*-{
      return this.debug_step;
   }-*/;
}   

