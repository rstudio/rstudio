/*
 * ToolbarPopupMenuButton.java
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
package org.rstudio.core.client.widget;

import org.rstudio.studio.client.common.icons.StandardIcons;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;

public class ToolbarPopupMenuButton extends ToolbarButton
                                    implements HasValueChangeHandlers<String>
{
   public ToolbarPopupMenuButton()
   {
      this(true);
   }

   public ToolbarPopupMenuButton(boolean showText)
   {
      super("",
            StandardIcons.INSTANCE.empty_command(),
            new ToolbarPopupMenu());
      showText_ = showText;
   }

   @Override
   public HandlerRegistration addValueChangeHandler(
         ValueChangeHandler<String> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   public void addMenuItem(String text, final String value)
   {
      ToolbarPopupMenu menu = getMenu();

      menu.addItem(new MenuItem(text, new Command()
      {
         @Override
         public void execute()
         {
            setText(value);
         }
      }));
   }

   public void addMenuItem(final String value)
   {
      addMenuItem(value, value);
   }
   
   public void clearMenu()
   {
      getMenu().clearItems();
   }
   
   @Override
   public void setText(String text)
   {
      setText(text, true);
   }
   
   public void setText(String text, boolean fireEvent)
   {
      boolean changed = !text_.equals(text);
      
      text_ = text;
      if (showText_)
      {
         super.setText(text);
      }
      
      if (changed && fireEvent)
         ValueChangeEvent.fire(ToolbarPopupMenuButton.this, text);
   }
   
   private String text_;
   private boolean showText_;
}
