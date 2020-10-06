/*
 * PanmirrorToolbar.java
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

import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorToolbar extends SecondaryToolbar implements RequiresResize
{
   public PanmirrorToolbar()
   {
      super(false, "Panmirror Editor Toolbar");
      getElement().getStyle().setHeight(getHeight(), Unit.PX);
   }
   
   @Override
   public int getHeight()
   {
      return 23;
   }
 
   public void init(PanmirrorToolbarCommands commands, PanmirrorMenus menus, HasFindReplace findReplace)
   { 
      
      commands_ = commands;
      menus_ = menus;
      commandObjects_.clear();
      removeAllWidgets();
      
      PanmirrorToolbarRadioMenu blockMenu = createBlockMenu();
      addLeftTextMenu(addRadioMenu(blockMenu));
      
      addLeftSeparator();
      
      formatWidgets_ = addWidgetGroup(
         addLeftButton(PanmirrorCommands.Strong),
         addLeftButton(PanmirrorCommands.Em),
         addLeftButton(PanmirrorCommands.Code),
         addLeftSeparator(),
         addLeftButton(PanmirrorCommands.ClearFormatting),
         addLeftSeparator()
      );
      
      blockWidgets_ = addWidgetGroup(
         addLeftButton(PanmirrorCommands.BulletList),
         addLeftButton(PanmirrorCommands.OrderedList),
         addLeftButton(PanmirrorCommands.Blockquote),     
         addLeftSeparator() 
      );
      
      insertWidgets_ = addWidgetGroup(
         addLeftButton(PanmirrorCommands.Link),
         addLeftButton(PanmirrorCommands.Citation),
         addLeftSeparator(),
         addLeftButton(PanmirrorCommands.Image),
         addLeftSeparator()
      );
      
      
      PanmirrorToolbarMenu formatMenu = new PanmirrorToolbarMenu(commands_, menus_.format);
      addLeftTextMenu(new ToolbarMenuButton("Format", "Format", null, formatMenu, false));
            
      addLeftSeparator();
      
      PanmirrorToolbarMenu insertMenu = new PanmirrorToolbarMenu(commands_, menus_.insert);
      addLeftTextMenu(new ToolbarMenuButton("Insert", "Insert", null, insertMenu, false)); 
      
      if (haveAnyOf(PanmirrorCommands.TableInsertTable)) 
      {
         addLeftSeparator();
         PanmirrorToolbarMenu tableMenu = new PanmirrorToolbarMenu(commands_, menus_.table);
         addLeftTextMenu(new ToolbarMenuButton("Table", "Table", null, tableMenu, false));
      }
             
      if (findReplace != null)
      {
         addLeftSeparator();
         findReplaceButton_ = new ToolbarButton(
            ToolbarButton.NoText,
            "Find/Replace",
            FindReplaceBar.getFindIcon(),
            new ClickHandler() {
               public void onClick(ClickEvent event)
               {
                  boolean show = !findReplace.isFindReplaceShowing();
                  findReplace.showFindReplace(show);
               }
            });
         addLeftWidget(findReplaceButton_);
      }
   }
  
   
   public void sync(boolean images)
   {
      commandObjects_.forEach((object) -> object.sync(images));
      invalidateSeparators();
      onResize();
   }
   
   public void setFindReplaceLatched(boolean latched)
   {
      if (findReplaceButton_ != null)
      {
         findReplaceButton_.setLeftImage(latched ? 
            FindReplaceBar.getFindLatchedIcon() : 
            FindReplaceBar.getFindIcon()
         );
      }
   }
   
   @Override 
   public Widget addLeftSeparator()
   {
      Widget separator = super.addLeftSeparator();
      separator.addStyleName(RES.styles().toolbarSeparator());
      return separator;
   }
   
   @Override
   public void onResize()
   {
      int width = getOffsetWidth();
      if (width == 0)
         return;
         
      formatWidgets_.setVisible(width > 475);
      blockWidgets_.setVisible(width > 555);
      insertWidgets_.setVisible(width > 610);
      
   }
   
   
   private PanmirrorToolbarRadioMenu createBlockMenu()
   {
      PanmirrorToolbarRadioMenu blockMenu = new PanmirrorToolbarRadioMenu("Normal", "Block Format", commands_);
      blockMenu.addCommand(PanmirrorCommands.Paragraph);
      blockMenu.addSeparator();
      blockMenu.addCommand(PanmirrorCommands.Heading1);
      blockMenu.addCommand(PanmirrorCommands.Heading2);
      blockMenu.addCommand(PanmirrorCommands.Heading3);
      blockMenu.addCommand(PanmirrorCommands.Heading4);
      blockMenu.addCommand(PanmirrorCommands.Heading5);
      blockMenu.addCommand(PanmirrorCommands.Heading6);
      blockMenu.addSeparator();
      blockMenu.addCommand(PanmirrorCommands.CodeBlock);
      return blockMenu;
   }
   

   
   private boolean haveAnyOf(String...ids)
   {
      for (String id : ids)
      {
         if (commands_.get(id).isVisible())
            return true;
      }
      return false;
   }
  
   
   private Widget addLeftButton(String id)
   {
      return addLeftWidget(addButton(id));
   }
   
   private void addLeftTextMenu(ToolbarMenuButton menuButton)
   {
      addLeftWidget(menuButton);
      menuButton.addStyleName(RES.styles().toolbarTextMenuButton());
   }
   
   private PanmirrorToolbarRadioMenu addRadioMenu(PanmirrorToolbarRadioMenu menu)
   {
      commandObjects_.add(menu);
      return menu;
   }
   
   private PanmirrorCommandButton addButton(String id)
   {
      PanmirrorCommandButton button = new PanmirrorCommandButton(commands_.get(id));
      commandObjects_.add(button);
      return button;
   }
   
   private HorizontalPanel addWidgetGroup(Widget...widgets)
   {
      HorizontalPanel group = new HorizontalPanel();
      for (Widget widget : widgets)
         group.add(widget);
      addLeftWidget(group);
      return group;
   }
   
   private static final PanmirrorToolbarResources RES = PanmirrorToolbarResources.INSTANCE;
   
   private HorizontalPanel formatWidgets_ = null;
   private HorizontalPanel insertWidgets_ = null;
   private HorizontalPanel blockWidgets_ = null;
  
   private ToolbarButton findReplaceButton_ = null;
   
   private PanmirrorToolbarCommands commands_ = null;
   private PanmirrorMenus menus_ = null;
   private ArrayList<PanmirrorCommandUIObject> commandObjects_ = new ArrayList<PanmirrorCommandUIObject>();
}
