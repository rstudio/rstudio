/*
 * GridViewerFrame.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.core.client.widget;

import java.util.LinkedHashMap;
import java.util.Map;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
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
         // show_summary=0: this frame runs in data_source=data mode (data is
         // pushed in client-side, with no server-side cached frame), so the
         // column-summary sidebar -- which fetches stats from the grid_data
         // endpoint -- has nothing to summarize here. Hide it.
         "grid_resource/gridviewer.html?data_source=data&max_cols=-1&show_summary=0",
         false,
         null, 
         GridViewerStyles.getCustomStyle(),
         null,
         false,
         enableThemes);

      // The page's API (setOption/setData in DataViewer.js) is not there
      // until gridviewer.html has loaded, and a reload empties the grid. The
      // frame is re-inserted, and so reloaded, whenever its tab panel changes
      // (WorkbenchTabPanel.setTabs clears and re-adds every tab), so the
      // latest pushes are kept and replayed from every load.
      addLoadHandler(event -> onGridViewerLoaded());
   }
   
   public void onAttach()
   {
      super.onAttach();
   }
   
   public void setData(JavaScriptObject data)
   {
      data_ = data;
      hasData_ = true;

      if (isReady())
         setDataNative(getWindow(), data);
   }
   
   public void setOption(String option, String value)
   {
      options_.put(option, value);

      if (isReady())
         setOptionNative(getWindow(), option, value);
   }
   
   public void setColumnDefinitionsUIVisible(
      boolean value, 
      Operation onColumnOpen,
      Operation onColumnDismiss)
   {
      columnDefinitionsUIVisible_ = value;
      onColumnOpen_ = onColumnOpen;
      onColumnDismiss_ = onColumnDismiss;
      hasColumnDefinitionsUI_ = true;

      if (isReady())
      {
         setColumnDefinitionsUIVisibleNative(
               getWindow(),
               value,
               onColumnOpen,
               onColumnDismiss);
      }
   }
   
   public DataImportDataActiveColumn getActiveColumn()
   {
      WindowEx gridViewerFrameWindow = getIFrame().getContentWindow();
      return getActiveColumn(gridViewerFrameWindow);
   }

   private boolean isReady()
   {
      return canSetOptions(getWindow());
   }

   private void onGridViewerLoaded()
   {
      WindowEx window = getWindow();
      if (!canSetOptions(window))
      {
         // Chrome can fire load for the initial about:blank; anything else is
         // the page having loaded without its script (or a redirect, e.g. to
         // sign-in), and nothing will be shown.
         String url = getLocationHref(window);
         if (!StringUtil.equals(url, "about:blank"))
            Debug.logWarning("Grid viewer frame loaded without its API: " + url);
         return;
      }

      // Same order as the callers: options first, since setData reads them.
      for (Map.Entry<String, String> option : options_.entrySet())
         setOptionNative(window, option.getKey(), option.getValue());

      if (hasData_)
         setDataNative(window, data_);

      if (hasColumnDefinitionsUI_)
      {
         setColumnDefinitionsUIVisibleNative(
               window,
               columnDefinitionsUIVisible_,
               onColumnOpen_,
               onColumnDismiss_);
      }
   }

   // The frame has a contentWindow (about:blank) before gridviewer.html loads,
   // so wait for its API; setData also needs the document to be parsed.
   private static native boolean canSetOptions(WindowEx frameContentWindow) /*-{
      try {
         return !!frameContentWindow &&
                typeof frameContentWindow.setOption === "function" &&
                typeof frameContentWindow.setData === "function" &&
                frameContentWindow.document.readyState !== "loading";
      } catch (e) {
         // cross-origin window
         return false;
      }
   }-*/;

   private static native String getLocationHref(WindowEx frameContentWindow) /*-{
      try {
         return frameContentWindow ? frameContentWindow.location.href : null;
      } catch (e) {
         return null;
      }
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

   // The latest pushes, replayed on every load of the page.
   private final Map<String, String> options_ = new LinkedHashMap<>();
   private JavaScriptObject data_;
   private boolean hasData_;
   private boolean columnDefinitionsUIVisible_;
   private Operation onColumnOpen_;
   private Operation onColumnDismiss_;
   private boolean hasColumnDefinitionsUI_;
}
