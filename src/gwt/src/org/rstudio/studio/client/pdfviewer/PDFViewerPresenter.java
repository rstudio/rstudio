/*
 * PDFViewerPresenter.java
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

import org.rstudio.studio.client.pdfviewer.model.PDFViewerParams;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PDFViewerPresenter implements IsWidget
{
   public interface Display extends IsWidget
   {     
      void setURL(String url);
   }
   
   @Inject
   public PDFViewerPresenter(Display view)
   {
      view_ = view;
   }

   public void onActivated(PDFViewerParams params)
   {
   }
   
   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   
   private final Display view_;
}
