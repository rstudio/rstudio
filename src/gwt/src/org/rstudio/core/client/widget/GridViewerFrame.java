/*
 * GridViewer.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JsArray;

public class GridViewerFrame extends RStudioFrame
{
   public GridViewerFrame()
   {
      super("grid_resource/gridviewer.html?data_source=data");
      
      setSize("100%", "100%");
   }
   
   public void onAttach()
   {
      super.onAttach();
   }
   
   public void setData(JsArray<JsObject> data)
   {
      WindowEx gridViewerFrameWindow = getIFrame().getContentWindow();
      setDataNative(gridViewerFrameWindow, data);
   }
   
   private final native void setDataNative(WindowEx frameContentWindow, JsArray<JsObject> data) /*-{
      frameContentWindow.setData(data);
   }-*/;
}
