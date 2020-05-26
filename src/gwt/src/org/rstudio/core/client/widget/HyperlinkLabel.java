/*
 * HyperlinkLabel.java
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
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.theme.res.ThemeStyles;

public class HyperlinkLabel extends Label
{
   public HyperlinkLabel()
   {
      super();
      this.setStyleName("rstudio-HyperlinkLabel");
      Roles.getLinkRole().set(getElement());
      getElement().setTabIndex(0);
   }
   
   public HyperlinkLabel(String caption, Command clickHandler)
   {
      super(caption); 
      clickHandler_ = clickHandler;
      this.setStyleName("rstudio-HyperlinkLabel");
      this.addStyleName(ThemeStyles.INSTANCE.handCursor());
      Roles.getLinkRole().set(getElement());
      getElement().setTabIndex(0);
   }
   
   public HyperlinkLabel(String caption)
   {
      this(caption, null);
   }
   
   public void setClickHandler(Command clickHandler)
   {
      clickHandler_ = clickHandler;
      registerClickHandler();
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler) {
      Debug.logWarning("HyperlinkLabel: for keyboard support use setClickHandler instead of addClickHandler");
      return super.addClickHandler(handler);
   }

   private class MouseHandlers implements MouseOverHandler,
                                          MouseOutHandler
   {
      public void onMouseOver(MouseOverEvent event)
      {
         if (!alwaysUnderline_)
            addStyleDependentName("Link");
      }

      public void onMouseOut(MouseOutEvent event)
      {
         if ( !alwaysUnderline_)
            removeStyleDependentName("Link");
      }
   }
   
   public void setAlwaysUnderline(boolean alwaysUnderline)
   {
      alwaysUnderline_ = alwaysUnderline;
      if (alwaysUnderline_)
         addStyleDependentName("Link");
      else
         removeStyleDependentName("Link");
   }
   
   public void setClearUnderlineOnClick(boolean clearOnClick)
   {
      clearUnderlineOnClick_ = clearOnClick;
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
      return addDomHandler(handler, KeyPressEvent.getType());
   }

   @Override 
   protected void onLoad()
   {
      releaseOnUnload_.add(addMouseOverHandler(mouseHandlers_));
      releaseOnUnload_.add(addMouseOutHandler(mouseHandlers_));
      registerClickHandler();
   }

   @Override
   protected void onUnload()
   {
      releaseOnUnload_.removeHandler();
   }

   private void registerClickHandler()
   {
      if (isAttached() && clickHandler_ != null)
      {
         releaseOnUnload_.add(super.addClickHandler(event -> click()));

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
      
      if (clearUnderlineOnClick_)
         removeStyleDependentName("Link");
      clickHandler_.execute();
   }

   private final MouseHandlers mouseHandlers_ = new MouseHandlers();
   private Command clickHandler_;
   private final HandlerRegistrations releaseOnUnload_ = new HandlerRegistrations();

   private boolean alwaysUnderline_ = false;
   private boolean clearUnderlineOnClick_ = false;
}
