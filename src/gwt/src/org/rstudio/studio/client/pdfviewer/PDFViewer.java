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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfResult;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.events.SynctexViewPdfEvent;
import org.rstudio.studio.client.common.synctex.model.PdfLocation;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerEvent;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerHandler;
import org.rstudio.studio.client.pdfviewer.events.PageClickEvent;
import org.rstudio.studio.client.pdfviewer.model.PdfJsWindow;
import org.rstudio.studio.client.pdfviewer.model.SyncTexCoordinates;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PDFLoadEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PDFViewer implements CompilePdfCompletedEvent.Handler,
                                  ShowPDFViewerHandler,
                                  SynctexViewPdfEvent.Handler,
                                  PDFLoadEvent.Handler,
                                  PageClickEvent.Handler
{
   @Inject
   public PDFViewer(EventBus eventBus,
                    final ApplicationServerOperations server,
                    final GlobalDisplay display,
                    final SatelliteManager satelliteManager,
                    final Synctex synctex)
   {  
      display_ = display;
      server_ = server;
      synctex_ = synctex;
      
      eventBus.addHandler(ShowPDFViewerEvent.TYPE, this);
      eventBus.addHandler(CompilePdfCompletedEvent.TYPE, this);
      eventBus.addHandler(SynctexViewPdfEvent.TYPE, this);
      eventBus.addHandler(PDFLoadEvent.TYPE, this);
      PdfJsWindow.addPDFLoadHandler(this);
      PdfJsWindow.addPageClickHandler(this);
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
               pdfJsWindow_.injectUiOnLoad();
            }
         });
         display_.openMinimalWindow(url, false, 1000, 1000, options);
      }
   }

   @Override
   public void onPDFLoad(PDFLoadEvent event)
   {
      if (executeOnLoad_ != null)
      {
         executeOnLoad_.execute();
         executeOnLoad_ = null;
      }
   } 

   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {
      CompilePdfResult result = event.getResult();
      if (pdfJsWindow_ == null || !result.getSucceeded())
         return;
      
      pdfJsWindow_.initializeEvents();

      final PdfLocation pdfLocation = result.getPdfLocation();
      FileSystemItem pdf = FileSystemItem.createFile(result.getPdfPath());
      pdfJsWindow_.openPdf(server_.getFileUrl(pdf), 
            result.getPdfPath().equals(StringUtil.notNull(lastSuccessfulPdfPath_))
            ? pdfJsWindow_.getCurrentScale() : 1);
      executeOnLoad_ = new Command()
      {
         @Override
         public void execute()
         {
            if (pdfLocation != null)
            {
               pdfJsWindow_.goToPage(pdfLocation.getPage());
            }
         }
      };
      lastSuccessfulPdfPath_ = result.getPdfPath();
   }

   @Override
   public void onSynctexViewPdf(SynctexViewPdfEvent event)
   {
      pdfJsWindow_.goToPage(event.getPdfLocation().getPage());
      pdfJsWindow_.navigateTo(event.getPdfLocation());
   }
   
   @Override
   public void onPageClick(PageClickEvent event)
   {
      focusMainWindow();
      synctexInverseSearch(event.getCoordinates(), true);
   }

   private void synctexInverseSearch(SyncTexCoordinates coord, 
                                     boolean fromClick)
   {
      String pdfPath = lastSuccessfulPdfPath_;
      if (pdfPath != null)
      {
         synctex_.inverseSearch(PdfLocation.create(pdfPath,
                                                   coord.getPageNum(),
                                                   coord.getX(), 
                                                   coord.getY(), 
                                                   0, 
                                                   0,
                                                   fromClick));
      }
   }
   
   private final native void focusMainWindow() /*-{
      $wnd.focus();
   }-*/;

   private PdfJsWindow pdfJsWindow_;
   private Command executeOnLoad_;
   private String lastSuccessfulPdfPath_;

   private final GlobalDisplay display_;
   private final ApplicationServerOperations server_;
   private final Synctex synctex_;
}
