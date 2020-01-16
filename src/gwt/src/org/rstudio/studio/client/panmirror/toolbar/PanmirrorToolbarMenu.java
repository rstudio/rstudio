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

package org.rstudio.studio.client.panmirror.toolbar;

import java.util.ArrayList;

import org.rstudio.core.client.widget.ToolbarPopupMenu;

public class PanmirrorToolbarMenu extends ToolbarPopupMenu
{
   
   public PanmirrorToolbarMenu(PanmirrorToolbarCommands commands)
   {
      commands_ = commands;
   }
   
   public PanmirrorToolbarMenu(PanmirrorToolbarMenu parent, PanmirrorToolbarCommands commands)
   {
      super(parent);
      commands_ = commands;
   }
   
   @Override
   public void getDynamicPopupMenu 
      (final ToolbarPopupMenu.DynamicPopupMenuCallback callback)
   {
      uiObjects_.forEach(object -> object.sync());
      callback.onPopupMenu(this);
   }
   
   public void addCommand(String id)
   {
      PanmirrorCommandMenuItem item = new PanmirrorCommandMenuItem(commands_.get(id));
      addItem(item);
      uiObjects_.add(item); 
   }
  
   
 
   private final ArrayList<PanmirrorCommandUIObject> uiObjects_ = new ArrayList<PanmirrorCommandUIObject>();
   private final PanmirrorToolbarCommands commands_;
}
