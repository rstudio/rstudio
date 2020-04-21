/*
 * PanmirrorToolbarCommands.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

import java.util.HashMap;

import com.google.gwt.aria.client.MenuitemRole;
import com.google.gwt.aria.client.Roles;

public class PanmirrorToolbarCommands
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
      add(PanmirrorCommands.Div, "Div Block...");
      add(PanmirrorCommands.AttrEdit, "Edit Attributes...");
      add(PanmirrorCommands.ClearFormatting, "Clear Formatting");
      
      // raw
      add(PanmirrorCommands.TexInline, "TeX Inline", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TexBlock, "TeX Block", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.HTMLInline, "HTML Inline...");
      add(PanmirrorCommands.HTMLBlock, "HTML Block", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.RawInline, "Raw Inline...");
      add(PanmirrorCommands.RawBlock, "Raw Block...");

      // lists
      add(PanmirrorCommands.BulletList, "Bullet List", Roles.getMenuitemcheckboxRole(), icons.BULLET_LIST);
      add(PanmirrorCommands.OrderedList, "Numbered List", Roles.getMenuitemcheckboxRole(), icons.NUMBERED_LIST);
      add(PanmirrorCommands.TightList, "Tight List", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.ListItemSink, "Sink Item");
      add(PanmirrorCommands.ListItemLift, "Lift Item");
      add(PanmirrorCommands.ListItemCheck, "Item Checkbox");
      add(PanmirrorCommands.ListItemCheckToggle, "Item Checked", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.OrderedListEdit, "List Attributes...");
      
      // tables
      add(PanmirrorCommands.TableInsertTable, "Insert Table...", icons.TABLE);
      add(PanmirrorCommands.TableToggleHeader, "Table Header", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableToggleCaption, "Table Caption", Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAddColumnAfter, "Insert Column Right", "Insert %d Columns Right", null);
      add(PanmirrorCommands.TableAddColumnBefore, "Insert Column Left", "Insert %d Columns Left", null);
      add(PanmirrorCommands.TableDeleteColumn, "Delete Column", "Delete %d Columns", null);
      add(PanmirrorCommands.TableAddRowAfter, "Insert Row Below", "Insert %d Rows Below", null);
      add(PanmirrorCommands.TableAddRowBefore, "Insert Row Above", "Insert %d Rows Above", null);
      add(PanmirrorCommands.TableDeleteRow, "Delete Row", "Delete %d Rows", null);
      add(PanmirrorCommands.TableDeleteTable, "Delete Table");
      add(PanmirrorCommands.TableNextCell, "Next Cell");
      add(PanmirrorCommands.TablePreviousCell, "Previous Cell");
      add(PanmirrorCommands.TableAlignColumnLeft, "Left");
      add(PanmirrorCommands.TableAlignColumnRight, "Right");
      add(PanmirrorCommands.TableAlignColumnCenter, "Center");
      add(PanmirrorCommands.TableAlignColumnDefault, "Default");
     
      // insert
      add(PanmirrorCommands.Link, "Link...", icons.LINK);
      add(PanmirrorCommands.RemoveLink, "Remove Link");
      add(PanmirrorCommands.Image, "Image...", icons.IMAGE);
      add(PanmirrorCommands.Footnote, "Footnote");
      add(PanmirrorCommands.HorizontalRule, "Horizontal Rule");
      add(PanmirrorCommands.ParagraphInsert, "Paragraph");
      add(PanmirrorCommands.YamlMetadata, "YAML Block");
      add(PanmirrorCommands.RmdChunk, "Code Chunk", icons.RMD_CHUNK);
      add(PanmirrorCommands.InsertDiv, "Div Block...");
      add(PanmirrorCommands.InlineMath, "Inline Math");
      add(PanmirrorCommands.DisplayMath, "Display Math");
      add(PanmirrorCommands.DefinitionList, "Definition List");
      add(PanmirrorCommands.DefinitionTerm, "Term");
      add(PanmirrorCommands.DefinitionDescription, "Description");
      add(PanmirrorCommands.Citation, "Citation...");   
      add(PanmirrorCommands.CrossReference, "Cross Reference");
   }
   
   public PanmirrorCommandUI get(String id)
   {
      return commandsUI_.get(id);
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
