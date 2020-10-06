/*
 * SimpleButton.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FocusWidget;
import org.rstudio.core.client.command.AppCommand;

public class SimpleButton extends FocusWidget
   implements HasClickHandlers
{
   interface MyBinder extends UiBinder<Element, SimpleButton> {}
   private static final MyBinder binder = GWT.create(MyBinder.class);
   static {
      ((Resources)GWT.create(Resources.class)).styles().ensureInjected();
   }

   interface Resources extends ClientBundle
   {
      @Source("SimpleButton.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String simpleButton();
   }

   public SimpleButton(AppCommand command)
   {
      this();
      setText(command.getButtonLabel());
      setTitle(command.getTooltip());
      addClickHandler(command);
   }

   public SimpleButton(String text)
   {
      this(text, false);
   }

   public SimpleButton(String text, boolean asHtml)
   {
      this();
      setText(text, asHtml);
   }

   public SimpleButton()
   {
      setElement(binder.createAndBindUi(this));
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   public void setText(String text)
   {
      setText(text, false);
   }

   public void setText(String text, boolean asHtml)
   {
      if (asHtml)
         content_.setInnerHTML(text);
      else
         content_.setInnerText(text);
   }

   public void click()
   {
      ((ButtonElement)getElement().cast()).click();
   }

   @UiField
   DivElement content_;
}
