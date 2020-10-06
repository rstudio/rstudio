/*
 * PanmirrorToolbarMenu.java
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

package org.rstudio.studio.client.panmirror.command;

import java.util.ArrayList;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.ToolbarPopupMenu;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.MenuItem;

public class PanmirrorToolbarMenu extends ToolbarPopupMenu implements PanmirrorCommandUIObject
{
   public PanmirrorToolbarMenu(PanmirrorToolbarCommands commands)
   {
      init(commands);
   }
   
   public PanmirrorToolbarMenu(PanmirrorToolbarCommands commands, PanmirrorMenuItem[] items)
   {
      this(commands);
      addItems(this, items);
   }
   
   
   private PanmirrorToolbarMenu(PanmirrorToolbarMenu parent, PanmirrorToolbarCommands commands)
   {
      super(parent);
      init(commands);
   }
   
   private void init(PanmirrorToolbarCommands commands)
   {
      commands_ = commands;
      addStyleName(RES.styles().toolbarPopupMenu());
      setAutoOpen(true);
   }
   
   @Override
   public void sync(boolean images)
   {
      uiObjects_.forEach(object -> object.sync(images));
   }
   
   @Override
   public void getDynamicPopupMenu 
      (final ToolbarPopupMenu.DynamicPopupMenuCallback callback)
   {
      sync(true);
      callback.onPopupMenu(this);
   }
   
   public void addCommand(String id)
   {
      addCommand(id, null);
   }
   
   public void addCommand(String id, String menuText)
   {
      PanmirrorCommandMenuItem item = new PanmirrorCommandMenuItem(commands_.get(id), menuText);
      addItem(item);
      uiObjects_.add(item); 
   }
   
   public PanmirrorToolbarMenu addSubmenu(String text)
   { 
      PanmirrorToolbarMenu submenu = new PanmirrorToolbarMenu(this, commands_);
      submenu.addMenuBarStyle(RES.styles().toolbarPopupSubmenu());
      addItem(new MenuItem(menuText(text)), submenu);
      uiObjects_.add(submenu);
      return submenu;
   }
   
   public void addItems(PanmirrorMenuItem[] items)
   {
      addItems(this, items);
   }
   
   private void addItems(PanmirrorToolbarMenu menu, PanmirrorMenuItem[] items)
   {
      for (PanmirrorMenuItem item : items)
      {
         if (item.exec != null && item.text != null)
         {
            MenuItem menuItem = new MenuItem(SafeHtmlUtils.fromTrustedString(item.text), () -> {
               item.exec.call();
            });
            menu.addItem(menuItem);
         }
         else if (item.command != null)
         {
            menu.addCommand(item.command);
         }
         else if (item.subMenu != null && item.text != null)
         {
            if (haveCommandsFrom(item.subMenu.items))
            {
               PanmirrorToolbarMenu subMenu = menu.addSubmenu(item.text);
               addItems(subMenu, item.subMenu.items);
            }
         }
         else if (item.separator)
         {
            menu.addSeparator();
         }
      }
   }
   
   private boolean haveCommandsFrom(PanmirrorMenuItem[] items)
   {
      for (PanmirrorMenuItem item : items)
      {  
         if (item.exec != null && item.text != null)
         {
            return true;
         }
         else if (item.command != null)
         {
            if (commands_.get(item.command) != null)
               return true;
         }
         else if (item.subMenu != null && item.text != null)
         {
            if (haveCommandsFrom(item.subMenu.items))
               return true;
         }
      }
      
     return false;
   }
  
  
   private static SafeHtml menuText(String text)
   {
      return SafeHtmlUtils.fromTrustedString(AppCommand.formatMenuLabel(null, text, null));
   }
   
   private static final PanmirrorToolbarResources RES = PanmirrorToolbarResources.INSTANCE;
   private final ArrayList<PanmirrorCommandUIObject> uiObjects_ = new ArrayList<>();
   private PanmirrorToolbarCommands commands_;
   
}
