/*
 * RmdTemplateFormat.java
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

public class RmdTemplateFormat extends JavaScriptObject
{
   protected RmdTemplateFormat()
   {
   }
   
   public final native String getName() /*-{
      return this.format_name;
   }-*/;
   
   public final native String getUiName() /*-{
      return this.format_ui_name;
   }-*/;
   
   public final native JsArrayString getOptions() /*-{
      return this.format_options;
   }-*/;

   public final native String getNotes() /*-{
      return this.format_notes || "";
   }-*/;

   public final native String getExtension() /*-{
      return this.format_extension;
   }-*/;
   
}
