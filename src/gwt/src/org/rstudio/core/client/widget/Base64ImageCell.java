/*
 * Base64ImageCell.java
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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class Base64ImageCell extends AbstractCell<String>
{
   public Base64ImageCell()
   {
      width_ = 0;
      height_ = 0;
   }
   
   public Base64ImageCell(int width, int height)
   {
      width_  = width;
      height_ = height;
   }

   @Override
   public void render(com.google.gwt.cell.client.Cell.Context ctx, String value,
         SafeHtmlBuilder builder)
   {
      if (value != null)
      {
         builder.appendHtmlConstant("<img src=\"" + value + "\"");
         // apply width and height if specified
         if (width_ > 0)
            builder.appendHtmlConstant(" width=\"" + width_ + "\"");
         if (height_ > 0)
            builder.appendHtmlConstant(" height=\"" + height_ + "\"");
         builder.appendHtmlConstant("/>");
      }
   }
   
   private final int width_;
   private final int height_;
}
