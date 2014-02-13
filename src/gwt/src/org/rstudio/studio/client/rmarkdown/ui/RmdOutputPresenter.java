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
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.rpubs.RPubsPresenter;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RmdOutputPresenter implements IsWidget, RPubsPresenter.Context
{
   public interface Binder 
          extends CommandBinder<Commands, RmdOutputPresenter>
   {}

   public interface Display extends IsWidget
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
                             RPubsPresenter rpubsPresenter,
                             Session session,
                             Commands commands,
                             EventBus eventBus)
   {
      view_ = view;
      globalDisplay_ = globalDisplay;
      session_ = session;
      rpubsPresenter.setContext(this);
      
      binder.bind(commands, this);  
      
      initializeEvents();
   }     

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   @Override
   public String getContextId()
   {
      return "RMarkdownPreview";
   }

   @Override
   public String getTitle()
   {
      String title = view_.getTitle();
      if (title != null && !title.isEmpty())
         return title;
      
      String htmlFile = getHtmlFile();
      if (htmlFile != null)
      {
         FileSystemItem fsi = FileSystemItem.createFile(htmlFile);
         return fsi.getStem();
      }
      else
      {
         return "(Untitled)";
      }
   }

   @Override
   public String getHtmlFile()
   {
      return params_ == null ? 
         null : params_.getOutputFile();
   }

   @Override
   public boolean isPublished()
   {
      return params_ == null ?
          false : params_.getResult().getRpubsPublished();
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
      view_.showOutput(params, session_.getSessionInfo().getAllowRpubsPublish(), 
                       refresh);
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
      params_.setAnchor(view_.getAnchor());
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
   private final Session session_;
   
   private RmdPreviewParams params_;
}