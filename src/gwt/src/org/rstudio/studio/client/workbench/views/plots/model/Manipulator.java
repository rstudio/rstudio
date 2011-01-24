package org.rstudio.studio.client.workbench.views.plots.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class Manipulator extends JavaScriptObject
{
   protected Manipulator()
   {
   }
   
   public static class Control extends JavaScriptObject
   {
      public final static int SLIDER = 0;
      public final static int PICKER = 1;
      
      protected Control()
      {
      }
      
      public final native int getType() /*-{
         return this.type[0];
      }-*/;
   }
   
   public static class Slider extends Control
   {
      protected Slider()
      {
      }
       
      public final native double getMin() /*-{
         return this.min[0];
      }-*/;

      public final native double getMax() /*-{
         return this.max[0];
      }-*/;
   }
   
   public static class Picker extends Control
   {
      protected Picker()
      {
      }
           
      public final native JsArrayString getChoices() /*-{
         return this.choices;
      }-*/;
   }
   
   
   public final native String getCode() /*-{
      return this.manip_codeAsText[0];
   }-*/;
   
   public final native JsArrayString getVariables() /*-{
      return this.manip_variables;
   }-*/;
   
   public final native double getDoubleValue(String name) /*-{
      var valueArray = this[name];
      return valueArray[0];
   }-*/;
   
   public final native String getStringValue(String name) /*-{
      var valueArray = this[name];
      return valueArray[0];
   }-*/;
   
   public final native Control getControl(String name) /*-{
      var control = this.manip_controls[name];
      return control;
   }-*/;
}
