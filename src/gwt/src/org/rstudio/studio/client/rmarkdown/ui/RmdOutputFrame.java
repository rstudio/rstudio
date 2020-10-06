/*
 * RmdOutputFrame.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;

// Represents a frame that contains a rendered R Markdown document.
// Implementations currently include a frame in an IDE pane (RmdOutputFramePane)
// and a frame embedded in a satellite window (RmdOutputFrameSatellite)
public interface RmdOutputFrame 
{
   public void closeOutputFrame(boolean forReopen);
   public WindowEx getWindowObject();
   public void showRmdPreview(RmdPreviewParams params, boolean activate);
   public String getViewerType();
   RmdPreviewParams getPreviewParams();
   public int getScrollPosition();
   public String getAnchor();
}
