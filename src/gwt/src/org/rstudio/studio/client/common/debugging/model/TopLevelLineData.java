package org.rstudio.studio.client.common.debugging.model;

import org.rstudio.studio.client.workbench.views.environment.events.LineData;

public class TopLevelLineData extends LineData
{
   protected TopLevelLineData() {}
   
   public final native int getStep() /*-{
      return this.step;
   }-*/;
}
