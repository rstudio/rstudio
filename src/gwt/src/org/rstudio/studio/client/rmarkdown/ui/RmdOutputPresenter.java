/*
 * RmdOutputPresenter.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.common.presentation.SlideNavigationPresenter;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.shiny.ShinyDisconnectNotifier;
import org.rstudio.studio.client.shiny.ShinyDisconnectNotifier.ShinyDisconnectSource;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RmdOutputPresenter implements 
   IsWidget, 
   ShinyDisconnectSource
{
   public interface Binder 
          extends CommandBinder<Commands, RmdOutputPresenter>
   {}

   public interface Display extends IsWidget,
                                    SlideNavigationPresenter.Display
   {
      void showOutput(RmdPreviewParams params, boolean showPublish, 
                      boolean refresh);
      int getScrollPosition();
      void refresh();
      String getTitle();
      String getAnchor();
   }
   
   @Inject
   public RmdOutputPresenter(Display view,
                             Binder binder,
                             GlobalDisplay globalDisplay,
                             Session session,
                             Commands commands,
                             EventBus eventBus,
                             Satellite satellite,
                             UIPrefs prefs)
   {
      view_ = view;
      globalDisplay_ = globalDisplay;
      session_ = session;
      prefs_ = prefs;
      
      slideNavigationPresenter_ = new SlideNavigationPresenter(view_);
      disconnectNotifier_ = new ShinyDisconnectNotifier(this);
      
      satellite.addCloseHandler(new CloseHandler<Satellite>()
      {
         @Override
         public void onClose(CloseEvent<Satellite> event)
         {
            // don't process a close if we're being closed for reactivate 
            if (event.getTarget().isReactivatePending())
               return;

            // record scroll position and anchor (but try/catch because sometimes 
            // the document is null at this point)
            try
            {
               params_.setScrollPosition(getScrollPosition());
               params_.setAnchor(getAnchor());
            }
            catch (Exception e)
            {
            }
            
            // notify closed
            notifyRmdOutputClosed(params_);
         }
      });
   
      binder.bind(commands, this);  
      
      initializeEvents();
   }     

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   @Override
   public String getShinyUrl()
   {
      return StringUtil.makeAbsoluteUrl(params_.getOutputUrl());
   }

   @Override
   public void onShinyDisconnect()
   {
      WindowEx.get().close();
   }

   @Handler
   public void onViewerPopout()
   {
      if (params_.isShinyDocument())
         globalDisplay_.openWindow(params_.getOutputUrl());
      else
         globalDisplay_.showHtmlFile(params_.getOutputFile());
   }
   
   @Handler
   public void onViewerRefresh()
   {
      view_.refresh();
   }
   
   @Handler
   public void onRefreshSuperDevMode()
   {
      SuperDevMode.reload();
   }

   public void showOutput(RmdPreviewParams params) 
   {
      // detect whether we're really doing a refresh
      boolean refresh = params_ != null && 
            params_.getResult().equals(params.getResult());
      params_ = params;
      view_.showOutput(params, SessionUtils.showPublishUi(session_, prefs_), 
                       refresh);
   }
   
   private native void initializeEvents() /*-{  
      var thiz = this;
      $wnd.getRstudioFrameScrollPosition = $entry(function() {
         return thiz.@org.rstudio.studio.client.rmarkdown.ui.RmdOutputPresenter::getScrollPosition()();
      });

      $wnd.getRstudioFrameAnchor = $entry(function() {
         return thiz.@org.rstudio.studio.client.rmarkdown.ui.RmdOutputPresenter::getAnchor()();
      });
   }-*/;
   
   private final native void notifyRmdOutputClosed(JavaScriptObject params) /*-{
      $wnd.opener.notifyRmdOutputClosed(params);
   }-*/;

   private int getScrollPosition()
   {
      return view_.getScrollPosition();
   }
   
   private String getAnchor()
   {
      return view_.getAnchor();
   }

   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
   private final UIPrefs prefs_;
  
   private final SlideNavigationPresenter slideNavigationPresenter_;
   private final ShinyDisconnectNotifier disconnectNotifier_;
   
   private RmdPreviewParams params_;
}