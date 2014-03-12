/*
 * PDFViewer.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.pdfviewer;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerEvent;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerHandler;
import org.rstudio.studio.client.pdfviewer.model.PdfJsWindow;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PDFViewer implements CompilePdfCompletedEvent.Handler,
                                  ShowPDFViewerHandler
{
   @Inject
   public PDFViewer(EventBus eventBus,
                    final ApplicationServerOperations server,
                    final GlobalDisplay display,
                    final SatelliteManager satelliteManager)
   {  
      display_ = display;
      server_ = server;
      
      eventBus.addHandler(ShowPDFViewerEvent.TYPE, this);
      eventBus.addHandler(CompilePdfCompletedEvent.TYPE, this);
   }

   @Override
   public void onShowPDFViewer(ShowPDFViewerEvent event)
   {
      if (pdfJsWindow_ == null || pdfJsWindow_.isClosed())
      {
         String url = GWT.getHostPageBaseURL() + "pdf_js/web/viewer.html?file=";
         NewWindowOptions options = new NewWindowOptions();
         options.setCallback(new OperationWithInput<WindowEx>() 
         {
            @Override
            public void execute(WindowEx win)
            {
               pdfJsWindow_ = win.cast();
            }
         });
         display_.openMinimalWindow(url, false, 800, 1000, options);
      }
   }
   
   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {
      if (pdfJsWindow_ == null)
         return;
      
      FileSystemItem pdf = FileSystemItem.createFile(
            event.getResult().getPdfPath());
      pdfJsWindow_.openPdf(server_.getFileUrl(pdf), 1);
   }
   
   private PdfJsWindow pdfJsWindow_;
   
   private final GlobalDisplay display_;
   private final ApplicationServerOperations server_;
}
