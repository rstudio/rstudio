/*
 * PanmirrorToolbar.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.panmirror.command;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;

import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarSeparator;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.MarkdownToolbar;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

public class PanmirrorToolbar implements RequiresResize
{
   public void init(PanmirrorToolbarCommands commands, PanmirrorMenus menus, MarkdownToolbar toolbar)
   {

      commands_ = commands;
      menus_ = menus;
      toolbar_ = toolbar;
      commandObjects_.clear();

      if (toolbarPanel_ == null)
      {
         Widget sep = toolbar_.addLeftSeparator();
         sep.addStyleName(RES.styles().toolbarSeparator());
         sep.getElement().getStyle().setMarginLeft(7, Unit.PX);
         sep.getElement().getStyle().setMarginRight(7, Unit.PX);
         toolbar_.addVisualModeTools(sep);
         toolbarPanel_ = new HorizontalPanel();
         toolbar_.addVisualModeTools(toolbarPanel_);
      }

      for (int i = toolbarPanel_.getWidgetCount() - 1; i >= 0; i--)
         toolbarPanel_.remove(i);


      formatWidgets_ = addWidgetGroup(addLeftButton(PanmirrorCommands.Strong),
            addLeftButton(PanmirrorCommands.Em), addLeftButton(PanmirrorCommands.Code),
            addLeftSeparator());
      
      PanmirrorToolbarRadioMenu blockMenu = createBlockMenu();
      addLeftTextMenu(addRadioMenu(blockMenu));
      addLeftSeparator();

      blockWidgets_ = addWidgetGroup(addLeftButton(PanmirrorCommands.BulletList),
            addLeftButton(PanmirrorCommands.OrderedList), addLeftSeparator());

      insertWidgets_ = addWidgetGroup(addLeftButton(PanmirrorCommands.Link),
            addLeftButton(PanmirrorCommands.Image), addLeftSeparator());

      PanmirrorToolbarMenu formatMenu = new PanmirrorToolbarMenu(commands_, menus_.format);
      addLeftTextMenu(new ToolbarMenuButton(constants_.formatText(), constants_.formatTitle(), null,
            formatMenu, false));

      addLeftSeparator();

      PanmirrorToolbarMenu insertMenu = new PanmirrorToolbarMenu(commands_, menus_.insert);
      addLeftTextMenu(new ToolbarMenuButton(constants_.insertText(), constants_.insertTitle(), null,
            insertMenu, false));

      if (haveAnyOf(PanmirrorCommands.TableInsertTable))
      {
         addLeftSeparator();
         PanmirrorToolbarMenu tableMenu = new PanmirrorToolbarMenu(commands_, menus_.table);
         addLeftTextMenu(new ToolbarMenuButton(constants_.tableText(), constants_.tableTitle(),
               null, tableMenu, false));
      }
   }
   
   public void sync(boolean images)
   {
      commandObjects_.forEach((object) -> object.sync(images));
      toolbar_.invalidateSeparators();
      onResize();
   }

   public Widget addLeftSeparator()
   {
      Image sep = new ToolbarSeparator();
      sep.addStyleName(RES.styles().toolbarSeparator());
      toolbarPanel_.add(sep);
      return sep;
   }

   @Override
   public void onResize()
   {
      int width = toolbar_.getOffsetWidth();
      if (width == 0)
         return;

      formatWidgets_.setVisible(width > 470);
      blockWidgets_.setVisible(width > 530);
      insertWidgets_.setVisible(width > 590);

   }

   private PanmirrorToolbarRadioMenu createBlockMenu()
   {
      PanmirrorToolbarRadioMenu blockMenu = new PanmirrorToolbarRadioMenu(
            constants_.panmirrorBlockMenuDefaultText(), constants_.panMirrorBlockMenuTitle(),
            commands_);
      blockMenu.addCommand(PanmirrorCommands.Paragraph);
      blockMenu.addSeparator();
      blockMenu.addCommand(PanmirrorCommands.Heading1);
      blockMenu.addCommand(PanmirrorCommands.Heading2);
      blockMenu.addCommand(PanmirrorCommands.Heading3);
      blockMenu.addCommand(PanmirrorCommands.Heading4);
      blockMenu.addCommand(PanmirrorCommands.Heading5);
      blockMenu.addCommand(PanmirrorCommands.Heading6);
      return blockMenu;
   }

   private boolean haveAnyOf(String... ids)
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
      Widget button = addButton(id);
      toolbarPanel_.add(button);
      return button;
   }

   private void addLeftTextMenu(ToolbarMenuButton menuButton)
   {
      toolbarPanel_.add(menuButton);
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

   private HorizontalPanel addWidgetGroup(Widget... widgets)
   {
      HorizontalPanel group = new HorizontalPanel();
      for (Widget widget : widgets)
         group.add(widget);
      toolbarPanel_.add(group);
      return group;
   }

   private static final PanmirrorToolbarResources RES = PanmirrorToolbarResources.INSTANCE;

   private MarkdownToolbar toolbar_;
   private HorizontalPanel toolbarPanel_;

   private HorizontalPanel formatWidgets_ = null;
   private HorizontalPanel insertWidgets_ = null;
   private HorizontalPanel blockWidgets_ = null;

   private PanmirrorToolbarCommands commands_ = null;
   private PanmirrorMenus menus_ = null;
   private ArrayList<PanmirrorCommandUIObject> commandObjects_ = new ArrayList<>();
   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);
}
