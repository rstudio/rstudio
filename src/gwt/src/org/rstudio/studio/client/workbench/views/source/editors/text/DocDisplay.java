/*
 * DocDisplay.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.List;
import java.util.function.BiPredicate;

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.js.JsMap;
import org.rstudio.core.client.patch.TextChange;
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
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.EditorBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommandManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceGhostText;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Marker;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Selection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.TokenPredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ActiveScopeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.BreakpointMoveEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.BreakpointSetEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CommandClickEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FindRequestedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.HasDocumentChangedHandlers;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.HasFoldChangeHandlers;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.HasLineWidgetsChangedHandlers;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.HasRenderFinishedHandlers;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.UndoRedoEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDoc;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.events.ScrollYEvent;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasContextMenuHandlers;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

public interface DocDisplay extends HasValueChangeHandlers<Void>,
                                    HasFoldChangeHandlers,
                                    HasLineWidgetsChangedHandlers,
                                    IsWidget,
                                    HasContextMenuHandlers,
                                    HasFocusHandlers,
                                    HasKeyDownHandlers,
                                    HasRenderFinishedHandlers,
                                    HasDocumentChangedHandlers,
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
   
   public enum InsertionBehavior
   {
      EditorBehaviorsEnabled,
      EditorBehaviorsDisabled,
   }
   
   TextFileType getFileType();
   void setFileType(TextFileType fileType);
   void setFileType(TextFileType fileType, boolean suppressCompletion);
   void setFileType(TextFileType fileType, CompletionManager completionManager);
   void syncCompletionPrefs();
   void syncDiagnosticsPrefs();
   void setRnwCompletionContext(RnwCompletionContext rnwContext);
   void setCppCompletionContext(CppCompletionContext cppContext);
   void setRCompletionContext(CompletionContext rContext);
   void setEditorBehavior(EditorBehavior behavior);
   EditorBehavior getEditorBehavior();
   String getCode();
   JsArrayString getLines();
   JsArrayString getLines(int startRow, int endRow);
   void setCode(String code, boolean preserveCursorPosition);
   void insertCode(String code);
   void insertCode(String code, boolean unused);
   void insertCode(String code, InsertionBehavior behavior);
   void insertCode(InputEditorPosition position, String code);
   void applyChanges(TextChange[] changes);
   void applyChanges(TextChange[] changes, boolean preserveCursorPosition);
   void focus();
   boolean isFocused();
   void print();
   void codeCompletion();
   void goToHelp();
   void goToDefinition();
   String getSelectionValue();
   String getCurrentLine();
   String getCurrentLineUpToCursor();
   String getNextLineIndent();
   // This returns null for most file types, but for Sweave it returns "R" or
   // "TeX". Use SweaveFileType constants to test for these values.
   String getLanguageMode(Position position);
   String getModeId();

   boolean inMultiSelectMode();
   void exitMultiSelectMode();

   void quickAddNext();

   void yankBeforeCursor();
   void yankAfterCursor();
   void pasteLastYank();

   void clearSelection();
   void replaceSelection(String code);
   void replaceRange(Range range, String text);
   boolean moveSelectionToNextLine(boolean skipBlankLines);
   boolean moveSelectionToBlankLine();

   void expandSelection();
   void shrinkSelection();
   void clearSelectionHistory();

   void expandRaggedSelection();

   void reindent();
   void reindent(Range range);
   ChangeTracker getChangeTracker();

   String getCode(Position start, Position end);
   DocDisplay.AnchoredSelection createAnchoredSelection(Widget hostWidget,
                                                        Position start,
                                                        Position end);
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
   void onResize();

   void setLineHeight(double heightPct);
   void setHighlightSelectedLine(boolean on);
   void setHighlightSelectedWord(boolean on);
   void setShowLineNumbers(boolean on);
   void setRelativeLineNumbers(boolean relative);
   void setEnableKeyboardAccessibility(boolean keyboardAccessible);
   void setUseSoftTabs(boolean on);
   void setUseWrapMode(boolean on);
   boolean getUseWrapMode();
   void setTabSize(int tabSize);
   void autoDetectIndentation(boolean on);
   void setShowPrintMargin(boolean on);
   void setPrintMarginColumn(int column);
   void setShowInvisibles(boolean show);
   void setIndentGuides(String choice);
   void setBlinkingCursor(boolean blinking);
   void setScrollPastEndOfDocument(boolean enable);
   void setHighlightRFunctionCalls(boolean highlight);
   void setColorPreview(boolean show);
   void setRainbowParentheses(boolean rainbow);
   void setRainbowFencedDivs(boolean rainbow);
   boolean getRainbowParentheses();
   boolean getRainbowFencedDivs();
   void setBackgroundColor(String color);

   void setScrollLeft(int x);
   void setScrollTop(int y);
   void scrollTo(int x, int y);

   void enableSearchHighlight();
   void disableSearchHighlight();

   void setUseEmacsKeybindings(boolean use);
   boolean isEmacsModeOn();
   void clearEmacsMark();

   void setUseVimMode(boolean use);
   boolean isVimModeOn();
   boolean isVimInInsertMode();

   boolean isRendered();

   JsArray<AceFold> getFolds();
   String getFoldState(int row);
   void addFold(Range range);
   void addFoldFromRow(int row);
   void unfold(AceFold fold);
   void unfold(int row);
   void unfold(Range range);

   JsMap<Position> getMarks();
   void setMarks(JsMap<Position> marks);

   void toggleCommentLines();

   SpellingDoc getSpellingDoc();

   AceCommandManager getCommandManager();
   void setEditorCommandBinding(String id, List<KeySequence> keys);
   void resetCommands();

   HandlerRegistration addAttachHandler(AttachEvent.Handler handler);
   HandlerRegistration addEditorFocusHandler(FocusHandler handler);
   HandlerRegistration addEditorBlurHandler(BlurHandler handler);
   HandlerRegistration addCommandClickHandler(CommandClickEvent.Handler handler);
   HandlerRegistration addFindRequestedHandler(FindRequestedEvent.Handler handler);
   HandlerRegistration addCursorChangedHandler(CursorChangedEvent.Handler handler);
   HandlerRegistration addEditorModeChangedHandler(EditorModeChangedEvent.Handler handler);
   HandlerRegistration addSaveCompletedHandler(SaveFileEvent.Handler handler);
   HandlerRegistration addPasteHandler(PasteEvent.Handler handler);
   HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler);
   HandlerRegistration addCapturingKeyPressHandler(KeyPressHandler handler);
   HandlerRegistration addCapturingKeyUpHandler(KeyUpHandler handler);

   boolean isScopeTreeReady(int row);
   HandlerRegistration addScopeTreeReadyHandler(ScopeTreeReadyEvent.Handler handler);
   HandlerRegistration addActiveScopeChangedHandler(ActiveScopeChangedEvent.Handler handler);

   Position getCursorPosition();
   void setCursorPosition(Position position);
   int getCursorRow();
   int getCursorColumn();

   Position getCursorPositionScreen();

   void moveCursorBackward();
   void moveCursorBackward(int characters);
   void moveCursorForward();
   void moveCursorForward(int characters);

   void moveCursorNearTop();
   void moveCursorNearTop(int rowOffset);
   void ensureCursorVisible();
   void scrollCursorIntoViewIfNecessary();
   void scrollCursorIntoViewIfNecessary(int rowsAround);
   boolean isCursorInSingleLineString(boolean allowInComment);

   void gotoPageDown();
   void gotoPageUp();

   void ensureRowVisible(int row);

   InputEditorSelection search(String needle,
                               boolean backwards,
                               boolean wrap,
                               boolean caseSensitive,
                               boolean wholeWord,
                               Position start,
                               Range range,
                               boolean regexpModex);

   int getScrollLeft();
   void scrollToX(int x);

   int getScrollTop();
   void scrollToY(int y, int animateMs);

   void scrollToLine(int row, boolean center);

   void alignCursor(Position position, double ratio);
   void centerSelection();

   Scope getCurrentScope();
   Scope getCurrentChunk();
   Scope getCurrentSection();
   ScopeFunction getCurrentFunction(boolean allowAnonymous);

   Scope getScopeAtPosition(Position position);
   Scope getChunkAtPosition(Position position);
   ScopeFunction getFunctionAtPosition(Position position, boolean allowAnonymous);
   Scope getSectionAtPosition(Position position);
   boolean hasCodeModelScopeTree();
   JsArray<Scope> getScopeTree();
   InsertChunkInfo getInsertChunkInfo();

   void foldAll();
   void unfoldAll();
   void toggleFold();
   void setFoldStyle(String style); // see FoldStyle constants

   void jumpToMatching();
   void selectToMatching();
   void expandToMatching();

   void addCursorAbove();
   void addCursorBelow();
   void editLinesFromStart();

   HandlerRegistration addUndoRedoHandler(UndoRedoEvent.Handler handler);
   JavaScriptObject getCleanStateToken();
   boolean checkCleanStateToken(JavaScriptObject token);

   Selection getNativeSelection();
   Position getSelectionStart();
   Position getSelectionEnd();
   Range getSelectionRange();
   void setSelectionRange(Range range);

   int getLength(int row);
   int getRowCount();
   String getLine(int row);
   int getPixelWidth();

   Position positionFromIndex(int index);
   int indexFromPosition(Position position);

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

   void tokenizeDocument();
   void retokenizeDocument();
   Token getTokenAt(int row, int column);
   Token getTokenAt(Position position);
   JsArray<Token> getTokens(int row);

   TokenIterator createTokenIterator();
   TokenIterator createTokenIterator(Position position);

   Anchor createAnchor(Position pos);

   int getStartOfCurrentStatement();
   int getEndOfCurrentStatement();

   Range getMultiLineExpr(Position pos, int startRow, int endRow);
   Range getParagraph(Position pos, int startRow, int endRow);

   void highlightDebugLocation(
         SourcePosition startPos,
         SourcePosition endPos,
         boolean executing);
   void endDebugHighlighting();

   HandlerRegistration addBreakpointSetHandler
      (BreakpointSetEvent.Handler handler);
   HandlerRegistration addBreakpointMoveHandler
      (BreakpointMoveEvent.Handler handler);
   HandlerRegistration addScrollYHandler
      (ScrollYEvent.Handler handler);
   void addOrUpdateBreakpoint(Breakpoint breakpoint);
   void removeBreakpoint(Breakpoint breakpoint);
   void removeAllBreakpoints();
   void toggleBreakpointAtCursor();
   boolean hasBreakpoints();

   void setAnnotations(JsArray<AceAnnotation> annotations);
   void showLint(JsArray<LintItem> lint);
   void clearLint();
   
   JsMap<Marker> getMarkers(boolean inFront);
   void removeMarkersAtCursorPosition();
   void removeMarkersOnCursorLine();
   void removeMarkers(BiPredicate<AceAnnotation, Marker> predicate);
   void removeMarkersAtWord(String word);

   
   void beginCollabSession(CollabEditStartParams params, DirtyState dirtyState);
   boolean hasActiveCollabSession();
   boolean hasFollowingCollabSession();
   void endCollabSession();

   void setPopupVisible(boolean visible);
   boolean isPopupVisible();
   void selectAll(String needle);

   int getTabSize();
   void insertRoxygenSkeleton();

   long getLastModifiedTime();
   long getLastCursorChangedTime();

   void moveLinesUp();
   void moveLinesDown();
   void expandToLine();
   void copyLinesDown();
   void joinLines();
   void removeLine();

   void blockIndent();
   void blockOutdent();
   void splitIntoLines();

   int getFirstFullyVisibleRow();

   Rectangle getPositionBounds(Position position);
   Rectangle getRangeBounds(Range range);

   Position toDocumentPosition(ScreenCoordinates coordinates);
   ScreenCoordinates documentPositionToScreenCoordinates(Position position);
   Position screenCoordinatesToDocumentPosition(int pageX, int pageY);

   void forceImmediateRender();
   boolean isPositionVisible(Position position);

   int getFirstVisibleRow();
   int getLastVisibleRow();

   void showInfoBar(String message);

   void setDragEnabled(boolean enabled);

   boolean isSnippetsTabStopManagerActive();

   void addLineWidget(LineWidget widget);
   void removeLineWidget(LineWidget widget);
   void removeAllLineWidgets();
   void onLineWidgetChanged(LineWidget widget);

   boolean hasLineWidgets();
   JsArray<LineWidget> getLineWidgets();
   LineWidget getLineWidgetForRow(int row);

   boolean showChunkOutputInline();
   void setShowChunkOutputInline(boolean show);
   JsArray<ChunkDefinition> getChunkDefs();
   void setChunkLineExecState(int start, int end, int state);

   Position getDocumentEnd();

   void setInsertMatching(boolean value);
   void setSurroundSelectionPref(String value);

   void goToLineStart();
   void goToLineEnd();

   void toggleTokenInfo();
   void setTextInputAriaLabel(String label);
   
   AceGhostText getGhostText();
   void setGhostText(String text);
   boolean hasGhostText();
   void applyGhostText();
   void removeGhostText();
}
