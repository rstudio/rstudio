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

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;

import com.google.gwt.core.client.JsArray;

public class RmdRenderResult extends RmdSlideNavigationInfo
{
   protected RmdRenderResult() 
   {
   }
   
   public static native final RmdRenderResult createFromShinyDoc(
         RmdShinyDocInfo doc) /*-{
     return {
        succeeded: true,
        target_file: doc.target_file,
        target_encoding: "UTF-8",
        output_file: "",
        output_url: doc.url, 
        output_format: doc.output_format, 
        rpubs_published: false, 
        force_maximize: false,
        knitr_errors: [],
        is_shiny_document: true,
        preview_slide: doc.preview_slide,
        slide_navigation: doc.slide_navigation,
        runtime: doc.runtime,
        viewed: false
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
   
   public native final boolean getForceMaximize() /*-{
      return this.force_maximize;
   }-*/;
   
   public native final String getViewerType() /*-{
      return this.viewer_type;
   }-*/;
   
   public final boolean isHtml()
   {
      return getOutputFile().toLowerCase().endsWith(".html");
   }
   
   public final String getFormatName()
   {
      return getFormat().getFormatName();
   }
   
   public final native JsArray<SourceMarker> getKnitrErrors() /*-{
      return this.knitr_errors;
   }-*/;
   
   public final native boolean isShinyDocument() /*-{
      return this.is_shiny_document;
   }-*/;
   
   public final native boolean hasShinyContent() /*-{
      return this.has_shiny_content;
   }-*/;
   
   public final native String getRuntime() /*-{
      return this.runtime;
   }-*/;
   
   public final native String getWebsiteDir() /*-{
      return this.website_dir;
   }-*/;
   
   public final native boolean viewed() /*-{
      return !!this.viewed;
   }-*/;
   
   public final native boolean setViewed(boolean viewed) /*-{
      this.viewed = viewed;
   }-*/;

   public final boolean isHtmlPresentation()
   {
      return (isShinyDocument() || isHtml()) && getFormatName().endsWith(
                  RmdOutputFormat.OUTPUT_PRESENTATION_SUFFIX);
   }
   
   public final boolean isHtmlDashboard()
   {
      return (isShinyDocument() || isHtml()) && getFormatName().endsWith(
            RmdOutputFormat.OUTPUT_DASHBOARD_SUFFIX);
   }
   
   public final boolean getRestoreAnchor()
   {
      return isHtmlPresentation() || isHtmlDashboard();
   }
   
   public final boolean isWebsite()
   {
      return !StringUtil.isNullOrEmpty(getWebsiteDir());
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
