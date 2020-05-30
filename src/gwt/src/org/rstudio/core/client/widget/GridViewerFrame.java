/*
 * GridViewerFrame.java
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

package org.rstudio.core.client.widget;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.workbench.views.environment.dataimport.DataImportDataActiveColumn;

import com.google.gwt.core.client.JavaScriptObject;

public class GridViewerFrame extends RStudioThemedFrame
{
   public GridViewerFrame(String title)
   {
      this(title, false);
   }

   public GridViewerFrame(String title, boolean enableThemes)
   {
      super(
         title,
         "grid_resource/gridviewer.html?data_source=data",
         false,
         null, 
         GridViewerStyles.getCustomStyle(),
         null,
         false,
         enableThemes);
   }
   
   public void onAttach()
   {
      super.onAttach();
   }
   
   public void setData(JavaScriptObject data)
   {
      WindowEx gridViewerFrameWindow = getIFrame().getContentWindow();
      setDataNative(gridViewerFrameWindow, data);
   }
   
   public void setOption(String option, String value)
   {
      WindowEx gridViewerFrameWindow = getIFrame().getContentWindow();
      setOptionNative(gridViewerFrameWindow, option, value);
   }
   
   public void setColumnDefinitionsUIVisible(
      boolean value, 
      Operation onColumnOpen,
      Operation onColumnDismiss)
   {
      WindowEx gridViewerFrameWindow = getIFrame().getContentWindow();
      setColumnDefinitionsUIVisibleNative(
            gridViewerFrameWindow,
            value,
            onColumnOpen,
            onColumnDismiss);
   }
   
   public DataImportDataActiveColumn getActiveColumn()
   {
      WindowEx gridViewerFrameWindow = getIFrame().getContentWindow();
      return getActiveColumn(gridViewerFrameWindow);
   }

   public boolean isReady()
   {
      WindowEx gridViewerFrameWindow = getIFrame().getContentWindow();
      return canSetOptions(gridViewerFrameWindow);
   }

   private native boolean canSetOptions(WindowEx frameContentWindow) /*-{
      return typeof(frameContentWindow) !== " undefined";
   }-*/;
   
   private final native void setDataNative(
      WindowEx frameContentWindow,
      JavaScriptObject data) /*-{
      frameContentWindow.setData(data);
   }-*/;
   
   private final native void setOptionNative(
      WindowEx frameContentWindow,
      String option,
      String value) /*-{
      frameContentWindow.setOption(option, value);
   }-*/;
   
   private final native void setColumnDefinitionsUIVisibleNative(
      WindowEx frameContentWindow,
      boolean value,
      Operation onColumnOpen,
      Operation onColumnDismiss) /*-{
      frameContentWindow.setColumnDefinitionsUIVisible(
         value,
         $entry(function () {
            onColumnOpen.@org.rstudio.core.client.widget.Operation::execute()()
         }),
         $entry(function () {
            onColumnDismiss.@org.rstudio.core.client.widget.Operation::execute()()
         })
      );
   }-*/;
   
   private final native DataImportDataActiveColumn getActiveColumn(
      WindowEx frameContentWindow) /*-{
      return frameContentWindow.getActiveColumn();
   }-*/;
}
