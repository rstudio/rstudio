/*
 * PDFViewer.java
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
package org.rstudio.studio.client.pdfviewer;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.Operation;
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
import org.rstudio.studio.client.pdfviewer.events.LookupSynctexSourceEvent;
import org.rstudio.studio.client.pdfviewer.model.PdfJsWindow;
import org.rstudio.studio.client.pdfviewer.model.SyncTexCoordinates;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PDFLoadEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PdfJsLoadEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PdfJsWindowClosedEvent;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PDFViewer implements CompilePdfCompletedEvent.Handler,
                                  SynctexViewPdfEvent.Handler,
                                  PDFLoadEvent.Handler,
                                  LookupSynctexSourceEvent.Handler,
                                  PdfJsLoadEvent.Handler,
                                  PdfJsWindowClosedEvent.Handler,
                                  WindowOpenedEvent.Handler
{
   @Inject
   public PDFViewer(EventBus eventBus,
                    final ApplicationServerOperations server,
                    final GlobalDisplay display,
                    final SatelliteManager satelliteManager,
                    final Synctex synctex,
                    final UserPrefs prefs)
   {  
      display_ = display;
      server_ = server;
      synctex_ = synctex;
      prefs_ = prefs;
      
      eventBus.addHandler(CompilePdfCompletedEvent.TYPE, this);
      eventBus.addHandler(SynctexViewPdfEvent.TYPE, this);
      eventBus.addHandler(PDFLoadEvent.TYPE, this);
      eventBus.addHandler(WindowOpenedEvent.TYPE, this);
      PdfJsWindow.addPDFLoadHandler(this);
      PdfJsWindow.addPageClickHandler(this);
      PdfJsWindow.addWindowClosedHandler(this);
      PdfJsWindow.addPdfJsLoadHandler(this);

      // when this window is closed, automatically close the PDF.js window,
      // if it's open
      Window.addCloseHandler(new CloseHandler<Window>()
      {
         @Override
         public void onClose(CloseEvent<Window> event)
         {
            if (pdfJsWindow_ != null)
               pdfJsWindow_.close();
            pdfJsWindow_ = null;
         }
      });
   }

   @Override
   public void onPDFLoad(PDFLoadEvent event)
   {
      if (executeOnPdfLoad_ != null)
      {
         executeOnPdfLoad_.execute();
         executeOnPdfLoad_ = null;
      }
   } 

   @Override
   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {
      // only handle PDF compile events when we're the preferred viewer
      if (prefs_.pdfPreviewer().getValue() != UserPrefs.PDF_PREVIEWER_RSTUDIO)
         return;
      
      // only handle successful compiles
      final CompilePdfResult result = event.getResult();
      if (!result.getSucceeded())
         return;

      // when the PDF is finished rendering, optionally navigate to the desired
      // location, or set and restore the current location
      final PdfLocation pdfLocation = result.getPdfLocation();
      if (pdfLocation != null)
      {
         executeOnPdfLoad_ = new Operation()
         {
            @Override
            public void execute()
            {
               PdfJsWindow.navigateTo(pdfJsWindow_, pdfLocation);
            }
         };
      }
         
      lastSuccessfulPdfPath_ = result.getPdfPath();
      openPdfUrl(result.getViewPdfUrl(), result.isSynctexAvailable(), 
                 pdfLocation == null);
   }

   @Override
   public void onSynctexViewPdf(SynctexViewPdfEvent event)
   {
      if (event.getPdfLocation().getFile() == lastSuccessfulPdfPath_)
      {
         PdfJsWindow.navigateTo(pdfJsWindow_, event.getPdfLocation());
         if (Desktop.hasDesktopFrame())
         {
            Desktop.getFrame().activateMinimalWindow(WINDOW_NAME);
         }
      }
   }
   
   @Override
   public void onLookupSynctexSource(LookupSynctexSourceEvent event)
   {
      if (lastSuccessfulPdfPath_ != null)
      {
         if (Desktop.hasDesktopFrame())
         {
            Desktop.getFrame().bringMainFrameToFront();
         }
         else
         {
            focusMainWindow();
         }
         synctexInverseSearch(event.getCoordinates(), event.fromClick());
      }
   }

   @Override
   public void onPdfJsWindowClosed(PdfJsWindowClosedEvent event)
   {
      synctex_.notifyPdfViewerClosed(lastSuccessfulPdfPath_);
      locationHash_ = pdfJsWindow_.getLocationHash();
      pdfJsWindow_ = null;
      lastSuccessfulPdfPath_ = null;
   }
   
   @Override
   public void onWindowOpened(WindowOpenedEvent event)
   {
      if (event.getName() == WINDOW_NAME)
      {
         initializePdfJsWindow(event.getWindow());
      }
   }
   
   @Override
   public void onPdfJsLoad(PdfJsLoadEvent event)
   {
      if (executeOnPdfJsLoad_ != null)
      {
         executeOnPdfJsLoad_.execute();
         executeOnPdfJsLoad_ = null;
      }
   }
   
   public void viewPdfUrl(final String url, final Integer initialPage)
   {
      if (initialPage != null)
      {
         executeOnPdfLoad_ = new Operation()
         {
            @Override
            public void execute()
            {
               pdfJsWindow_.goToPage(initialPage.intValue());
            }
         };
      }
      lastSuccessfulPdfPath_ = null;
      openPdfUrl(url, false, initialPage == null);
   }
  
   // Private methods ---------------------------------------------------------
   
   private void openPdfUrl(final String url, final boolean synctex, 
                           boolean restorePosition)
   {
      int width = 1070;
      int height = 1200;
      Point pos = null;
      
      // if there's a window open, restore the position when we're done
      if (restorePosition && url == lastSuccessfulPdfUrl_)
      {
         // if we don't have an active window, we'll use the hash stored when
         // the window closed
         if (haveActivePdfJsWindow())
            locationHash_ = pdfJsWindow_.getLocationHash();
         executeOnPdfLoad_ = createRestorePositionOperation();
      }
      
      // create the operation to load the PDF--we'll call this when the window
      // is finished opening, or immediately if there's already a window open
      Operation loadPdf = new Operation()
      {
         @Override
         public void execute()
         {
            pdfJsWindow_.openPdf(server_.getApplicationURL(url), 0, synctex);
            lastSuccessfulPdfUrl_ = url;
         }
      };

      // in the browser we need to close and reopen the window
      if (haveActivePdfJsWindow() && !Desktop.hasDesktopFrame())
      {
         width = pdfJsWindow_.getOuterWidth();
         height = pdfJsWindow_.getOuterHeight();
         pos = Point.create(pdfJsWindow_.getLeft(), pdfJsWindow_.getTop());
         pdfJsWindow_.close();
         pdfJsWindow_ = null;
      }
      
      lastSuccessfulPdfUrl_ = null;
      if (!haveActivePdfJsWindow())
      {
          // open the window and continue
          String viewerUrl = 
                server_.getApplicationURL("pdf_js/web/viewer.html?file=");
          NewWindowOptions options = new NewWindowOptions();
          options.setName(WINDOW_NAME);
          options.setShowDesktopToolbar(false);
          if (pos != null)
             options.setPosition(pos);
          options.setCallback(new OperationWithInput<WindowEx>() 
          {
             @Override
             public void execute(WindowEx win)
             {
                initializePdfJsWindow(win);
             }
          });
          executeOnPdfJsLoad_ = loadPdf;
          display_.openWebMinimalWindow(viewerUrl, false, width, height, options);
      }
      else
      {
         // we already have an open window, activate it
         if (Desktop.hasDesktopFrame())
            Desktop.getFrame().activateMinimalWindow(WINDOW_NAME);
         
         loadPdf.execute();
      }
   }
   
   private boolean haveActivePdfJsWindow()
   {
      return pdfJsWindow_ != null && !pdfJsWindow_.isClosed();
   }
   
   private void initializePdfJsWindow(WindowEx win)
   {
      pdfJsWindow_ = win.cast();
      pdfJsWindow_.injectUiOnLoad(Desktop.hasDesktopFrame());
   }

   private void synctexInverseSearch(SyncTexCoordinates coord, 
                                     boolean fromClick)
   {
      synctex_.inverseSearch(PdfLocation.create(lastSuccessfulPdfPath_,
                                                coord.getPageNum(),
                                                coord.getX(), 
                                                coord.getY(), 
                                                0, 
                                                0,
                                                fromClick));
   }
   
   private Operation createRestorePositionOperation()
   {
      return new Operation()
      {
         @Override
         public void execute()
         {
            pdfJsWindow_.applyLocationHash(locationHash_);
            locationHash_ = null;
         }
      };
   }
   
   private final native void focusMainWindow() /*-{
      $wnd.focus();
   }-*/;

   private PdfJsWindow pdfJsWindow_;
   private String lastSuccessfulPdfPath_;
   private String lastSuccessfulPdfUrl_;
   private String locationHash_;

   // continuation operations for asynchronous operations: 
   // pdf.js loaded, PDF loaded in pdf.js
   private Operation executeOnPdfJsLoad_;
   private Operation executeOnPdfLoad_;

   private final GlobalDisplay display_;
   private final ApplicationServerOperations server_;
   private final Synctex synctex_;
   private final UserPrefs prefs_;
   
   private final static String WINDOW_NAME = "rstudio_pdfjs";
}
