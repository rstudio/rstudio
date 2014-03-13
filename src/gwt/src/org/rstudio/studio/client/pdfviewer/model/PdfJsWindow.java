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

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.pdfviewer.events.PageClickEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PDFLoadEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

public class PdfJsWindow extends WindowEx
{
   protected PdfJsWindow() 
   {
   }
   
   public final native void injectUiOnLoad() /*-{
      var win = this;
      this.addEventListener("load", function() {
         // hide the Open File button; we don't need it
         var openFileButton = win.document.getElementById("openFile");
         if (openFileButton) {
            openFileButton.style.display = "none";
         }
         
         // change the behavior of the Bookmark button to sync to the
         // appropriate location in the code
         var bookmarkButton = win.document.getElementById("viewBookmark");
         if (bookmarkButton) {
            bookmarkButton.title = "Sync editor location to PDF view";
            bookmarkButton.href = "";
            bookmarkButton.addEventListener("click", function(evt) {
               var page = win.PDFView.page;
               var x = win.scrollX;
               var y = win.scrollY;
               @org.rstudio.studio.client.pdfviewer.model.PdfJsWindow::firePageClickEvent(III)(page, x, y);
            });
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
      });
   }-*/;
   
   public final native void openPdf(String path, float scale) /*-{
      this.PDFView.open(path, scale);
   }-*/;
   
   public final native void navigateTo(JavaScriptObject dest) /*-{
      if (dest == null)
         return;

      // this.PDFView.setScale(dest.scale);
      this.scrollTo(dest.x, dest.y);
   }-*/;

   public final native void goToPage(int page) /*-{
      this.PDFView.page = page;
   }-*/;

   public final native JavaScriptObject getNavigateDest() /*-{
      if (this.PDFView.pages.length == 0)
         return null;
      return {
         scale: this.PDFView.currentScaleValue,
         x: this.scrollX,
         y: this.scrollY
      };
   }-*/;
   
   public final native float getCurrentScale() /*-{
      return this.PDFView.currentScaleValue;
   }-*/;
   
   public final native void initializeEvents() /*-{
      var _pdfView = this.PDFView;
      var _setInitialView = _pdfView.setInitialView;
      _pdfView.setInitialView = function(storedHash, scale) {
         _setInitialView.call(_pdfView, storedHash, scale);
         @org.rstudio.studio.client.pdfviewer.model.PdfJsWindow::firePDFLoadEvent()();
      };
   }-*/;

   private static void firePDFLoadEvent()
   {
      handlers_.fireEvent(new PDFLoadEvent());
   }
   
   private static void firePageClickEvent(int page, int x, int y)
   {
      SyncTexCoordinates coords = new SyncTexCoordinates(page, x, y);
      handlers_.fireEvent(new PageClickEvent(coords));
   }

   public static HandlerRegistration addPDFLoadHandler(
         PDFLoadEvent.Handler handler)
   {
      return handlers_.addHandler(PDFLoadEvent.TYPE, handler);
   }

   public static HandlerRegistration addPageClickHandler(
         PageClickEvent.Handler handler)
   {
      return handlers_.addHandler(PageClickEvent.TYPE, handler);
   }

   private static final HandlerManager handlers_ = 
         new HandlerManager(PdfJsWindow.class);
}
