/*
 * PDFViewerPanel.java
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
package org.rstudio.studio.client.pdfviewer.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.pdfviewer.PDFViewerPresenter;
import org.rstudio.studio.client.pdfviewer.pdfjs.PDFView;
import org.rstudio.studio.client.pdfviewer.pdfjs.PdfJs;

public class PDFViewerPanel extends Composite
                            implements PDFViewerPresenter.Display
{
   interface Binder extends UiBinder<Widget, PDFViewerPanel>
   {}

   public PDFViewerPanel()
   {       
      btnPrevious_ = new ThemedButton("Previous", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            PDFView.get().previousPage();
         }
      });
      
      btnNext_ = new ThemedButton("Next", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            PDFView.get().nextPage();
         }
      });
      
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      if (!once_)
      {
         once_ = true;
         PdfJs.load(new Command()
         {
            @Override
            public void execute()
            {
               loaded_ = true;
               if (initialUrl_ != null)
                  open(initialUrl_);
            }
         });
      }
   }

   @Override
   public void setURL(String url)
   {
      if (loaded_)
         open(url);
      else
         initialUrl_ = url;
   }

   private native void open(String url) /*-{
      $wnd.PDFView.open(url, 0);
   }-*/;

   
   @UiField(provided = true)
   protected ThemedButton btnNext_;
   @UiField(provided = true)
   protected ThemedButton btnPrevious_;
   
   private boolean loaded_;
   private String initialUrl_;
   private boolean once_;
}
