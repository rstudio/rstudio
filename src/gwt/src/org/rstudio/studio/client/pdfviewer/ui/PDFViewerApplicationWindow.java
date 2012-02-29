/*
 * PDFViewerApplicationWindow.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.pdfviewer.ui;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.pdfviewer.PDFViewerPresenter;
import org.rstudio.studio.client.pdfviewer.events.InitCompleteEvent;
import org.rstudio.studio.client.pdfviewer.model.PDFViewerParams;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;

@Singleton
public class PDFViewerApplicationWindow extends SatelliteWindow
                                        implements PDFViewerApplicationView
{

   @Inject
   public PDFViewerApplicationWindow(Provider<PDFViewerPresenter> pPresenter,
                                     Provider<EventBus> pEventBus,
                                     Provider<FontSizeManager> pFontSizeManager)
   {
      super(pEventBus, pFontSizeManager);
      pPresenter_ = pPresenter;
   }

   @Override
   protected void onInitialize(LayoutPanel mainPanel, JavaScriptObject params)
   {
      Window.setTitle("RStudio: Compile PDF");
      
      // create the presenter and activate it with the passed params
      PDFViewerParams pdfParams = params.<PDFViewerParams>cast();
      presenter_ = pPresenter_.get();
      presenter_.addInitCompleteHandler(new InitCompleteEvent.Handler()
      {
         @Override
         public void onInitComplete(InitCompleteEvent event)
         {
            initCompleted_ = true;
            fireEvent(new InitCompleteEvent());
         }
      });
      presenter_.onActivated(pdfParams);

      // PDF.js doesn't work correctly unless the viewer takes its natural
      // height and the window is allowed to scroll. So get rid of the main
      // panel and add the PDFViewer directly to the root panel.
      mainPanel.setVisible(false);
      RootPanel.get().add(presenter_);
   }

   @Override
   protected boolean allowScrolling()
   {
      return true;
   }

   @Override
   public void reactivate(JavaScriptObject params)
   {
      if (params != null)
      {
         PDFViewerParams pdfParams = params.<PDFViewerParams>cast();
         presenter_.onActivated(pdfParams);
      }
   }
   
   @Override 
   public Widget getWidget()
   {
      return this;
   }

   @Override
   public HandlerRegistration addInitCompleteHandler(
                                        final InitCompleteEvent.Handler handler)
   {
      if (initCompleted_)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               handler.onInitComplete(new InitCompleteEvent());
            }
         });
      }
      return addHandler(handler, InitCompleteEvent.TYPE);
   }

   private boolean initCompleted_;
   private final Provider<PDFViewerPresenter> pPresenter_;
   private PDFViewerPresenter presenter_;

}
