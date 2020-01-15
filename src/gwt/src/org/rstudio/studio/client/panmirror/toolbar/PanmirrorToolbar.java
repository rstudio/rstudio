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


import java.util.ArrayList;

import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.studio.client.panmirror.Panmirror;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommand;

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
      commands_ = new PanmirrorToolbarCommands(commands);
      commandButtons_.clear();
      
      // populate toolbar buttons
      addLeftButton(Panmirror.EditorCommands.Strong);
      addLeftButton(Panmirror.EditorCommands.Em);
      addLeftButton(Panmirror.EditorCommands.Code);
      addLeftSeparator();
      addLeftButton(Panmirror.EditorCommands.Blockquote);
      addLeftSeparator();
      addLeftButton(Panmirror.EditorCommands.CodeBlock);
      
      
      // populate menu
   }
   
   public void sync()
   {
      commandButtons_.forEach((button) -> button.sync());
   }
   
   private void addLeftButton(String id)
   {
      addLeftWidget(addButton(id));
   }
   
   private PanmirrorCommandToolbarButton addButton(String id)
   {
      PanmirrorCommandToolbarButton button = new PanmirrorCommandToolbarButton(commands_.get(id));
      commandButtons_.add(button);
      return button;
   }
   
   
  
   private PanmirrorToolbarCommands commands_ = null;
  
   private ArrayList<PanmirrorCommandToolbarButton> commandButtons_ = new ArrayList<PanmirrorCommandToolbarButton>();
   
}
