/*
 * PanmirrorToolbar.java
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


import java.util.HashMap;

import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.studio.client.panmirror.Panmirror;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommand;

import com.google.gwt.resources.client.ImageResource;

public class PanmirrorToolbar extends SecondaryToolbar
{
   public PanmirrorToolbar()
   {
      super(false, "Panmirror Editor Toolbar");
      
     
   }
   
   @Override
   public int getHeight()
   {
      return 22;
   }
 
   public void init(PanmirrorCommand[] commands)
   { 
      // set commands
      commands_ = commands;
      
      // add all known commands
      addCommand(Panmirror.EditorCommands.Strong, "Bold", RES.bold());
      addCommand(Panmirror.EditorCommands.Em, "Italic", RES.italic());
      
      
      // populate toolbar buttons
      addLeftButton(Panmirror.EditorCommands.Strong);
      addLeftButton(Panmirror.EditorCommands.Em);
      
      
      // populate menu
   }
   
   private void addLeftButton(String id)
   {
      addLeftWidget(new PanmirrorCommandToolbarButton(commandsUI_.get(id)));
   }
   
   private void addCommand(String id, String menuText, ImageResource image)
   {
      // lookup the underlying command
      PanmirrorCommand command = null;
      for (PanmirrorCommand cmd : commands_) {
         if (cmd.id == id) {
            command = cmd;
            break;
         }
      }
      // add it
      commandsUI_.put(id, new PanmirrorCommandUI(command, menuText, image));
   }
  
   private PanmirrorCommand[] commands_ = null;
   private final HashMap<String,PanmirrorCommandUI> commandsUI_ = new HashMap<String,PanmirrorCommandUI>();
   
   private static final PanmirrorToolbarResources RES = PanmirrorToolbarResources.INSTANCE;
}
