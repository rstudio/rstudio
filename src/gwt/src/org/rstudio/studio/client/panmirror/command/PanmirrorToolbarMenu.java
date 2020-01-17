/*
 * PanmirrorToolbarMenu.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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
   
   private PanmirrorToolbarMenu(PanmirrorToolbarMenu parent, PanmirrorToolbarCommands commands)
   {
      super(parent);
      init(commands);
   }
   
   private void init(PanmirrorToolbarCommands commands)
   {
      commands_ = commands;
      addStyleName(RES.styles().toolbarPopupMenu());
      getElement().getStyle().setZIndex(1000);
   }
   
   @Override
   public void getDynamicPopupMenu 
      (final ToolbarPopupMenu.DynamicPopupMenuCallback callback)
   {
      sync();
      callback.onPopupMenu(this);
   }
   
   public void addCommand(String id)
   {
      PanmirrorCommandMenuItem item = new PanmirrorCommandMenuItem(commands_.get(id));
      addItem(item);
      uiObjects_.add(item); 
   }
   
   public PanmirrorToolbarMenu addSubmenu(String text)
   { 
      PanmirrorToolbarMenu submenu = new PanmirrorToolbarMenu(this, commands_);
      submenu.addMenuBarStyle(RES.styles().toolbarPopupMenu());
      addItem(new MenuItem(menuText(text)), submenu);
      uiObjects_.add(submenu);
      return submenu;
   }
  
   @Override
   public void sync()
   {
      uiObjects_.forEach(object -> object.sync());
   }
   
   private static SafeHtml menuText(String text)
   {
      return SafeHtmlUtils.fromTrustedString(AppCommand.formatMenuLabel(null, text, null));
   }
   
   private static final PanmirrorToolbarResources RES = PanmirrorToolbarResources.INSTANCE;
   private final ArrayList<PanmirrorCommandUIObject> uiObjects_ = new ArrayList<PanmirrorCommandUIObject>();
   private PanmirrorToolbarCommands commands_;
   
}
