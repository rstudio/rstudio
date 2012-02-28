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
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.PageChangeEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.events.ScaleChangeEvent;

public class PDFView extends JavaScriptObject
{
   protected PDFView()
   {
   }
   
   public static native PDFView get() /*-{
      return $wnd.PDFView;
   }-*/;

   public final native void nextPage() /*-{
      this.page++;
   }-*/;

   public final native void previousPage() /*-{
      this.page-- ;
   }-*/;
   
   public final native int currentPage() /*-{
      return this.page;
   }-*/;

   public final native void goToPage(int page) /*-{
      this.page = page;
   }-*/;

   public final native void zoomIn() /*-{
      this.zoomIn() ;
   }-*/;
   
   public final native void zoomOut() /*-{
      this.zoomIn() ;
   }-*/;

   public final HandlerRegistration addPageChangeHandler(PageChangeEvent.Handler handler)
   {
      return handlers_.addHandler(PageChangeEvent.TYPE, handler);
   }

   public final HandlerRegistration addScaleChangeHandler(ScaleChangeEvent.Handler handler)
   {
      return handlers_.addHandler(ScaleChangeEvent.TYPE, handler);
   }

   public native static void initializeEvents() /*-{
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

   private static final HandlerManager handlers_ =
         new HandlerManager(PDFView.class);
}
