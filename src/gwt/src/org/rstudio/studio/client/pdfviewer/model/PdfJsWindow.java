/*
 * PDFJsWindow.java
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
package org.rstudio.studio.client.pdfviewer.model;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.synctex.model.PdfLocation;
import org.rstudio.studio.client.pdfviewer.events.LookupSynctexSourceEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PDFLoadEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PdfJsLoadEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PdfJsWindowClosedEvent;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler;

// This class wraps a reference to the window hosting PDF.js, and is intended to
// encapsulate its interface. Any coupling with the UI or internals of PDF.js
// goes here.
public class PdfJsWindow extends WindowEx
{
   protected PdfJsWindow() 
   {
   }
   
   public final native void injectUiOnLoad(boolean forDesktop) /*-{
      var win = this;
      
      // runs when we detect that the document has fully loaded
      var initUi = function() {
         // inject our own CSS
         var rstudioCss = win.document.createElement("link");
         rstudioCss.rel = "stylesheet";
         rstudioCss.href = "viewer-rstudio.css";
         win.document.head.appendChild(rstudioCss);
      
         // hide the Open File and Bookmark buttons; these don't make sense in
         // our context
         var openFileButton = win.document.getElementById("openFile");
         if (openFileButton) {
            openFileButton.style.display = "none";
         }
         var bookmarkButton = win.document.getElementById("viewBookmark");
         if (bookmarkButton) {
            bookmarkButton.style.display = "none";
         }

         // create a Jump to Source toolbar button (image and style are applied
         // in viewer-rstudio.css)
         var jumpToSource = win.document.createElement("button");
         jumpToSource.className = "toolbarButton";
         jumpToSource.id = "jumpToSource";
         jumpToSource.title = "Sync editor location to PDF view";
         jumpToSource.addEventListener("click", function(evt) {
            @org.rstudio.studio.client.pdfviewer.model.PdfJsWindow::fireLookupCurrentViewEvent(Lorg/rstudio/studio/client/pdfviewer/model/PdfJsWindow;)(win);
         });

         var jumpToSourceContent = win.document.createElement("span");
         jumpToSourceContent.innerText = "Jump to Source Location";
         jumpToSource.appendChild(jumpToSourceContent);

         // put the Jump to Source button on the right toolbar
         var toolbarRight = win.document.getElementById("toolbarViewerRight");
         if (toolbarRight) {
            toolbarRight.insertBefore(jumpToSource, toolbarRight.firstChild);
         }
         
         // make the sidebar open by default
         var container = win.document.getElementById("outerContainer");
         if (container) {
            container.className += " sidebarOpen";
            win.PDFView.sidebarOpen = true;
         }
         var sidebarToggle = win.document.getElementById("sidebarToggle");
         if (sidebarToggle) {
            sidebarToggle.className += " toggled";
         }
         
         win.document.title = "RStudio: View PDF";

         // override the set initial view function so we know when the PDF has 
         // finished loading
         var _pdfView = win.PDFView;
         var _setInitialView = _pdfView.setInitialView;
         _pdfView.setInitialView = function(storedHash, scale) {
            _setInitialView.call(_pdfView, storedHash, scale);
            @org.rstudio.studio.client.pdfviewer.model.PdfJsWindow::firePDFLoadEvent()();
         }
         
         // stub out the set-title functions so our window title sticks
         _pdfView.setTitle = function() {};
         _pdfView.setTitleUsingUrl = function() {};
         
         // fire the load event
         @org.rstudio.studio.client.pdfviewer.model.PdfJsWindow::fireLoadEvent()();
      };

      // starts a timer that initializes the UI when the PDFView object appears
      var startInitUiTimer = function()
      {
         var n = 0, t = 0;
         var checkPDFViewAvailable = function() {
            n++;
            if (typeof(win.PDFView) === "object") {
               // PDFView is available now, initialize it
               initUi();
               clearInterval(t);
               return true;
            }
            else if (n > 100) {
               // cap at 100 tries (~5s)
               clearInterval(t);
            }
            return false;
         }
         if (!checkPDFViewAvailable()) {
            // if the PDFView object isn't available yet, check every 50ms 
            // until it is
            t = window.setInterval(checkPDFViewAvailable, 50);
         }
      }

      // if the document is finished loading, start watching for the PDFView
      // object to become available; if not, we can hook the load event 
      if (document.readyState === "complete")
         startInitUiTimer();
      else
         this.addEventListener("load", initUi);
      
      var unloadEvt = forDesktop ? "unload" : "beforeunload";
      this.addEventListener(unloadEvt, function() {
         @org.rstudio.studio.client.pdfviewer.model.PdfJsWindow::fireWindowClosedEvent()();
      });
      
      this.addEventListener("click", function(evt) {
         @org.rstudio.studio.client.pdfviewer.model.PdfJsWindow::firePageClickEvent(Lorg/rstudio/studio/client/pdfviewer/model/PdfJsWindow;Lcom/google/gwt/dom/client/NativeEvent;Lcom/google/gwt/dom/client/Element;)(win, evt, evt.target);
      });
   }-*/;
   
   public final native void openPdf(String path, float scale, 
                                    boolean synctex) /*-{
      this.PDFView.open(path, scale);
      var jumpToSource = this.document.getElementById("jumpToSource");
      if (jumpToSource)
      {
         jumpToSource.style.display = synctex ? "inline-block" : "none";
      }
   }-*/;
   
   public final native void goToPage(int page) /*-{
      this.PDFView.page = page;
   }-*/;

   public final native int getCurrentPage () /*-{
      return this.PDFView.page;
   }-*/;

   public final native String getLocationHash() /*-{
      var bookmark = this.document.getElementById("viewBookmark");
      if (bookmark !== null)
      {
         return bookmark.href.substring(bookmark.href.indexOf("#") + 1);
      }
      return null;
   }-*/;
   
   public final native void applyLocationHash(String hash) /*-{
      if (hash !== null)
         this.PDFView.setHash(hash);
   }-*/;
   
   public final native float getCurrentScale() /*-{
      return this.PDFView.currentScale;
   }-*/;

   private static void firePDFLoadEvent()
   {
      handlers_.fireEvent(new PDFLoadEvent());
   }
   
   private static void fireLookupCurrentViewEvent(PdfJsWindow win)
   {
      SyncTexCoordinates coords = getBoundaryCoordinates(win, true);
      handlers_.fireEvent(new LookupSynctexSourceEvent(coords, false));
   }
   
   private static void fireWindowClosedEvent()
   {
      handlers_.fireEvent(new PdfJsWindowClosedEvent());
   }
   
   private static void fireLoadEvent()
   {
      handlers_.fireEvent(new PdfJsLoadEvent());
   }
   
   public static HandlerRegistration addPDFLoadHandler(
         PDFLoadEvent.Handler handler)
   {
      return handlers_.addHandler(PDFLoadEvent.TYPE, handler);
   }

   public static HandlerRegistration addPageClickHandler(
         LookupSynctexSourceEvent.Handler handler)
   {
      return handlers_.addHandler(LookupSynctexSourceEvent.TYPE, handler);
   }

   public static HandlerRegistration addWindowClosedHandler(
         PdfJsWindowClosedEvent.Handler handler)
   {
      return handlers_.addHandler(PdfJsWindowClosedEvent.TYPE, handler);
   }

   public static HandlerRegistration addPdfJsLoadHandler(
         PdfJsLoadEvent.Handler handler)
   {
      return handlers_.addHandler(PdfJsLoadEvent.TYPE, handler);
   }

   private static void firePageClickEvent(PdfJsWindow win, 
                                          NativeEvent nativeEvent, 
                                          Element el)
   {
      if (!DomUtils.isCommandClick(nativeEvent))
         return;
      
      Element pageEl = el;
      while (pageEl != null)
      {
         if (pageEl.getId().matches("^pageContainer([\\d]+)$"))
         {
            break;
         }

         pageEl = pageEl.getParentElement();
      }

      if (pageEl == null)
         return;

      int page = getContainerPageNum(pageEl);

      int pageX = nativeEvent.getClientX() +
                  win.getDocument().getScrollLeft() +
                  win.getDocument().getBody().getScrollLeft() -
                  pageEl.getAbsoluteLeft();
      int pageY = nativeEvent.getClientY() +
                  win.getDocument().getDocumentElement().getScrollTop() +
                  win.getDocument().getBody().getScrollTop() -
                  pageEl.getAbsoluteTop();

      handlers_.fireEvent(new LookupSynctexSourceEvent(new SyncTexCoordinates(
            page,
            (int) ((pageX / win.getCurrentScale() / 96) * 72),
            (int) ((pageY / win.getCurrentScale() / 96) * 72)), true));
   }

   private static SyncTexCoordinates getBoundaryCoordinates(
         PdfJsWindow win, 
         boolean top)
   {
      Element container = getViewerContainer(win);
      int scrollY = container.getScrollTop();

      // linear probe our way to the current page
      Element viewerEl = win.getDocument().getElementById("viewer");
      for (int i = 1; i < viewerEl.getChildCount(); i+=2)
      {
         Node childNode = viewerEl.getChild(i);
         if (Element.is(childNode))
         {
            Element el = Element.as(childNode);

            if (DomUtils.bottomRelativeTo(container, el) > scrollY)
            {
               int pageNum = getContainerPageNum(el);
               int pageY = scrollY - DomUtils.topRelativeTo(container, el);
               if (pageY < 0)
                  pageY = 0;

               if (!top)
                  pageY += container.getClientHeight();
               
               return new SyncTexCoordinates(
                     pageNum,
                     0,
                     (int) ((pageY / win.getCurrentScale() / 96) * 72));
            }
         }
      }
      return null;
   }

   public static void navigateTo(final PdfJsWindow win, 
                                 final PdfLocation pdfLocation)
   {
      double factor = win.getCurrentScale()* 96 / 72;

      final double x = pdfLocation.getX() * factor;
      final double y = pdfLocation.getY() * factor;
      final double w = pdfLocation.getWidth() * factor;
      final double h = pdfLocation.getHeight() * factor;

      final Value<Integer> retries = new Value<Integer>(0);

      // Sometimes pageContainer is null during load, so retry every 100ms
      // until it's not, or we've tried 40 times.
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            Element pageContainer = win.getDocument().getElementById(
                  "pageContainer" + pdfLocation.getPage());

            if (pageContainer == null)
            {
               retries.setValue(retries.getValue() + 1);
               return retries.getValue() < 40;
            }

            if (pdfLocation.isFromClick())
            {
               final DivElement div = win.getDocument().createDivElement();
               div.getStyle().setPosition(Style.Position.ABSOLUTE);
               div.getStyle().setTop(y, Unit.PX);
               div.getStyle().setLeft(x, Unit.PX);
               div.getStyle().setWidth(w, Unit.PX);
               div.getStyle().setHeight(h, Unit.PX);
               div.getStyle().setBackgroundColor("rgba(0, 126, 246, 0.1)");
               div.getStyle().setProperty("transition", "opacity 4s");
               // use DomUtils to set transition styles so gwt doesn't assert
               // an invalid style name (no camelCase) in debug mode
               DomUtils.setStyle(div, "-moz-transition", "opacity 4s");
               DomUtils.setStyle(div, "-webkit-transition", "opacity 4s");

               pageContainer.appendChild(div);

               Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand()
               {
                  @Override
                  public boolean execute()
                  {
                     div.getStyle().setOpacity(0.0);
                     return false;
                  }
               }, 2000);
            }

            // scroll to the page
            win.goToPage(pdfLocation.getPage());

            // if the target isn't on-screen then scroll to it
            SyncTexCoordinates boundary = getBoundaryCoordinates(win, false);
            if (boundary != null && 
                pdfLocation.getY() > boundary.getY())
            {
               Element container = getViewerContainer(win);
               container.setScrollTop(Math.max(
                     0, 
                     DomUtils.topRelativeTo(
                           getViewerContainer(win), 
                           pageContainer) + (int) y - 180));
            }

            win.focus();
            return false;
         }
      }, 100);
   }

   private static int getContainerPageNum(Element container)
   {
      return Integer.parseInt(
            container.getId().substring("pageContainer".length()));
   }
   
   private static Element getViewerContainer(PdfJsWindow win)
   {
      return win.getDocument().getElementById("viewerContainer");
   }

   private static final HandlerManager handlers_ = 
         new HandlerManager(PdfJsWindow.class);
}
