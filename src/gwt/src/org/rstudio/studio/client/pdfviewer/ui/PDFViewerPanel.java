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

import org.rstudio.studio.client.pdfviewer.PDFViewerPresenter;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;

public class PDFViewerPanel extends ResizeComposite 
                            implements PDFViewerPresenter.Display
{
   public PDFViewerPanel()
   {
      panel_ = new LayoutPanel();
      panel_.getElement().getStyle().setBackgroundColor("white");
      
      initWidget(panel_);
   }
   
  
   @Override
   public void setURL(String url)
   {
      // remove existing
      if (pdfWidget_ != null)
      {
         panel_.remove(pdfWidget_);
         pdfWidget_ = null;
      }
      
      // create new
      pdfWidget_ = new PDFWidget(url);
      panel_.add(pdfWidget_);
      panel_.setWidgetLeftRight(pdfWidget_, 0, Unit.PX, 0, Unit.PX);
      panel_.setWidgetTopBottom(pdfWidget_, 0, Unit.PX, 0, Unit.PX);
      
   }
   
   
   
   
   
   private LayoutPanel panel_;
   private PDFWidget pdfWidget_ = null;
}
