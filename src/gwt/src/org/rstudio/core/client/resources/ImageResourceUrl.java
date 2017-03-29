/*
 * ImageResourceUrl.java
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

public class ImageResourceUrl implements ImageResource
{
   public ImageResourceUrl(SafeUri url, int width, int height)
   {
      url_ = url;
      width_ = width;
      height_ = height;
   }
   
   @Override
   public String getName()
   {
      return "";
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
      return url_;
   }

   @Override
   public int getTop()
   {
      return 0;
   }

   @SuppressWarnings("deprecation")
   @Override
   public String getURL()
   {
      return url_.asString();
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

   public SafeHtml getSafeHtml()
   {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();

      sb.appendHtmlConstant("<img src=\"");
      sb.appendHtmlConstant(getSafeUri().asString());
      sb.appendHtmlConstant("\" width=\"");
      sb.appendHtmlConstant(new Integer(getWidth()).toString());
      sb.appendHtmlConstant("\" height=\"");
      sb.appendHtmlConstant(new Integer(getHeight()).toString());
      sb.appendHtmlConstant("\">");

      return sb.toSafeHtml();
   }

   private SafeUri url_;
   private int width_;
   private int height_;
}