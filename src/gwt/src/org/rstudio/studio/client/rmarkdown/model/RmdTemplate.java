/*
 * RmdTemplate.java
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

import org.rstudio.core.client.JsArrayUtil;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class RmdTemplate extends JavaScriptObject
{
   protected RmdTemplate() 
   {
   }
   
   public static final native RmdTemplate create() /*-{
      return {
         template_name: "",
         template_formats: [ ],
         template_options: [ ]
      };
   }-*/;

   public final native String getName() /*-{
      return this.template_name;
   }-*/;

   public final native JsArray<RmdTemplateFormat> getFormats() /*-{
      return this.template_formats;
   }-*/;

   public final native JsArray<RmdTemplateFormatOption> getOptions() /*-{
      return this.template_options;
   }-*/;
   
   public final RmdTemplateFormat getFormat(String format)
   {
      return JsArrayUtil.jsFindInCollection(getFormats(), "format_name", 
                                            format);
   }

   public final RmdTemplateFormatOption getOption(String option)
   {
      return JsArrayUtil.jsFindInCollection(getOptions(), "option_name", 
                                            option);
   }
   
   public final static RmdTemplate getTemplate(JsArray<RmdTemplate> haystack,
                                               String needle) 
   {
      return JsArrayUtil.jsFindInCollection(haystack, "template_name", needle);
   }
}
