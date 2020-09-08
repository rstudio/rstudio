/*
 * PanmirrorToolbarCommands.java
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
import java.util.HashMap;
import java.util.List;

import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;

import com.google.gwt.aria.client.MenuitemRole;
import com.google.gwt.aria.client.Roles;

public class PanmirrorToolbarCommands implements CommandPaletteEntrySource
{ 
   public PanmirrorToolbarCommands(PanmirrorCommand[] commands)
   {
      PanmirrorCommandIcons icons = PanmirrorCommandIcons.INSTANCE;

      // init commands
      commands_ = commands;
      
      // text editing
      add(PanmirrorCommands.Undo, "Undo");
      add(PanmirrorCommands.Redo, "Redo");
      add(PanmirrorCommands.SelectAll, "Select All");
      
      // formatting
      add(PanmirrorCommands.Strong, "Bold", icons.BOLD);
      add(PanmirrorCommands.Em, "Italic", icons.ITALIC);
      add(PanmirrorCommands.Code, "Code", icons.CODE);
      add(PanmirrorCommands.Strikeout, "Strikeout");
      add(PanmirrorCommands.Superscript, "Superscript");
      add(PanmirrorCommands.Subscript, "Subscript");
      add(PanmirrorCommands.Smallcaps, "Small Caps");
      add(PanmirrorCommands.Span, "Span...");
      add(PanmirrorCommands.Paragraph, "Normal", Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading1, "Heading 1", Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading2, "Heading 2", Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading3, "Heading 3", Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading4, "Heading 4", Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading5, "Heading 5", Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading6, "Heading 6", Roles.getMenuitemradioRole());
      add(PanmirrorCommands.CodeBlock, "Code Block", Roles.getMenuitemradioRole());
      add(PanmirrorCommands.CodeBlockFormat, "Code Block...");
      
      add(PanmirrorCommands.Blockquote, "Blockquote", Roles.getMenuitemcheckboxRole(), icons.BLOCKQUOTE);
      add(PanmirrorCommands.LineBlock, "Line Block", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.Div, "Div...");
      add(PanmirrorCommands.AttrEdit, "Edit Attributes...");
      add(PanmirrorCommands.ClearFormatting, "Clear Formatting", icons.CLEAR_FORMATTING);
      
      // raw
      add(PanmirrorCommands.TexInline, "TeX Inline", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TexBlock, "TeX Block", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.HTMLInline, "HTML Inline...");
      add(PanmirrorCommands.HTMLBlock, "HTML Block", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.RawInline, "Raw Inline...");
      add(PanmirrorCommands.RawBlock, "Raw Block...");
      
      // chunk
      add(PanmirrorCommands.RCodeChunk, "R");
      add(PanmirrorCommands.BashCodeChunk, "Bash");
      add(PanmirrorCommands.D3CodeChunk, "D3");
      add(PanmirrorCommands.PythonCodeChunk, "Python");
      add(PanmirrorCommands.RcppCodeChunk, "Rcpp");
      add(PanmirrorCommands.SQLCodeChunk, "SQL");
      add(PanmirrorCommands.StanCodeChunk, "Stan");

      // lists
      add(PanmirrorCommands.BulletList, "Bullet List", Roles.getMenuitemcheckboxRole(), icons.BULLET_LIST);
      add(PanmirrorCommands.OrderedList, "Numbered List", Roles.getMenuitemcheckboxRole(), icons.NUMBERED_LIST);
      add(PanmirrorCommands.TightList, "Tight List", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.ListItemSink, "Sink Item");
      add(PanmirrorCommands.ListItemLift, "Lift Item");
      add(PanmirrorCommands.ListItemCheck, "Item Checkbox");
      add(PanmirrorCommands.ListItemCheckToggle, "Item Checked", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.EditListProperties, "List Attributes...");
      
      // tables
      add(PanmirrorCommands.TableInsertTable, "Insert Table...", icons.TABLE);
      add(PanmirrorCommands.TableToggleHeader, "Table Header", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableToggleCaption, "Table Caption", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAddColumnAfter, "Table:::Insert Column Right", "Insert %d Columns Right", null);
      add(PanmirrorCommands.TableAddColumnBefore, "Table:::Insert Column Left", "Insert %d Columns Left", null);
      add(PanmirrorCommands.TableDeleteColumn, "Table:::Delete Column", "Table:::Delete %d Columns", null);
      add(PanmirrorCommands.TableAddRowAfter, "Table:::Insert Row Below", "Table:::Insert %d Rows Below", null);
      add(PanmirrorCommands.TableAddRowBefore, "Table:::Insert Row Above", "Table:::Insert %d Rows Above", null);
      add(PanmirrorCommands.TableDeleteRow, "Table:::Delete Row", "Delete %d Rows", null);
      add(PanmirrorCommands.TableDeleteTable, "Delete Table");
      add(PanmirrorCommands.TableNextCell, "Table:::Next Cell");
      add(PanmirrorCommands.TablePreviousCell, "Table:::Previous Cell");
      add(PanmirrorCommands.TableAlignColumnLeft, "Table Align Column:::Left", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAlignColumnRight, "Table Align Column:::Right", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAlignColumnCenter, "Table Align Column:::Center", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAlignColumnDefault, "Table Align Column:::Default", Roles.getMenuitemcheckboxRole());
     
      // insert
      add(PanmirrorCommands.OmniInsert, "Any...", icons.OMNI);
      add(PanmirrorCommands.Link, "Link...", icons.LINK);
      add(PanmirrorCommands.RemoveLink, "Remove Link");
      add(PanmirrorCommands.Image, "Image...", icons.IMAGE);
      add(PanmirrorCommands.Footnote, "Footnote");
      add(PanmirrorCommands.HorizontalRule, "Horizontal Rule");
      add(PanmirrorCommands.ParagraphInsert, "Paragraph");
      add(PanmirrorCommands.HTMLComment, "Comment", icons.COMMENT);
      add(PanmirrorCommands.YamlMetadata, "YAML Block");
      add(PanmirrorCommands.Shortcode, "Shortcode");
      add(PanmirrorCommands.InsertDiv, "Div...");
      add(PanmirrorCommands.InlineMath, "Inline Math");
      add(PanmirrorCommands.DisplayMath, "Display Math");
      add(PanmirrorCommands.DefinitionList, "Definition List");
      add(PanmirrorCommands.DefinitionTerm, "Term");
      add(PanmirrorCommands.DefinitionDescription, "Description");
      add(PanmirrorCommands.Citation, "Citation...", icons.CITATION);   
      add(PanmirrorCommands.CrossReference, "Cross Reference");
      add(PanmirrorCommands.InsertEmoji, "Insert Emoji...");
      add(PanmirrorCommands.InsertSymbol, "Insert Unicode...");
      add(PanmirrorCommands.EmDash, "Insert:::Em Dash (—)");
      add(PanmirrorCommands.EnDash, "Insert:::En Dash (–)");
      add(PanmirrorCommands.NonBreakingSpace, "Insert:::Non-Breaking Space");
      add(PanmirrorCommands.HardLineBreak, "Insert:::Hard Line Break");
      
      // outline
      add(PanmirrorCommands.GoToNextSection, "Go to Next Section");
      add(PanmirrorCommands.GoToPreviousSection, "Go to Previous Section");
   }
   
   public PanmirrorCommandUI get(String id)
   {
      return commandsUI_.get(id);
   }
   
   public boolean exec(String id)
   {
      PanmirrorCommandUI command = get(id);
      if (command != null)
      {
         if (command.isEnabled())
         {
            command.execute();
         }
         return true;
      }
      else
      {
         return false;
      }
   }
   
   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      List<CommandPaletteItem> items = new ArrayList<CommandPaletteItem>();
      for (PanmirrorCommandUI cmd: commandsUI_.values())
      {
         if (cmd != null && cmd.isVisible())
         {
            items.add(new PanmirrorCommandPaletteItem(cmd));
         }
      }
      return items;
   }

   private void add(String id, String menuText)
   {
      add(id, menuText, Roles.getMenuitemRole());
   }
   
   private void add(String id, String menuText, String pluralMenuText, String image)
   {
      add(id, menuText, pluralMenuText, Roles.getMenuitemRole(), image);
   }
   
   private void add(String id, String menuText, String image)
   {
      add(id, menuText, Roles.getMenuitemRole(), image);
   }
   
   
   private void add(String id, String menuText, MenuitemRole role)
   {
      add(id, menuText, role, null);
   }
   
   private void add(String id, String menuText, MenuitemRole role, String image)
   {
      add(id, menuText, null, role, image);
   }
   
   private void add(String id, String menuText, String pluralMenuText, MenuitemRole role, String image)
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
      commandsUI_.put(id, new PanmirrorCommandUI(command, menuText, pluralMenuText, role, image));
   }
   
   private PanmirrorCommand[] commands_ = null;
   private final HashMap<String,PanmirrorCommandUI> commandsUI_ = new HashMap<String,PanmirrorCommandUI>();
}
