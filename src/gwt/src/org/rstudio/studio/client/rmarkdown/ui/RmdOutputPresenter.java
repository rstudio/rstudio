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

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RmdOutputPresenter implements IsWidget
{
   public interface Binder 
          extends CommandBinder<Commands, RmdOutputPresenter>
   {}

   public interface Display extends IsWidget
   {
      void showOutput(RmdPreviewParams params, boolean refresh);
      int getScrollPosition();
      void refresh();
   }
   
   @Inject
   public RmdOutputPresenter(Display view,
                             GlobalDisplay globalDisplay,
                             Binder binder,
                             final Commands commands,
                             EventBus eventBus)
   {
      view_ = view;
      globalDisplay_ = globalDisplay;
      
      binder.bind(commands, this);  
      
      initializeEvents();
   }     

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   @Handler
   public void onViewerPopout()
   {
      globalDisplay_.showHtmlFile(params_.getOutputFile());
   }
   
   @Handler
   public void onViewerRefresh()
   {
      view_.refresh();
   }

   public void showOutput(RmdPreviewParams params) 
   {
      // detect whether we're really doing a refresh
      boolean refresh = params_ != null && 
            params_.getOutputFile().equals(params.getOutputFile());
      params_ = params;
      view_.showOutput(params, refresh);
   }
   
   private native void initializeEvents() /*-{  
      var thiz = this;   
      $wnd.addEventListener(
            "unload",
            $entry(function() {
               thiz.@org.rstudio.studio.client.rmarkdown.ui.RmdOutputPresenter::onClose()();
            }),
            true);
            
      $wnd.getRstudioFrameScrollPosition = $entry(function() {
         return thiz.@org.rstudio.studio.client.rmarkdown.ui.RmdOutputPresenter::getScrollPosition()();
      });
   }-*/;
   
   private void onClose() 
   {
      params_.setScrollPosition(view_.getScrollPosition());
      notifyRmdOutputClosed(params_);
   }
   
   private final native void notifyRmdOutputClosed(JavaScriptObject params) /*-{
      $wnd.opener.notifyRmdOutputClosed(params);
   }-*/;
   
   private int getScrollPosition()
   {
      return view_.getScrollPosition();
   }

   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   
   private RmdPreviewParams params_;
}