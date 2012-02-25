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
package org.rstudio.studio.client.pdfviewer;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ResizeComposite;

public class PDFViewerPanel extends ResizeComposite 
                            implements PDFViewerPresenter.Display
{
   public PDFViewerPanel()
   {
      label_ =  new RequiresResizeLabel();
      
      initWidget(label_);
   }
   
  
   @Override
   public void setURL(String url)
   {
      label_.setText("PDF URL: " + url);
      
   }
   
   private class RequiresResizeLabel extends Label implements RequiresResize
   {

      @Override
      public void onResize()
      {
         // TODO Auto-generated method stub
         
      }
      
   }
   
   private RequiresResizeLabel label_;
}
