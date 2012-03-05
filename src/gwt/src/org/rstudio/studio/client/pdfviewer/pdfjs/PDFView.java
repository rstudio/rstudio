/*
 * PDFView.java
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
package org.rstudio.studio.client.pdfviewer.pdfjs;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PDFLoadEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PageChangeEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.ScaleChangeEvent;

public class PDFView extends JavaScriptObject
{
   protected PDFView()
   {
   }

   public static native void nextPage() /*-{
      $wnd.PDFView.page++;
   }-*/;

   public static native void previousPage() /*-{
      $wnd.PDFView.page-- ;
   }-*/;
   
   public static native int currentPage() /*-{
      return $wnd.PDFView.page;
   }-*/;

   public static native int pageCount() /*-{
      return $wnd.PDFView.pages.length;
   }-*/;

   public static native void goToPage(int page) /*-{
      $wnd.PDFView.page = page;
   }-*/;

   public static native void zoomIn() /*-{
      $wnd.PDFView.zoomIn() ;
   }-*/;
   
   public static native void zoomOut() /*-{
      $wnd.PDFView.zoomOut() ;
   }-*/;

   public native static void parseScale(String value) /*-{
      $wnd.PDFView.parseScale(value);
   }-*/;

   public static HandlerRegistration addPageChangeHandler(PageChangeEvent.Handler handler)
   {
      return handlers_.addHandler(PageChangeEvent.TYPE, handler);
   }

   public static HandlerRegistration addScaleChangeHandler(ScaleChangeEvent.Handler handler)
   {
      return handlers_.addHandler(ScaleChangeEvent.TYPE, handler);
   }

   public static HandlerRegistration addPDFLoadHandler(PDFLoadEvent.Handler handler)
   {
      return handlers_.addHandler(PDFLoadEvent.TYPE, handler);
   }

   public native static void initializeEvents() /*-{

      var _load = $wnd.PDFView.load;
      $wnd.PDFView.load = $entry(function(data, scale) {
         _load.call($wnd.PDFView, data, scale);
         @org.rstudio.studio.client.pdfviewer.pdfjs.PDFView::firePDFLoadEvent()();
      });

      $wnd.addEventListener(
            "pagechange",
            $entry(function(evt) {
               @org.rstudio.studio.client.pdfviewer.pdfjs.PDFView::firePageChangeEvent()();
            }),
            true);

      $wnd.addEventListener(
            "scalechange",
            $entry(function(evt) {
               @org.rstudio.studio.client.pdfviewer.pdfjs.PDFView::fireScaleChangeEvent()();
            }),
            true);
   }-*/;

   private static void firePageChangeEvent()
   {
      handlers_.fireEvent(new PageChangeEvent());
   }

   private static void fireScaleChangeEvent()
   {
      handlers_.fireEvent(new ScaleChangeEvent());
   }

   private static void firePDFLoadEvent()
   {
      handlers_.fireEvent(new PDFLoadEvent());
   }

   public static void setLoadingVisible(boolean visible)
   {
      Element el = Document.get().getElementById("loading");
      if (visible)
         el.removeAttribute("hidden");
      else
         el.setAttribute("hidden", "hidden");
   }

   private static final HandlerManager handlers_ =
         new HandlerManager(PDFView.class);
}
