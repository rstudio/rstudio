package org.rstudio.studio.client.workbench.views.plots.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ManipulatorControl extends JavaScriptObject
{
   public final static int SLIDER = 0;
   public final static int PICKER = 1;
   
   protected ManipulatorControl()
   {
   }
   
   public final native int getType() /*-{
      return type;
   }-*/;
   
   public final native double getMin() /*-{
      return this.min;
   }-*/;

   public final native double getMax() /*-{
      return this.max;
   }-*/;

   

}
