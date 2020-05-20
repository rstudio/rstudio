/*
 * ProgressImage.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.resources.CoreResources;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

public class ProgressImage extends Composite
{
   public ProgressImage()
   {
      this(CoreResources.INSTANCE.progress());
   }
   
   public ProgressImage(ImageResource image)
   {
      image_ = image;
      panel_ = new SimplePanel();
      initWidget(panel_);
   }
   
   public void show(boolean show)
   {
      if (!show)
      {
         panel_.setWidget(null);
         setVisible(false);
      }
      else
      {
         final Image img = new Image(image_);
         panel_.setWidget(img);
         setVisible(true);
      }
   }

   private ImageResource image_;
   private SimplePanel panel_;
}
