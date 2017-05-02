/*
 * ImageResource2x.java
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
package org.rstudio.core.client.resources;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;

public class ImageResource2x implements ImageResource
{
   public ImageResource2x(ImageResource ref2x)
   {
      this(null, ref2x);
   }

   public ImageResource2x(ImageResource ref, ImageResource ref2x)
   {
      ref_ = ref;
      ref2x_ = ref2x;
   }

   private boolean getUse2xResolution()
   {
      return use2xResolution_ && ref2x_ != null;
   }

   private ImageResource getResource()
   {
      return getUse2xResolution() ? ref2x_ : ref_;
   }
   
   @Override
   public String getName()
   {
      return getResource().getName();
   }

   @Override
   public int getHeight()
   {
      return getUse2xResolution() ? getResource().getHeight() / 2 : getResource().getHeight();
   }

   @Override
   public int getLeft()
   {
      return getResource().getLeft();
   }

   @Override
   public SafeUri getSafeUri()
   {
      return getResource().getSafeUri();
   }

   @Override
   public int getTop()
   {
      return getResource().getTop();
   }

   @SuppressWarnings("deprecation")
   @Override
   public String getURL()
   {
      return getResource().getURL();
   }

   @Override
   public int getWidth()
   {
      return getUse2xResolution() ? getResource().getWidth() / 2 : getResource().getWidth();
   }

   @Override
   public boolean isAnimated()
   {
      return getResource().isAnimated();
   }

   public SafeHtml getSafeHtml()
   {
      if (html_ == null)
      {
         SafeHtmlBuilder sb = new SafeHtmlBuilder();

         sb.appendHtmlConstant("<img src=\"");
         sb.appendHtmlConstant(getSafeUri().asString());
         sb.appendHtmlConstant("\" width=\"");
         sb.appendHtmlConstant(new Integer(getWidth()).toString());
         sb.appendHtmlConstant("\" height=\"");
         sb.appendHtmlConstant(new Integer(getHeight()).toString());
         sb.appendHtmlConstant("\">");
         
         html_ = sb.toSafeHtml();
      }

      return html_;
   }
   
   private ImageResource ref_;
   private ImageResource ref2x_;
   private SafeHtml html_;
   private boolean use2xResolution_ = true;
}