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

    @DefaultMessage("Visual Editor")
    @Key("visualEditorText")
    String visualEditorText();

    @DefaultMessage("visual editor {0} ")
    @Key("visualEditorLabel")
    String visualEditorLabel(String menuText);

    @DefaultMessage("Format")
    @Key("formatTitle")
    String formatTitle();

    @DefaultMessage("Insert")
    @Key("insertTitle")
    String insertTitle();

    @DefaultMessage("Table")
    @Key("tableTitle")
    String tableTitle();

    @DefaultMessage("Find/Replace")
    @Key("findReplaceTitle")
    String findReplaceTitle();

    @DefaultMessage("Format")
    @Key("formatText")
    String formatText();

    @DefaultMessage("Insert")
    @Key("insertText")
    String insertText();

    @DefaultMessage("Table")
    @Key("tableText")
    String tableText();

    @DefaultMessage("Normal")
    @Key("panmirrorBlockMenuDefaultText")
    String panmirrorBlockMenuDefaultText();

    @DefaultMessage("Block Format")
    @Key("panMirrorBlockMenuTitle")
    String panMirrorBlockMenuTitle();

    @DefaultMessage("Undo")
    @Key("undoMenuText")
    String undoMenuText();

    @DefaultMessage("Redo")
    @Key("redoMenuText")
    String redoMenuText();

    @DefaultMessage("Select All")
    @Key("selectAllMenuText")
    String selectAllMenuText();

    @DefaultMessage("Bold")
    @Key("boldMenuText")
    String boldMenuText();

    @DefaultMessage("Italic")
    @Key("italicMenuText")
    String italicMenuText();

    @DefaultMessage("Code")
    @Key("codeMenuText")
    String codeMenuText();

    @DefaultMessage("Strikeout")
    @Key("strikeoutMenuText")
    String strikeoutMenuText();

    @DefaultMessage("Superscript")
    @Key("superscriptMenuText")
    String superscriptMenuText();

    @DefaultMessage("Subscript")
    @Key("subscriptMenuText")
    String subscriptMenuText();

    @DefaultMessage("Small Caps")
    @Key("smallCapsMenuText")
    String smallCapsMenuText();

    @DefaultMessage("Underline")
    @Key("underlineMenuText")
    String underlineMenuText();

    @DefaultMessage("Span...")
    @Key("spanMenuText")
    String spanMenuText();

    @DefaultMessage("Normal")
    @Key("normalMenuText")
    String normalMenuText();

    @DefaultMessage("Header 1")
    @Key("heading1MenuText")
    String heading1MenuText();

    @DefaultMessage("Header 2")
    @Key("heading2MenuText")
    String heading2MenuText();

    @DefaultMessage("Header 3")
    @Key("heading3MenuText")
    String heading3MenuText();

    @DefaultMessage("Header 4")
    @Key("heading4MenuText")
    String heading4MenuText();

    @DefaultMessage("Header 5")
    @Key("heading5MenuText")
    String heading5MenuText();

    @DefaultMessage("Header 6")
    @Key("heading6MenuText")
    String heading6MenuText();

    @DefaultMessage("Code")
    @Key("codeBlockMenuText")
    String codeBlockMenuText();

    @DefaultMessage("Code Block...")
    @Key("codeBlockFormatMenuText")
    String codeBlockFormatMenuText();

    @DefaultMessage("Blockquote")
    @Key("blockquoteMenuText")
    String blockquoteMenuText();

    @DefaultMessage("Line Block")
    @Key("linkBlockMenuText")
    String linkBlockMenuText();

    @DefaultMessage("Div...")
    @Key("divMenuText")
    String divMenuText();

    @DefaultMessage("Edit Attributes...")
    @Key("editAttributesMenuText")
    String editAttributesMenuText();

    @DefaultMessage("Clear Formatting")
    @Key("clearFormattingMenuText")
    String clearFormattingMenuText();

    @DefaultMessage("TeX Inline")
    @Key("texInlineMenuText")
    String texInlineMenuText();

    @DefaultMessage("TeX Block")
    @Key("texBlockMenuText")
    String texBlockMenuText();

    @DefaultMessage("HTML Inline...")
    @Key("htmlInlineMenuText")
    String htmlInlineMenuText();

    @DefaultMessage("HTML Block")
    @Key("htmlBlockMenuText")
    String htmlBlockMenuText();

    @DefaultMessage("Raw Inline...")
    @Key("rawInlineMenuText")
    String rawInlineMenuText();

    @DefaultMessage("Raw Block...")
    @Key("rawBlockMenuText")
    String rawBlockMenuText();

    @DefaultMessage("Expand Chunk")
    @Key("expandChunkMenuText")
    String expandChunkMenuText();

    @DefaultMessage("Collapse Chunk")
    @Key("collapseChunkMenuText")
    String collapseChunkMenuText();

    @DefaultMessage("Expand All Chunks")
    @Key("expandAllChunksMenuText")
    String expandAllChunksMenuText();

    @DefaultMessage("Collapse All Chunks")
    @Key("collapseAllChunksMenuText")
    String collapseAllChunksMenuText();

    @DefaultMessage("Bulleted List")
    @Key("bulletedListMenuText")
    String bulletedListMenuText();

    @DefaultMessage("Numbered List")
    @Key("numberedListMenuText")
    String numberedListMenuText();

    @DefaultMessage("Tight List")
    @Key("tightListMenuText")
    String tightListMenuText();

    @DefaultMessage("Sink Item")
    @Key("sinkItemMenuText")
    String sinkItemMenuText();

    @DefaultMessage("Lift Item")
    @Key("liftItemMenuText")
    String liftItemMenuText();

    @DefaultMessage("Item Checkbox")
    @Key("itemCheckboxMenuText")
    String itemCheckboxMenuText();

    @DefaultMessage("Item Checked")
    @Key("itemCheckedMenuText")
    String itemCheckedMenuText();

    @DefaultMessage("List Attributes...")
    @Key("listAttributesMenuText")
    String listAttributesMenuText();

    @DefaultMessage("Insert Table...")
    @Key("insertTableMenuText")
    String insertTableMenuText();

    @DefaultMessage("Table Header")
    @Key("tableHeaderMenuText")
    String tableHeaderMenuText();

    @DefaultMessage("Table Caption")
    @Key("tableCaptionMenuText")
    String tableCaptionMenuText();

    @DefaultMessage("Table:::Insert Column Right")
    @Key("tableInsertColumnRightMenuText")
    String tableInsertColumnRightMenuText();

    @DefaultMessage("Insert %d Columns Right")
    @Key("tableInsertColumnRightPluralMenuText")
    String tableInsertColumnRightPluralMenuText();

    @DefaultMessage("Table:::Insert Column Left")
    @Key("tableInsertColumnLeftMenuText")
    String tableInsertColumnLeftMenuText();

    @DefaultMessage("Insert %d Columns Left")
    @Key("tableInsertColumnLeftPluralMenuText")
    String tableInsertColumnLeftPluralMenuText();

    @DefaultMessage("Table:::Delete Column")
    @Key("tableDeleteColumnMenuText")
    String tableDeleteColumnMenuText();

    @DefaultMessage("Table:::Delete %d Columns")
    @Key("tableDeleteColumnPluralMenuText")
    String tableDeleteColumnPluralMenuText();

    @DefaultMessage("Table:::Insert Row Below")
    @Key("tableInsertRowBelowMenuText")
    String tableInsertRowBelowMenuText();

    @DefaultMessage("Table:::Insert %d Rows Below")
    @Key("tableInsertRowBelowPluralMenuText")
    String tableInsertRowBelowPluralMenuText();

    @DefaultMessage("Table:::Insert Row Above")
    @Key("tableInsertRowAboveMenuText")
    String tableInsertRowAboveMenuText();

    @DefaultMessage("Table:::Insert %d Rows Above")
    @Key("tableInsertRowAbovePluralMenuText")
    String tableInsertRowAbovePluralMenuText();

    @DefaultMessage("Table:::Delete Row")
    @Key("tableDeleteRowMenuText")
    String tableDeleteRowMenuText();

    @DefaultMessage("Delete %d Rows")
    @Key("tableDeleteRowPluralMenuText")
    String tableDeleteRowPluralMenuText();

    @DefaultMessage("Delete Table")
    @Key("deleteTableMenuText")
    String deleteTableMenuText();

    @DefaultMessage("Table:::Next Cell")
    @Key("tableNextCellMenuText")
    String tableNextCellMenuText();

    @DefaultMessage("Table:::Previous Cell")
    @Key("tablePreviousCellMenuText")
    String tablePreviousCellMenuText();

    @DefaultMessage("Table Align Column:::Left")
    @Key("tableAlignColumnLeftMenuText")
    String tableAlignColumnLeftMenuText();

    @DefaultMessage("Table Align Column:::Right")
    @Key("tableAlignColumnRightMenuText")
    String tableAlignColumnRightMenuText();

    @DefaultMessage("Table Align Column:::Center")
    @Key("tableAlignColumnCenterMenuText")
    String tableAlignColumnCenterMenuText();

    @DefaultMessage("Table Align Column:::Default")
    @Key("tableAlignColumnDefaultMenuText")
    String tableAlignColumnDefaultMenuText();

    @DefaultMessage("Any...")
    @Key("anyMenuText")
    String anyMenuText();

    @DefaultMessage("Table...")
    @Key("tableMenuText")
    String tableMenuText();

    @DefaultMessage("Link...")
    @Key("linkMenuText")
    String linkMenuText();

    @DefaultMessage("Remove Link")
    @Key("removeLinkMenuText")
    String removeLinkMenuText();

    @DefaultMessage("Figure / Image...")
    @Key("figureImageMenuText")
    String figureImageMenuText();

    @DefaultMessage("Footnote")
    @Key("footnoteMenuText")
    String footnoteMenuText();

    @DefaultMessage("Horizontal Rule")
    @Key("horizontalRuleMenuText")
    String horizontalRuleMenuText();

    @DefaultMessage("Paragraph")
    @Key("paragraphMenuText")
    String paragraphMenuText();

    @DefaultMessage("Comment")
    @Key("commentMenuText")
    String commentMenuText();

    @DefaultMessage("YAML Block")
    @Key("yamlBlockMenuText")
    String yamlBlockMenuText();

    @DefaultMessage("Shortcode")
    @Key("shortcodeMenuText")
    String shortcodeMenuText();

    @DefaultMessage("Inline Math")
    @Key("inlineMathMenuText")
    String inlineMathMenuText();

    @DefaultMessage("Display Math")
    @Key("displayMathMenuText")
    String displayMathMenuText();

    @DefaultMessage("Definition List")
    @Key("definitionListMenuText")
    String definitionListMenuText();

    @DefaultMessage("Term")
    @Key("termMenuText")
    String termMenuText();

    @DefaultMessage("Description")
    @Key("descriptionMenuText")
    String descriptionMenuText();

    @DefaultMessage("Citation...")
    @Key("citationMenuText")
    String citationMenuText();

    @DefaultMessage("Cross Reference")
    @Key("crossReferenceMenuText")
    String crossReferenceMenuText();

    @DefaultMessage("Insert Emoji...")
    @Key("insertEmojiMenuText")
    String insertEmojiMenuText();

    @DefaultMessage("Insert Unicode...")
    @Key("insertUnicodeMenuText")
    String insertUnicodeMenuText();

    @DefaultMessage("Insert:::Em Dash (—)")
    @Key("insertEmDashMenuText")
    String insertEmDashMenuText();

    @DefaultMessage("Insert:::En Dash (–)")
    @Key("insertEnDashMenuText")
    String insertEnDashMenuText();

    @DefaultMessage("Insert:::Non-Breaking Space")
    @Key("insertNonBreakingSpaceMenuText")
    String insertNonBreakingSpaceMenuText();

    @DefaultMessage("Insert:::Hard Line Break")
    @Key("insertHardLinkBreakMenuText")
    String insertHardLinkBreakMenuText();

    @DefaultMessage("Insert:::Tabset...")
    @Key("insertTabsetMenuText")
    String insertTabsetMenuText();

    @DefaultMessage("Insert:::Callout...")
    @Key("insertCalloutMenuText")
    String insertCalloutMenuText();

    @DefaultMessage("Go to Next Section")
    @Key("goToNextSectionMenuText")
    String goToNextSectionMenuText();

    @DefaultMessage("Go to Previous Section")
    @Key("goToPreviousSectionMenuText")
    String goToPreviousSectionMenuText();

    @DefaultMessage("Go to Next Chunk")
    @Key("goToNextChunkMenuText")
    String goToNextChunkMenuText();

    @DefaultMessage("Insert:::Slide Pause")
    @Key("insertSlidePauseMenuText")
    String insertSlidePauseMenuText();

    @DefaultMessage("Insert:::Slide Notes")
    @Key("insertSlideNotesMenuText")
    String insertSlideNotesMenuText();

    @DefaultMessage("Insert:::Slide Columns")
    @Key("insertSlideColumnsMenuText")
    String insertSlideColumnsMenuText();

    @DefaultMessage("Edit Attributes")
    @Key("editAttributesCaption")
    String editAttributesCaption();

    @DefaultMessage("Span Attributes")
    @Key("spanAttributesCaption")
    String spanAttributesCaption();

    @DefaultMessage("Unwrap Span")
    @Key("unwrapSpanRemoveButtonCaption")
    String unwrapSpanRemoveButtonCaption();

    @DefaultMessage("Div Attributes")
    @Key("divAttributesCaption")
    String divAttributesCaption();

    @DefaultMessage("Callout")
    @Key("calloutCaption")
    String calloutCaption();

    @DefaultMessage("Unwrap Div")
    @Key("unwrapDivTitle")
    String unwrapDivTitle();

    @DefaultMessage("Type: ")
    @Key("typeLabel")
    String typeLabel();

    @DefaultMessage("Appearance: ")
    @Key("appearanceLabel")
    String appearanceLabel();

    @DefaultMessage("Show icon")
    @Key("showIconLabel")
    String showIconLabel();

    @DefaultMessage("Caption:")
    @Key("captionLabel")
    String captionLabel();

    @DefaultMessage("(Optional)")
    @Key("optionalPlaceholder")
    String optionalPlaceholder();

    @DefaultMessage("Div")
    @Key("divTabList")
    String divTabList();

    @DefaultMessage("Callout")
    @Key("calloutText")
    String calloutText();

    @DefaultMessage("Attributes")
    @Key("attributesText")
    String attributesText();

    @DefaultMessage("Code Block")
    @Key("codeBlockText")
    String codeBlockText();

    @DefaultMessage("Language")
    @Key("languageFormLabel")
    String languageFormLabel();

    @DefaultMessage("(optional)")
    @Key("optionalFormLabel")
    String optionalFormLabel();

    @DefaultMessage("Figure")
    @Key("figureLabel")
    String figureLabel();

    @DefaultMessage("Image")
    @Key("imageLabel")
    String imageLabel();

    @DefaultMessage("Width:")
    @Key("widthLabel")
    String widthLabel();

    @DefaultMessage("Height:")
    @Key("heightLabel")
    String heightLabel();

    @DefaultMessage("(Auto)")
    @Key("autoText")
    String autoText();

    @DefaultMessage("Lock ratio")
    @Key("lockRatioText")
    String lockRatioText();

    @DefaultMessage("default")
    @Key("defaultAlignLabel")
    String defaultAlignLabel();
    

    @DefaultMessage("Left")
    @Key("leftLabel")
    String leftLabel();

    @DefaultMessage("Center")
    @Key("centerLabel")
    String centerLabel();

    @DefaultMessage("Right")
    @Key("rightLabel")
    String rightLabel();

    @DefaultMessage("Alignment")
    @Key("legendText")
    String legendText();

    @DefaultMessage("Alternative text:")
    @Key("alternativeTextLabel")
    String alternativeTextLabel();

    @DefaultMessage("Link to:")
    @Key("linkToLabel")
    String linkToLabel();

    @DefaultMessage("LaTeX environment:")
    @Key("latexEnvironmentLabel")
    String latexEnvironmentLabel();

    @DefaultMessage("Title attribute:")
    @Key("titleAttributeLabel")
    String titleAttributeLabel();

    @DefaultMessage("Advanced")
    @Key("advancedLabel")
    String advancedLabel();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("You must provide a value for image width.")
    @Key("errorMessage")
    String errorMessage();

    @DefaultMessage("Units")
    @Key("unitsLabel")
    String unitsLabel();

    @DefaultMessage("Link")
    @Key("linkLabel")
    String linkLabel();

    @DefaultMessage("Remove Link")
    @Key("removeLinkTitle")
    String removeLinkTitle();

    @DefaultMessage("Text:")
    @Key("textFormLabel")
    String textFormLabel();

    @DefaultMessage("Title/Tooltip:")
    @Key("titleToolTipLabel")
    String titleToolTipLabel();

    @DefaultMessage("You must provide a value for the link target.")
    @Key("validateErrorMessage")
    String validateErrorMessage();

    @DefaultMessage("List")
    @Key("listLabel")
    String listLabel();

    @DefaultMessage("(Default for presentation)")
    @Key("defaultChoiceList")
    String defaultChoiceList();

    @DefaultMessage("Incremental (one item at a time)")
    @Key("incrementalChoiceList")
    String incrementalChoiceList();

    @DefaultMessage("Non-Incremental (all items at once)")
    @Key("nonIncrementalChoiceList")
    String nonIncrementalChoiceList();

    @DefaultMessage("Example")
    @Key("exampleChoice")
    String exampleChoice();

    @DefaultMessage("Decimal")
    @Key("decimalChoice")
    String decimalChoice();

    @DefaultMessage("Edit Equation ID")
    @Key("editEquationCaption")
    String editEquationCaption();

    @DefaultMessage("Invalid ID")
    @Key("invalidIDCaption")
    String invalidIDCaption();

    @DefaultMessage("Equation IDs must start with eq-")
    @Key("invalidIDMessage")
    String invalidIDMessage();

    @DefaultMessage("Raw {0}")
    @Key("modelDialogCaption")
    String modelDialogCaption(String caption);

    @DefaultMessage("Inline")
    @Key("inlineText")
    String inlineText();

    @DefaultMessage("Block")
    @Key("blockText")
    String blockText();

    @DefaultMessage("Remove Format")
    @Key("removeFormatText")
    String removeFormatText();

    @DefaultMessage("No Content Specified")
    @Key("validateCaption")
    String validateCaption();

    @DefaultMessage("You must provide content to apply the raw format to.")
    @Key("validateMessage")
    String validateMessage();

    @DefaultMessage("Image (File or URL):")
    @Key("imageChooserLabel")
    String imageChooserLabel();

    @DefaultMessage("Browse...")
    @Key("browseLabel")
    String browseLabel();

    @DefaultMessage("Choose Image")
    @Key("chooseImageCaption")
    String chooseImageCaption();

    @DefaultMessage("Looking Up DOI..")
    @Key("onProgressMessage")
    String onProgressMessage();

    @DefaultMessage("You must provide a value for the citation id.")
    @Key("errorValidateMessage")
    String errorValidateMessage();

    @DefaultMessage("Please provide a validation citation Id.")
    @Key("citationErrorMessage")
    String citationErrorMessage();

    @DefaultMessage("Please select a unique citation Id.")
    @Key("uniqueCitationErrorMessage")
    String uniqueCitationErrorMessage();

    @DefaultMessage("You must select a bibliography.")
    @Key("bibliographyErrorMessage")
    String bibliographyErrorMessage();

    @DefaultMessage("You must provide a bibliography file name.")
    @Key("bibliographyFileNameErrorMessage")
    String bibliographyFileNameErrorMessage();

    @DefaultMessage("DOI Unavailable")
    @Key("doiUnavailableCaption")
    String doiUnavailableCaption();

    @DefaultMessage("Citation from DOI")
    @Key("citationDOITitle")
    String citationDOITitle();

    @DefaultMessage("Citation from ")
    @Key("citationFromText")
    String citationFromText();

    @DefaultMessage("An error occurred while loading citation data for this DOI.")
    @Key("kUnknownError")
    String kUnknownError();

    @DefaultMessage("Citation data for this DOI couldn''t be found.")
    @Key("kNoDataError")
    String kNoDataError();

    @DefaultMessage("Unable to reach server to load citation data for this DOI.")
    @Key("kServerError")
    String kServerError();

    @DefaultMessage("Insert Table")
    @Key("insertTableCaption")
    String insertTableCaption();

    @DefaultMessage("Insert Tabset")
    @Key("insertTabsetCaption")
    String insertTabsetCaption();

    @DefaultMessage("Tab names:")
    @Key("tabNamesFormLabel")
    String tabNamesFormLabel();

    @DefaultMessage("Image")
    @Key("imageTabListLabel")
    String imageTabListLabel();

    @DefaultMessage("Tabs")
    @Key("tabsText")
    String tabsText();

    @DefaultMessage("You must specify at least two tab names")
    @Key("tabSetErrorMessage")
    String tabSetErrorMessage();

    @DefaultMessage("(Tab {0}{1})")
    @Key("addTabCaptionInput")
    String addTabCaptionInput(int index, String required);

    @DefaultMessage("- Optional")
    @Key("optionalText")
    String optionalText();

    @DefaultMessage("Format:")
    @Key("formatLabel")
    String formatLabel();

    @DefaultMessage("(Choose Format)")
    @Key("chooseFormatLabel")
    String chooseFormatLabel();

    @DefaultMessage("{0} occurrences replaced.")
    @Key("rStudioGinjectorErrorMessage")
    String rStudioGinjectorErrorMessage(int replaced);

    @DefaultMessage("(chunk {0})")
    @Key("chunkText")
    String chunkText(int sequence);

    @DefaultMessage("Reading bibliography...")
    @Key("readingBibliographyProgressText")
    String readingBibliographyProgressText();

    @DefaultMessage("Saving bibliography...")
    @Key("savingBibliographyProgressText")
    String savingBibliographyProgressText();

    @DefaultMessage("Looking up DOI...")
    @Key("lookingUpDOIProgress")
    String lookingUpDOIProgress();

    @DefaultMessage("Loading Collections...")
    @Key("loadingCollectionsProgressText")
    String loadingCollectionsProgressText();

    @DefaultMessage("Reading Collections...")
    @Key("readingCollectionsProgressText")
    String readingCollectionsProgressText();

}
