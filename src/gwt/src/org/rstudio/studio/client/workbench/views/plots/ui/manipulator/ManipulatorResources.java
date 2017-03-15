/*
 * ManipulatorResources.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;

public interface ManipulatorResources extends ClientBundle
{
   public static final ManipulatorResources INSTANCE = GWT.create(ManipulatorResources.class);

   @Source("ManipulatorStyles.css")
   ManipulatorStyles manipulatorStyles();
   
   @Source("manipulateButton_2x.png")
   ImageResource manipulateButton2x();

   ImageResource manipulateProgress();
   
   @Source("manipulateSliderBar.png")
   DataResource manipulateSliderBar();
}
