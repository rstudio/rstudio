/*
 * Base64ImageResource.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;

public class Base64ImageResource implements ImageResource
{
   public Base64ImageResource(String name, String data, int width, int height)
   {
      name_ = name;
      data_ = data;
      width_ = width;
      height_ = height;
   }

   @Override
   public String getName()
   {
      return name_;
   }

   @Override
   public int getHeight()
   {
      return height_;
   }

   @Override
   public int getLeft()
   {
      return 0;
   }

   @Override
   public SafeUri getSafeUri()
   {
      return new SafeUri()
      {
         @Override
         public String asString()
         {
            return data_;
         }
      };
   }

   @Override
   public int getTop()
   {
      return 0;
   }

   @Override
   public String getURL()
   {
      return data_;
   }

   @Override
   public int getWidth()
   {
      return width_;
   }

   @Override
   public boolean isAnimated()
   {
      return false;
   }
   
   private final String name_;
   private final String data_;
   private final int width_;
   private final int height_;
}
