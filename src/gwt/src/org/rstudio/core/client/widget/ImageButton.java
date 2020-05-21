/*
 * ImageButton.java
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

import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;

/**
 * An image that behaves like a button.
 */
public class ImageButton extends FocusWidget implements HasClickHandlers
{
   public ImageButton()
   {
      this(null, null);
   }

   public ImageButton(String description, ImageResource image)
   {
      ButtonElement button = Document.get().createPushButtonElement();
      button.setClassName("rstudio-ImageButton");
      button.setAttribute("type", "button");

      if (image != null)
         image_ = new DecorativeImage(image);
      else
         image_ = new DecorativeImage();

      image_.getElement().getStyle().setCursor(Cursor.POINTER);
      button.insertFirst(image_.getElement());

      descriptionSpan_ = Document.get().createSpanElement();
      A11y.setVisuallyHidden(descriptionSpan_);
      button.appendChild(descriptionSpan_);
      setDescription(description);

      setElement(button);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   public Image getImage()
   {
      return image_;
   }

   public void setDescription(String description)
   {
      if (!StringUtil.isNullOrEmpty(description))
         descriptionSpan_.setInnerText(description);
   }

   public void setResource(ImageResource resource)
   {
      image_.setResource(resource);
   }

   private SpanElement descriptionSpan_;
   private DecorativeImage image_;
}
