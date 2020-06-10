/*
 * Manipulator.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
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
      public final static int CHECKBOX = 2;
      public final static int BUTTON = 3;
      
      protected Control()
      {
      }
      
      public final native int getType() /*-{
         return this.type[0];
      }-*/;
      
      public final native String getLabel() /*-{
         if (this.label)
            return this.label[0];
         else
            return null;
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
      
      public final native double getStep() /*-{
         return this.step[0];
      }-*/;
      
      public final native boolean getTicks() /*-{
         return this.ticks[0];
      }-*/;
      
      public final native double getInitialValue() /*-{
         return this.initialValue[0];
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
      
      public final native String getInitialValue() /*-{
         return this.initialValue[0];
      }-*/;

   }
   
   public static class CheckBox extends Control
   {
      protected CheckBox()
      {
      }
      
      public final native boolean getInitialValue() /*-{
         return this.initialValue[0];
      }-*/;
   }
   
   public static class Button extends Control
   {
      protected Button()
      {
      }  
   }
   
   
   public final native String getID() /*-{
      return this.id[0];
   }-*/;

   public final native JsArrayString getVariables() /*-{
      return this.variables;
   }-*/;
      
   public final native double getDoubleValue(String name) /*-{
      var valueArray = this.values[name];
      return valueArray[0];
   }-*/;
   
   public final native String getStringValue(String name) /*-{
      var valueArray = this.values[name];
      return valueArray[0];
   }-*/;
   
   public final native boolean getBooleanValue(String name) /*-{
      var valueArray = this.values[name];
      return valueArray[0];
   }-*/;
   
   public final native Control getControl(String name) /*-{
      var control = this.controls[name];
      return control;
   }-*/;
   
   public final boolean hasControls()
   {
      return getVariables() != null &&
             getVariables().length() > 0;
   }
}
