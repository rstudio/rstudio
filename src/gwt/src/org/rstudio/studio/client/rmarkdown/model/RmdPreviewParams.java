/*
 * RmdPreviewParams.java
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

import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JavaScriptObject;

public class RmdPreviewParams extends JavaScriptObject
{
   protected RmdPreviewParams()
   {
   }
   
   public static native RmdPreviewParams create(RmdRenderResult result, 
                                                int scrollPosition, 
                                                String anchor) /*-{
      return {
         'result': result,
         'scroll_position': scrollPosition,
         'anchor': anchor
      };
   }-*/;
   
   public native final RmdRenderResult getResult() /*-{
      return this.result;
   }-*/;

   public native final String getTargetFile() /*-{
      return this.result.target_file;
   }-*/;
   
   public native final String getOutputFile() /*-{
      return this.result.output_file;
   }-*/;
   
   public native final String getOutputUrl() /*-{
      return this.result.output_url;
   }-*/;
   
   public native final boolean isShinyDocument() /*-{
      return this.result.is_shiny_document;
   }-*/;
   
   public native final String getWebsiteDir() /*-{
      return this.result.website_dir;
   }-*/;
   
   public native final int getScrollPosition() /*-{
      return this.scroll_position;
   }-*/;
   
   public native final void setScrollPosition(int scrollPosition) /*-{
      this.scroll_position = scrollPosition;
   }-*/;
   
   public native final String getAnchor() /*-{
      return this.anchor;
   }-*/;
   
   public native final void setAnchor(String anchor) /*-{
      this.anchor = anchor;
   }-*/;
   
   public final Size getPreferredSize()
   {
      int chromeHeight = 100;
      String format = getResult().getFormatName();
      if (format == RmdOutputFormat.OUTPUT_IOSLIDES_PRESENTATION ||
          format == RmdOutputFormat.OUTPUT_SLIDY_PRESENTATION)
         return new Size(1100, 900 + chromeHeight);
      if (format == RmdOutputFormat.OUTPUT_REVEALJS_PRESENTATION)
         return new Size(1100, 900 + chromeHeight);
      
      // default size (html_document and others)
      return new Size(1180, 1000 + chromeHeight);
   }
   
   public final boolean isWebsiteRmd() 
   {
      return !StringUtil.isNullOrEmpty(getWebsiteDir());
   }
}
