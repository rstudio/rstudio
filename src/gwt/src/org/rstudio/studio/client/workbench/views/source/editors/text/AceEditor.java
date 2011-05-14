/*
 * AceEditor.java
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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.Style.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.DynamicIFrame;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.model.EventBasedChangeTracker;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager.InitCompletionFilter;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.NullCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

public class AceEditor implements DocDisplay, InputEditorDisplay
{
   private class Filter implements InitCompletionFilter
   {
      public boolean shouldComplete(NativeEvent event)
      {
         // Never complete if there's an active selection
         Range range = getSession().getSelection().getRange();
         if (!range.isEmpty())
            return false;

         // Don't consider Tab to be a completion if we're at the start of a
         // line (e.g. only zero or more whitespace characters between the
         // beginning of the line and the cursor)

         if (event.getKeyCode() != KeyCodes.KEY_TAB)
            return true;

         int col = range.getStart().getColumn();
         if (col == 0)
            return false;

         String row = getSession().getLine(range.getStart().getRow());
         return row.substring(0, col).trim().length() != 0;
      }
   }

   public static void preload()
   {
      load(null);
   }

   public static void load(final Command command)
   {
      aceLoader_.addCallback(new Callback()
      {
         public void onLoaded()
         {
            aceSupportLoader_.addCallback(new Callback()
            {
               public void onLoaded()
               {
                  if (command != null)
                     command.execute();
               }
            });
         }
      });
   }

   @Inject
   public AceEditor()
   {
      widget_ = new AceEditorWidget();
      completionManager_ = new NullCompletionManager();
      RStudioGinjector.INSTANCE.injectMembers(this);

      widget_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         public void onValueChange(ValueChangeEvent<Void> evt)
         {
            ValueChangeEvent.fire(AceEditor.this, null);
         }
      });
   }

   @Inject
   void initialize(CodeToolsServerOperations server)
   {
      server_ = server;
   }

   public void setFileType(TextFileType fileType)
   {
      setFileType(fileType, false);
   }

   public void setFileType(TextFileType fileType, boolean suppressCompletion)
   {
      fileType_ = fileType;
      updateLanguage(suppressCompletion);
   }

   private void updateLanguage(boolean suppressCompletion)
   {
      if (fileType_ == null)
         return;

      if (!suppressCompletion && fileType_.getEditorLanguage().useRCompletion())
      {
         completionManager_ = new RCompletionManager(this,
                                                     new CompletionPopupPanel(),
                                                     server_,
                                                     new Filter());
      }
      else
         completionManager_ = new NullCompletionManager();

      widget_.getEditor().setKeyboardHandler(
            new AceCompletionAdapter(completionManager_).getKeyboardHandler());

      getSession().setEditorMode(
            fileType_.getEditorLanguage().getParserName(),
            Desktop.isDesktop() && Desktop.getFrame().suppressSyntaxHighlighting());
      getSession().setUseWrapMode(fileType_.getWordWrap());
   }

   public String getCode()
   {
      return getSession().getValue();
   }

   public void setCode(String code, boolean preserveCursorPosition)
   {
      widget_.setCode(code);
      if (!preserveCursorPosition)
         widget_.getEditor().getSession().getSelection().moveCursorTo(0,
                                                                      0,
                                                                      false);
   }

   public void insertCode(String code, boolean blockMode)
   {
      // TODO: implement block mode
      getSession().replace(
            getSession().getSelection().getRange(), code);
   }

   public void focus()
   {
      widget_.getEditor().focus();
   }

   class PrintIFrame extends DynamicIFrame
   {
      public PrintIFrame(String code)
      {
         code_ = code;

         getElement().getStyle().setPosition(com.google.gwt.dom.client.Style.Position.ABSOLUTE);
         getElement().getStyle().setLeft(-5000, Unit.PX);
      }

      @Override
      protected void onFrameLoaded()
      {
         Document doc = getDocument();
         PreElement pre = doc.createPreElement();
         pre.setInnerText(code_);
         pre.getStyle().setProperty("whiteSpace", "pre-wrap");
         doc.getBody().appendChild(pre);

         getWindow().print();

         // Bug 1224: ace: print from source causes inability to reconnect
         // This was caused by the iframe being removed from the document too
         // quickly after the print job was sent. As a result, attempting to
         // navigate away from the page at any point afterwards would result
         // in the error "Document cannot change while printing or in Print
         // Preview". The only thing you could do is close the browser tab.
         // By inserting a 5-minute delay hopefully Firefox would be done with
         // whatever print related operations are important.
         Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
         {
            public boolean execute()
            {
               PrintIFrame.this.removeFromParent();
               return false;
            }
         }, 1000 * 60 * 5);
      }

      private final String code_;
   }

   public void print()
   {
      PrintIFrame printIFrame = new PrintIFrame(getCode());
      RootPanel.get().add(printIFrame);
   }

   public String getText()
   {
      return getSession().getLine(
            getSession().getSelection().getCursor().getRow());
   }

   public void setText(String string)
   {
      setCode(string, false);
   }

   public boolean hasSelection()
   {
      return true;
   }

   public InputEditorSelection getSelection()
   {
      Range selection = getSession().getSelection().getRange();
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), selection.getStart()),
            new AceInputEditorPosition(getSession(), selection.getEnd()));

   }

   public String getSelectionValue()
   {
      return getSession().getTextRange(
            getSession().getSelection().getRange());
   }

   public void setSelection(InputEditorSelection selection)
   {
      AceInputEditorPosition start = (AceInputEditorPosition)selection.getStart();
      AceInputEditorPosition end = (AceInputEditorPosition)selection.getEnd();
      getSession().getSelection().setSelectionRange(Range.fromPoints(
            start.getValue(), end.getValue()));
   }

   public Rectangle getCursorBounds()
   {
      Range range = getSession().getSelection().getRange();
      Renderer renderer = widget_.getEditor().getRenderer();
      ScreenCoordinates start = renderer.textToScreenCoordinates(
                  range.getStart().getRow(),
                  range.getStart().getColumn());
      ScreenCoordinates end = renderer.textToScreenCoordinates(
                  range.getEnd().getRow(),
                  range.getEnd().getColumn());
      // TODO: Use actual width and height
      return new Rectangle(start.getPageX(),
                           start.getPageY(),
                           end.getPageX() - start.getPageX(),
                           9);
   }

   public Rectangle getBounds()
   {
      return new Rectangle(
            widget_.getAbsoluteLeft(),
            widget_.getAbsoluteTop(),
            widget_.getOffsetWidth(),
            widget_.getOffsetHeight());
   }

   public void setFocus(boolean focused)
   {
      if (focused)
         widget_.getEditor().focus();
      else
         widget_.getEditor().blur();
   }

   public String replaceSelection(String value, boolean collapseSelection)
   {
      Selection selection = getSession().getSelection();
      String oldValue = getSession().getTextRange(selection.getRange());

      replaceSelection(value);

      if (collapseSelection)
      {
         collapseSelection(false);
      }

      return oldValue;
   }

   public boolean isSelectionCollapsed()
   {
      return getSession().getSelection().isEmpty();
   }

   public void clear()
   {
      setCode("", false);
   }

   public void collapseSelection(boolean collapseToStart)
   {
      Selection selection = getSession().getSelection();
      Range rng = selection.getRange();
      Position pos = collapseToStart ? rng.getStart() : rng.getEnd();
      selection.setSelectionRange(Range.fromPoints(pos, pos));
   }

   public InputEditorSelection getStart()
   {
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), Position.create(0, 0)));
   }

   public InputEditorSelection getEnd()
   {
      EditSession session = getSession();
      int rows = session.getLength();
      Position end = Position.create(rows, session.getLine(rows).length());
      return new InputEditorSelection(new AceInputEditorPosition(session, end));
   }

   public String getCurrentLine()
   {
      int row = widget_.getEditor()
            .getSession()
            .getSelection()
            .getRange()
            .getStart()
            .getRow();
      return getSession().getLine(row);
   }

   public void replaceCode(String code)
   {
      int endRow, endCol;

      endRow = getSession().getLength() - 1;
      if (endRow < 0)
      {
         endRow = 0;
         endCol = 0;
      }
      else
      {
         endCol = getSession().getLine(endRow).length();
      }

      Range range = Range.fromPoints(Position.create(0, 0),
                                     Position.create(endRow, endCol));
      getSession().replace(range, code);
   }

   public void replaceSelection(String code)
   {
      Range selRange = getSession().getSelection().getRange();
      Position position = getSession().replace(selRange, code);
      Range range = Range.fromPoints(selRange.getStart(), position);
      getSession().getSelection().setSelectionRange(range);
   }

   public boolean moveSelectionToNextLine(boolean skipBlankLines)
   {
      int curRow = getSession().getSelection().getCursor().getRow();
      while (++curRow < getSession().getLength())
      {
         String line = getSession().getLine(curRow);
         Pattern pattern = Pattern.create("[^\\s]");
         Match match = pattern.match(line, 0);
         if (skipBlankLines && match == null)
            continue;
         int col =  (match != null) ? match.getIndex() : 0;
         getSession().getSelection().moveCursorTo(curRow, col, false);
         return true;
      }
      return false;
   }

   public ChangeTracker getChangeTracker()
   {
      return new EventBasedChangeTracker<Void>(this);
   }

   public void fitSelectionToLines(boolean expand)
   {
      Range range = getSession().getSelection().getRange();
      Position start = range.getStart();
      Position newStart = start;

      if (start.getColumn() > 0)
      {
         if (expand)
         {
            newStart = Position.create(start.getRow(), 0);
         }
         else
         {
            String firstLine = getSession().getLine(start.getRow());
            if (firstLine.substring(0, start.getColumn()).trim().length() == 0)
               newStart = Position.create(start.getRow(), 0);
         }
      }

      Position end = range.getEnd();
      Position newEnd = end;
      if (expand)
      {
         int endRow = end.getRow();
         if (endRow == newStart.getRow() || end.getColumn() > 0)
         {
            // If selection ends at the start of a line, keep the selection
            // there--unless that means less than one line will be selected
            // in total.
            newEnd = Position.create(
                  endRow, getSession().getLine(endRow).length());
         }
      }
      else
      {
         while (newEnd.getRow() != newStart.getRow())
         {
            String line = getSession().getLine(newEnd.getRow());
            if (line.substring(0, newEnd.getColumn()).trim().length() != 0)
               break;

            int prevRow = newEnd.getRow() - 1;
            int len = getSession().getLine(prevRow).length();
            newEnd = Position.create(prevRow, len);
         }
      }

      getSession().getSelection().setSelectionRange(
            Range.fromPoints(newStart, newEnd));
   }

   public int getSelectionOffset(boolean start)
   {
      Range range = getSession().getSelection().getRange();
      if (start)
         return range.getStart().getColumn();
      else
         return range.getEnd().getColumn();
   }

   public void onActivate()
   {
      Scheduler.get().scheduleFinally(new RepeatingCommand()
      {
         public boolean execute()
         {
            widget_.onResize();
            widget_.onActivate();

            return false;
         }
      });
   }

   public void onVisibilityChanged(boolean visible)
   {
      if (visible)
         widget_.getEditor().getRenderer().updateFontSize();
   }

   public void setHighlightSelectedLine(boolean on)
   {
      widget_.getEditor().setHighlightActiveLine(on);
   }

   public void setShowLineNumbers(boolean on)
   {
      widget_.getEditor().getRenderer().setShowGutter(on);
   }

   public void setUseSoftTabs(boolean on)
   {
      getSession().setUseSoftTabs(on);
   }

   public void setTabSize(int tabSize)
   {
      getSession().setTabSize(tabSize);
   }

   public void setShowPrintMargin(boolean on)
   {
      widget_.getEditor().getRenderer().setShowPrintMargin(on);
   }

   public void setPadding(int padding)
   {
      widget_.getEditor().getRenderer().setPadding(padding);
   }

   public void setPrintMarginColumn(int column)
   {
      widget_.getEditor().getRenderer().setPrintMarginColumn(column);
   }

   public HandlerRegistration addCursorChangedHandler(final CursorChangedHandler handler)
   {
      return widget_.addCursorChangedHandler(handler);
   }

   public FunctionStart getCurrentFunction()
   {
      return getSession().getMode().getCurrentFunction(getCursorPosition());
   }

   public Position getCursorPosition()
   {
      return getSession().getSelection().getCursor();
   }

   public void setCursorPosition(Position position)
   {
      getSession().getSelection().setSelectionRange(
            Range.fromPoints(position, position));
   }

   public void moveCursorNearTop()
   {
      int screenRow = getSession().documentToScreenRow(getCursorPosition());
      widget_.getEditor().scrollToRow(Math.max(0, screenRow - 4));
   }

   public JsArray<FunctionStart> getFunctionTree()
   {
      return getSession().getMode().getFunctionTree();
   }

   public void setFontSize(double size)
   {
      // No change needed--the AceEditorWidget uses the "normalSize" style
      // However, we do need to resize the gutter
      widget_.getEditor().getRenderer().updateFontSize();
      widget_.forceResize();
   }

   public HandlerRegistration addValueChangeHandler(
         ValueChangeHandler<Void> handler)
   {
      return handlers_.addHandler(ValueChangeEvent.getType(), handler);
   }

   public HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler)
   {
      return widget_.addCapturingKeyDownHandler(handler);
   }

   public HandlerRegistration addCapturingKeyPressHandler(KeyPressHandler handler)
   {
      return widget_.addCapturingKeyPressHandler(handler);
   }

   public HandlerRegistration addCapturingKeyUpHandler(KeyUpHandler handler)
   {
      return widget_.addCapturingKeyUpHandler(handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public Widget toWidget()
   {
      return widget_;
   }

   public EditSession getSession()
   {
      return widget_.getEditor().getSession();
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return widget_.addBlurHandler(handler);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return widget_.addClickHandler(handler);
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return widget_.addFocusHandler(handler);
   }

   public Widget getWidget()
   {
      return widget_;
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return widget_.addKeyDownHandler(handler);
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return widget_.addKeyPressHandler(handler);
   }

   public void autoHeight()
   {
      widget_.autoHeight();
   }

   public void scrollToCursor(ScrollPanel scrollPanel, int padding)
   {
      DomUtils.ensureVisibleVert(
            scrollPanel.getElement(),
            widget_.getEditor().getRenderer().getCursorElement(),
            padding);
   }

   private final HandlerManager handlers_ = new HandlerManager(this);
   private final AceEditorWidget widget_;
   private CompletionManager completionManager_;
   private CodeToolsServerOperations server_;
   private TextFileType fileType_;

   private static final ExternalJavaScriptLoader aceLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.acejs().getUrl());
   private static final ExternalJavaScriptLoader aceSupportLoader_ =
         new ExternalJavaScriptLoader(AceResources.INSTANCE.acesupportjs().getUrl());
}
