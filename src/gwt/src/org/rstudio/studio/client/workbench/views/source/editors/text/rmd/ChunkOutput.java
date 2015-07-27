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

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;

import com.google.gwt.core.client.JavaScriptObject;

public class ChunkOutput extends JavaScriptObject
{
   protected ChunkOutput()
   {
   }
   
   public static native final ChunkOutput create(int row,
                                                 int rowCount,
                                                 String htmlRef) /*-{
      return {
        row: row,
        row_count: rowCount,
        html_ref: htmlRef 
      };
   }-*/;
   
   public static final ChunkOutput fromLineWidget(LineWidget lineWidget)
   {
      return create(lineWidget.getRow(), 
                    lineWidget.getRowCount(),
                    lineWidget.getData());
   }
   
   public native final int getRow()  /*-{
      return this.row;
   }-*/;
   
   public native final int getRowCount()  /*-{
      return this.row_count;
   }-*/;
   
   public native final String getHtmlRef() /*-{
      return this.html_ref;
   }-*/;
}
