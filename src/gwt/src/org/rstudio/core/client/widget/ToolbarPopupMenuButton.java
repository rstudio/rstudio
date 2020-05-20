/*
 * ToolbarPopupMenuButton.java
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

import org.rstudio.studio.client.common.icons.StandardIcons;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

public class ToolbarPopupMenuButton extends ToolbarMenuButton
                                    implements HasValueChangeHandlers<String>
{
   public ToolbarPopupMenuButton(String title, boolean showText, boolean rightAlignMenu)
   {
      super(ToolbarButton.NoText,
            title,
            StandardIcons.INSTANCE.empty_command(),
            new ToolbarPopupMenu(),
            rightAlignMenu);
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
      addMenuItem(new MenuItem(text, (ScheduledCommand) null), value);
   }

   public void addMenuItem(final String value)
   {
      addMenuItem(value, value);
   }

   public void addMenuItem(final MenuItem item, final String value)
   {
      final ScheduledCommand cmd = item.getScheduledCommand();
      item.setScheduledCommand(new Command()
      {
         @Override
         public void execute()
         {
            setText(value);
            if (cmd != null)
               cmd.execute();
         }
      });
      getMenu().addItem(item);
   }
   
   public void addMenuItem(MenuBar subMenu, String label)
   {
      SafeHtmlBuilder html = new SafeHtmlBuilder();
      html.appendHtmlConstant("<span style=\"margin-left: 25px;\">");
      html.appendEscaped(label);
      html.appendHtmlConstant("</span>");
      getMenu().addItem(html.toSafeHtml(), subMenu);
   }
   
   public void addSeparator()
   {
      getMenu().addSeparator();
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
      boolean changed = text_ == null || !text_.equals(text);
      
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
