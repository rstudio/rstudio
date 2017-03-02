/*
 * ImageResource.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;

public class ImageResource2x implements ImageResource
{
   public ImageResource2x(ImageResource ref)
   {
      ref_ = ref;
   }
   
   @Override
   public String getName()
   {
      return ref_.getName();
   }

   @Override
   public int getHeight()
   {
      return ref_.getHeight() / 2;
   }

   @Override
   public int getLeft()
   {
      return ref_.getLeft();
   }

   @Override
   public SafeUri getSafeUri()
   {
      return ref_.getSafeUri();
   }

   @Override
   public int getTop()
   {
      return ref_.getTop();
   }

   @Override
   public String getURL()
   {
      return ref_.getURL();
   }

   @Override
   public int getWidth()
   {
      return ref_.getWidth() / 2;
   }

   @Override
   public boolean isAnimated()
   {
      return ref_.isAnimated();
   }
   
   private ImageResource ref_;
}