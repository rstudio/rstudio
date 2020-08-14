/*
 * WindowFrameButton.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import org.rstudio.core.client.ClassIds;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CanSetControlId;
import org.rstudio.core.client.widget.DoubleClickState;

/**
 * Minimize/maximize/restore buttons in logical windows (the 4 quadrants)
 */
public class WindowFrameButton extends FocusWidget
                               implements CanSetControlId
{
   public WindowFrameButton(String name, WindowState state)
   {
      super(new HTML().getElement());

      name_ = name;
      defaultState_ = state;
      maximized_ = false;
      exclusive_ = false;
      Roles.getButtonRole().set(getElement());
      updateLabel();
      getElement().setTabIndex(0);
      addStyleName(ThemeStyles.INSTANCE.handCursor());
   }

   public void setExclusive(boolean exclusive)
   {
      exclusive_ = exclusive;
      updateLabel();
   }

   public void setMaximized(boolean maximized)
   {
      maximized_ = maximized;
      updateLabel();
   }

   /**
    * update accessibility label based on button's default state (max, min) and current
    * state of its window (normal, maximized, or exclusive)
    */
   private  void updateLabel()
   {
      WindowState computedState = defaultState_;
      if (maximized_ || exclusive_)
         computedState = WindowState.NORMAL; // "restore"
      Roles.getButtonRole().setAriaLabelProperty(getElement(), stateString(computedState) + " " + name_);
   }

   public void setClickHandler(Command clickHandler)
   {
      clickHandler_ = clickHandler;
      registerClickHandler();
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      Debug.logWarning("WindowFrameButton: for keyboard support use setClickHandler instead of addClickHandler");
      return super.addClickHandler(handler);
   }

   private void registerClickHandler()
   {
      if (isAttached() && clickHandler_ != null)
      {
         releaseOnUnload_.add(super.addClickHandler(clickEvent ->
         {
            clickEvent.preventDefault();
            clickEvent.stopPropagation();
            if (!doubleClickState_.checkForDoubleClick(clickEvent.getNativeEvent()))
               click();
         }));

         releaseOnUnload_.add(addKeyPressHandler(event ->
         {
            char charCode = event.getCharCode();
            if (charCode == KeyCodes.KEY_ENTER || charCode == KeyCodes.KEY_SPACE)
            {
               event.preventDefault();
               event.stopPropagation();
               click();
            }
         }));
      }
   }

   private void click()
   {
      if (clickHandler_ == null)
         return;
      clickHandler_.execute();
   }

   @Override
   protected void onLoad()
   {
      registerClickHandler();
   }

   @Override
   protected void onUnload()
   {
      releaseOnUnload_.removeHandler();
   }

   @Override
   public void setElementId(String id)
   {
      getElement().setId(id);
   }

   public void setClassId(String classId, String panelName)
   {
      ClassIds.assignClassId(getElement(), classId + "_" + ClassIds.idSafeString(panelName));
   }

   private String stateString(WindowState state)
   {
      switch (state)
      {
      case MINIMIZE:
         return "Minimize";

      case MAXIMIZE:
         return "Maximize";

      case NORMAL:
      default:
         return "Restore";

      case HIDE:
         return "Hide";

      case EXCLUSIVE:
         return "Exclusive";
      }
   }
   
   private final String name_;
   private final WindowState defaultState_;

   // modifiers for accessible label based on overall state of logical window
   private boolean maximized_;
   private boolean exclusive_;

   private Command clickHandler_;
   private final HandlerRegistrations releaseOnUnload_ = new HandlerRegistrations();
   private final DoubleClickState doubleClickState_ = new DoubleClickState();
}
