/*
 * WebMenuCallback.java
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
package org.rstudio.core.client.command.impl;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuBar;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.MenuCallback;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.AttachEvent;

import java.util.Stack;

public class WebMenuCallback implements MenuCallback
{
   public void beginMainMenu()
   {
      AppMenuBar mainMenu = new AppMenuBar(false);
      mainMenu.setEscClosesAll(false);
      menuStack_.push(mainMenu);
   }

   public void beginMenu(String label)
   {
      AppMenuBar newMenu = new AppMenuBar(true);
      newMenu.setEscClosesAll(false);

      // Adjust the z-index of the displayed sub-menu, so that it (and any
      // adorning contents) are rendered in front of their parents.
      final int depth = menuStack_.size();
      newMenu.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            ElementPredicate callback = (Element el) -> {
               return el.getParentElement().getTagName().toLowerCase().contentEquals("body");
            };

            Element popupEl = DomUtils.findParentElement(newMenu.getElement(), callback);
            if (popupEl == null)
               return;

            Style style = DomUtils.getComputedStyles(popupEl);
            int oldIndex = StringUtil.parseInt(style.getZIndex(), -1);
            if (oldIndex == -1)
               return;

            int newIndex = oldIndex + depth;
            popupEl.getStyle().setZIndex(newIndex);
         }
      });

      label = AppMenuItem.replaceMnemonics(label, "");
      head().addItem(label, newMenu);

      menuStack_.push(newMenu);
   }

   public void addCommand(String commandId, AppCommand command)
   {
      head().addItem(command.createMenuItem(true));
   }

   public void addSeparator()
   {
      head().addSeparator();
   }

   public void endMenu()
   {
      menuStack_.pop();
   }

   public void endMainMenu()
   {
      result_ = menuStack_.pop();
   }

   public AppMenuBar getMenu()
   {
      return result_;
   }

   private AppMenuBar head()
   {
      return menuStack_.peek();
   }

   private final Stack<AppMenuBar> menuStack_ = new Stack<AppMenuBar>();
   private AppMenuBar result_;
}
