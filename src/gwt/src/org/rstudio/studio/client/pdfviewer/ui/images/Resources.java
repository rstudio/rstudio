/*
 * Resources.java
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
package org.rstudio.studio.client.pdfviewer.ui.images;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundle
{
   @Source("FileActionsIcon.png")
   ImageResource fileActionsIcon();

   @Source("NextPageIcon.png")
   ImageResource nextPageIcon();

   @Source("PreviousPageIcon.png")
   ImageResource previousPageIcon();

   @Source("SizeButton.png")
   ImageResource sizeButton();

   @Source("SizeButtonPressed.png")
   ImageResource sizeButtonPressed();

   @Source("ZoomButtonLeft.png")
   ImageResource zoomButtonLeft();

   @Source("ZoomButtonLeftPressed.png")
   ImageResource zoomButtonLeftPressed();

   @Source("ZoomButtonRight.png")
   ImageResource zoomButtonRight();

   @Source("ZoomButtonRightPressed.png")
   ImageResource zoomButtonRightPressed();

   @Source("ZoomInIcon.png")
   ImageResource zoomInIcon();

   @Source("ZoomOutIcon.png")
   ImageResource zoomOutIcon();

   @Source("ThumbnailsIcon.png")
   ImageResource thumbnailsIcon();
}
