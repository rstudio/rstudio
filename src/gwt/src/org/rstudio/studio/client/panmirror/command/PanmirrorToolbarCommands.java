/*
 * PanmirrorToolbarCommands.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.palette.model.CommandPaletteEntryProvider;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;

import com.google.gwt.aria.client.MenuitemRole;
import com.google.gwt.aria.client.Roles;
import org.rstudio.studio.client.palette.ui.CommandPalette;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;

public class PanmirrorToolbarCommands implements CommandPaletteEntryProvider
{ 
   public PanmirrorToolbarCommands(PanmirrorCommand[] commands)
   {
      PanmirrorCommandIcons icons = PanmirrorCommandIcons.INSTANCE;

      // init commands
      commands_ = commands;
      
      // text editing
      add(PanmirrorCommands.Undo, constants_.undoMenuText());
      add(PanmirrorCommands.Redo, constants_.redoMenuText());
      add(PanmirrorCommands.SelectAll, constants_.selectAllMenuText());
      
      // formatting
      add(PanmirrorCommands.Strong, constants_.boldMenuText(), icons.BOLD);
      add(PanmirrorCommands.Em, constants_.italicMenuText(), icons.ITALIC);
      add(PanmirrorCommands.Code, constants_.codeMenuText(), icons.CODE);
      add(PanmirrorCommands.Strikeout, constants_.strikeoutMenuText());
      add(PanmirrorCommands.Superscript, constants_.superscriptMenuText());
      add(PanmirrorCommands.Subscript, constants_.subscriptMenuText());
      add(PanmirrorCommands.Smallcaps, constants_.smallCapsMenuText());
      add(PanmirrorCommands.Underline, constants_.underlineMenuText(), icons.UNDERLINE);
      add(PanmirrorCommands.Span, constants_.spanMenuText());
      add(PanmirrorCommands.Paragraph, constants_.normalMenuText(), Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading1, constants_.heading1MenuText(), Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading2, constants_.heading2MenuText(), Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading3, constants_.heading3MenuText(), Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading4, constants_.heading4MenuText(), Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading5, constants_.heading5MenuText(), Roles.getMenuitemradioRole());
      add(PanmirrorCommands.Heading6, constants_.heading6MenuText(), Roles.getMenuitemradioRole());
      add(PanmirrorCommands.CodeBlock, constants_.codeBlockMenuText(), Roles.getMenuitemradioRole());
      add(PanmirrorCommands.CodeBlockFormat, constants_.codeBlockFormatMenuText());
      
      add(PanmirrorCommands.Blockquote, constants_.blockquoteMenuText(), Roles.getMenuitemcheckboxRole(), icons.BLOCKQUOTE);
      add(PanmirrorCommands.LineBlock, constants_.linkBlockMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.Div, constants_.divMenuText());
      add(PanmirrorCommands.AttrEdit, constants_.editAttributesMenuText());
      add(PanmirrorCommands.ClearFormatting, constants_.clearFormattingMenuText(), icons.CLEAR_FORMATTING);
      
      // raw
      add(PanmirrorCommands.TexInline, constants_.texInlineMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TexBlock, constants_.texBlockMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.HTMLInline, constants_.htmlInlineMenuText());
      add(PanmirrorCommands.HTMLBlock, constants_.htmlBlockMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.RawInline, constants_.rawInlineMenuText());
      add(PanmirrorCommands.RawBlock, constants_.rawBlockMenuText());
      
      // chunk
      add(PanmirrorCommands.RCodeChunk, "R");
      add(PanmirrorCommands.BashCodeChunk, "Bash");
      add(PanmirrorCommands.D3CodeChunk, "D3");
      add(PanmirrorCommands.PythonCodeChunk, "Python");
      add(PanmirrorCommands.JuliaCodeChunk, "Julia");
      add(PanmirrorCommands.RcppCodeChunk, "Rcpp");
      add(PanmirrorCommands.SQLCodeChunk, "SQL");
      add(PanmirrorCommands.StanCodeChunk, "Stan");
      add(PanmirrorCommands.MermaidCodeChunk, "Mermaid");
      add(PanmirrorCommands.GraphVizCodeChunk, "GraphViz");
      add(PanmirrorCommands.ExpandChunk, constants_.expandChunkMenuText(), false);
      add(PanmirrorCommands.CollapseChunk, constants_.collapseChunkMenuText(), false);
      add(PanmirrorCommands.ExpandAllChunks, constants_.expandAllChunksMenuText(), false);
      add(PanmirrorCommands.CollapseAllChunks, constants_.collapseAllChunksMenuText(), false);

      // lists
      add(PanmirrorCommands.BulletList, constants_.bulletedListMenuText(), Roles.getMenuitemcheckboxRole(), icons.BULLET_LIST);
      add(PanmirrorCommands.OrderedList, constants_.numberedListMenuText(), Roles.getMenuitemcheckboxRole(), icons.NUMBERED_LIST);
      add(PanmirrorCommands.TightList, constants_.tightListMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.ListItemSink, constants_.sinkItemMenuText());
      add(PanmirrorCommands.ListItemLift, constants_.liftItemMenuText());
      add(PanmirrorCommands.ListItemCheck, constants_.itemCheckboxMenuText());
      add(PanmirrorCommands.ListItemCheckToggle, constants_.itemCheckedMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.EditListProperties, constants_.listAttributesMenuText());
      
      // tables
      add(PanmirrorCommands.TableInsertTable, constants_.insertTableMenuText(), icons.TABLE);
      add(PanmirrorCommands.TableToggleHeader, constants_.tableHeaderMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableToggleCaption, constants_.tableCaptionMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAddColumnAfter, constants_.tableInsertColumnRightMenuText(), constants_.tableInsertColumnRightPluralMenuText(), null);
      add(PanmirrorCommands.TableAddColumnBefore, constants_.tableInsertColumnLeftMenuText(), constants_.tableInsertColumnLeftPluralMenuText(), null);
      add(PanmirrorCommands.TableDeleteColumn, constants_.tableDeleteColumnMenuText(), constants_.tableDeleteColumnPluralMenuText(), null);
      add(PanmirrorCommands.TableAddRowAfter, constants_.tableInsertRowBelowMenuText(), constants_.tableInsertRowBelowPluralMenuText(), null);
      add(PanmirrorCommands.TableAddRowBefore, constants_.tableInsertRowAboveMenuText(), constants_.tableInsertRowAbovePluralMenuText(), null);
      add(PanmirrorCommands.TableDeleteRow, constants_.tableDeleteRowMenuText(), constants_.tableDeleteRowPluralMenuText(), null);
      add(PanmirrorCommands.TableDeleteTable, constants_.deleteTableMenuText());
      add(PanmirrorCommands.TableNextCell, constants_.tableNextCellMenuText());
      add(PanmirrorCommands.TablePreviousCell, constants_.tablePreviousCellMenuText());
      add(PanmirrorCommands.TableAlignColumnLeft, constants_.tableAlignColumnLeftMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAlignColumnRight, constants_.tableAlignColumnRightMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAlignColumnCenter, constants_.tableAlignColumnCenterMenuText(), Roles.getMenuitemcheckboxRole());
      add(PanmirrorCommands.TableAlignColumnDefault, constants_.tableAlignColumnDefaultMenuText(), Roles.getMenuitemcheckboxRole());
     
      // insert
      add(PanmirrorCommands.OmniInsert, constants_.anyMenuText(), icons.OMNI);
      add(PanmirrorCommands.Table, constants_.tableMenuText(), icons.TABLE);
      add(PanmirrorCommands.Link, constants_.linkMenuText(), icons.LINK);
      add(PanmirrorCommands.RemoveLink, constants_.removeLinkMenuText());
      add(PanmirrorCommands.Image, constants_.figureImageMenuText(), icons.IMAGE);
      add(PanmirrorCommands.Footnote, constants_.footnoteMenuText());
      add(PanmirrorCommands.HorizontalRule, constants_.horizontalRuleMenuText());
      add(PanmirrorCommands.ParagraphInsert, constants_.paragraphMenuText());
      add(PanmirrorCommands.HTMLComment, constants_.commentMenuText(), icons.COMMENT);
      add(PanmirrorCommands.YamlMetadata, constants_.yamlBlockMenuText());
      add(PanmirrorCommands.Shortcode, constants_.shortcodeMenuText());
      add(PanmirrorCommands.InsertDiv, constants_.divMenuText());
      add(PanmirrorCommands.InlineMath, constants_.inlineMathMenuText());
      add(PanmirrorCommands.DisplayMath, constants_.displayMathMenuText());
      add(PanmirrorCommands.DefinitionList, constants_.definitionListMenuText());
      add(PanmirrorCommands.DefinitionTerm, constants_.termMenuText());
      add(PanmirrorCommands.DefinitionDescription, constants_.descriptionMenuText());
      add(PanmirrorCommands.Citation, constants_.citationMenuText(), icons.CITATION);
      add(PanmirrorCommands.CrossReference, constants_.crossReferenceMenuText());
      add(PanmirrorCommands.InsertEmoji, constants_.insertEmojiMenuText());
      add(PanmirrorCommands.InsertSymbol, constants_.insertUnicodeMenuText());
      add(PanmirrorCommands.EmDash, constants_.insertEmDashMenuText());
      add(PanmirrorCommands.EnDash, constants_.insertEnDashMenuText());
      add(PanmirrorCommands.NonBreakingSpace, constants_.insertNonBreakingSpaceMenuText());
      add(PanmirrorCommands.HardLineBreak, constants_.insertHardLinkBreakMenuText());
      add(PanmirrorCommands.Tabset, constants_.insertTabsetMenuText());
      add(PanmirrorCommands.Callout, constants_.insertCalloutMenuText());
      
      // outline
      add(PanmirrorCommands.GoToNextSection, constants_.goToNextSectionMenuText());
      add(PanmirrorCommands.GoToPreviousSection, constants_.goToPreviousSectionMenuText());
      add(PanmirrorCommands.GoToNextChunk, constants_.goToNextChunkMenuText());
      add(PanmirrorCommands.GoToPreviousChunk, constants_.goToPreviousSectionMenuText());
      
      // slides
      add(PanmirrorCommands.InsertSlidePause, constants_.insertSlidePauseMenuText());
      add(PanmirrorCommands.InsertSlideNotes, constants_.insertSlideNotesMenuText());
      add(PanmirrorCommands.InsertSlideColumns, constants_.insertSlideColumnsMenuText());
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
      List<CommandPaletteItem> items = new ArrayList<>();
      for (PanmirrorCommandUI cmd: commandsUI_.values())
      {
         if (cmd != null && cmd.isVisible() && cmd.getCommandPallette())
         {
            items.add(new PanmirrorCommandPaletteItem(cmd));
         }
      }
      return items;
   }

   @Override
   public CommandPaletteItem getCommandPaletteItem(String id)
   {
      if (StringUtil.isNullOrEmpty(id))
      {
         return null;
      }

      PanmirrorCommandUI cmd = commandsUI_.get(id);
      if (cmd == null)
      {
         Debug.logWarning("Command palette requested unknown command from visual editor: '" + id + "'");
      }

      return new PanmirrorCommandPaletteItem(cmd);
   }

   @Override
   public String getProviderScope()
   {
      return CommandPalette.SCOPE_VISUAL_EDITOR;
   }

   private void add(String id, String menuText)
   {
      add(id, menuText, Roles.getMenuitemRole());
   }
   
   private void add(String id, String menuText, boolean commandPallette)
   {
      add(id, menuText, null, Roles.getMenuitemRole(), null, commandPallette);
   }
   
   private void add(String id, String menuText, String pluralMenuText, String image)
   {
      add(id, menuText, pluralMenuText, Roles.getMenuitemRole(), image, true);
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
      add(id, menuText, null, role, image, true);
   }
   
   private void add(String id, String menuText, String pluralMenuText, MenuitemRole role, String image, boolean commandPallette)
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
      commandsUI_.put(id, new PanmirrorCommandUI(command, menuText, pluralMenuText, role, image, commandPallette));
   }
   
   private PanmirrorCommand[] commands_ = null;
   private final HashMap<String,PanmirrorCommandUI> commandsUI_ = new HashMap<>();
   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);
}
