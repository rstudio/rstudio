/*
 * PDFWidget.java
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

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;

import org.rstudio.studio.client.pdfviewer.pdfjs.PdfJs;

public class PDFWidget extends Composite
{
   public PDFWidget(String pdfUrl)
   {
      pdfUrl_ = pdfUrl;
      canvas_ = Canvas.createIfSupported();
      initWidget(canvas_);
   }

   @Override
   protected void onLoad()
   {
      if (!initialized_)
      {
         initialized_ = true;

         PdfJs.load(new Command()
         {
            @Override
            public void execute()
            {
               loadPdf(pdfUrl_, canvas_.getContext2d());
            }
         });
      }
   }

   private native void loadPdf(String pdfUrl, Context2d context2d) /*-{
      $wnd.alert("got here: " + pdfUrl);
      $wnd.PDFJS.getPdf(pdfUrl, function(data) {
         $wnd.alert("got here 2");
         try
         {
        //
        // Instantiate PDFDoc with PDF data
        //
        var pdf = new $wnd.PDFJS.PDFDoc(data);
        var page = pdf.getPage(1);
        var scale = 1.5;

        //
        // Render PDF page into canvas context
        //
        page.startRendering(context2d);
         }
         catch (e)
         {
            $wnd.alert(e);
         }
      });
   }-*/;

   private final Canvas canvas_;
   private boolean initialized_;
   private final String pdfUrl_;
}
