/*
 * PDFViewerPresenter.java
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
package org.rstudio.studio.client.pdfviewer;

import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.pdfviewer.events.InitCompleteEvent;
import org.rstudio.studio.client.pdfviewer.model.PDFViewerParams;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PDFViewerPresenter implements IsWidget
{
   public interface Display extends IsWidget
   {     
      void setURL(String url);

      HandlerRegistration addInitCompleteHandler(
                                             InitCompleteEvent.Handler handler);
   }
   
   @Inject
   public PDFViewerPresenter(Display view,
                             EventBus eventBus)
   {
      view_ = view;
      
      eventBus.addHandler(CompilePdfCompletedEvent.TYPE, 
                          new CompilePdfCompletedEvent.Handler()
      {   
         @Override
         public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
         {
            if (event.getSucceeded())
            {
               view_.setURL(event.getPdfUrl());
            }
            
         }
      });
   }

   public void onActivated(PDFViewerParams params)
   {
   }
   
   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   public HandlerRegistration addInitCompleteHandler(InitCompleteEvent.Handler handler)
   {
      return view_.addInitCompleteHandler(handler);
   }


   private final Display view_;
}
