/*
 * ChunkOutput.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import com.google.gwt.core.client.JavaScriptObject;

public class ChunkOutput extends JavaScriptObject
{
   public static final String LINE_WIDGET_TYPE = "ChunkOutput";
   
   protected ChunkOutput()
   {
   }
   
   public static native final ChunkOutput create(int row,
                                                 int rowCount,
                                                 boolean visible,
                                                 String htmlRef) /*-{
      return {
        row: row,
        row_count: rowCount,
        visible: visible,
        html_ref: htmlRef 
      };
   }-*/;
   
   public native final int getRow()  /*-{
      return this.row;
   }-*/;
   
   public native final int getRowCount()  /*-{
      return this.row_count;
   }-*/;
   
   public native final boolean getVisible() /*-{
      return this.visible;
   }-*/;   
   
   public native final String getHtmlRef() /*-{
      return this.html_ref;
   }-*/;
   
   public final boolean equalTo(ChunkOutput other)
   {
      return getRow() == other.getRow() &&
             getRowCount() == other.getRowCount() &&
             getVisible() == other.getVisible() &&
             getHtmlRef() == other.getHtmlRef();
   }
}
