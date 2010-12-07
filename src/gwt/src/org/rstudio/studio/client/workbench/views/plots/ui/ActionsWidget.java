/*
 * ActionsWidget.java
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
package org.rstudio.studio.client.workbench.views.plots.ui;

import com.google.gwt.user.client.ui.Composite;
import org.rstudio.core.client.widget.ImageFrame;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

public abstract class ActionsWidget extends Composite
{
   public abstract void onPlotChanged(String plotDownloadUrl,
                                      int width,
                                      int height);

   public abstract boolean shouldPositionOnTopRight();

   public void initialize(ImageFrame imagePreview, PlotsServerOperations server)
   {
      imagePreview_ = imagePreview;
      server_ = server;
   }

   protected ImageFrame imagePreview_;
   protected PlotsServerOperations server_;
}
