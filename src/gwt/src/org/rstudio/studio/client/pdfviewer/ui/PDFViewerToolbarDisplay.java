/*
 * PDFViewerToolbarDisplay.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.pdfviewer.ui;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.HasButtonMethods;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasValue;

public interface PDFViewerToolbarDisplay
{
   HasButtonMethods getViewPdfExternal();
   HasButtonMethods getJumpToSource();
   HasButtonMethods getPrevButton();
   HasButtonMethods getNextButton();
   HasButtonMethods getThumbnailsButton();
   HasClickHandlers getZoomOut();
   HasClickHandlers getZoomIn();
   HasValue<String> getPageNumber();
   void selectPageNumber();
   void setPageCount(int pageCount);
   void setPdfFile(FileSystemItem pdfFile);
}
