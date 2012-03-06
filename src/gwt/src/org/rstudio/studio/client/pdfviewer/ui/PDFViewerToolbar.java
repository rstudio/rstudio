/*
 * PDFViewerToolbar.java
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
import com.google.gwt.event.dom.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.InlineToolbarButton;
import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.core.client.widget.SpanLabel;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.pdfviewer.ui.images.Resources;

public class PDFViewerToolbar extends Composite
      implements PDFViewerToolbarDisplay
{
   interface Binder extends UiBinder<Widget, PDFViewerToolbar>
   {}

   public PDFViewerToolbar()
   {
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      final Resources resources = GWT.create(Resources.class);

      zoomOut_.addMouseDownHandler(new MouseDownHandler()
      {
         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            zoomOut_.setResource(resources.zoomButtonLeftPressed());
         }
      });
      zoomOut_.addMouseUpHandler(new MouseUpHandler()
      {
         @Override
         public void onMouseUp(MouseUpEvent event)
         {
            zoomOut_.setResource(resources.zoomButtonLeft());
         }
      });
      zoomOut_.addMouseOutHandler(new MouseOutHandler()
      {
         @Override
         public void onMouseOut(MouseOutEvent event)
         {
            zoomOut_.setResource(resources.zoomButtonLeft());
         }
      });
      zoomIn_.addMouseDownHandler(new MouseDownHandler()
      {
         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            zoomIn_.setResource(resources.zoomButtonRightPressed());
         }
      });
      zoomIn_.addMouseUpHandler(new MouseUpHandler()
      {
         @Override
         public void onMouseUp(MouseUpEvent event)
         {
            zoomIn_.setResource(resources.zoomButtonRight());
         }
      });
      zoomIn_.addMouseOutHandler(new MouseOutHandler()
      {
         @Override
         public void onMouseOut(MouseOutEvent event)
         {
            zoomIn_.setResource(resources.zoomButtonRight());
         }
      });
   }

   @Override
   public HasClickHandlers getPrevButton()
   {
      return btnPrevious_;
   }

   @Override
   public HasClickHandlers getNextButton()
   {
      return btnNext_;
   }

   @Override
   public HasClickHandlers getThumbnailsButton()
   {
      return btnThumbnails_;
   }

   @Override
   public void setPageCount(int pageCount)
   {
      pageCountLabel_.setText(pageCount + "");
   }

   @Override
   public void setPdfFile(FileSystemItem pdfFile)
   {
      filename_.setText(pdfFile.getName());
      filename_.setTitle(pdfFile.getPath());
   }

   @Override
   public HasClickHandlers getZoomOut()
   {
      return zoomOut_;
   }

   @Override
   public HasClickHandlers getZoomIn()
   {
      return zoomIn_;
   }

   @Override
   public HasValue<String> getPageNumber()
   {
      return pageNumber_;
   }

   @Override
   public void selectPageNumber()
   {
      pageNumber_.selectAll();
   }
   
   @Override
   public void setActionsMenu(ToolbarPopupMenu menu)
   {
      btnActions_.setMenu(menu);
   }

   @UiField
   InlineToolbarButton btnActions_;
   @UiField
   InlineToolbarButton btnPrevious_;
   @UiField
   InlineToolbarButton btnNext_;
   @UiField
   InlineToolbarButton btnThumbnails_;
   @UiField
   SpanLabel pageCountLabel_;
   @UiField
   Anchor filename_;
   @UiField
   Image zoomOut_;
   @UiField
   Image zoomIn_;
   @UiField
   NumericTextBox pageNumber_;
   @UiField
   Image fileIcon_;
}
