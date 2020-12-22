/*
 * LeftRightToggleButton.java
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

import com.google.gwt.aria.client.PressedValue;
import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.ClassIds;
import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

public class LeftRightToggleButton extends Widget
   implements HasClickHandlers
{
   interface Resources extends ClientBundle
   {
      @Source("images/LeftToggleLeftOn.png")
      ImageResource leftToggleLeftOn();
      @Source("images/LeftToggleRightOn.png")
      ImageResource leftToggleRightOn();
      @Source("images/LeftToggleLeftOff.png")
      ImageResource leftToggleLeftOff();
      @Source("images/LeftToggleRightOff.png")
      ImageResource leftToggleRightOff();
      @Source("images/RightToggleLeftOn.png")
      ImageResource rightToggleLeftOn();
      @Source("images/RightToggleRightOn.png")
      ImageResource rightToggleRightOn();
      @Source("images/RightToggleLeftOff.png")
      ImageResource rightToggleLeftOff();
      @Source("images/RightToggleRightOff.png")
      ImageResource rightToggleRightOff();

      @Source("LeftRightToggleButton.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String container();
      String leftLeft();
      String leftRight();
      String rightLeft();
      String rightRight();
      String leftOn();
      String rightOn();
   }

   interface Binder extends UiBinder<Element, LeftRightToggleButton>
   {}

   public LeftRightToggleButton(String leftLabel, String rightLabel,
                                boolean leftIsOn)
   {
      setElement(GWT.<Binder>create(Binder.class).createAndBindUi(this));
      Styles styles = GWT.<Resources>create(Resources.class).styles();
      left_.setInnerText(leftLabel);
      left_.addClassName(ThemeResources.INSTANCE.themeStyles().handCursor());
      right_.setInnerText(rightLabel);
      right_.addClassName(ThemeResources.INSTANCE.themeStyles().handCursor());
      if (leftIsOn)
         addStyleName(styles.leftOn());
      else
         addStyleName(styles.rightOn());

      setClassId();
      Roles.getButtonRole().setAriaPressedState(left_, leftIsOn ? PressedValue.TRUE : PressedValue.FALSE);
      Roles.getButtonRole().setAriaPressedState(right_, leftIsOn ? PressedValue.FALSE : PressedValue.TRUE);
   }

   public void setClassId()
   {
      ClassIds.assignClassId(getElement(),
         ClassIds.LEFT_RIGHT_TOGGLE_BTN + " _ " +
            left_.getInnerText() + "_" +
            right_.getInnerText());
      ClassIds.assignClassId(left_, ClassIds.LEFT_TOGGLE_BTN + "_"  + left_.getInnerText());
      ClassIds.assignClassId(right_, ClassIds.RIGHT_TOGGLE_BTN + "_"  + right_.getInnerText());
   }
   
   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   static
   {
      GWT.<Resources>create(Resources.class).styles().ensureInjected();
   }

   @UiField
   Element left_;
   @UiField
   Element right_;
}
