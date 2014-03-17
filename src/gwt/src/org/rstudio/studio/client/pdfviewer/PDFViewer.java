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
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfResult;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.satellite.events.WindowOpenedEvent;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.events.SynctexViewPdfEvent;
import org.rstudio.studio.client.common.synctex.model.PdfLocation;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerEvent;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerHandler;
import org.rstudio.studio.client.pdfviewer.events.LookupSynctexSourceEvent;
import org.rstudio.studio.client.pdfviewer.model.PdfJsWindow;
import org.rstudio.studio.client.pdfviewer.model.SyncTexCoordinates;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PDFLoadEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PdfJsWindowClosedEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PDFViewer implements CompilePdfCompletedEvent.Handler,
                                  ShowPDFViewerHandler,
                                  SynctexViewPdfEvent.Handler,
                                  PDFLoadEvent.Handler,
                                  LookupSynctexSourceEvent.Handler,
                                  PdfJsWindowClosedEvent.Handler,
                                  WindowOpenedEvent.Handler
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
      eventBus.addHandler(WindowOpenedEvent.TYPE, this);
      PdfJsWindow.addPDFLoadHandler(this);
      PdfJsWindow.addPageClickHandler(this);
      PdfJsWindow.addWindowClosedHandler(this);

      // when this window is closed, automatically close the PDF.js window,
      // if it's open
      Window.addCloseHandler(new CloseHandler<Window>()
      {
         @Override
         public void onClose(CloseEvent<Window> event)
         {
            if (pdfJsWindow_ != null)
               pdfJsWindow_.close();
         }
      });
   }

   @Override
   public void onShowPDFViewer(ShowPDFViewerEvent event)
   {
      openPdfJsWindow();
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

   @Override
   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {
      CompilePdfResult result = event.getResult();
      if (!result.getSucceeded())
         return;
      
      pdfJsWindow_.initializeEvents();

      final PdfLocation pdfLocation = result.getPdfLocation();
      pdfJsWindow_.openPdf("/" + result.getViewPdfUrl(),
            result.getPdfPath().equals(StringUtil.notNull(lastSuccessfulPdfPath_))
            ? pdfJsWindow_.getCurrentScale() : 1);
      executeOnLoad_ = new Command()
      {
         @Override
         public void execute()
         {
            if (pdfLocation != null)
            {
               PdfJsWindow.navigateTo(pdfJsWindow_, pdfLocation);
            }
         }
      };
      lastSuccessfulPdfPath_ = result.getPdfPath();
   }

   @Override
   public void onSynctexViewPdf(SynctexViewPdfEvent event)
   {
      if (event.getPdfLocation().getFile().equals(lastSuccessfulPdfPath_))
         PdfJsWindow.navigateTo(pdfJsWindow_, event.getPdfLocation());
   }
   
   @Override
   public void onLookupSynctexSource(LookupSynctexSourceEvent event)
   {
      if (Desktop.isDesktop())
      {
         Desktop.getFrame().bringMainFrameToFront();
      }
      else
      {
         focusMainWindow();
      }
      synctexInverseSearch(event.getCoordinates(), event.fromClick());
   }

   @Override
   public void onPdfJsWindowClosed(PdfJsWindowClosedEvent event)
   {
      synctex_.notifyPdfViewerClosed(lastSuccessfulPdfPath_);
   }
   
   @Override
   public void onWindowOpened(WindowOpenedEvent event)
   {
      if (event.getName().equals(WINDOW_NAME))
      {
         completePdfJsWindowLoad(event.getWindow());
      }
   }
  
   // Private methods ---------------------------------------------------------
   
   private void openPdfJsWindow()
   {
      int width = 1070;
      int height = 1200;
      if (pdfJsWindow_ != null && !pdfJsWindow_.isClosed())
      {
         width = pdfJsWindow_.getOuterWidth();
         height = pdfJsWindow_.getOuterHeight();
         pdfJsWindow_.close();
         pdfJsWindow_ = null;
      }
      String url = GWT.getHostPageBaseURL() + "pdf_js/web/viewer.html?file=";
      NewWindowOptions options = new NewWindowOptions();
      options.setName(WINDOW_NAME);
      options.setCallback(new OperationWithInput<WindowEx>() 
      {
         @Override
         public void execute(WindowEx win)
         {
            completePdfJsWindowLoad(win);
         }
      });
      display_.openMinimalWindow(url, false, width, height, options);
      lastSuccessfulPdfPath_ = null;
   }
   
   private void completePdfJsWindowLoad(WindowEx win)
   {
      PdfJsWindow pdfJs = win.cast();
      pdfJsWindow_ = pdfJs;
      pdfJsWindow_.injectUiOnLoad();
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
   
   private final static String WINDOW_NAME = "rstudio_pdfjs";
}
