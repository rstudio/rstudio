/*
 * PlumberAPIPresenter.java
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
package org.rstudio.studio.client.plumber;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.plumber.events.PlumberAPIStatusEvent;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PlumberAPIPresenter implements 
      IsWidget, 
      PlumberAPIStatusEvent.Handler 
{
   public interface Binder 
          extends CommandBinder<Commands, PlumberAPIPresenter>
   {}

   public interface Display extends IsWidget
   {
      String getDocumentTitle();
      String getUrl();
      String getAbsoluteUrl();
      void showApp(PlumberAPIParams params);
      void reloadApp();
   }
   
   @Inject
   public PlumberAPIPresenter(Display view,
                              GlobalDisplay globalDisplay,
                              Binder binder,
                              final Commands commands,
                              Satellite satellite)
   {
      view_ = view;
      satellite_ = satellite;
      globalDisplay_ = globalDisplay;
      
      binder.bind(commands, this);  
      
      initializeEvents();
   }     

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   @Override
   public void onPlumberAPIStatus(PlumberAPIStatusEvent event)
   {
      if (StringUtil.equals(event.getParams().getState(), PlumberAPIParams.STATE_RELOADING))
      {
         view_.reloadApp();
      }
   }

   @Handler
   public void onReloadPlumberAPI()
   {
      view_.reloadApp();
   }
   
   @Handler
   public void onViewerPopout()
   {
      globalDisplay_.openWindow(params_.getUrl());
   }

   public void loadApp(PlumberAPIParams params) 
   {
      params_ = params;
      view_.showApp(params);
   }
   
   private native void initializeEvents() /*-{  
      var thiz = this;   
      $wnd.addEventListener(
            "unload",
            $entry(function() {
               thiz.@org.rstudio.studio.client.plumber.PlumberAPIPresenter::onClose()();
            }),
            true);
   }-*/;
   
   private void onClose()
   {
      // don't stop the app if the window is closing just to be opened again. 
      // (we close and reopen as a workaround to forcefully activate the window
      // on browsers that don't permit manual event reactivation)
      if (satellite_.isReactivatePending())
         return;
      
      PlumberAPIParams params = PlumberAPIParams.create(
            params_.getPath(), 
            params_.getUrl(), 
            PlumberAPIParams.STATE_STOPPING);
      notifyPlumberAPIClosed(params);
   }
   
   private final native void closePlumberAPI() /*-{
      $wnd.close();
   }-*/;
   
   private final native void notifyPlumberAPIClosed(JavaScriptObject params) /*-{
      $wnd.opener.notifyPlumberAPIClosed(params);
   }-*/;

   private final native void notifyPlumberAPIDisconnected(JavaScriptObject params) /*-{
      if ($wnd.opener)
         $wnd.opener.notifyPlumberAPIDisconnected(params);
   }-*/;

   private final Display view_;
   private final Satellite satellite_;
   private final GlobalDisplay globalDisplay_;
   private PlumberAPIParams params_;
}
