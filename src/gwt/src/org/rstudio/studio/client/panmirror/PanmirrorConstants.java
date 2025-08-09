/*
 * PanmirrorConstants.java
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
package org.rstudio.studio.client.panmirror;

public interface PanmirrorConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Visual Editor".
     *
     * @return translated "Visual Editor"
     */
    String visualEditorText();

    /**
     * Translated "visual editor ".
     *
     * @return translated "visual editor {0}"
     */
    String visualEditorLabel(String menuText);

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    String formatTitle();

    /**
     * Translated "Insert".
     *
     * @return translated "Insert"
     */
    String insertTitle();

    /**
     * Translated "Table".
     *
     * @return translated "Table"
     */
    String tableTitle();

    /**
     * Translated "Find/Replace".
     *
     * @return translated "Find/Replace"
     */
    String findReplaceTitle();

    /**
     * Translated "Format".
     *
     * @return translated "Format"
     */
    String formatText();


    /**
     * Translated "Insert".
     *
     * @return translated "Insert"
     */
    String insertText();

    /**
     * Translated "Table".
     *
     * @return translated "Table"
     */
    String tableText();


    /**
     * Translated "Normal".
     *
     * @return translated "Normal"
     */
    String panmirrorBlockMenuDefaultText();

    /**
     * Translated "Block Format".
     *
     * @return translated "Block Format"
     */
    String panMirrorBlockMenuTitle();

    /**
     * Translated "Undo".
     *
     * @return translated "Undo"
     */
    String undoMenuText();

    /**
     * Translated "Redo".
     *
     * @return translated "Redo"
     */
    String redoMenuText();

    /**
     * Translated "Select All".
     *
     * @return translated "Select All"
     */
    String selectAllMenuText();

    /**
     * Translated "Bold".
     *
     * @return translated "Bold"
     */
    String boldMenuText();

    /**
     * Translated "Italic".
     *
     * @return translated "Italic"
     */
    String italicMenuText();

    /**
     * Translated "Code".
     *
     * @return translated "Code"
     */
    String codeMenuText();

    /**
     * Translated "Strikeout".
     *
     * @return translated "Strikeout"
     */
    String strikeoutMenuText();

    /**
     * Translated "Superscript".
     *
     * @return translated "Superscript"
     */
    String superscriptMenuText();

    /**
     * Translated "Subscript".
     *
     * @return translated "Subscript"
     */
    String subscriptMenuText();

    /**
     * Translated "Small Caps".
     *
     * @return translated "Small Caps"
     */
    String smallCapsMenuText();

    /**
     * Translated "Underline".
     *
     * @return translated "Underline"
     */
    String underlineMenuText();

    /**
     * Translated "Span...".
     *
     * @return translated "Span..."
     */
    String spanMenuText();

    /**
     * Translated "Normal".
     *
     * @return translated "Normal"
     */
    String normalMenuText();

    /**
     * Translated "Header 1".
     *
     * @return translated "Header 1"
     */
    String heading1MenuText();

    /**
     * Translated "Header 2".
     *
     * @return translated "Header 2"
     */
    String heading2MenuText();

    /**
     * Translated "Header 3".
     *
     * @return translated "Header 3"
     */
    String heading3MenuText();

    /**
     * Translated "Header 4".
     *
     * @return translated "Header 4"
     */
    String heading4MenuText();

    /**
     * Translated "Header 5".
     *
     * @return translated "Header 5"
     */
    String heading5MenuText();

    /**
     * Translated "Header 6".
     *
     * @return translated "Header 6"
     */
    String heading6MenuText();

    /**
     * Translated "Code".
     *
     * @return translated "Code"
     */
    String codeBlockMenuText();

    /**
     * Translated "Code Block...".
     *
     * @return translated "Code Block..."
     */
    String codeBlockFormatMenuText();

    /**
     * Translated "Blockquote".
     *
     * @return translated "Blockquote"
     */
    String blockquoteMenuText();

    /**
     * Translated "Line Block".
     *
     * @return translated "Line Block"
     */
    String linkBlockMenuText();

    /**
     * Translated "Line Block".
     *
     * @return translated "Line Block"
     */
    String divMenuText();

    /**
     * Translated "Edit Attributes...".
     *
     * @return translated "Edit Attributes..."
     */
    String editAttributesMenuText();

    /**
     * Translated "Clear Formatting".
     *
     * @return translated "Clear Formatting"
     */
    String clearFormattingMenuText();


    /**
     * Translated "TeX Inline".
     *
     * @return translated "TeX Inline"
     */
    String texInlineMenuText();


    /**
     * Translated "TeX Block".
     *
     * @return translated "TeX Block"
     */
    String texBlockMenuText();

    /**
     * Translated "HTML Inline...".
     *
     * @return translated "HTML Inline..."
     */
    String htmlInlineMenuText();

    /**
     * Translated "HTML Block".
     *
     * @return translated "HTML Block"
     */
    String htmlBlockMenuText();

    /**
     * Translated "Raw Inline...".
     *
     * @return translated "Raw Inline..."
     */
    String rawInlineMenuText();

    /**
     * Translated "Raw Block...".
     *
     * @return translated "Raw Block..."
     */
    String rawBlockMenuText();

    /**
     * Translated "Expand Chunk".
     *
     * @return translated "Expand Chunk"
     */
    String expandChunkMenuText();

    /**
     * Translated "Collapse Chunk".
     *
     * @return translated "Collapse Chunk"
     */
    String collapseChunkMenuText();

    /**
     * Translated "Expand All Chunks".
     *
     * @return translated "Expand All Chunks"
     */
    String expandAllChunksMenuText();

    /**
     * Translated "Collapse All Chunks".
     *
     * @return translated "Collapse All Chunks"
     */
    String collapseAllChunksMenuText();

    /**
     * Translated "Bulleted List".
     *
     * @return translated "Bulleted List"
     */
    String bulletedListMenuText();

    /**
     * Translated "Numbered List".
     *
     * @return translated "Numbered List"
     */
    String numberedListMenuText();

    /**
     * Translated "Tight List".
     *
     * @return translated "Tight List"
     */
    String tightListMenuText();

    /**
     * Translated "Sink Item".
     *
     * @return translated "Sink Item"
     */
    String sinkItemMenuText();

    /**
     * Translated "Lift Item".
     *
     * @return translated "Lift Item"
     */
    String liftItemMenuText();

    /**
     * Translated "Item Checkbox".
     *
     * @return translated "Item Checkbox"
     */
    String itemCheckboxMenuText();

    /**
     * Translated "Item Checked".
     *
     * @return translated "Item Checked"
     */
    String itemCheckedMenuText();

    /**
     * Translated "List Attributes...".
     *
     * @return translated "List Attributes..."
     */
    String listAttributesMenuText();

    /**
     * Translated "Insert Table...".
     *
     * @return translated "Insert Table..."
     */
    String insertTableMenuText();

    /**
     * Translated "Table Header".
     *
     * @return translated "Table Header"
     */
    String tableHeaderMenuText();

    /**
     * Translated "Table Caption".
     *
     * @return translated "Table Caption"
     */
    String tableCaptionMenuText();


    /**
     * Translated "Table:::Insert Column Right".
     *
     * @return translated "Table:::Insert Column Right"
     */
    String tableInsertColumnRightMenuText();

    /**
     * Translated "Insert %d Columns Right".
     *
     * @return translated "Insert %d Columns Right"
     */
    String tableInsertColumnRightPluralMenuText();

    /**
     * Translated "Table:::Insert Column Left".
     *
     * @return translated "Table:::Insert Column Left"
     */
    String tableInsertColumnLeftMenuText();

    /**
     * Translated "Insert %d Columns Left".
     *
     * @return translated "Insert %d Columns Left"
     */
    String tableInsertColumnLeftPluralMenuText();

    /**
     * Translated "Table:::Delete Column".
     *
     * @return translated "Table:::Delete Column"
     */
    String tableDeleteColumnMenuText();

    /**
     * Translated "Table:::Delete %d Columns".
     *
     * @return translated "Table:::Delete %d Columns"
     */
    String tableDeleteColumnPluralMenuText();

    /**
     * Translated "Table:::Insert Row Below".
     *
     * @return translated "Table:::Insert Row Below"
     */
    String tableInsertRowBelowMenuText();

    /**
     * Translated "Table:::Insert %d Rows Below".
     *
     * @return translated "Table:::Insert %d Rows Below"
     */
    String tableInsertRowBelowPluralMenuText();

    /**
     * Translated "Table:::Insert Row Above".
     *
     * @return translated "Table:::Insert Row Above"
     */
    String tableInsertRowAboveMenuText();

    /**
     * Translated "Table:::Insert %d Rows Above".
     *
     * @return translated "Table:::Insert %d Rows Above"
     */
    String tableInsertRowAbovePluralMenuText();

    /**
     * Translated "Table:::Insert %d Rows Above".
     *
     * @return translated "Table:::Delete Row"
     */
    String tableDeleteRowMenuText();

    /**
     * Translated "Delete %d Rows".
     *
     * @return translated "Delete %d Rows"
     */
    String tableDeleteRowPluralMenuText();

    /**
     * Translated "Delete Table".
     *
     * @return translated "Delete Table"
     */
    String deleteTableMenuText();

    /**
     * Translated "Table:::Next Cell".
     *
     * @return translated "Table:::Next Cell"
     */
    String tableNextCellMenuText();

    /**
     * Translated "Table:::Previous Cell".
     *
     * @return translated "Table:::Previous Cell"
     */
    String tablePreviousCellMenuText();

    /**
     * Translated "Table Align Column:::Left".
     *
     * @return translated "Table Align Column:::Left"
     */
    String tableAlignColumnLeftMenuText();

    /**
     * Translated "Table Align Column:::Right".
     *
     * @return translated "Table Align Column:::Right"
     */
    String tableAlignColumnRightMenuText();

    /**
     * Translated "Table Align Column:::Center".
     *
     * @return translated "Table Align Column:::Center"
     */
    String tableAlignColumnCenterMenuText();

    /**
     * Translated "Table Align Column:::Default".
     *
     * @return translated "Table Align Column:::Default"
     */
    String tableAlignColumnDefaultMenuText();

    /**
     * Translated "Any...".
     *
     * @return translated "Any..."
     */
    String anyMenuText();

    /**
     * Translated "Table...".
     *
     * @return translated "Table..."
     */
    String tableMenuText();

    /**
     * Translated "Table...".
     *
     * @return translated "Link..."
     */
    String linkMenuText();

    /**
     * Translated "Remove Link".
     *
     * @return translated "Remove Link"
     */
    String removeLinkMenuText();

    /**
     * Translated "Figure / Image...".
     *
     * @return translated "Figure / Image..."
     */
    String figureImageMenuText();

    /**
     * Translated "Footnote".
     *
     * @return translated "Footnote"
     */
    String footnoteMenuText();

    /**
     * Translated "Horizontal Rule".
     *
     * @return translated "Horizontal Rule"
     */
    String horizontalRuleMenuText();

    /**
     * Translated "Paragraph".
     *
     * @return translated "Paragraph"
     */
    String paragraphMenuText();

    /**
     * Translated "Comment".
     *
     * @return translated "Comment"
     */
    String commentMenuText();

    /**
     * Translated "YAML Block".
     *
     * @return translated "YAML Block"
     */
    String yamlBlockMenuText();

    /**
     * Translated "Shortcode".
     *
     * @return translated "Shortcode"
     */
    String shortcodeMenuText();

    /**
     * Translated "Inline Math".
     *
     * @return translated "Inline Math"
     */
    String inlineMathMenuText();

    /**
     * Translated "Display Math".
     *
     * @return translated "Display Math"
     */
    String displayMathMenuText();

    /**
     * Translated "Definition List".
     *
     * @return translated "Definition List"
     */
    String definitionListMenuText();

    /**
     * Translated "Term".
     *
     * @return translated "Term"
     */
    String termMenuText();

    /**
     * Translated "Description".
     *
     * @return translated "Description"
     */
    String descriptionMenuText();

    /**
     * Translated "Citation...".
     *
     * @return translated "Citation..."
     */
    String citationMenuText();

    /**
     * Translated "Cross Reference".
     *
     * @return translated "Cross Reference"
     */
    String crossReferenceMenuText();

    /**
     * Translated "Insert Emoji...".
     *
     * @return translated "Insert Emoji..."
     */
    String insertEmojiMenuText();

    /**
     * Translated "Insert Unicode...".
     *
     * @return translated "Insert Unicode..."
     */
    String insertUnicodeMenuText();

    /**
     * Translated "Insert:::Em Dash (—)".
     *
     * @return translated "Insert:::Em Dash (—)"
     */
    String insertEmDashMenuText();

    /**
     * Translated "Insert:::En Dash (–)".
     *
     * @return translated "Insert:::En Dash (–)"
     */
    String insertEnDashMenuText();

    /**
     * Translated "Insert:::Non-Breaking Space".
     *
     * @return translated "Insert:::Non-Breaking Space"
     */
    String insertNonBreakingSpaceMenuText();

    /**
     * Translated "Insert:::Hard Line Break".
     *
     * @return translated "Insert:::Hard Line Break"
     */
    String insertHardLinkBreakMenuText();

    /**
     * Translated "Insert:::Tabset...".
     *
     * @return translated "Insert:::Tabset..."
     */
    String insertTabsetMenuText();

    /**
     * Translated "Insert:::Callout...".
     *
     * @return translated "Insert:::Callout..."
     */
    String insertCalloutMenuText();

    /**
     * Translated "Go to Next Section".
     *
     * @return translated "Go to Next Section"
     */
    String goToNextSectionMenuText();

    /**
     * Translated "Go to Previous Section".
     *
     * @return translated "Go to Previous Section"
     */
    String goToPreviousSectionMenuText();

    /**
     * Translated "Go to Next Chunk".
     *
     * @return translated "Go to Next Chunk"
     */
    String goToNextChunkMenuText();

    /**
     * Translated "Insert:::Slide Pause".
     *
     * @return translated "Insert:::Slide Pause"
     */
    String insertSlidePauseMenuText();

    /**
     * Translated "Insert:::Slide Notes".
     *
     * @return translated "Insert:::Slide Notes"
     */
    String insertSlideNotesMenuText();

    /**
     * Translated "Insert:::Slide Columns".
     *
     * @return translated "Insert:::Slide Columns"
     */
    String insertSlideColumnsMenuText();

    /**
     * Translated "Edit Attributes".
     *
     * @return translated "Edit Attributes"
     */
    String editAttributesCaption();

    /**
     * Translated "Span Attributes".
     *
     * @return translated "Span Attributes"
     */
    String spanAttributesCaption();


    /**
     * Translated "Unwrap Span".
     *
     * @return translated "Unwrap Span"
     */
    String unwrapSpanRemoveButtonCaption();

    /**
     * Translated "Div Attributes".
     *
     * @return translated "Div Attributes"
     */
    String divAttributesCaption();

    /**
     * Translated "Callout".
     *
     * @return translated "Callout"
     */
    String calloutCaption();

    /**
     * Translated "Unwrap Div".
     *
     * @return translated "Unwrap Div"
     */
    String unwrapDivTitle();


    /**
     * Translated "Type: ".
     *
     * @return translated "Type: "
     */
    String typeLabel();



    /**
     * Translated "Appearance: ".
     *
     * @return translated "Appearance: "
     */
    String appearanceLabel();

    /**
     * Translated "Show icon".
     *
     * @return translated "Show icon"
     */
    String showIconLabel();

    /**
     * Translated "Caption:".
     *
     * @return translated "Caption:"
     */
    String captionLabel();

    /**
     * Translated "(Optional)".
     *
     * @return translated "(Optional)"
     */
    String optionalPlaceholder();

    /**
     * Translated "Div".
     *
     * @return translated "Div"
     */
    String divTabList();

    /**
     * Translated "Callout".
     *
     * @return translated "Callout"
     */
    String calloutText();

    /**
     * Translated "Attributes".
     *
     * @return translated "Attributes"
     */
    String attributesText();

    /**
     * Translated "Code Block".
     *
     * @return translated "Code Block"
     */
    String codeBlockText();

    /**
     * Translated "Language".
     *
     * @return translated "Language"
     */
    String languageFormLabel();

    /**
     * Translated "(optional)".
     *
     * @return translated "(optional)"
     */
    String optionalFormLabel();


    /**
     * Translated "Figure".
     *
     * @return translated "Figure"
     */
    String figureLabel();

    /**
     * Translated "Image".
     *
     * @return translated "Image"
     */
    String imageLabel();

    /**
     * Translated "Width:".
     *
     * @return translated "Width:"
     */
    String widthLabel();

    /**
     * Translated "Height:".
     *
     * @return translated "Height:"
     */
    String heightLabel();

    /**
     * Translated "(Auto)".
     *
     * @return translated "(Auto):"
     */
    String autoText();

    /**
     * Translated "Lock ratio".
     *
     * @return translated "Lock ratio"
     */
    String lockRatioText();

    /**
     * Translated "Default".
     *
     * @return translated "Default"
     */
    String defaultAlignLabel();
    
    /**
     * Translated "Left".
     *
     * @return translated "Left"
     */
    String leftLabel();

    /**
     * Translated "Center".
     *
     * @return translated "Center"
     */
    String centerLabel();

    /**
     * Translated "Right".
     *
     * @return translated "Right"
     */
    String rightLabel();

    /**
     * Translated "Alignment".
     *
     * @return translated "Alignment"
     */
    String legendText();

    /**
     * Translated "Alternative text:".
     *
     * @return translated "Alternative text:"
     */
    String alternativeTextLabel();

    /**
     * Translated "Link to:".
     *
     * @return translated "Link to:"
     */
    String linkToLabel();

    /**
     * Translated "LaTeX environment:".
     *
     * @return translated "LaTeX environment:"
     */
    String latexEnvironmentLabel();

    /**
     * Translated "Title attribute:".
     *
     * @return translated "Title attribute:"
     */
    String titleAttributeLabel();

    /**
     * Translated "Advanced".
     *
     * @return translated "Advanced"
     */
    String advancedLabel();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCaption();

    /**
     * Translated "You must provide a value for image width.".
     *
     * @return translated "You must provide a value for image width."
     */
    String errorMessage();

    /**
     * Translated "Units".
     *
     * @return translated "Units"
     */
    String unitsLabel();

    /**
     * Translated "Link".
     *
     * @return translated "Link"
     */
    String linkLabel();

    /**
     * Translated "Remove Link".
     *
     * @return translated "Remove Link"
     */
    String removeLinkTitle();

    /**
     * Translated "Text:".
     *
     * @return translated "Text:"
     */
    String textFormLabel();

    /**
     * Translated "Title/Tooltip:".
     *
     * @return translated "Title/Tooltip:"
     */
    String titleToolTipLabel();

    /**
     * Translated "You must provide a value for the link target.".
     *
     * @return translated "You must provide a value for the link target."
     */
    String validateErrorMessage();

    /**
     * Translated "List".
     *
     * @return translated "List"
     */
    String listLabel();

    /**
     * Translated "(Default for presentation)".
     *
     * @return translated "(Default for presentation)"
     */
    String defaultChoiceList();

    /**
     * Translated "Incremental (one item at a time)".
     *
     * @return translated "Incremental (one item at a time)"
     */
    String incrementalChoiceList();

    /**
     * Translated "Non-Incremental (all items at once)".
     *
     * @return translated "Non-Incremental (all items at once)"
     */
    String nonIncrementalChoiceList();

    /**
     * Translated "Example".
     *
     * @return translated "Example"
     */
    String exampleChoice();

    /**
     * Translated "Decimal".
     *
     * @return translated "Decimal"
     */
    String decimalChoice();

    /**
     * Translated "Edit Equation ID".
     *
     * @return translated "Edit Equation ID"
     */
    String editEquationCaption();

    /**
     * Translated "Invalid ID".
     *
     * @return translated "Invalid ID"
     */
    String invalidIDCaption();

    /**
     * Translated "Equation IDs must start with eq-".
     *
     * @return translated "Equation IDs must start with eq-"
     */
    String invalidIDMessage();

    /**
     * Translated "Raw {0}".
     *
     * @return translated "Raw {0}"
     */
    String modelDialogCaption(String caption);

    /**
     * Translated "Inline".
     *
     * @return translated "Inline"
     */
    String inlineText();

    /**
     * Translated "Block".
     *
     * @return translated "Block"
     */
    String blockText();

    /**
     * Translated "Remove Format".
     *
     * @return translated "Remove Format"
     */
    String removeFormatText();

    /**
     * Translated "No Content Specified".
     *
     * @return translated "No Content Specified"
     */
    String validateCaption();

    /**
     * Translated "You must provide content to apply the raw format to.".
     *
     * @return translated "You must provide content to apply the raw format to."
     */
    String validateMessage();

    /**
     * Translated "Image (File or URL):".
     *
     * @return translated "Image (File or URL):"
     */
    String imageChooserLabel();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    String browseLabel();

    /**
     * Translated "Choose Image".
     *
     * @return translated "Choose Image"
     */
    String chooseImageCaption();

    /**
     * Translated "Looking Up DOI..".
     *
     * @return translated "Looking Up DOI.."
     */
    String onProgressMessage();

    /**
     * Translated "You must provide a value for the citation id.".
     *
     * @return translated "You must provide a value for the citation id."
     */
    String errorValidateMessage();

    /**
     * Translated "Please provide a validation citation Id.".
     *
     * @return translated "Please provide a validation citation Id."
     */
    String citationErrorMessage();

    /**
     * Translated "Please select a unique citation Id.".
     *
     * @return translated "Please select a unique citation Id."
     */
    String uniqueCitationErrorMessage();

    /**
     * Translated "You must select a bibliography.".
     *
     * @return translated "You must select a bibliography."
     */
    String bibliographyErrorMessage();

    /**
     * Translated "You must provide a bibliography file name.".
     *
     * @return translated "You must provide a bibliography file name."
     */
    String bibliographyFileNameErrorMessage();

    /**
     * Translated "DOI Unavailable".
     *
     * @return translated "DOI Unavailable"
     */
    String doiUnavailableCaption();

    /**
     * Translated "Citation from DOI".
     *
     * @return translated "Citation from DOI"
     */
    String citationDOITitle();

    /**
     * Translated "Citation from ".
     *
     * @return translated "Citation from "
     */
    String citationFromText();

    /**
     * Translated "An error occurred while loading citation data for this DOI.".
     *
     * @return translated "An error occurred while loading citation data for this DOI."
     */
    String kUnknownError();

    /**
     * Translated "Citation data for this DOI couldn't be found.".
     *
     * @return translated "Citation data for this DOI couldn't be found."
     */
    String kNoDataError();

    /**
     * Translated "Unable to reach server to load citation data for this DOI.".
     *
     * @return translated "Unable to reach server to load citation data for this DOI."
     */
    String kServerError();

    /**
     * Translated "Insert Table".
     *
     * @return translated "Insert Table"
     */
    String insertTableCaption();

    /**
     * Translated "Insert Tabset".
     *
     * @return translated "Insert Tabset"
     */
    String insertTabsetCaption();

    /**
     * Translated "Tab names:".
     *
     * @return translated "Tab names:"
     */
    String tabNamesFormLabel();

    /**
     * Translated "Image".
     *
     * @return translated "Image"
     */
    String imageTabListLabel();

    /**
     * Translated "Tabs".
     *
     * @return translated "Tabs"
     */
    String tabsText();

    /**
     * Translated "You must specify at least two tab names".
     *
     * @return translated "You must specify at least two tab names"
     */
    String tabSetErrorMessage();

    /**
     * Translated "(Tab {0}{1})".
     *
     * @return translated "(Tab {0}{1})"
     */
    String addTabCaptionInput(int index, String required);

    /**
     * Translated "- Optional".
     *
     * @return translated "- Optional"
     */
    String optionalText();

    /**
     * Translated "Format:".
     *
     * @return translated "Format:"
     */
    String formatLabel();

    /**
     * Translated "(Choose Format)".
     *
     * @return translated "(Choose Format)"
     */
    String chooseFormatLabel();


    /**
     * Translated "{0} occurrences replaced.".
     *
     * @return translated "{0} occurrences replaced."
     */
    String rStudioGinjectorErrorMessage(int replaced);

    /**
     * Translated "(chunk {0})".
     *
     * @return translated "(chunk {0})"
     */
    String chunkText(int sequence);

    /**
     * Translated "Reading bibliography...".
     *
     * @return translated "Reading bibliography..."
     */
    String readingBibliographyProgressText();

    /**
     * Translated "Saving bibliography...".
     *
     * @return translated "Saving bibliography..."
     */
    String savingBibliographyProgressText();

    /**
     * Translated "Looking up DOI...".
     *
     * @return translated "Looking up DOI..."
     */
    String lookingUpDOIProgress();

    /**
     * Translated "Loading Collections...".
     *
     * @return translated "Loading Collections..."
     */
    String loadingCollectionsProgressText();

    /**
     * Translated "Reading Collections...".
     *
     * @return translated "Reading Collections..."
     */
    String readingCollectionsProgressText();

}
