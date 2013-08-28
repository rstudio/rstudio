/*
 * ImageMenuLabel.java
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

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class ImageMenuLabel extends Composite implements MenuLabel
{
   public ImageMenuLabel(ImageResource res, String text)
   {
      HTMLPanel panel = new HTMLPanel("");
      initWidget(panel);

      label_ = new Label(text);
      label_.setWordWrap(false);
      image_ = new Image(res);
      panel.add(image_);
      panel.add(label_);

      image_.getElement().getStyle().setVerticalAlign(VerticalAlign.MIDDLE);
      label_.getElement().getStyle().setMarginLeft(2, Unit.PX);
      label_.getElement().getStyle().setDisplay(Display.INLINE);
   }
   
   public void setText(String text)
   {
      label_.setText(text);
   }
   
   public void setImage(ImageResource res)
   {
      AbstractImagePrototype proto = AbstractImagePrototype.create(res);
      proto.applyTo(image_);
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return label_.addClickHandler(handler);
   }

   private Label label_;
   private Image image_;
}
