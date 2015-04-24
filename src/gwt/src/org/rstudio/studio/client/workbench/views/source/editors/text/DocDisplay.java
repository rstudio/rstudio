/*
 * DocDisplay.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.common.debugging.model.Breakpoint;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.TokenPredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.BreakpointMoveEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.BreakpointSetEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CommandClickEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FindRequestedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.HasFoldChangeHandlers;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.UndoRedoHandler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;

import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

public interface DocDisplay extends HasValueChangeHandlers<Void>,
                                    HasFoldChangeHandlers,
                                    IsWidget,
                                    HasFocusHandlers,
                                    HasKeyDownHandlers,
                                    InputEditorDisplay,
                                    NavigableSourceEditor
{
   public interface AnchoredSelection
   {
      String getValue();
      Range getRange();
      void apply();
      void detach();
   }
   TextFileType getFileType();
   void setFileType(TextFileType fileType);
   void setFileType(TextFileType fileType, boolean suppressCompletion);
   void setFileType(TextFileType fileType, CompletionManager completionManager);
   void syncCompletionPrefs();
   void syncDiagnosticsPrefs();
   void setRnwCompletionContext(RnwCompletionContext rnwContext);
   void setCppCompletionContext(CppCompletionContext cppContext);
   void setRCompletionContext(RCompletionContext rContext);
   String getCode();
   void setCode(String code, boolean preserveCursorPosition);
   void insertCode(String code, boolean blockMode);
   void focus();
   boolean isFocused();
   void print();
   void codeCompletion();
   void goToHelp();
   void goToFunctionDefinition();
   String getSelectionValue();
   String getCurrentLine();
   String getCurrentLineUpToCursor();
   String getNextLineIndent();
   // This returns null for most file types, but for Sweave it returns "R" or
   // "TeX". Use SweaveFileType constants to test for these values.
   String getLanguageMode(Position position);
   void replaceSelection(String code);
   boolean moveSelectionToNextLine(boolean skipBlankLines);
   boolean moveSelectionToBlankLine(); 
   void reindent();
   void reindent(Range range);
   ChangeTracker getChangeTracker();

   String getCode(Position start, Position end);
   DocDisplay.AnchoredSelection createAnchoredSelection(Position start,
                                             Position end);
   String getCode(InputEditorSelection selection);

   void fitSelectionToLines(boolean expand);
   int getSelectionOffset(boolean start);
   
   // Fix bug 964
   void onActivate();

   void setReadOnly(boolean readOnly);
   void setFontSize(double size);

   void onVisibilityChanged(boolean visible);

   void setHighlightSelectedLine(boolean on);
   void setHighlightSelectedWord(boolean on);
   void setShowLineNumbers(boolean on);
   void setUseSoftTabs(boolean on);
   void setUseWrapMode(boolean on);
   void setTabSize(int tabSize);
   void setShowPrintMargin(boolean on);
   void setPrintMarginColumn(int column);
   void setShowInvisibles(boolean show);
   void setShowIndentGuides(boolean show);
   void setBlinkingCursor(boolean blinking);
   
   void setUseVimMode(boolean use);
   boolean isVimModeOn();
   boolean isVimInInsertMode();

   JsArray<AceFold> getFolds();
   void addFold(Range range);
   void addFoldFromRow(int row);
   void unfold(AceFold fold);
   void unfold(int row);
   void unfold(Range range);
   
   void toggleCommentLines();

   HandlerRegistration addEditorFocusHandler(FocusHandler handler);
   
   HandlerRegistration addCommandClickHandler(CommandClickEvent.Handler handler);
   
   HandlerRegistration addFindRequestedHandler(FindRequestedEvent.Handler handler);
   
   HandlerRegistration addCursorChangedHandler(CursorChangedHandler handler);
   
   Position getCursorPosition();
   void setCursorPosition(Position position);
   void moveCursorNearTop();
   void moveCursorNearTop(int rowOffset);
   void ensureCursorVisible();
   boolean isCursorInSingleLineString();
   
   InputEditorSelection search(String needle,
                               boolean backwards,
                               boolean wrap,
                               boolean caseSensitive,
                               boolean wholeWord,
                               Position start,
                               Range range,
                               boolean regexpModex);
   
   void insertCode(InputEditorPosition position, String code);
   
   int getScrollLeft();
   void scrollToX(int x);
   
   int getScrollTop();
   void scrollToY(int y);
   
   Scope getCurrentScope();
   Scope getCurrentChunk();
   Scope getCurrentChunk(Position position);
   ScopeFunction getCurrentFunction(boolean allowAnonymous);
   Scope getCurrentSection();
   ScopeFunction getFunctionAtPosition(Position position, boolean allowAnonymous);
   Scope getSectionAtPosition(Position position);
   boolean hasScopeTree();
   JsArray<Scope> getScopeTree();
   InsertChunkInfo getInsertChunkInfo();

   void foldAll();
   void unfoldAll();
   void toggleFold();
   
   void jumpToMatching();
   void selectToMatching();
   void expandToMatching();

   HandlerRegistration addUndoRedoHandler(UndoRedoHandler handler);
   JavaScriptObject getCleanStateToken();
   boolean checkCleanStateToken(JavaScriptObject token);

   Position getSelectionStart();
   Position getSelectionEnd();
   Range getSelectionRange();
   void setSelectionRange(Range range);
   int getLength(int row);
   int getRowCount();

   String getLine(int row);
   
   char getCharacterAtCursor();
   char getCharacterBeforeCursor();
   
   String debug_getDocumentDump();
   void debug_setSessionValueDirectly(String s);

   // HACK: This should not use Ace-specific data structures
   InputEditorSelection createSelection(Position pos1, Position pos2);
   
   // HACK: InputEditorPosition should just become AceInputEditorPosition
   Position selectionToPosition(InputEditorPosition pos);
   
   InputEditorPosition createInputEditorPosition(Position pos);

   Iterable<Range> getWords(TokenPredicate tokenPredicate,
                            CharClassifier charClassifier,
                            Position start,
                            Position end);

   String getTextForRange(Range range);

   Anchor createAnchor(Position pos);
   
   int getStartOfCurrentStatement();
   int getEndOfCurrentStatement();
   
   void highlightDebugLocation(
         SourcePosition startPos,
         SourcePosition endPos,
         boolean executing);
   void endDebugHighlighting();
   
   HandlerRegistration addBreakpointSetHandler
      (BreakpointSetEvent.Handler handler);
   HandlerRegistration addBreakpointMoveHandler
      (BreakpointMoveEvent.Handler handler);
   void addOrUpdateBreakpoint(Breakpoint breakpoint);
   void removeBreakpoint(Breakpoint breakpoint);
   void removeAllBreakpoints();
   void toggleBreakpointAtCursor();
   boolean hasBreakpoints();
   
   void setAnnotations(JsArray<AceAnnotation> annotations);
   void showLint(JsArray<LintItem> lint);
   void clearLint();
   void removeMarkersAtCursorPosition();
   void removeMarkersOnCursorLine();
   
   void setPopupVisible(boolean visible);
   boolean isPopupVisible();
   void selectAll(String needle);
   
   int getTabSize();
   void insertRoxygenSkeleton();
   
   long getLastModifiedTime();
   long getLastCursorChangedTime();
   
   void blockOutdent();
   
   Rectangle getPositionBounds(Position position);
}
