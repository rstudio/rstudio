package org.rstudio.studio.client.pdfviewer.model;

import org.rstudio.core.client.dom.WindowEx;

public class PdfJsWindow extends WindowEx
{
   protected PdfJsWindow() 
   {
   }
   
   public final native void openPdf(String path, float scale) /*-{
      this.PDFView.open(path, scale);
   }-*/;
}
