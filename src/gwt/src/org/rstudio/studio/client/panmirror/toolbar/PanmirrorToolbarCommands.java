/*
 * PanmirrorToolbarCommands.java
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

import org.rstudio.studio.client.panmirror.Panmirror;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommand;

import com.google.gwt.resources.client.ImageResource;

public class PanmirrorToolbarCommands
{
   public PanmirrorToolbarCommands(PanmirrorCommand[] commands)
   {
      // init commands
      commands_ = commands;
      
      // text editing
      add(Panmirror.EditorCommands.Undo, "Undo");
      add(Panmirror.EditorCommands.Redo, "Redo");
      add(Panmirror.EditorCommands.SelectAll, "Select All");
      
      // formatting
      add(Panmirror.EditorCommands.Strong, "Bold", RES.bold());
      add(Panmirror.EditorCommands.Em, "Italic", RES.italic());
      add(Panmirror.EditorCommands.Code, "Code", RES.code());
      add(Panmirror.EditorCommands.Strikeout, "Strikeout");
      add(Panmirror.EditorCommands.Superscript, "Superscript");
      add(Panmirror.EditorCommands.Subscript, "Subscript");
      add(Panmirror.EditorCommands.Smallcaps, "Small Caps");
      add(Panmirror.EditorCommands.Span, "Span...");
      add(Panmirror.EditorCommands.Paragraph, "Normal");
      add(Panmirror.EditorCommands.Heading1, "Heading 1");
      add(Panmirror.EditorCommands.Heading2, "Heading 2");
      add(Panmirror.EditorCommands.Heading3, "Heading 3");
      add(Panmirror.EditorCommands.Heading4, "Heading 4");
      add(Panmirror.EditorCommands.Heading5, "Heading 5");
      add(Panmirror.EditorCommands.Heading6, "Heading 6");
      add(Panmirror.EditorCommands.CodeBlock, "Code Block", RES.code_block());
      add(Panmirror.EditorCommands.Blockquote, "Blockquote", RES.citation());
      add(Panmirror.EditorCommands.LineBlock, "Line Block");
      add(Panmirror.EditorCommands.Div, "Section/Div...");
      add(Panmirror.EditorCommands.AttrEdit, "Edit Attributes...");
      
      // lists
      

      
      
      
   }
   
   public PanmirrorCommandUI get(String id)
   {
      return commandsUI_.get(id);
   }
   
   
   private void add(String id, String menuText)
   {
      add(id, menuText, null);
   }
   
   private void add(String id, String menuText, ImageResource image)
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
   
   private static final PanmirrorToolbarResources RES = PanmirrorToolbarResources.INSTANCE;
   
   private PanmirrorCommand[] commands_ = null;
   private final HashMap<String,PanmirrorCommandUI> commandsUI_ = new HashMap<String,PanmirrorCommandUI>();

}
