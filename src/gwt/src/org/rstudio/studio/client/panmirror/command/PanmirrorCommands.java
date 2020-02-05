/*
 * PanmirrorCommands.java
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

import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class PanmirrorCommands
{
   // editing
   public String Undo;
   public String Redo;
   public String SelectAll;

    // formatting
   public String Strong;
   public String Em;
   public String Code;
   public String Strikeout;
   public String Superscript;
   public String Subscript;
   public String Smallcaps;
   public String Paragraph;
   public String Heading1;
   public String Heading2;
   public String Heading3;
   public String Heading4;
   public String Heading5;
   public String Heading6;
   public String CodeBlock;
   public String Blockquote;
   public String LineBlock;
   public String AttrEdit;
   public String Span;
   public String Div;

    // lists
   public String BulletList;
   public String OrderedList;
   public String TightList;
   public String ListItemSink;
   public String ListItemLift;
   public String ListItemSplit;
   public String ListItemCheck;
   public String ListItemCheckToggle;
   public String OrderedListEdit;

    // tables
   public String TableInsertTable;
   public String TableToggleHeader;
   public String TableToggleCaption;
   public String TableNextCell;
   public String TablePreviousCell;
   public String TableAddColumnBefore;
   public String TableAddColumnAfter;
   public String TableDeleteColumn;
   public String TableAddRowBefore;
   public String TableAddRowAfter;
   public String TableDeleteRow;
   public String TableDeleteTable;
   public String TableAlignColumnLeft;
   public String TableAlignColumnRight;
   public String TableAlignColumnCenter;
   public String TableAlignColumnDefault;

    // insert
   public String Link;
   public String Image;
   public String Footnote;
   public String ParagraphInsert;
   public String HorizontalRule;
   public String InlineMath;
   public String DisplayMath;
   public String RawInline;
   public String RawBlock;
   public String YamlMetadata;
   public String RmdChunk;
   public String InlineLatex;
   public String Citation;
   public String DefinitionList;
   public String DefinitionTerm;
   public String DefinitionDescription;
}
