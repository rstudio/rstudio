/*
 * SmallButton.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FocusWidget;
import org.rstudio.core.client.command.AppCommand;

public class SmallButton extends FocusWidget
   implements HasClickHandlers
{
   interface MyBinder extends UiBinder<Element, SmallButton> {}
   private static final MyBinder binder = GWT.create(MyBinder.class);
   static {
      ((Resources)GWT.create(Resources.class)).styles().ensureInjected();
   }

   interface Resources extends ClientBundle
   {
      @Source("SmallButton.css")
      Styles styles();

      ImageResource smallButtonLeft();
      ImageResource smallButtonRight();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource smallButtonTile();
   }

   interface Styles extends CssResource
   {
      String smallButton();
      String buttonLeft();
      String buttonCenter();
      String buttonRight();
      String buttonContent();
   }

   public SmallButton(AppCommand command)
   {
      this();
      setText(command.getButtonLabel());
      setTitle(command.getTooltip());
      addClickHandler(command);
   }

   public SmallButton(String text)
   {
      this(text, false);
   }

   public SmallButton(String text, boolean asHtml)
   {
      this();
      setText(text, asHtml);
   }

   public SmallButton()
   {
      setElement(binder.createAndBindUi(this));
      Roles.getPresentationRole().set(table_);
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

   public void fillWidth()
   {
      table_.setWidth("100%");
   }

   public void click()
   {
      ((ButtonElement)getElement().cast()).click();
   }

   @UiField
   DivElement content_;
   @UiField
   TableElement table_;
}
