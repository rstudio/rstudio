/*
 * PanmirrorToolbarRadioMenu.java
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

import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ToolbarMenuButton;

public class PanmirrorToolbarRadioMenu extends ToolbarMenuButton implements PanmirrorCommandUIObject
{
   public PanmirrorToolbarRadioMenu(String defaultText, String title, PanmirrorToolbarCommands commands)
   {
      super(defaultText, title, null, new PanmirrorToolbarMenu(commands), false);
      defaultText_ = defaultText;
      menu_ = (PanmirrorToolbarMenu)getMenu();
      commands_ = commands;
   }
   
   public void addCommand(String id)
   {
      commandIds_.add(id);
      menu_.addCommand(id);
      
      // update and apply minwidth
      PanmirrorCommandUI command = commands_.get(id);
      Size textSize = DomMetrics.measureHTML(command.getMenuText(), ThemeStyles.INSTANCE.toolbarButtonLabel());
      if (textSize.width > minWidth_)
      {
         minWidth_ = textSize.width;
         setWidth(minWidth_ + + 15 + "px");
      }
      
   }
   
   public void addSeparator()
   {
      menu_.addSeparator();
   }
   
   @Override
   public void sync(boolean images)
   {
      // set our text to whatever command is active
      String text = defaultText_;
      for (String id : commandIds_) 
      {
         PanmirrorCommandUI command = commands_.get(id);
         if (command.isVisible() && command.isEnabled() && command.isActive())
         {
            text = command.getMenuText();
            break;
         }
      }
      
      if (text != null)
      {
         setText(text);
      }
      else
      {
         text = defaultText_;
      }    
   }
   
   private final String defaultText_;
   private int minWidth_ = 0;
   private final PanmirrorToolbarMenu menu_;
   private final ArrayList<String> commandIds_ = new ArrayList<String>();
   private final PanmirrorToolbarCommands commands_;
}
