/*
 * ExplicitSizeWidget.java
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

package org.rstudio.core.client.theme;

import com.google.gwt.user.client.ui.Widget;

public class ExplicitSizeWidget implements ExplicitSize 
{
   public ExplicitSizeWidget(Widget widget, int height, int width)
   {
      height_ = height;
      width_ = width;
      widget_ = widget;
   }
   
   public ExplicitSizeWidget(Widget widget, ExplicitSize explicitSize)
   {
      widget_ = widget;
      explicitSize_ = explicitSize;
   }
   
   @Override
   public int getHeight()
   {
      return explicitSize_ == null ? height_ : explicitSize_.getHeight();
   }
   
   @Override
   public int getWidth()
   {
      return explicitSize_ == null ? width_ : explicitSize_.getWidth();
   }
   
   public Widget getWidget()
   {
      return widget_;
   }
     
   private int height_;
   private int width_;
   private Widget widget_;
   private ExplicitSize explicitSize_;
}
