/*
 * DocDisplay.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceClickEvent.Handler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.UndoRedoHandler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;

public interface DocDisplay extends HasValueChangeHandlers<Void>,
                                    IsWidget,
                                    HasFocusHandlers,
                                    HasKeyDownHandlers,
                                    InputEditorDisplay,
                                    NavigableSourceEditor
{
   public interface AnchoredSelection
   {
      String getValue();
      void apply();
      void detach();
   }
   void setFileType(TextFileType fileType);
   void setFileType(TextFileType fileType, boolean suppressCompletion);
   void setFileType(TextFileType fileType, CompletionManager completionManager);
   String getCode();
   void setCode(String code, boolean preserveCursorPosition);
   void insertCode(String code, boolean blockMode);
   void focus();
   void print();
   void goToFunctionDefinition();
   String getSelectionValue();
   String getCurrentLine();
   void replaceSelection(String code);
   boolean moveSelectionToNextLine(boolean skipBlankLines);
   void reindent();
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

   HandlerRegistration addAceClickHandler(Handler handler);
   
   HandlerRegistration addCursorChangedHandler(CursorChangedHandler handler);
   Position getCursorPosition();
   void setCursorPosition(Position position);

   
   InputEditorSelection search(String needle,
                               boolean backwards,
                               boolean wrap,
                               boolean caseSensitive,
                               boolean wholeWord,
                               boolean selectionOnly,
                               boolean regexpModex);
   
   void insertCode(InputEditorPosition position, String code);
   
   int getScrollLeft();
   void scrollToX(int x);
   
   int getScrollTop();
   void scrollToY(int y);
   
   FunctionStart getCurrentFunction();
   JsArray<FunctionStart> getFunctionTree();

   HandlerRegistration addUndoRedoHandler(UndoRedoHandler handler);
   JavaScriptObject getCleanStateToken();
   boolean checkCleanStateToken(JavaScriptObject token);

   Position getSelectionStart();
   Position getSelectionEnd();
   int getLength(int row);
   int getRowCount();

   String getLine(int row);
   
   void debug_forceTopsToZero();
   String debug_getDocumentDump();
   void debug_setSessionValueDirectly(String s);

   // HACK: This should not use Ace-specific data structures
   InputEditorSelection createSelection(Position pos1, Position pos2);
   
   // HACK: InputEditorPosition should just become AceInputEditorPosition
   Position selectionToPosition(InputEditorPosition pos);
}