/*
 * FancyButton.java
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
package org.rstudio.studio.client.application.ui.appended;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FocusWidget;

public class FancyButton extends FocusWidget
{
   static
   {
      Resources res = GWT.create(Resources.class);
      res.styles().ensureInjected();
   }

   interface Resources extends ClientBundle
   {
      @Source("FancyButton.css")
      Styles styles();
      
      @ImageOptions(repeatStyle = RepeatStyle.None)
      ImageResource buttonLeft();

      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource buttonTile();
      
      @ImageOptions(repeatStyle = RepeatStyle.None)
      ImageResource buttonRight();
   }

   interface Styles extends CssResource
   {
      String fancy();
      String left();
      String inner();
      String right();
   }

   interface Binder extends UiBinder<ButtonElement, FancyButton> {}

   public FancyButton()
   {
      Binder binder = GWT.create(Binder.class);
      setElement(binder.createAndBindUi(this));
   }

   public void setText(String text)
   {
      content_.setInnerText(text);
   }

   public HandlerRegistration addClickHandler(ClickHandler clickHandler)
   {
      return addDomHandler(clickHandler, ClickEvent.getType());
   }

   @UiField
   TableCellElement content_;
}
