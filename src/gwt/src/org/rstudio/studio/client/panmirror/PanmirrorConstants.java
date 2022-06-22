/*
 * PanmirrorConstants.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.panmirror;

public interface PanmirrorConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Visual Editor".
     *
     * @return translated "Visual Editor"
     */
    @DefaultMessage("Visual Editor")
    @Key("visualEditorText")
    String visualEditorText();

    /**
     * Translated "visual editor ".
     *
     * @return translated "visual editor {0}"
     */
    @DefaultMessage("visual editor {0} ")
    @Key("visualEditorLabel")
    String visualEditorLabel(String menuText);

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    @DefaultMessage("Format")
    @Key("formatTitle")
    String formatTitle();

    /**
     * Translated "Insert".
     *
     * @return translated "Insert"
     */
    @DefaultMessage("Insert")
    @Key("insertTitle")
    String insertTitle();

    /**
     * Translated "Table".
     *
     * @return translated "Table"
     */
    @DefaultMessage("Table")
    @Key("tableTitle")
    String tableTitle();

    /**
     * Translated "Find/Replace".
     *
     * @return translated "Find/Replace"
     */
    @DefaultMessage("Find/Replace")
    @Key("findReplaceTitle")
    String findReplaceTitle();

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    @DefaultMessage("Format")
    @Key("formatText")
    String formatText();


    /**
     * Translated "Insert".
     *
     * @return translated "Insert"
     */
    @DefaultMessage("Insert")
    @Key("insertText")
    String insertText();

    /**
     * Translated "Table".
     *
     * @return translated "Table"
     */
    @DefaultMessage("Table")
    @Key("tableText")
    String tableText();


    /**
     * Translated "Normal".
     *
     * @return translated "Normal"
     */
    @DefaultMessage("Normal")
    @Key("panmirrorBlockMenuDefaultText")
    String panmirrorBlockMenuDefaultText();

    /**
     * Translated "Block Format".
     *
     * @return translated "Block Format"
     */
    @DefaultMessage("Block Format")
    @Key("panMirrorBlockMenuTitle")
    String panMirrorBlockMenuTitle();

    /**
     * Translated "Undo".
     *
     * @return translated "Undo"
     */
    @DefaultMessage("Undo")
    @Key("undoMenuText")
    String undoMenuText();

    /**
     * Translated "Redo".
     *
     * @return translated "Redo"
     */
    @DefaultMessage("Redo")
    @Key("redoMenuText")
    String redoMenuText();

    /**
     * Translated "Select All".
     *
     * @return translated "Select All"
     */
    @DefaultMessage("Select All")
    @Key("selectAllMenuText")
    String selectAllMenuText();

    /**
     * Translated "Bold".
     *
     * @return translated "Bold"
     */
    @DefaultMessage("Bold")
    @Key("boldMenuText")
    String boldMenuText();

    /**
     * Translated "Italic".
     *
     * @return translated "Italic"
     */
    @DefaultMessage("Italic")
    @Key("italicMenuText")
    String italicMenuText();

    /**
     * Translated "Code".
     *
     * @return translated "Code"
     */
    @DefaultMessage("Code")
    @Key("codeMenuText")
    String codeMenuText();

    /**
     * Translated "Strikeout".
     *
     * @return translated "Strikeout"
     */
    @DefaultMessage("Strikeout")
    @Key("strikeoutMenuText")
    String strikeoutMenuText();

    /**
     * Translated "Superscript".
     *
     * @return translated "Superscript"
     */
    @DefaultMessage("Superscript")
    @Key("superscriptMenuText")
    String superscriptMenuText();

    /**
     * Translated "Subscript".
     *
     * @return translated "Subscript"
     */
    @DefaultMessage("Subscript")
    @Key("subscriptMenuText")
    String subscriptMenuText();

    /**
     * Translated "Small Caps".
     *
     * @return translated "Small Caps"
     */
    @DefaultMessage("Small Caps")
    @Key("smallCapsMenuText")
    String smallCapsMenuText();

    /**
     * Translated "Underline".
     *
     * @return translated "Underline"
     */
    @DefaultMessage("Underline")
    @Key("underlineMenuText")
    String underlineMenuText();

    /**
     * Translated "Span...".
     *
     * @return translated "Span..."
     */
    @DefaultMessage("Span...")
    @Key("spanMenuText")
    String spanMenuText();

    /**
     * Translated "Normal".
     *
     * @return translated "Normal"
     */
    @DefaultMessage("Normal")
    @Key("normalMenuText")
    String normalMenuText();

    /**
     * Translated "Header 1".
     *
     * @return translated "Header 1"
     */
    @DefaultMessage("Header 1")
    @Key("heading1MenuText")
    String heading1MenuText();

    /**
     * Translated "Header 2".
     *
     * @return translated "Header 2"
     */
    @DefaultMessage("Header 2")
    @Key("heading2MenuText")
    String heading2MenuText();

    /**
     * Translated "Header 3".
     *
     * @return translated "Header 3"
     */
    @DefaultMessage("Header 3")
    @Key("heading3MenuText")
    String heading3MenuText();

    /**
     * Translated "Header 4".
     *
     * @return translated "Header 4"
     */
    @DefaultMessage("Header 4")
    @Key("heading4MenuText")
    String heading4MenuText();

    /**
     * Translated "Header 5".
     *
     * @return translated "Header 5"
     */
    @DefaultMessage("Header 5")
    @Key("heading5MenuText")
    String heading5MenuText();

    /**
     * Translated "Header 6".
     *
     * @return translated "Header 6"
     */
    @DefaultMessage("Header 6")
    @Key("heading6MenuText")
    String heading6MenuText();

    /**
     * Translated "Code".
     *
     * @return translated "Code"
     */
    @DefaultMessage("Code")
    @Key("codeBlockMenuText")
    String codeBlockMenuText();

    /**
     * Translated "Code Block...".
     *
     * @return translated "Code Block..."
     */
    @DefaultMessage("Code Block...")
    @Key("codeBlockFormatMenuText")
    String codeBlockFormatMenuText();

    /**
     * Translated "Blockquote".
     *
     * @return translated "Blockquote"
     */
    @DefaultMessage("Blockquote")
    @Key("blockquoteMenuText")
    String blockquoteMenuText();

    /**
     * Translated "Line Block".
     *
     * @return translated "Line Block"
     */
    @DefaultMessage("Line Block")
    @Key("linkBlockMenuText")
    String linkBlockMenuText();

    /**
     * Translated "Line Block".
     *
     * @return translated "Line Block"
     */
    @DefaultMessage("Div...")
    @Key("divMenuText")
    String divMenuText();

    /**
     * Translated "Edit Attributes...".
     *
     * @return translated "Edit Attributes..."
     */
    @DefaultMessage("Edit Attributes...")
    @Key("editAttributesMenuText")
    String editAttributesMenuText();

    /**
     * Translated "Clear Formatting".
     *
     * @return translated "Clear Formatting"
     */
    @DefaultMessage("Clear Formatting")
    @Key("clearFormattingMenuText")
    String clearFormattingMenuText();


    /**
     * Translated "TeX Inline".
     *
     * @return translated "TeX Inline"
     */
    @DefaultMessage("TeX Inline")
    @Key("texInlineMenuText")
    String texInlineMenuText();


    /**
     * Translated "TeX Block".
     *
     * @return translated "TeX Block"
     */
    @DefaultMessage("TeX Block")
    @Key("texBlockMenuText")
    String texBlockMenuText();

    /**
     * Translated "HTML Inline...".
     *
     * @return translated "HTML Inline..."
     */
    @DefaultMessage("HTML Inline...")
    @Key("htmlInlineMenuText")
    String htmlInlineMenuText();

    /**
     * Translated "HTML Block".
     *
     * @return translated "HTML Block"
     */
    @DefaultMessage("HTML Block")
    @Key("htmlBlockMenuText")
    String htmlBlockMenuText();

    /**
     * Translated "Raw Inline...".
     *
     * @return translated "Raw Inline..."
     */
    @DefaultMessage("Raw Inline...")
    @Key("rawInlineMenuText")
    String rawInlineMenuText();

    /**
     * Translated "Raw Block...".
     *
     * @return translated "Raw Block..."
     */
    @DefaultMessage("Raw Block...")
    @Key("rawBlockMenuText")
    String rawBlockMenuText();

    /**
     * Translated "Expand Chunk".
     *
     * @return translated "Expand Chunk"
     */
    @DefaultMessage("Expand Chunk")
    @Key("expandChunkMenuText")
    String expandChunkMenuText();

    /**
     * Translated "Collapse Chunk".
     *
     * @return translated "Collapse Chunk"
     */
    @DefaultMessage("Collapse Chunk")
    @Key("collapseChunkMenuText")
    String collapseChunkMenuText();

    /**
     * Translated "Expand All Chunks".
     *
     * @return translated "Expand All Chunks"
     */
    @DefaultMessage("Expand All Chunks")
    @Key("expandAllChunksMenuText")
    String expandAllChunksMenuText();

    /**
     * Translated "Collapse All Chunks".
     *
     * @return translated "Collapse All Chunks"
     */
    @DefaultMessage("Collapse All Chunks")
    @Key("collapseAllChunksMenuText")
    String collapseAllChunksMenuText();

    /**
     * Translated "Bulleted List".
     *
     * @return translated "Bulleted List"
     */
    @DefaultMessage("Bulleted List")
    @Key("bulletedListMenuText")
    String bulletedListMenuText();

    /**
     * Translated "Numbered List".
     *
     * @return translated "Numbered List"
     */
    @DefaultMessage("Numbered List")
    @Key("numberedListMenuText")
    String numberedListMenuText();

    /**
     * Translated "Tight List".
     *
     * @return translated "Tight List"
     */
    @DefaultMessage("Tight List")
    @Key("tightListMenuText")
    String tightListMenuText();

    /**
     * Translated "Sink Item".
     *
     * @return translated "Sink Item"
     */
    @DefaultMessage("Sink Item")
    @Key("sinkItemMenuText")
    String sinkItemMenuText();

    /**
     * Translated "Lift Item".
     *
     * @return translated "Lift Item"
     */
    @DefaultMessage("Lift Item")
    @Key("liftItemMenuText")
    String liftItemMenuText();

    /**
     * Translated "Item Checkbox".
     *
     * @return translated "Item Checkbox"
     */
    @DefaultMessage("Item Checkbox")
    @Key("itemCheckboxMenuText")
    String itemCheckboxMenuText();

    /**
     * Translated "Item Checked".
     *
     * @return translated "Item Checked"
     */
    @DefaultMessage("Item Checked")
    @Key("itemCheckedMenuText")
    String itemCheckedMenuText();

    /**
     * Translated "List Attributes...".
     *
     * @return translated "List Attributes..."
     */
    @DefaultMessage("List Attributes...")
    @Key("listAttributesMenuText")
    String listAttributesMenuText();

    /**
     * Translated "Insert Table...".
     *
     * @return translated "Insert Table..."
     */
    @DefaultMessage("Insert Table...")
    @Key("insertTableMenuText")
    String insertTableMenuText();

    /**
     * Translated "Table Header".
     *
     * @return translated "Table Header"
     */
    @DefaultMessage("Table Header")
    @Key("tableHeaderMenuText")
    String tableHeaderMenuText();

    /**
     * Translated "Table Caption".
     *
     * @return translated "Table Caption"
     */
    @DefaultMessage("Table Caption")
    @Key("tableCaptionMenuText")
    String tableCaptionMenuText();


    /**
     * Translated "Table:::Insert Column Right".
     *
     * @return translated "Table:::Insert Column Right"
     */
    @DefaultMessage("Table:::Insert Column Right")
    @Key("tableInsertColumnRightMenuText")
    String tableInsertColumnRightMenuText();

    /**
     * Translated "Insert %d Columns Right".
     *
     * @return translated "Insert %d Columns Right"
     */
    @DefaultMessage("Insert %d Columns Right")
    @Key("tableInsertColumnRightPluralMenuText")
    String tableInsertColumnRightPluralMenuText();

    /**
     * Translated "Table:::Insert Column Left".
     *
     * @return translated "Table:::Insert Column Left"
     */
    @DefaultMessage("Table:::Insert Column Left")
    @Key("tableInsertColumnLeftMenuText")
    String tableInsertColumnLeftMenuText();

    /**
     * Translated "Insert %d Columns Left".
     *
     * @return translated "Insert %d Columns Left"
     */
    @DefaultMessage("Insert %d Columns Left")
    @Key("tableInsertColumnLeftPluralMenuText")
    String tableInsertColumnLeftPluralMenuText();

    /**
     * Translated "Table:::Delete Column".
     *
     * @return translated "Table:::Delete Column"
     */
    @DefaultMessage("Table:::Delete Column")
    @Key("tableDeleteColumnMenuText")
    String tableDeleteColumnMenuText();

    /**
     * Translated "Table:::Delete %d Columns".
     *
     * @return translated "Table:::Delete %d Columns"
     */
    @DefaultMessage("Table:::Delete %d Columns")
    @Key("tableDeleteColumnPluralMenuText")
    String tableDeleteColumnPluralMenuText();

    /**
     * Translated "Table:::Insert Row Below".
     *
     * @return translated "Table:::Insert Row Below"
     */
    @DefaultMessage("Table:::Insert Row Below")
    @Key("tableInsertRowBelowMenuText")
    String tableInsertRowBelowMenuText();

    /**
     * Translated "Table:::Insert %d Rows Below".
     *
     * @return translated "Table:::Insert %d Rows Below"
     */
    @DefaultMessage("Table:::Insert %d Rows Below")
    @Key("tableInsertRowBelowPluralMenuText")
    String tableInsertRowBelowPluralMenuText();

    /**
     * Translated "Table:::Insert Row Above".
     *
     * @return translated "Table:::Insert Row Above"
     */
    @DefaultMessage("Table:::Insert Row Above")
    @Key("tableInsertRowAboveMenuText")
    String tableInsertRowAboveMenuText();

    /**
     * Translated "Table:::Insert %d Rows Above".
     *
     * @return translated "Table:::Insert %d Rows Above"
     */
    @DefaultMessage("Table:::Insert %d Rows Above")
    @Key("tableInsertRowAbovePluralMenuText")
    String tableInsertRowAbovePluralMenuText();

    /**
     * Translated "Table:::Insert %d Rows Above".
     *
     * @return translated "Table:::Delete Row"
     */
    @DefaultMessage("Table:::Delete Row")
    @Key("tableDeleteRowMenuText")
    String tableDeleteRowMenuText();

    /**
     * Translated "Delete %d Rows".
     *
     * @return translated "Delete %d Rows"
     */
    @DefaultMessage("Delete %d Rows")
    @Key("tableDeleteRowPluralMenuText")
    String tableDeleteRowPluralMenuText();

    /**
     * Translated "Delete Table".
     *
     * @return translated "Delete Table"
     */
    @DefaultMessage("Delete Table")
    @Key("deleteTableMenuText")
    String deleteTableMenuText();

    /**
     * Translated "Table:::Next Cell".
     *
     * @return translated "Table:::Next Cell"
     */
    @DefaultMessage("Table:::Next Cell")
    @Key("tableNextCellMenuText")
    String tableNextCellMenuText();

    /**
     * Translated "Table:::Previous Cell".
     *
     * @return translated "Table:::Previous Cell"
     */
    @DefaultMessage("Table:::Previous Cell")
    @Key("tablePreviousCellMenuText")
    String tablePreviousCellMenuText();

    /**
     * Translated "Table Align Column:::Left".
     *
     * @return translated "Table Align Column:::Left"
     */
    @DefaultMessage("Table Align Column:::Left")
    @Key("tableAlignColumnLeftMenuText")
    String tableAlignColumnLeftMenuText();

    /**
     * Translated "Table Align Column:::Right".
     *
     * @return translated "Table Align Column:::Right"
     */
    @DefaultMessage("Table Align Column:::Right")
    @Key("tableAlignColumnRightMenuText")
    String tableAlignColumnRightMenuText();

    /**
     * Translated "Table Align Column:::Center".
     *
     * @return translated "Table Align Column:::Center"
     */
    @DefaultMessage("Table Align Column:::Center")
    @Key("tableAlignColumnCenterMenuText")
    String tableAlignColumnCenterMenuText();

    /**
     * Translated "Table Align Column:::Default".
     *
     * @return translated "Table Align Column:::Default"
     */
    @DefaultMessage("Table Align Column:::Default")
    @Key("tableAlignColumnDefaultMenuText")
    String tableAlignColumnDefaultMenuText();

    /**
     * Translated "Any...".
     *
     * @return translated "Any..."
     */
    @DefaultMessage("Any...")
    @Key("anyMenuText")
    String anyMenuText();

    /**
     * Translated "Table...".
     *
     * @return translated "Table..."
     */
    @DefaultMessage("Table...")
    @Key("tableMenuText")
    String tableMenuText();

    /**
     * Translated "Table...".
     *
     * @return translated "Link..."
     */
    @DefaultMessage("Link...")
    @Key("linkMenuText")
    String linkMenuText();

    /**
     * Translated "Remove Link".
     *
     * @return translated "Remove Link"
     */
    @DefaultMessage("Remove Link")
    @Key("removeLinkMenuText")
    String removeLinkMenuText();

    /**
     * Translated "Figure / Image...".
     *
     * @return translated "Figure / Image..."
     */
    @DefaultMessage("Figure / Image...")
    @Key("figureImageMenuText")
    String figureImageMenuText();

    /**
     * Translated "Footnote".
     *
     * @return translated "Footnote"
     */
    @DefaultMessage("Footnote")
    @Key("footnoteMenuText")
    String footnoteMenuText();

    /**
     * Translated "Horizontal Rule".
     *
     * @return translated "Horizontal Rule"
     */
    @DefaultMessage("Horizontal Rule")
    @Key("horizontalRuleMenuText")
    String horizontalRuleMenuText();

    /**
     * Translated "Paragraph".
     *
     * @return translated "Paragraph"
     */
    @DefaultMessage("Paragraph")
    @Key("paragraphMenuText")
    String paragraphMenuText();

    /**
     * Translated "Comment".
     *
     * @return translated "Comment"
     */
    @DefaultMessage("Comment")
    @Key("commentMenuText")
    String commentMenuText();

    /**
     * Translated "YAML Block".
     *
     * @return translated "YAML Block"
     */
    @DefaultMessage("YAML Block")
    @Key("yamlBlockMenuText")
    String yamlBlockMenuText();

    /**
     * Translated "Shortcode".
     *
     * @return translated "Shortcode"
     */
    @DefaultMessage("Shortcode")
    @Key("shortcodeMenuText")
    String shortcodeMenuText();

    /**
     * Translated "Inline Math".
     *
     * @return translated "Inline Math"
     */
    @DefaultMessage("Inline Math")
    @Key("inlineMathMenuText")
    String inlineMathMenuText();

    /**
     * Translated "Display Math".
     *
     * @return translated "Display Math"
     */
    @DefaultMessage("Display Math")
    @Key("displayMathMenuText")
    String displayMathMenuText();

    /**
     * Translated "Definition List".
     *
     * @return translated "Definition List"
     */
    @DefaultMessage("Definition List")
    @Key("definitionListMenuText")
    String definitionListMenuText();

    /**
     * Translated "Term".
     *
     * @return translated "Term"
     */
    @DefaultMessage("Term")
    @Key("termMenuText")
    String termMenuText();

    /**
     * Translated "Description".
     *
     * @return translated "Description"
     */
    @DefaultMessage("Description")
    @Key("descriptionMenuText")
    String descriptionMenuText();

    /**
     * Translated "Citation...".
     *
     * @return translated "Citation..."
     */
    @DefaultMessage("Citation...")
    @Key("citationMenuText")
    String citationMenuText();

    /**
     * Translated "Cross Reference".
     *
     * @return translated "Cross Reference"
     */
    @DefaultMessage("Cross Reference")
    @Key("crossReferenceMenuText")
    String crossReferenceMenuText();

    /**
     * Translated "Insert Emoji...".
     *
     * @return translated "Insert Emoji..."
     */
    @DefaultMessage("Insert Emoji...")
    @Key("insertEmojiMenuText")
    String insertEmojiMenuText();

    /**
     * Translated "Insert Unicode...".
     *
     * @return translated "Insert Unicode..."
     */
    @DefaultMessage("Insert Unicode...")
    @Key("insertUnicodeMenuText")
    String insertUnicodeMenuText();

    /**
     * Translated "Insert:::Em Dash (—)".
     *
     * @return translated "Insert:::Em Dash (—)"
     */
    @DefaultMessage("Insert:::Em Dash (—)")
    @Key("insertEmDashMenuText")
    String insertEmDashMenuText();

    /**
     * Translated "Insert:::En Dash (–)".
     *
     * @return translated "Insert:::En Dash (–)"
     */
    @DefaultMessage("Insert:::En Dash (–)")
    @Key("insertEnDashMenuText")
    String insertEnDashMenuText();

    /**
     * Translated "Insert:::Non-Breaking Space".
     *
     * @return translated "Insert:::Non-Breaking Space"
     */
    @DefaultMessage("Insert:::Non-Breaking Space")
    @Key("insertNonBreakingSpaceMenuText")
    String insertNonBreakingSpaceMenuText();

    /**
     * Translated "Insert:::Hard Line Break".
     *
     * @return translated "Insert:::Hard Line Break"
     */
    @DefaultMessage("Insert:::Hard Line Break")
    @Key("insertHardLinkBreakMenuText")
    String insertHardLinkBreakMenuText();

    /**
     * Translated "Insert:::Tabset...".
     *
     * @return translated "Insert:::Tabset..."
     */
    @DefaultMessage("Insert:::Tabset...")
    @Key("insertTabsetMenuText")
    String insertTabsetMenuText();

    /**
     * Translated "Insert:::Callout...".
     *
     * @return translated "Insert:::Callout..."
     */
    @DefaultMessage("Insert:::Callout...")
    @Key("insertCalloutMenuText")
    String insertCalloutMenuText();

    /**
     * Translated "Go to Next Section".
     *
     * @return translated "Go to Next Section"
     */
    @DefaultMessage("Go to Next Section")
    @Key("goToNextSectionMenuText")
    String goToNextSectionMenuText();

    /**
     * Translated "Go to Previous Section".
     *
     * @return translated "Go to Previous Section"
     */
    @DefaultMessage("Go to Previous Section")
    @Key("goToPreviousSectionMenuText")
    String goToPreviousSectionMenuText();

    /**
     * Translated "Go to Next Chunk".
     *
     * @return translated "Go to Next Chunk"
     */
    @DefaultMessage("Go to Next Chunk")
    @Key("goToNextChunkMenuText")
    String goToNextChunkMenuText();

    /**
     * Translated "Insert:::Slide Pause".
     *
     * @return translated "Insert:::Slide Pause"
     */
    @DefaultMessage("Insert:::Slide Pause")
    @Key("insertSlidePauseMenuText")
    String insertSlidePauseMenuText();

    /**
     * Translated "Insert:::Slide Notes".
     *
     * @return translated "Insert:::Slide Notes"
     */
    @DefaultMessage("Insert:::Slide Notes")
    @Key("insertSlideNotesMenuText")
    String insertSlideNotesMenuText();

    /**
     * Translated "Insert:::Slide Columns".
     *
     * @return translated "Insert:::Slide Columns"
     */
    @DefaultMessage("Insert:::Slide Columns")
    @Key("insertSlideColumnsMenuText")
    String insertSlideColumnsMenuText();

    /**
     * Translated "Edit Attributes".
     *
     * @return translated "Edit Attributes"
     */
    @DefaultMessage("Edit Attributes")
    @Key("editAttributesCaption")
    String editAttributesCaption();

    /**
     * Translated "Span Attributes".
     *
     * @return translated "Span Attributes"
     */
    @DefaultMessage("Span Attributes")
    @Key("spanAttributesCaption")
    String spanAttributesCaption();


    /**
     * Translated "Unwrap Span".
     *
     * @return translated "Unwrap Span"
     */
    @DefaultMessage("Unwrap Span")
    @Key("unwrapSpanRemoveButtonCaption")
    String unwrapSpanRemoveButtonCaption();

    /**
     * Translated "Div Attributes".
     *
     * @return translated "Div Attributes"
     */
    @DefaultMessage("Div Attributes")
    @Key("divAttributesCaption")
    String divAttributesCaption();

    /**
     * Translated "Callout".
     *
     * @return translated "Callout"
     */
    @DefaultMessage("Callout")
    @Key("calloutCaption")
    String calloutCaption();

    /**
     * Translated "Unwrap Div".
     *
     * @return translated "Unwrap Div"
     */
    @DefaultMessage("Unwrap Div")
    @Key("unwrapDivTitle")
    String unwrapDivTitle();


    /**
     * Translated "Type: ".
     *
     * @return translated "Type: "
     */
    @DefaultMessage("Type: ")
    @Key("typeLabel")
    String typeLabel();



    /**
     * Translated "Appearance: ".
     *
     * @return translated "Appearance: "
     */
    @DefaultMessage("Appearance: ")
    @Key("appearanceLabel")
    String appearanceLabel();

    /**
     * Translated "Show icon".
     *
     * @return translated "Show icon"
     */
    @DefaultMessage("Show icon")
    @Key("showIconLabel")
    String showIconLabel();

    /**
     * Translated "Caption:".
     *
     * @return translated "Caption:"
     */
    @DefaultMessage("Caption:")
    @Key("captionLabel")
    String captionLabel();

    /**
     * Translated "(Optional)".
     *
     * @return translated "(Optional)"
     */
    @DefaultMessage("(Optional)")
    @Key("optionalPlaceholder")
    String optionalPlaceholder();

    /**
     * Translated "Div".
     *
     * @return translated "Div"
     */
    @DefaultMessage("Div")
    @Key("divTabList")
    String divTabList();

    /**
     * Translated "Callout".
     *
     * @return translated "Callout"
     */
    @DefaultMessage("Callout")
    @Key("calloutText")
    String calloutText();

    /**
     * Translated "Attributes".
     *
     * @return translated "Attributes"
     */
    @DefaultMessage("Attributes")
    @Key("attributesText")
    String attributesText();

    /**
     * Translated "Code Block".
     *
     * @return translated "Code Block"
     */
    @DefaultMessage("Code Block")
    @Key("codeBlockText")
    String codeBlockText();

    /**
     * Translated "Language".
     *
     * @return translated "Language"
     */
    @DefaultMessage("Language")
    @Key("languageFormLabel")
    String languageFormLabel();

    /**
     * Translated "(optional)".
     *
     * @return translated "(optional)"
     */
    @DefaultMessage("(optional)")
    @Key("optionalFormLabel")
    String optionalFormLabel();


    /**
     * Translated "Figure".
     *
     * @return translated "Figure"
     */
    @DefaultMessage("Figure")
    @Key("figureLabel")
    String figureLabel();

    /**
     * Translated "Image".
     *
     * @return translated "Image"
     */
    @DefaultMessage("Image")
    @Key("imageLabel")
    String imageLabel();

    /**
     * Translated "Width:".
     *
     * @return translated "Width:"
     */
    @DefaultMessage("Width:")
    @Key("widthLabel")
    String widthLabel();

    /**
     * Translated "Height:".
     *
     * @return translated "Height:"
     */
    @DefaultMessage("Height:")
    @Key("heightLabel")
    String heightLabel();

    /**
     * Translated "(Auto)".
     *
     * @return translated "(Auto):"
     */
    @DefaultMessage("(Auto)")
    @Key("autoText")
    String autoText();

    /**
     * Translated "Lock ratio".
     *
     * @return translated "Lock ratio"
     */
    @DefaultMessage("Lock ratio")
    @Key("lockRatioText")
    String lockRatioText();

    /**
     * Translated "Default".
     *
     * @return translated "Default"
     */
    @DefaultMessage("default")
    @Key("defaultAlignLabel")
    String defaultAlignLabel();
    
    /**
     * Translated "Left".
     *
     * @return translated "Left"
     */
    @DefaultMessage("Left")
    @Key("leftLabel")
    String leftLabel();

    /**
     * Translated "Center".
     *
     * @return translated "Center"
     */
    @DefaultMessage("Center")
    @Key("centerLabel")
    String centerLabel();

    /**
     * Translated "Right".
     *
     * @return translated "Right"
     */
    @DefaultMessage("Right")
    @Key("rightLabel")
    String rightLabel();

    /**
     * Translated "Alignment".
     *
     * @return translated "Alignment"
     */
    @DefaultMessage("Alignment")
    @Key("legendText")
    String legendText();

    /**
     * Translated "Alternative text:".
     *
     * @return translated "Alternative text:"
     */
    @DefaultMessage("Alternative text:")
    @Key("alternativeTextLabel")
    String alternativeTextLabel();

    /**
     * Translated "Link to:".
     *
     * @return translated "Link to:"
     */
    @DefaultMessage("Link to:")
    @Key("linkToLabel")
    String linkToLabel();

    /**
     * Translated "LaTeX environment:".
     *
     * @return translated "LaTeX environment:"
     */
    @DefaultMessage("LaTeX environment:")
    @Key("latexEnvironmentLabel")
    String latexEnvironmentLabel();

    /**
     * Translated "Title attribute:".
     *
     * @return translated "Title attribute:"
     */
    @DefaultMessage("Title attribute:")
    @Key("titleAttributeLabel")
    String titleAttributeLabel();

    /**
     * Translated "Advanced".
     *
     * @return translated "Advanced"
     */
    @DefaultMessage("Advanced")
    @Key("advancedLabel")
    String advancedLabel();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "You must provide a value for image width.".
     *
     * @return translated "You must provide a value for image width."
     */
    @DefaultMessage("You must provide a value for image width.")
    @Key("errorMessage")
    String errorMessage();

    /**
     * Translated "Units".
     *
     * @return translated "Units"
     */
    @DefaultMessage("Units")
    @Key("unitsLabel")
    String unitsLabel();

    /**
     * Translated "Link".
     *
     * @return translated "Link"
     */
    @DefaultMessage("Link")
    @Key("linkLabel")
    String linkLabel();

    /**
     * Translated "Remove Link".
     *
     * @return translated "Remove Link"
     */
    @DefaultMessage("Remove Link")
    @Key("removeLinkTitle")
    String removeLinkTitle();

    /**
     * Translated "Text:".
     *
     * @return translated "Text:"
     */
    @DefaultMessage("Text:")
    @Key("textFormLabel")
    String textFormLabel();

    /**
     * Translated "Title/Tooltip:".
     *
     * @return translated "Title/Tooltip:"
     */
    @DefaultMessage("Title/Tooltip:")
    @Key("titleToolTipLabel")
    String titleToolTipLabel();

    /**
     * Translated "You must provide a value for the link target.".
     *
     * @return translated "You must provide a value for the link target."
     */
    @DefaultMessage("You must provide a value for the link target.")
    @Key("validateErrorMessage")
    String validateErrorMessage();

    /**
     * Translated "List".
     *
     * @return translated "List"
     */
    @DefaultMessage("List")
    @Key("listLabel")
    String listLabel();

    /**
     * Translated "(Default for presentation)".
     *
     * @return translated "(Default for presentation)"
     */
    @DefaultMessage("(Default for presentation)")
    @Key("defaultChoiceList")
    String defaultChoiceList();

    /**
     * Translated "Incremental (one item at a time)".
     *
     * @return translated "Incremental (one item at a time)"
     */
    @DefaultMessage("Incremental (one item at a time)")
    @Key("incrementalChoiceList")
    String incrementalChoiceList();

    /**
     * Translated "Non-Incremental (all items at once)".
     *
     * @return translated "Non-Incremental (all items at once)"
     */
    @DefaultMessage("Non-Incremental (all items at once)")
    @Key("nonIncrementalChoiceList")
    String nonIncrementalChoiceList();

    /**
     * Translated "Example".
     *
     * @return translated "Example"
     */
    @DefaultMessage("Example")
    @Key("exampleChoice")
    String exampleChoice();

    /**
     * Translated "Decimal".
     *
     * @return translated "Decimal"
     */
    @DefaultMessage("Decimal")
    @Key("decimalChoice")
    String decimalChoice();

    /**
     * Translated "Edit Equation ID".
     *
     * @return translated "Edit Equation ID"
     */
    @DefaultMessage("Edit Equation ID")
    @Key("editEquationCaption")
    String editEquationCaption();

    /**
     * Translated "Invalid ID".
     *
     * @return translated "Invalid ID"
     */
    @DefaultMessage("Invalid ID")
    @Key("invalidIDCaption")
    String invalidIDCaption();

    /**
     * Translated "Equation IDs must start with eq-".
     *
     * @return translated "Equation IDs must start with eq-"
     */
    @DefaultMessage("Equation IDs must start with eq-")
    @Key("invalidIDMessage")
    String invalidIDMessage();

    /**
     * Translated "Raw {0}".
     *
     * @return translated "Raw {0}"
     */
    @DefaultMessage("Raw {0}")
    @Key("modelDialogCaption")
    String modelDialogCaption(String caption);

    /**
     * Translated "Inline".
     *
     * @return translated "Inline"
     */
    @DefaultMessage("Inline")
    @Key("inlineText")
    String inlineText();

    /**
     * Translated "Block".
     *
     * @return translated "Block"
     */
    @DefaultMessage("Block")
    @Key("blockText")
    String blockText();

    /**
     * Translated "Remove Format".
     *
     * @return translated "Remove Format"
     */
    @DefaultMessage("Remove Format")
    @Key("removeFormatText")
    String removeFormatText();

    /**
     * Translated "No Content Specified".
     *
     * @return translated "No Content Specified"
     */
    @DefaultMessage("No Content Specified")
    @Key("validateCaption")
    String validateCaption();

    /**
     * Translated "You must provide content to apply the raw format to.".
     *
     * @return translated "You must provide content to apply the raw format to."
     */
    @DefaultMessage("You must provide content to apply the raw format to.")
    @Key("validateMessage")
    String validateMessage();

    /**
     * Translated "Image (File or URL):".
     *
     * @return translated "Image (File or URL):"
     */
    @DefaultMessage("Image (File or URL):")
    @Key("imageChooserLabel")
    String imageChooserLabel();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    @DefaultMessage("Browse...")
    @Key("browseLabel")
    String browseLabel();

    /**
     * Translated "Choose Image".
     *
     * @return translated "Choose Image"
     */
    @DefaultMessage("Choose Image")
    @Key("chooseImageCaption")
    String chooseImageCaption();

    /**
     * Translated "Looking Up DOI..".
     *
     * @return translated "Looking Up DOI.."
     */
    @DefaultMessage("Looking Up DOI..")
    @Key("onProgressMessage")
    String onProgressMessage();

    /**
     * Translated "You must provide a value for the citation id.".
     *
     * @return translated "You must provide a value for the citation id."
     */
    @DefaultMessage("You must provide a value for the citation id.")
    @Key("errorValidateMessage")
    String errorValidateMessage();

    /**
     * Translated "Please provide a validation citation Id.".
     *
     * @return translated "Please provide a validation citation Id."
     */
    @DefaultMessage("Please provide a validation citation Id.")
    @Key("citationErrorMessage")
    String citationErrorMessage();

    /**
     * Translated "Please select a unique citation Id.".
     *
     * @return translated "Please select a unique citation Id."
     */
    @DefaultMessage("Please select a unique citation Id.")
    @Key("uniqueCitationErrorMessage")
    String uniqueCitationErrorMessage();

    /**
     * Translated "You must select a bibliography.".
     *
     * @return translated "You must select a bibliography."
     */
    @DefaultMessage("You must select a bibliography.")
    @Key("bibliographyErrorMessage")
    String bibliographyErrorMessage();

    /**
     * Translated "You must provide a bibliography file name.".
     *
     * @return translated "You must provide a bibliography file name."
     */
    @DefaultMessage("You must provide a bibliography file name.")
    @Key("bibliographyFileNameErrorMessage")
    String bibliographyFileNameErrorMessage();

    /**
     * Translated "DOI Unavailable".
     *
     * @return translated "DOI Unavailable"
     */
    @DefaultMessage("DOI Unavailable")
    @Key("doiUnavailableCaption")
    String doiUnavailableCaption();

    /**
     * Translated "Citation from DOI".
     *
     * @return translated "Citation from DOI"
     */
    @DefaultMessage("Citation from DOI")
    @Key("citationDOITitle")
    String citationDOITitle();

    /**
     * Translated "Citation from ".
     *
     * @return translated "Citation from "
     */
    @DefaultMessage("Citation from ")
    @Key("citationFromText")
    String citationFromText();

    /**
     * Translated "An error occurred while loading citation data for this DOI.".
     *
     * @return translated "An error occurred while loading citation data for this DOI."
     */
    @DefaultMessage("An error occurred while loading citation data for this DOI.")
    @Key("kUnknownError")
    String kUnknownError();

    /**
     * Translated "Citation data for this DOI couldn't be found.".
     *
     * @return translated "Citation data for this DOI couldn't be found."
     */
    @DefaultMessage("Citation data for this DOI couldn''t be found.")
    @Key("kNoDataError")
    String kNoDataError();

    /**
     * Translated "Unable to reach server to load citation data for this DOI.".
     *
     * @return translated "Unable to reach server to load citation data for this DOI."
     */
    @DefaultMessage("Unable to reach server to load citation data for this DOI.")
    @Key("kServerError")
    String kServerError();

    /**
     * Translated "Insert Table".
     *
     * @return translated "Insert Table"
     */
    @DefaultMessage("Insert Table")
    @Key("insertTableCaption")
    String insertTableCaption();

    /**
     * Translated "Insert Tabset".
     *
     * @return translated "Insert Tabset"
     */
    @DefaultMessage("Insert Tabset")
    @Key("insertTabsetCaption")
    String insertTabsetCaption();

    /**
     * Translated "Tab names:".
     *
     * @return translated "Tab names:"
     */
    @DefaultMessage("Tab names:")
    @Key("tabNamesFormLabel")
    String tabNamesFormLabel();

    /**
     * Translated "Image".
     *
     * @return translated "Image"
     */
    @DefaultMessage("Image")
    @Key("imageTabListLabel")
    String imageTabListLabel();

    /**
     * Translated "Tabs".
     *
     * @return translated "Tabs"
     */
    @DefaultMessage("Tabs")
    @Key("tabsText")
    String tabsText();

    /**
     * Translated "You must specify at least two tab names".
     *
     * @return translated "You must specify at least two tab names"
     */
    @DefaultMessage("You must specify at least two tab names")
    @Key("tabSetErrorMessage")
    String tabSetErrorMessage();

    /**
     * Translated "(Tab {0}{1})".
     *
     * @return translated "(Tab {0}{1})"
     */
    @DefaultMessage("(Tab {0}{1})")
    @Key("addTabCaptionInput")
    String addTabCaptionInput(int index, String required);

    /**
     * Translated "- Optional".
     *
     * @return translated "- Optional"
     */
    @DefaultMessage("- Optional")
    @Key("optionalText")
    String optionalText();

    /**
     * Translated "Format:".
     *
     * @return translated "Format:"
     */
    @DefaultMessage("Format:")
    @Key("formatLabel")
    String formatLabel();

    /**
     * Translated "(Choose Format)".
     *
     * @return translated "(Choose Format)"
     */
    @DefaultMessage("(Choose Format)")
    @Key("chooseFormatLabel")
    String chooseFormatLabel();


    /**
     * Translated "{0} occurrences replaced.".
     *
     * @return translated "{0} occurrences replaced."
     */
    @DefaultMessage("{0} occurrences replaced.")
    @Key("rStudioGinjectorErrorMessage")
    String rStudioGinjectorErrorMessage(int replaced);

    /**
     * Translated "(chunk {0})".
     *
     * @return translated "(chunk {0})"
     */
    @DefaultMessage("(chunk {0})")
    @Key("chunkText")
    String chunkText(int sequence);

    /**
     * Translated "Reading bibliography...".
     *
     * @return translated "Reading bibliography..."
     */
    @DefaultMessage("Reading bibliography...")
    @Key("readingBibliographyProgressText")
    String readingBibliographyProgressText();

    /**
     * Translated "Saving bibliography...".
     *
     * @return translated "Saving bibliography..."
     */
    @DefaultMessage("Saving bibliography...")
    @Key("savingBibliographyProgressText")
    String savingBibliographyProgressText();

    /**
     * Translated "Looking up DOI...".
     *
     * @return translated "Looking up DOI..."
     */
    @DefaultMessage("Looking up DOI...")
    @Key("lookingUpDOIProgress")
    String lookingUpDOIProgress();

    /**
     * Translated "Loading Collections...".
     *
     * @return translated "Loading Collections..."
     */
    @DefaultMessage("Loading Collections...")
    @Key("loadingCollectionsProgressText")
    String loadingCollectionsProgressText();

    /**
     * Translated "Reading Collections...".
     *
     * @return translated "Reading Collections..."
     */
    @DefaultMessage("Reading Collections...")
    @Key("readingCollectionsProgressText")
    String readingCollectionsProgressText();

}
