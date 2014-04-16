/*
 * RmdRenderResult.java
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

import org.rstudio.studio.client.common.compile.CompileError;
import org.rstudio.studio.client.common.presentation.model.SlideNavigation;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class RmdRenderResult extends JavaScriptObject
{
   protected RmdRenderResult() 
   {
   }
   
   public static native final RmdRenderResult createFromShinyUrl(String file,
                                                          String shinyUrl) /*-{
     return {
        succeeded: true,
        target_file: file,
        target_encoding: "UTF-8",
        output_file: "",
        output_url: shinyUrl, 
        output_format: null, 
        rpubs_published: false, 
        knitr_errors: [],
        is_shiny_document: true,
        preview_side: -1,
        slide_navigation: null,
        output_format: {
           format_name: "html_document", 
           self_contained: false
        }
     };
   }-*/;
  
   public native final boolean getSucceeded() /*-{
      return this.succeeded;
   }-*/;

   public native final String getTargetFile() /*-{
      return this.target_file;
   }-*/;
   
   public native final String getTargetEncoding() /*-{
      return this.target_encoding;
   }-*/;
   
   public native final int getTargetLine() /*-{
      return this.target_line;
   }-*/;
   
   public native final String getOutputFile() /*-{
      return this.output_file;
   }-*/;
   
   public native final String getOutputUrl() /*-{
      return this.output_url;
   }-*/;
   
   public native final RmdOutputFormat getFormat() /*-{
      return this.output_format;
   }-*/;
   
   public native final boolean getRpubsPublished() /*-{
      return this.rpubs_published;
   }-*/;
   
   public final boolean isHtml()
   {
      return getOutputFile().toLowerCase().endsWith(".html");
   }
   
   public final String getFormatName()
   {
      return getFormat().getFormatName();
   }
   
   public final native int getPreviewSlide() /*-{
      return this.preview_slide;
   }-*/;
   
   public final native SlideNavigation getSlideNavigation() /*-{
      return this.slide_navigation;
   }-*/;
   
   public final native JsArray<CompileError> getKnitrErrors() /*-{
      return this.knitr_errors;
   }-*/;
   
   public final native boolean isShinyDocument() /*-{
      return this.is_shiny_document;
   }-*/;
   
   public final native boolean hasShinyContent() /*-{
      return this.has_shiny_content;
   }-*/;

   public final boolean isHtmlPresentation()
   {
      return isHtml() && getFormatName().endsWith(
                  RmdOutputFormat.OUTPUT_PRESENTATION_SUFFIX);
   }
   
   // indicates whether this result represents the same *output* document as
   // another result (must match name and type)
   public final boolean equals(RmdRenderResult other)
   {
      // for Shiny documents, match on input source
      if (isShinyDocument() && other.isShinyDocument())
         return getTargetFile().equals(other.getTargetFile());
      else
         return getOutputFile().equals(other.getOutputFile()) &&
                getFormatName().equals(other.getFormatName());
   }
}
