/*
 * DataViewerPresenter.java
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
package org.rstudio.studio.client.dataviewer;

import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DataViewerPresenter implements 
      IsWidget
      
{
   public interface Display extends IsWidget
   {
      void showData(DataItem item);
   }
   
   @Inject
   public DataViewerPresenter(Display view,
                              SourceServerOperations server)
   {
      view_ = view;
      server_ = server;
      initializeEvents();
   }     

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   public void showData(DataItem item)
   {
      // if data is already visible, clear the cache
      if (item_ != null)
      {
         server_.removeCachedData(item_.getCacheKey(), 
                                  new VoidServerRequestCallback());
      }
      item_ = item;
      view_.showData(item);
   }
   
   private native void initializeEvents() /*-{  
      var thiz = this;   
      $wnd.addEventListener(
            "unload",
            $entry(function() {
               thiz.@org.rstudio.studio.client.dataviewer.DataViewerPresenter::onClose()();
            }),
            true);
   }-*/;
   
   private void onClose()
   {
      if (item_ != null)
         server_.removeCachedData(item_.getCacheKey(), 
                                  new VoidServerRequestCallback());
   }
   
   private DataItem item_;
   private final Display view_;
   private final SourceServerOperations server_;
}