/*
 * PackageNameWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.widget.images.ProgressImages;

public class PackageNameWidget extends Composite implements HasClickHandlers
{
   public PackageNameWidget(String name)
   {
      HorizontalPanel panel = new HorizontalPanel();
      anchor_ = new Anchor(name, false);
      panel.add(anchor_);
      imagePanel_ = new SimplePanel();
      panel.add(imagePanel_);

      initWidget(panel);
   }

   public String getName()
   {
      return anchor_.getText();
   }

   public void showProgress(boolean show)
   {
      if (!show)
         imagePanel_.setWidget(null);
      else
      {
         final Image img = ProgressImages.createSmall();
         img.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
         img.getElement().getStyle().setMarginTop(-5, Style.Unit.PX);
         img.getElement().getStyle().setMarginBottom(-5, Style.Unit.PX);
         imagePanel_.setWidget(img);
      }
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return anchor_.addClickHandler(handler);
   }



   private final SimplePanel imagePanel_;
   private final Anchor anchor_;
}
