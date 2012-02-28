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

   public final native void goToPage(int page) /*-{
      this.page = page;
   }-*/;

   public final native void zoomIn() /*-{
      this.zoomIn() ;
   }-*/;
   
   public final native void zoomOut() /*-{
      this.zoomIn() ;
   }-*/;
}
