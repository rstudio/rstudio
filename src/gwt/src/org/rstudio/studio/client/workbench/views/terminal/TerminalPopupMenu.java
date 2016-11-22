/*
 * ActiveTerminalToolbarButton.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class TerminalPopupMenu extends ToolbarPopupMenu
{
   public TerminalPopupMenu(SessionInfo sessionInfo, Commands commands)
   {
      commands_ = commands;
      //sessionInfo_ = sessionInfo;
      
      // TODO (gary) Read active terminal from session, or always revert to
      // first terminal in list?
      activeTerminal_ = null;
   }
   
   @Override
   public void getDynamicPopupMenu(final DynamicPopupMenuCallback callback)
   { 
      // clean out existing entries
      clearItems(); 
      
      addItem(commands_.newTerminal().createMenuItem(false));

      // ensure the menu doesn't get too narrow
      addSeparator(225);

      // TODO (gary) dynamically create based on open terminals
       
      addItem(commands_.renameTerminal().createMenuItem(false));
      addItem(commands_.clearTerminalScrollbackBuffer().createMenuItem(false));
      addSeparator();
      addItem(commands_.closeTerminal().createMenuItem(false));
      callback.onPopupMenu(this);
   }
   
   public ToolbarButton getToolbarButton()
   {
      if (toolbarButton_ == null)
      {
         // TODO (gary) flesh this out
         String buttonText = "Terminal";
          
         toolbarButton_ = new ToolbarButton(
                buttonText, 
                StandardIcons.INSTANCE.empty_command(),
                this, 
                false);
          
         if (activeTerminal_ != null)
         {
            toolbarButton_.setTitle(activeTerminal_);
         }
      }
       
       return toolbarButton_;
   }
   
   private ToolbarButton toolbarButton_;
   private final Commands commands_;
   //private final SessionInfo sessionInfo_;
   private String activeTerminal_;
}
