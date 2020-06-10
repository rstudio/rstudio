/*
 * RmdFrontMatterOutputOptions.java
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

package org.rstudio.studio.client.rmarkdown.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class RmdFrontMatterOutputOptions extends JavaScriptObject
{
   protected RmdFrontMatterOutputOptions()
   {
   }
   
   public final static native RmdFrontMatterOutputOptions create() /*-{
      return {};
   }-*/;
   
   public final native JsArrayString getOptionList() /*-{
      return Object.getOwnPropertyNames(this);
   }-*/;
   
   public final native String getOptionValue(String optionName) /*-{
      var option = this[optionName];
      if (typeof option === "undefined")
         return null;
      else if (option === null)
         return "null";
      else 
         return option.toString();
   }-*/;
   
   public final native void removeOption(String optionName) /*-{
      delete this[optionName];
   }-*/;
   
   public final native void setOptionValue(RmdTemplateFormatOption option,
                                           String value) /*-{
      switch (option.option_type)
      {
         case "boolean":
            this[option.option_name] = (value === "true");
            break;
         case "float":
            this[option.option_name] = parseFloat(value);
            break;
         default:
            this[option.option_name] = value;
      }
   }-*/;
}
