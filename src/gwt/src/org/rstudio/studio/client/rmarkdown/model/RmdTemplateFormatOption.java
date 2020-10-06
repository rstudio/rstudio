/*
 * RmdTemplateFormatOption.java
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

public class RmdTemplateFormatOption extends JavaScriptObject
{
   protected RmdTemplateFormatOption()
   {
   }
   
   public final native String getName() /*-{
      return this.option_name;
   }-*/;

   public final native String getUiName() /*-{
      return this.option_ui_name;
   }-*/;

   public final native String getType() /*-{
      return this.option_type;
   }-*/;

   public final native String getDefaultValue() /*-{
      return this.option_default;
   }-*/;

   public final native JsArrayString getChoiceList() /*-{
      return this.option_list;
   }-*/;
   
   public final native String getFormatName() /*-{
      return this.option_format || "";
   }-*/;
   
   public final native boolean isTransferable() /*-{
      return this.option_transferable || false;
   }-*/;

   public final native boolean isAddHeader() /*-{
      return this.option_add_header || false;
   }-*/;

   public final native boolean isNullable() /*-{
      return this.option_nullable || false;
   }-*/;
   public final native String getCategory() /*-{
      return this.option_category || "General";
   }-*/;


   public final static String TYPE_BOOLEAN = "boolean";
   public final static String TYPE_CHOICE = "choice";
   public final static String TYPE_FLOAT = "float";
   public final static String TYPE_FILE = "file";
   public final static String TYPE_INTEGER = "integer";
   public final static String TYPE_STRING = "string";
}
