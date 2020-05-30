/*
 * FancyButton.java
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
package org.rstudio.studio.client.application.ui.appended;

import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
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
   }

   interface Styles extends CssResource
   {
      String fancy();
   }

   interface Binder extends UiBinder<ButtonElement, FancyButton> {}

   public FancyButton()
   {
      Binder binder = GWT.create(Binder.class);
      setElement(binder.createAndBindUi(this));
      addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
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
   ButtonElement content_;
}
