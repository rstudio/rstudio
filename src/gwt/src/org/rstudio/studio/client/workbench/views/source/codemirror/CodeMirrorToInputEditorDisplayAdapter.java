/*
 * CodeMirrorToInputEditorDisplayAdapter.java
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
package org.rstudio.studio.client.workbench.views.source.codemirror;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import org.rstudio.codemirror.client.CodeMirror;
import org.rstudio.codemirror.client.CodeMirror.CursorPosition;
import org.rstudio.codemirror.client.CodeMirror.LineHandle;
import org.rstudio.codemirror.client.CodeMirrorEditor;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.NativeWindow;
import org.rstudio.core.client.events.NativeKeyDownHandler;
import org.rstudio.core.client.events.NativeKeyPressHandler;
import org.rstudio.studio.client.workbench.views.console.shell.BraceHighlighter;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;

import java.util.ArrayList;

public class CodeMirrorToInputEditorDisplayAdapter 
      implements InputEditorDisplay,
                 BraceHighlighter.BraceHighlighterDisplay<Node>
{
   private class CodeMirrorPosition extends InputEditorPosition
   {
      public CodeMirrorPosition(LineHandle line, int position)
      {
         super(line, position);
         lineHandle_ = line;
      }

      public CodeMirrorPosition(CursorPosition cursorPos)
      {
         this(cursorPos.getLine(), cursorPos.getCharacter());
      }

      @Override
      protected int compareLineTo(Object other)
      {
         int thisLine = editor_.lineNumber(lineHandle_);
         int otherLine = editor_.lineNumber((LineHandle) other);
         return thisLine - otherLine;
      }

      @Override
      public InputEditorPosition movePosition(int position, boolean relative)
      {
         return new CodeMirrorPosition(
               lineHandle_,
               relative ? getPosition() + position : position);
      }

      @Override
      public int getLineLength()
      {
         return editor_.lineContent(lineHandle_).length();
      }

      @Override
      public String toString()
      {
         return "Line " + editor_.lineNumber(lineHandle_) +
                " Position " + getPosition();
      }

      private final LineHandle lineHandle_;
   }

   public CodeMirrorToInputEditorDisplayAdapter(CodeMirrorEditor editor)
   {
      editor_ = editor.getRawEditor() ;
      wrapper_ = editor ;
      handlerManager_ = new HandlerManager(this) ;
   }

   public Rectangle getCursorBounds()
   {
      NativeWindow win = editor_.getWin() ;
      Rectangle bounds = DomUtils.getCursorBounds(win.getDocument()) ;
      
      return new Rectangle(
            bounds.getLeft() + wrapper_.getAbsoluteLeft() - win.getPageXOffset(),
            bounds.getTop() + wrapper_.getAbsoluteTop() - win.getPageYOffset(),
            bounds.getWidth(),
            bounds.getHeight()) ;
   }

   public Rectangle getBounds()
   {
      throw new UnsupportedOperationException("not implemented");
   }

   public InputEditorSelection getSelection()
   {
      if (!hasSelection())
         return null ;

      CursorPosition startPos = editor_.cursorPosition(true);
      CursorPosition endPos = editor_.cursorPosition(false);
      
      return new InputEditorSelection(
            new CodeMirrorPosition(startPos),
            new CodeMirrorPosition(endPos));
   }

   public String getText()
   {
      if (!hasSelection())
         return null;
      
      LineHandle currentLine = editor_.cursorPosition(true).getLine() ;
      return editor_.lineContent(currentLine) ;
   }

   public Node[] getTokensToHighlight(InputEditorSelection selection,
                                             boolean forward)
   {
      CodeMirror.SpanAndOffset spanAndOffset = editor_.getSpanAtSelection(
            (LineHandle) selection.getStart().getLine(),
            selection.getStart().getPosition(),
            forward);

      if (spanAndOffset == null)
         return null;

      SpanElement span = spanAndOffset.getSpan();
      int offset = spanAndOffset.getOffset();

      try
      {
         // Tokens contain trailing whitespace. We don't want to match
         // on this whitespace.
         int indexToCheck = offset + (forward ? 0 : -1);
         String spanText = span.getInnerText();
         if (indexToCheck >= 0 && indexToCheck < spanText.length())
         {
            char c = spanText.charAt(indexToCheck);
            if (c == ' ' || c == '\u00A0')
            {
               // Yup, it's whitespace, we're outta here.
               return null;
            }
         }
      }
      catch (Exception e)
      {
         Debug.log("Off-by-one error: HTML=" + span.getInnerHTML() +
                   ", offset=" + offset +
                   ", forward=" + forward);
      }

      SpanElement match = CodeMirrorBraceMatcher.findMatch(span);
      if (match == null)
         match = CodeMirrorStringMatcher.findMatch(span);

      if (match == null)
         return null;

      return new Node[] {span, match};
   }

   public Object attachStyle(Node[] tokens, final String style)
   {
      ArrayList<Command> cookies = new ArrayList<Command>();
      for (Node n : tokens)
         if (n != null)
            cookies.add(attachStyleInternal(n, style));
      return cookies;
   }

   public Command attachStyleInternal(Node token, final String style)
   {
      final SpanElement span = (SpanElement) token;
      span.setClassName(span.getClassName() + " " + style);
      return new Command()
      {
         public void execute()
         {
            span.setClassName(span.getClassName().replaceFirst(
                  " " + style, 
                  ""));
         }
      };
   }

   @SuppressWarnings("unchecked")
   public void unattachStyle(Object cookie)
   {
      for (Command c : (ArrayList<Command>)cookie)
         if (c != null)
            c.execute();
   }

   public boolean hasSelection()
   {
      return true;
   }

   public boolean isSelectionCollapsed()
   {
      return hasSelection() && getSelection().isEmpty() ;
   }

   public String replaceSelection(String value, boolean collapseSelection)
   {
      String original = editor_.selection();
      editor_.replaceSelection(value);
      if (collapseSelection)
      {
         CursorPosition position = editor_.cursorPosition(false);
         editor_.selectLine(position.getLine(), position.getCharacter());
      }
      return original;
   }

   public void setFocus(boolean focused)
   {
      if (focused)
         editor_.focus() ;
   }

   public void beginSetSelection(InputEditorSelection selection, final Command callback)
   {
      if (selection.isEmpty())
      {
         editor_.selectLine(
               (LineHandle) selection.getStart().getLine(),
               selection.getStart().getPosition());
      }
      else
      {
         editor_.selectLines(
               (LineHandle) selection.getStart().getLine(),
               selection.getStart().getPosition(),
               (LineHandle) selection.getEnd().getLine(),
               selection.getEnd().getPosition());
      }

      Timer timer = new Timer()
      {
         @Override
         public void run()
         {
            if (callback != null)
               callback.execute();
         }
      };
      if (DomUtils.isSelectionAsynchronous())
         timer.schedule(100);
      else
         timer.run();
   }

   public void clear()
   {
      editor_.setCode("") ;
   }

   public void collapseSelection(boolean collapseToStart)
   {
      CursorPosition pos = editor_.cursorPosition(collapseToStart);
      editor_.selectLine(pos.getLine(), pos.getCharacter());
   }

   public InputEditorSelection getStart()
   {
      return new InputEditorSelection(
         new CodeMirrorPosition(editor_.firstLine(), 0),
         new CodeMirrorPosition(editor_.firstLine(), 0));
   }

   public InputEditorSelection getEnd()
   {
      LineHandle lastLine = editor_.lastLine();
      int len = editor_.lineContent(lastLine).length();
      return new InputEditorSelection(
         new CodeMirrorPosition(lastLine, len),
         new CodeMirrorPosition(lastLine, len));
   }

   public boolean isCursorOnTopLine()
   {
      return editor_.lineNumber(editor_.cursorPosition(true).getLine()) == 1;
   }

   public boolean isCursorOnBottomLine()
   {
      return editor_.cursorPosition(false).getLine() == editor_.lastLine();
   }

   public void setText(String string)
   {
      editor_.setCode(string);
   }

   // Yeah, this doesn't actually work.
   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return handlerManager_.addHandler(FocusEvent.getType(), handler) ;
   }

   // Yeah, this doesn't actually work.
   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return handlerManager_.addHandler(BlurEvent.getType(), handler) ;
   }
   
   public void fireEvent(GwtEvent<?> event)
   {
      handlerManager_.fireEvent(event) ;
   }

   public HandlerRegistration addNativeKeyDownHandler(NativeKeyDownHandler handler)
   {
      return wrapper_.addNativeKeyDownHandler(handler);
   }

   public HandlerRegistration addNativeKeyPressHandler(NativeKeyPressHandler handler)
   {
      return wrapper_.addNativeKeyPressHandler(handler);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return wrapper_.addEditorClickHandler(handler);
   }

   private final CodeMirror editor_ ;
   private final CodeMirrorEditor wrapper_ ;
   private final HandlerManager handlerManager_ ;
}
