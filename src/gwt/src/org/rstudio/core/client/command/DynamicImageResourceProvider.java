/*
 * DynamicImageResourceProvider.java
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

package org.rstudio.core.client.command;

import java.util.ArrayList;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;

public class DynamicImageResourceProvider implements ImageResourceProvider
{
   public DynamicImageResourceProvider(ImageResource image)
   {
      imageResource_ = image;
   }
   
   @Override
   public ImageResource getImageResource()
   {
      return imageResource_;
   }
   
   @Override
   public void addRenderedImage(Image image)
   {
      renderedImages_.add(image);
   }
   
   public void setImageResource(ImageResource image)
   {
      imageResource_ = image;
      AbstractImagePrototype protoImage = AbstractImagePrototype.create(image);
      for (Image img: renderedImages_)
      {
         protoImage.applyTo(img);
      }
   }
   
   private ImageResource imageResource_;
   private ArrayList<Image> renderedImages_ = new ArrayList<Image>();
}