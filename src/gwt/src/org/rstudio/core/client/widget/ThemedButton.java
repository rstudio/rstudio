/*
 * ThemedButton.java
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
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FocusWidget;

public class ThemedButton extends FocusWidget implements HasClickHandlers
{
   static final Resources RESOURCES = GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }

   interface Resources extends ClientBundle
   {
      @Source("ThemedButton.css")
      Styles styles();

      @Source("buttonLeftFocusEnabled.png")
      DataResource buttonLeftFocusEnabled();
      @Source("buttonLeftFocusSelected.png")
      DataResource buttonLeftFocusSelected();
      @Source("buttonLeftFocusPressed.png")
      DataResource buttonLeftFocusPressed();
      @Source("buttonLeftEnabled.png")
      DataResource buttonLeftEnabled();
      @Source("buttonLeftDisabled.png")
      DataResource buttonLeftDisabled();
      @Source("buttonLeftSelected.png")
      DataResource buttonLeftSelected();
      @Source("buttonLeftPressed.png")
      DataResource buttonLeftPressed();
      @Source("buttonRightFocusEnabled.png")
      DataResource buttonRightFocusEnabled();
      @Source("buttonRightFocusSelected.png")
      DataResource buttonRightFocusSelected();
      @Source("buttonRightFocusPressed.png")
      DataResource buttonRightFocusPressed();
      @Source("buttonRightEnabled.png")
      DataResource buttonRightEnabled();
      @Source("buttonRightDisabled.png")
      DataResource buttonRightDisabled();
      @Source("buttonRightSelected.png")
      DataResource buttonRightSelected();
      @Source("buttonRightPressed.png")
      DataResource buttonRightPressed();
      @Source("buttonTileFocusEnabled.png")
      DataResource buttonTileFocusEnabled();
      @Source("buttonTileFocusSelected.png")
      DataResource buttonTileFocusSelected();
      @Source("buttonTileFocusPressed.png")
      DataResource buttonTileFocusPressed();
      @Source("buttonTileEnabled.png")
      DataResource buttonTileEnabled();
      @Source("buttonTileDisabled.png")
      DataResource buttonTileDisabled();
      @Source("buttonTileSelected.png")
      DataResource buttonTileSelected();
      @Source("buttonTilePressed.png")
      DataResource buttonTilePressed();
   }

   interface Styles extends CssResource
   {
      String themedButton();
      String left();
      String buttonLeft();
      String buttonCenter();
      String buttonRight();
      String buttonContent();
      String tight();
   }

   interface MyUiBinder extends UiBinder<ButtonElement, ThemedButton>{}
   private static final MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

   public ThemedButton()
   {
      this("");
   }

   public ThemedButton(String title)
   {
      this(title, null);
   }

   public ThemedButton(String title, ClickHandler clickHandler)
   {
      button_ = uiBinder.createAndBindUi(this);
      setElement(button_);

      setStylePrimaryName("gwt-Button");
      addStyleName(RESOURCES.styles().themedButton());

      content_.setInnerText(title);

      if (clickHandler != null)
         addClickHandler(clickHandler);

      Roles.getPresentationRole().set(wrapper_);
   }

   public void setLeftAligned(boolean isLeft)
   {
      if (isLeft)
         addStyleName(RESOURCES.styles().left());
      else
         removeStyleName(RESOURCES.styles().left());
   }

   public HandlerRegistration addClickHandler(final ClickHandler clickHandler)
   {
      // Suppress click event if button is disabled
      return addDomHandler(clickEvent ->
      {
         if (isEnabled())
            clickHandler.onClick(clickEvent);
      }, ClickEvent.getType());
   }

   public boolean isEnabled()
   {
      return !button_.isDisabled();
   }

   public void setEnabled(boolean isEnabled)
   {
      button_.setDisabled(!isEnabled);
   }

   public void setDefault(boolean isDefault)
   {
      if (isDefault != isDefault_)
      {
         isDefault_ = isDefault;
         if (isDefault_)
            addStyleDependentName("DefaultDialogAction");
         else
            removeStyleDependentName("DefaultDialogAction");
      }
   }

   public void setTight(boolean tight)
   {
      if (tight)
         addStyleName(RESOURCES.styles().tight());
      else
         removeStyleName(RESOURCES.styles().tight());
   }

   public boolean isDefault()
   {
      return isDefault_;
   }

   public void setText(String text)
   {
      content_.setInnerText(text);
   }

   public void click()
   {
      button_.click();
   }

   public void setWrapperWidth(String width)
   {
      wrapper_.setWidth(width);
   }

   final ButtonElement button_;
   boolean isDefault_ = false;

   @UiField
   DivElement content_;
   @UiField
   TableElement wrapper_;
}
