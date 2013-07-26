package org.rstudio.studio.client.common.debugging.model;

import org.rstudio.studio.client.workbench.views.environment.events.LineData;

public class TopLevelLineData extends LineData
{
   protected TopLevelLineData() {}
   
   public static final int STATE_PAUSED = 0;
   public static final int STATE_INJECTION_SITE = 1;
   public static final int STATE_FINISHED = 2;
   
   public final native int getStep() /*-{
      return this.step;
   }-*/;
   
   public final native boolean getFinished() /*-{
      return this.state == 2;
   }-*/;
   
   public final native int getState() /*-{
      return this.state;
   }-*/;
   
   public final native boolean getNeedsBreakpointInjection() /*-{
      return this.needs_breakpoint_injection;
   }-*/;
}
