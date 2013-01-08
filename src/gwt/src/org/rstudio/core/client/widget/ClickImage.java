/*
 * ClickImage.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;

public class ClickImage extends Image
{
   public ClickImage()
   {
      super();
      commonInit();
   }

   public ClickImage(ImageResource resource)
   {
      super(resource);
      commonInit();
   }

   public ClickImage(String url)
   {
      super(url);
      commonInit();
   }

   public ClickImage(String url, int left, int top, int width, int height)
   {
      super(url, left, top, width, height);
      commonInit();
   }

   public ClickImage(Element element)
   {
      super(element);
      commonInit();
   }

   private void commonInit()
   {
      getElement().getStyle().setCursor(Cursor.POINTER);
   }
}
