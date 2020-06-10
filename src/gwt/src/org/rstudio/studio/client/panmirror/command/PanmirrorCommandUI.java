/*
 * PanmirrorCommandUI.java
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


import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.aria.client.MenuitemRole;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.resources.client.ImageResource;

public class PanmirrorCommandUI implements ScheduledCommand
{
   public PanmirrorCommandUI(PanmirrorCommand command, String menuText, MenuitemRole role, String image)
   {
      this(command, menuText, null, role, image);
   }
   
   
   public PanmirrorCommandUI(PanmirrorCommand command, 
                             String menuText, 
                             String pluralMenuFormat,
                             MenuitemRole role, 
                             String image)
   {
      this.command_ = command;
      this.menuText_ = menuText;
      this.pluralMenuFormat_ = pluralMenuFormat;
      this.menuRole_ = role;
      this.image_ = image;
      this.shortcut_ = getShortcut(command);
      this.keySequence_ = getKeySequence(command);
   }
   
   public String getId()
   {
      return command_.id;
   }

   public String getMenuText()
   {
      String menuText = getBaseMenuText();
      String[] menuParts = menuText.split(":::", 2);
      if (menuParts.length == 1)
         return menuParts[0];
      else
         return menuParts[1];
   }
   
   public String getFullMenuText()
   {
      return getBaseMenuText().replaceFirst(":::", " ");
   }
   
   public String getDesc()
   {
      return getFullMenuText();
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

   public KeySequence getKeySequence()
   {
      return keySequence_;
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
   
   private String getBaseMenuText()
   {
      String menuText = menuText_;
      int plural = command_ != null ? command_.plural() : 1;
      if (plural > 1 && pluralMenuFormat_ != null) 
         menuText = pluralMenuFormat_.replaceAll("%d", Integer.toString(plural));
      return menuText;
   }
   
   private static KeySequence getKeySequence(PanmirrorCommand command)
   {
      if (command != null && command.keymap.length > 0)
      {
         // normalize to RStudio shortcut string
         String key = command.keymap[0];
         key = key.replace('-', '+');
         key = key.replace("Mod", BrowseCap.isMacintosh() ? "Cmd" : "Ctrl");
         
         // capitalize the last 
         return KeySequence.fromShortcutString(key);
      }
      else
      {
         return null;
      }
   }
  
   private static String getShortcut(PanmirrorCommand command)
   {     
      KeySequence keySequence = getKeySequence(command);
      if (keySequence != null)
         return keySequence.toString(true);
      else
         return null;
   }
   
   private final PanmirrorCommand command_;
   private final String menuText_;
   private final String pluralMenuFormat_;
   private final MenuitemRole menuRole_;
   private final String image_;
   private final String shortcut_;
   private final KeySequence keySequence_;
   
   private final static PanmirrorCommandIcons icons_ = PanmirrorCommandIcons.INSTANCE;
   
     
}
