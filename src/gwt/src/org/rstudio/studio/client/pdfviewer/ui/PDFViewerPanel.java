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
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.dom.WindowEx;
import com.google.inject.Inject;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.pdfviewer.PDFViewerPresenter;
import org.rstudio.studio.client.pdfviewer.events.InitCompleteEvent;
import org.rstudio.studio.client.pdfviewer.pdfjs.PdfJs;

public class PDFViewerPanel extends Composite
                            implements PDFViewerPresenter.Display
{
   interface Binder extends UiBinder<Widget, PDFViewerPanel>
   {}

   @Inject
   public PDFViewerPanel()
   {
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      toolbar_.addStyleName(ThemeStyles.INSTANCE.toolbar());
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

               fireEvent(new InitCompleteEvent());
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
   
   @Override
   public void closeWindow()
   {
      WindowEx.get().close();
   }

   @Override
   public HandlerRegistration addInitCompleteHandler(
                                              InitCompleteEvent.Handler handler)
   {
      return addHandler(handler, InitCompleteEvent.TYPE);
   }

   private native void open(String url) /*-{
      $wnd.PDFView.open(url, 0);
   }-*/;

   private boolean loaded_;
   private String initialUrl_;
   private boolean once_;
   @UiField
   Toolbar toolbar_;
}
