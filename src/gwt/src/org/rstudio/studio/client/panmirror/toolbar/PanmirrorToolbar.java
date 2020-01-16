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
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
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
      commandObjects_.clear();
      
      // populate toolbar buttons
      addLeftButton(Panmirror.EditorCommands.Strong);
      addLeftButton(Panmirror.EditorCommands.Em);
      addLeftButton(Panmirror.EditorCommands.Code);
      
      addLeftSeparator();
      
      addLeftButton(Panmirror.EditorCommands.BulletList);
      addLeftButton(Panmirror.EditorCommands.OrderedList);
      addLeftButton(Panmirror.EditorCommands.Blockquote);
      
      addLeftSeparator();
      
      PanmirrorToolbarMenu tableMenu = new PanmirrorToolbarMenu(commands_);
      tableMenu.addCommand(Panmirror.EditorCommands.TableInsertTable);
      tableMenu.addSeparator();
      tableMenu.addCommand(Panmirror.EditorCommands.TableToggleHeader);
      tableMenu.addCommand(Panmirror.EditorCommands.TableToggleCaption);
      tableMenu.addSeparator();
      PanmirrorToolbarMenu alignMenu = new PanmirrorToolbarMenu(tableMenu, commands_);
      alignMenu.addCommand(Panmirror.EditorCommands.TableAlignColumnLeft);
      alignMenu.addCommand(Panmirror.EditorCommands.TableAlignColumnCenter);
      alignMenu.addCommand(Panmirror.EditorCommands.TableAlignColumnRight);
      alignMenu.addSeparator();
      alignMenu.addCommand(Panmirror.EditorCommands.TableAlignColumnDefault);
      tableMenu.addSubmenu("Align Column", alignMenu);
      tableMenu.addSeparator();
      tableMenu.addCommand(Panmirror.EditorCommands.TableAddRowBefore);
      tableMenu.addCommand(Panmirror.EditorCommands.TableAddRowAfter);
      tableMenu.addCommand(Panmirror.EditorCommands.TableAddColumnBefore);
      tableMenu.addCommand(Panmirror.EditorCommands.TableAddColumnAfter);
      tableMenu.addSeparator();
      tableMenu.addCommand(Panmirror.EditorCommands.TableDeleteRow);
      tableMenu.addCommand(Panmirror.EditorCommands.TableDeleteColumn);
      tableMenu.addCommand(Panmirror.EditorCommands.TableDeleteTable);
      addLeftWidget(new ToolbarMenuButton(ToolbarButton.NoText, ToolbarButton.NoTitle, RES.th(), tableMenu));
      
      addLeftSeparator();
      
      addLeftButton(Panmirror.EditorCommands.Link);
      addLeftButton(Panmirror.EditorCommands.Image);
      
      addLeftSeparator();
      
      addLeftButton(Panmirror.EditorCommands.CodeBlock);
      
      
      // populate menu
   }
   
   public void sync()
   {
      commandObjects_.forEach((object) -> object.sync());
   }
   
   private void addLeftButton(String id)
   {
      addLeftWidget(addButton(id));
   }
   
   private PanmirrorCommandToolbarButton addButton(String id)
   {
      PanmirrorCommandToolbarButton button = new PanmirrorCommandToolbarButton(commands_.get(id));
      commandObjects_.add(button);
      return button;
   }
   
   
   private static final PanmirrorToolbarResources RES = PanmirrorToolbarResources.INSTANCE;
  
   private PanmirrorToolbarCommands commands_ = null;
  
   private ArrayList<PanmirrorCommandUIObject> commandObjects_ = new ArrayList<PanmirrorCommandUIObject>();
   
}
