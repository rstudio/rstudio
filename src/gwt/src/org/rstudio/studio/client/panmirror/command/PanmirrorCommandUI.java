/*
 * PanmirrorCommandUI.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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


import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.aria.client.MenuitemRole;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.resources.client.ImageResource;

public class PanmirrorCommandUI implements ScheduledCommand
{
   
   
   public PanmirrorCommandUI(PanmirrorCommand command, String menuText, MenuitemRole role, String image)
   {
      this.command_ = command;
      this.menuText_ = menuText;
      this.menuRole_ = role;
      this.image_ = image;
      this.shortcut_ = getShortcut(command);
   }
   
   public String getMenuText()
   {
      return menuText_;
   }
   
   public String getDesc()
   {
      return menuText_;
   }
   
   public String getTooltip()
   {
      String tooltip = getDesc();
      String shortcut = getShortcut();
      if (shortcut != null)
      {
         tooltip = tooltip + " (" + DomUtils.htmlToText(shortcut) + ")";
      }
      return tooltip;
   }
     
   public String getShortcut()
   {
      return shortcut_;
   }
   
   public MenuitemRole getMenuRole()
   {
      return menuRole_;
   }
   
   public ImageResource getImage()
   {
      return icons_.get(this.image_);
   }
   
   public boolean isVisible()
   {
      return command_ != null;
   }
   
   
   public boolean isEnabled()
   {
      if (command_ != null)
         return command_.isEnabled();
      else
         return false;
   }
   
   public boolean isActive()
   {
      if (command_ != null)
         return command_.isActive();
      else
         return false;
   }
   
   public void execute()
   {
      if (command_ != null)
         command_.execute();
   }
  
   private static String getShortcut(PanmirrorCommand command)
   {     
      if (command != null && command.keymap.length > 0) 
      {
         // normalize to RStudio shortcut string
         String key = command.keymap[0];
         key = key.replace('-', '+');
         key = key.replace("Mod", "Cmd");
         
         // capitalize the last 
         KeySequence keySequence = KeySequence.fromShortcutString(key);
         return keySequence.toString(true);
      }
      else
      {
         return null;
      }
     
   }
   
   private final PanmirrorCommand command_;
   private final String menuText_;
   private final MenuitemRole menuRole_;
   private final String image_;
   private final String shortcut_;
   
   private final static PanmirrorCommandIcons icons_ = PanmirrorCommandIcons.INSTANCE;
   
     
}
