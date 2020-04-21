/*
 * PanmirrorToolbar.java
 *
 * Copyright (C) 2009-20 byRStudio, PBC
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
 
   public void init(PanmirrorCommand[] commands, HasFindReplace findReplace)
   { 
      
      commands_ = new PanmirrorToolbarCommands(commands);
      commandObjects_.clear();
      removeAllWidgets();
      
      PanmirrorToolbarRadioMenu blockMenu = createBlockMenu();
      addLeftTextMenu(addRadioMenu(blockMenu));
      
      addLeftSeparator();
      
      formatWidgets_ = addWidgetGroup(
         addLeftSeparator(),
         addLeftButton(PanmirrorCommands.Strong),
         addLeftButton(PanmirrorCommands.Em),
         addLeftButton(PanmirrorCommands.Code),
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
         addLeftButton(PanmirrorCommands.Image),
         addLeftSeparator()
      );
      
      
      PanmirrorToolbarMenu formatMenu = createFormatMenu();
      addLeftTextMenu(new ToolbarMenuButton("Format", "Format", null, formatMenu, false));
            
      addLeftSeparator();
      
      PanmirrorToolbarMenu insertMenu = createInsertMenu();
      addLeftTextMenu(new ToolbarMenuButton("Insert", "Insert", null, insertMenu, false)); 
      
      if (haveAnyOf(PanmirrorCommands.TableInsertTable)) 
      {
         addLeftSeparator();
         PanmirrorToolbarMenu tableMenu = createTableMenu();
         addLeftTextMenu(new ToolbarMenuButton("Table", "Table", null, tableMenu, false));
      }
             
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
  
   
   public void sync(boolean images)
   {
      commandObjects_.forEach((object) -> object.sync(images));
      invalidateSeparators();
      onResize();
   }
   
   public void setFindReplaceLatched(boolean latched)
   {
      findReplaceButton_.setLeftImage(latched ? 
         FindReplaceBar.getFindLatchedIcon() : 
         FindReplaceBar.getFindIcon()
     );
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
           
      showGroup(formatWidgets_, width > 400);
      showGroup(blockWidgets_, width > 480);
      showGroup(insertWidgets_, width > 535);
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
   
   
   private PanmirrorToolbarMenu createFormatMenu()
   {
      PanmirrorToolbarMenu formatMenu = new PanmirrorToolbarMenu(commands_);
      PanmirrorToolbarMenu textMenu = formatMenu.addSubmenu("Text");
      textMenu.addCommand(PanmirrorCommands.Strong);
      textMenu.addCommand(PanmirrorCommands.Em);
      textMenu.addCommand(PanmirrorCommands.Code);
      textMenu.addCommand(PanmirrorCommands.Strikeout);
      textMenu.addCommand(PanmirrorCommands.Superscript);
      textMenu.addCommand(PanmirrorCommands.Subscript);
      textMenu.addCommand(PanmirrorCommands.Smallcaps);
      textMenu.addSeparator();
      textMenu.addCommand(PanmirrorCommands.Span);
      PanmirrorToolbarMenu listMenu = formatMenu.addSubmenu("Bullets & Numbering");
      listMenu.addCommand(PanmirrorCommands.BulletList);
      listMenu.addCommand(PanmirrorCommands.OrderedList);
      listMenu.addCommand(PanmirrorCommands.TightList);
      listMenu.addSeparator();
      listMenu.addCommand(PanmirrorCommands.ListItemCheck);
      listMenu.addCommand(PanmirrorCommands.ListItemCheckToggle);
      listMenu.addSeparator();
      listMenu.addCommand(PanmirrorCommands.ListItemSink);
      listMenu.addCommand(PanmirrorCommands.ListItemLift);
      listMenu.addSeparator();
      listMenu.addCommand(PanmirrorCommands.OrderedListEdit);
      formatMenu.addCommand(PanmirrorCommands.Blockquote);
      formatMenu.addSeparator();
      formatMenu.addCommand(PanmirrorCommands.CodeBlockFormat);
      formatMenu.addSeparator();
      formatMenu.addCommand(PanmirrorCommands.Div);
      formatMenu.addCommand(PanmirrorCommands.LineBlock);
      formatMenu.addSeparator();
      if (haveAnyOf(PanmirrorCommands.RawBlock, 
            PanmirrorCommands.TexInline, 
            PanmirrorCommands.HTMLInline))
      {
         PanmirrorToolbarMenu rawMenu = formatMenu.addSubmenu("Raw");
         rawMenu.addCommand(PanmirrorCommands.TexInline);
         rawMenu.addCommand(PanmirrorCommands.TexBlock);
         rawMenu.addSeparator();
         rawMenu.addCommand(PanmirrorCommands.HTMLInline);
         rawMenu.addCommand(PanmirrorCommands.HTMLBlock);
         rawMenu.addSeparator();
         rawMenu.addCommand(PanmirrorCommands.RawInline);
         rawMenu.addCommand(PanmirrorCommands.RawBlock);
         formatMenu.addSeparator();
      }  
      formatMenu.addCommand(PanmirrorCommands.AttrEdit);
      formatMenu.addSeparator();
      formatMenu.addCommand(PanmirrorCommands.ClearFormatting);
      return formatMenu;
   }
   
   private PanmirrorToolbarMenu createInsertMenu()
   {
      PanmirrorToolbarMenu insertMenu = new PanmirrorToolbarMenu(commands_);
      insertMenu.addCommand(PanmirrorCommands.Image);
      insertMenu.addCommand(PanmirrorCommands.Link);
      insertMenu.addSeparator();
      insertMenu.addCommand(PanmirrorCommands.HorizontalRule);
      insertMenu.addSeparator();
      insertMenu.addCommand(PanmirrorCommands.ParagraphInsert);
      insertMenu.addCommand(PanmirrorCommands.InsertDiv);
      insertMenu.addCommand(PanmirrorCommands.CodeBlockFormat);
      insertMenu.addSeparator();
      insertMenu.addCommand(PanmirrorCommands.RmdChunk);
      insertMenu.addCommand(PanmirrorCommands.YamlMetadata);
      insertMenu.addSeparator();
      if (haveAnyOf(PanmirrorCommands.DefinitionList,
                    PanmirrorCommands.DefinitionTerm,
                    PanmirrorCommands.DefinitionDescription))
      {
         PanmirrorToolbarMenu definitionMenu = insertMenu.addSubmenu("Definition");
         definitionMenu.addCommand(PanmirrorCommands.DefinitionList);
         definitionMenu.addSeparator();
         definitionMenu.addCommand(PanmirrorCommands.DefinitionTerm);
         definitionMenu.addCommand(PanmirrorCommands.DefinitionDescription);
         insertMenu.addSeparator();
      }
      insertMenu.addCommand(PanmirrorCommands.InlineMath);
      insertMenu.addCommand(PanmirrorCommands.DisplayMath);
      insertMenu.addSeparator();
      insertMenu.addCommand(PanmirrorCommands.CrossReference);
      insertMenu.addSeparator();
      insertMenu.addCommand(PanmirrorCommands.Footnote);
      insertMenu.addCommand(PanmirrorCommands.Citation);
      insertMenu.addSeparator();
      
      return insertMenu;
   }
   
   private PanmirrorToolbarMenu createTableMenu()
   {
      PanmirrorToolbarMenu tableMenu = new PanmirrorToolbarMenu(commands_);
      tableMenu.addCommand(PanmirrorCommands.TableInsertTable);
      tableMenu.addSeparator();
      tableMenu.addCommand(PanmirrorCommands.TableToggleHeader);
      tableMenu.addCommand(PanmirrorCommands.TableToggleCaption);
      tableMenu.addSeparator();
      PanmirrorToolbarMenu alignMenu = tableMenu.addSubmenu("Align Column");
      alignMenu.addCommand(PanmirrorCommands.TableAlignColumnLeft);
      alignMenu.addCommand(PanmirrorCommands.TableAlignColumnCenter);
      alignMenu.addCommand(PanmirrorCommands.TableAlignColumnRight);
      alignMenu.addSeparator();
      alignMenu.addCommand(PanmirrorCommands.TableAlignColumnDefault);
      tableMenu.addSeparator();
      tableMenu.addCommand(PanmirrorCommands.TableAddRowBefore);
      tableMenu.addCommand(PanmirrorCommands.TableAddRowAfter);
      tableMenu.addSeparator();
      tableMenu.addCommand(PanmirrorCommands.TableAddColumnBefore);
      tableMenu.addCommand(PanmirrorCommands.TableAddColumnAfter);
      tableMenu.addSeparator();
      tableMenu.addCommand(PanmirrorCommands.TableDeleteRow);
      tableMenu.addCommand(PanmirrorCommands.TableDeleteColumn);
      tableMenu.addSeparator();
      tableMenu.addCommand(PanmirrorCommands.TableDeleteTable);
      return tableMenu;
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
   
   private ArrayList<Widget> addWidgetGroup(Widget...widgets)
   {
      ArrayList<Widget> group = new ArrayList<Widget>();
      for (Widget widget : widgets)
         group.add(widget);
      return group;
   }
   
   private void showGroup(ArrayList<Widget> group, boolean show)
   {
      if (group !=  null) 
      {
         group.forEach((widget) -> {
           widget.setVisible(show);
         });
      }
   }
   
   
   private static final PanmirrorToolbarResources RES = PanmirrorToolbarResources.INSTANCE;
   
   private ArrayList<Widget> formatWidgets_ = null;
   private ArrayList<Widget> insertWidgets_ = null;
   private ArrayList<Widget> blockWidgets_ = null;
  
   private ToolbarButton findReplaceButton_ = null;
   
   private PanmirrorToolbarCommands commands_ = null;
   private ArrayList<PanmirrorCommandUIObject> commandObjects_ = new ArrayList<PanmirrorCommandUIObject>();
}
