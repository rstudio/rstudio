/*
 * RmdPreviewParams.java
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

public class RmdPreviewParams extends JavaScriptObject
{
   protected RmdPreviewParams()
   {
   }
   
   public static native RmdPreviewParams create(RmdRenderResult result, 
                                                int scrollPosition) /*-{
      return {
         output_file: result.output_file,
         output_url: result.output_url,
         scroll_position: scrollPosition
      };
   }-*/;
   
   public native final String getOutputFile() /*-{
      return this.output_file;
   }-*/;
   
   public native final String getOutputUrl() /*-{
      return this.output_url;
   }-*/;
   
   public native final int getScrollPosition() /*-{
      return this.scroll_position;
   }-*/;
   
   public native final void setScrollPosition(int scrollPosition) /*-{
      this.scroll_position = scrollPosition;
   }-*/;
}
