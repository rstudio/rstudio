/*
 * CoreResources.java
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
package org.rstudio.core.client.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;

public interface CoreResources extends ClientBundle
{
   public static final CoreResources INSTANCE = GWT.create(CoreResources.class);

   @CssResource.NotStrict
   CoreStyles styles();

   @Source("progress_gray.gif")
   DataResource progress_gray_as_data();
   ImageResource progress();
   ImageResource progress_gray();
   ImageResource progress_large();
   ImageResource progress_large_gray();
   ImageResource iconEmpty();

   @Source("clear.gif")
   DataResource clear();
}
