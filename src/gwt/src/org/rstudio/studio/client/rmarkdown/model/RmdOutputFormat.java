/*
 * RmdOutputFormat.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

public class RmdOutputFormat extends JavaScriptObject
{
   protected RmdOutputFormat() 
   {
   }
   
   public native final String getFormatName() /*-{
      return this.format_name;
   }-*/;
   
   // Implementation note: it'd be better to have a generic <T> function here, 
   // but unfortunately GWT always uses a dynamic cast on the output of a
   // JSNI generic to convert to the boxed type. For instance, even if the 
   // generic is invoked with type "boolean", an attempt is made to cast the
   // value to "Boolean", which fails since it's an unboxed native value. 
   public native final boolean getFormatOption(String option, 
                                               boolean defaultValue) /*-{
      if (typeof this.format_options === "undefined" ||
          this.format_options === null)
          return defaultValue;
      var optionValue = this.format_options[option];
      if (typeof optionValue === "undefined")
         return defaultValue;
      else if (typeof optionValue === "object" && optionValue.length === 1) 
         return optionValue[0];
      else
         return optionValue;
   }-*/;   
   
   public final boolean isSelfContained()
   {
      return getFormatOption(FORMAT_SELF_CONTAINED, false);
   }

   // output format name strings from the rmarkdown package (not exhaustive)
   public final static String OUTPUT_HTML_DOCUMENT = "html_document";
   public final static String OUTPUT_BEAMER_PRESENTATION = "beamer_presentation";
   public final static String OUTPUT_REVEALJS_PRESENTATION = "revealjs_presentation";
   public final static String OUTPUT_IOSLIDES_PRESENTATION = "ioslides_presentation";
   public final static String OUTPUT_PRESENTATION_SUFFIX = "_presentation";
   public final static String OUTPUT_WORD_DOCUMENT = "word_document";
   public final static String OUTPUT_PDF_DOCUMENT = "pdf_document";

   // format option strings from the rmarkdown package
   public final static String FORMAT_SELF_CONTAINED = "self_contained";
}

