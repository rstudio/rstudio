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

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class GridViewer extends Composite
{
   private static GridViewerUiBinder uiBinder = GWT
         .create(GridViewerUiBinder.class);
   
   private final String id_;

   interface GridViewerUiBinder extends UiBinder<Widget, GridViewer>
   {
   }

   public GridViewer()
   {
      id_ = String.valueOf((new Date()).getTime() % 1000);
      
      initWidget(uiBinder.createAndBindUi(this));
      initWidgetNative(id_);
   }

   public String onLoadData(String id)
   {
      // Validate this is the correct widget
      if (id != id_)
      {
         return null;
      }
      
      return "sample data";
   }

   @UiFactory
   GridViewerFrame makeGridViewerWidget()
   {
      GridViewerFrame gridViewer = new GridViewerFrame(id_);
      return gridViewer;
   }
   
   @UiField
   GridViewerFrame gridViewerFrame_;
   
   private final native void initWidgetNative(String id) /*-{
      var this_ = this;
      
      if (!$wnd.onLoadGridViewer) $wnd.onLoadGridViewer = [];
      
      $wnd.onLoadGridViewer[id] = $entry(function(id) {
         return this_.@org.rstudio.core.client.widget.GridViewer::onLoadData(Ljava/lang/String;)(id);
      });
   }-*/;
}
