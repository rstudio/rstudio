/*
 * CodeMirrorEditor.java
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
package org.rstudio.codemirror.client;

import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import org.rstudio.codemirror.client.CodeMirror.CursorPosition;
import org.rstudio.codemirror.client.CodeMirror.LineHandle;
import org.rstudio.codemirror.client.events.EditorFocusedEvent;
import org.rstudio.codemirror.client.events.EditorFocusedHandler;
import org.rstudio.codemirror.client.resources.CodeMirrorResources;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.NativeWindow;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.*;

import java.util.ArrayList;

public abstract class CodeMirrorEditor extends Composite
                                       implements HasNativeKeyHandlers,
                                                  RequiresResize
{
   public CodeMirrorEditor()
   {
      // create parent element for code mirror
      codeMirrorPanel_ = new SimplePanel();
      initWidget(codeMirrorPanel_);
      getElement().getStyle().setPosition(Position.RELATIVE);
   }

   public CodeMirror getRawEditor()
   {
      return codeMirror_ ;
   }

   public String getCode()
   {
      if (!editorLoaded_)
         throw new IllegalStateException("Editor has not finished loading");
      return codeMirror_.getCode();
   }

   public void setCode(String code)
   {
      if (code == null)
         code = "";
      final String finalCode = code;
      executeOrDefer(new Command() {
         public void execute()
         {
            suspendChangeNotification_ = true;
            try
            {
               codeMirror_.setCode(finalCode);
               if (finalCode.length() == 0
                   && BrowseCap.INSTANCE.mozEmptyContentEditableHack())
               {
                  // Prevents empty documents from showing the cursor on the
                  // right in Firefox.
                  codeMirror_.getWin().getDocument().getBody().setInnerHTML(
                        "<br _moz_editor_bogus_node=\"TRUE\" _moz_dirty=\"\"/>");
               }
            }
            finally
            {
               suspendChangeNotification_ = false;
            }
         }
      });
   }

   public void insertCode(final String code1, final boolean blockMode)
   {
      executeOrDefer(new Command()
      {
         public void execute()
         {
            String code = code1;
            
            boolean insertLeadingBreak = false;
            CursorPosition start = codeMirror_.cursorPosition(true);

            if (blockMode)
            {
               // Would it be possible to do proper indentation?

               // Insert an extra \n at the beginning if there is non-whitespace
               // content before the cursor
               if (start != null)
               {
                  String startLine = codeMirror_.lineContent(start.getLine());
                  if (startLine != null
                      && startLine.length() >= start.getCharacter()
                      && !startLine.substring(0,
                                              start.getCharacter()).trim().equals(""))
                  {
                     code = "\n" + code;
                     insertLeadingBreak = true;
                  }
               }

               // If there's no non-whitespace content after the cursor, then
               // strip off the \n at the end of our code chunk, if any
               CursorPosition end = codeMirror_.cursorPosition(false);
               if (end != null)
               {
                  String endLine = codeMirror_.lineContent(end.getLine());
                  if (endLine != null
                      && endLine.length() >= end.getCharacter()
                      && endLine.substring(end.getCharacter()).trim().equals(""))
                  {
                     code = StringUtil.chomp(code);
                  }
               }
            }

            codeMirror_.replaceSelection(code);

            if (start != null)
            {
               if (insertLeadingBreak)
               {
                  int lineNum = codeMirror_.lineNumber(start.getLine()) + 1;
                  start = CursorPosition.create(codeMirror_.nthLine(lineNum), 0);
               }
               else
               {
                  start = CursorPosition.create(start.getLine(), 0);
               }

               final CursorPosition newEnd = codeMirror_.cursorPosition(false);
               codeMirror_.selectLines(start.getLine(),
                                       start.getCharacter(),
                                       newEnd.getLine(),
                                       newEnd.getCharacter());
               new Timer() {
                  @Override
                  public void run()
                  {
                     codeMirror_.selectLine(newEnd.getLine(), newEnd.getCharacter());
                  }
               }.schedule(750);
            }
         }
      });
   }

   public void setTextWrapping(final boolean wrapping)
   {
      executeOrDefer(new Command()
      {
         public void execute()
         {
            codeMirror_.setTextWrapping(wrapping);
         }
      });
   }

   public void focus()
   {
      executeOrDefer(new Command() {
         public void execute()
         {
            codeMirror_.getWin().focus();
            codeMirror_.focus() ;
         }
      });
   }
   
   public void print()
   {
      codeMirror_.print();
   }
   
   public String getSelection()
   {
      return codeMirror_.selection();
   }

   public String getCurrentLine()
   {
      CodeMirror.CursorPosition pos = codeMirror_.cursorPosition(true);
      if (pos == null)
         return null;

      return codeMirror_.lineContent(pos.getLine());
   }

   public HandlerRegistration addNativeKeyDownHandler(
         NativeKeyDownHandler handler)
   {
      return addHandler(handler, NativeKeyDownEvent.TYPE);
   }

   public HandlerRegistration addNativeKeyPressHandler(
         NativeKeyPressHandler handler)
   {
      return addHandler(handler, NativeKeyPressEvent.TYPE);
   }

   public HandlerRegistration addEditorClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   @Override
   protected final void onLoad()
   {
      CodeMirrorConfig config = CodeMirrorConfig.create();
      
      configure(config);
      
      // defer full initialization until the element is loaded
      setInitCallback(config, new CodeMirrorCallback() {
         public void execute(CodeMirror cm)
         {
            assert codeMirror_ == null || cm == codeMirror_ ;
            codeMirror_ = cm ;
            onEditorLoaded() ;
         }
      }) ;
      
      setCursorActivityCallback(config, new Command() {
         public void execute()
         {
            onCursorActivity();
         }
      }) ;

      setOnChangeCallback(config, new Command() {
         public void execute()
         {
            if (suspendChangeNotification_)
               return;
            onChange();
         }
      }) ;

      setSaveCallback(config, new Command()
      {
         public void execute()
         {
            onSave();
         }
      });

      codeMirror_ = createCodeMirror(codeMirrorPanel_.getElement(), config);
      fillParent(codeMirror_.getWrapping().getStyle());
   }

   private void fillParent(Style style)
   {
      style.setPosition(Style.Position.ABSOLUTE);
      style.setLeft(0, Style.Unit.PX);
      style.setRight(0, Style.Unit.PX);
      style.setTop(0, Style.Unit.PX);
      style.setBottom(0, Style.Unit.PX);
      style.setProperty("width", null);
      style.setProperty("height", null);
   }

   /**
    * Subclasses should override to customize the config object.
    */
   protected abstract void configure(CodeMirrorConfig config);

   protected void onEditorLoaded()
   {
      editorLoaded_ = true ;

      codeMirror_.setKeyDownHandler(new CodeMirrorKeyHandler()
      {
         public boolean handleKey(NativeEvent event)
         {
            return NativeKeyDownEvent.fire(event, CodeMirrorEditor.this);
         }
      });

      codeMirror_.setKeyPressHandler(new CodeMirrorKeyHandler()
      {
         public boolean handleKey(NativeEvent event)
         {
            return NativeKeyPressEvent.fire(event, CodeMirrorEditor.this);
         }
      });

      addClickCallback(codeMirror_, new CommandWithNativeEvent()
      {
         public void execute(NativeEvent event)
         {
            NativeEvent e = Document.get().createClickEvent(1,
                                                            event.getScreenX(),
                                                            event.getScreenY(),
                                                            event.getClientX(),
                                                            event.getClientY(),
                                                            event.getCtrlKey(),
                                                            event.getAltKey(),
                                                            event.getShiftKey(),
                                                            event.getMetaKey());
            getElement().dispatchEvent(e);
         }
      });

      addFocusCallback(codeMirror_, new CommandWithNativeEvent()
      {
         public void execute(NativeEvent event)
         {
            fireEvent(new EditorFocusedEvent());
         }
      });

      for (Command c : deferredCommands_)
         c.execute();
      deferredCommands_.clear();
   }

   public HandlerRegistration addEditorFocusedHandler(EditorFocusedHandler h)
   {
      return addHandler(h, EditorFocusedEvent.TYPE);
   }
   
   protected void onCursorActivity()
   {
   }

   protected void onChange()
   {
   }

   protected void onSave()
   {
   }
   
   private native CodeMirror createCodeMirror(Element element,
                                              CodeMirrorConfig config) /*-{
      return new $wnd.CodeMirror(element, config);
   }-*/;
   
   private void executeOrDefer(Command command)
   {
      if (editorLoaded_)
         command.execute();
      else
         deferredCommands_.add(command);
   }
   
   private static final native void setInitCallback(
                                          CodeMirrorConfig config, 
                                          CodeMirrorCallback handler) /*-{
      config.initCallback = function(arg) {
         handler.@org.rstudio.codemirror.client.CodeMirrorCallback::execute(Lorg/rstudio/codemirror/client/CodeMirror;)(arg);
      };
   }-*/;
   
   private static final native void setCursorActivityCallback(
                                          CodeMirrorConfig config,
                                          Command command) /*-{
      config.cursorActivity = function() {
         command.@com.google.gwt.user.client.Command::execute()();
      };
   }-*/;

   private static final native void setOnChangeCallback(
                                          CodeMirrorConfig config,
                                          Command command) /*-{
      config.onChange = function() {
         command.@com.google.gwt.user.client.Command::execute()();
      };
   }-*/;

   private static final native void setSaveCallback(
                                          CodeMirrorConfig config,
                                          Command command) /*-{
      config.saveFunction = function() {
         command.@com.google.gwt.user.client.Command::execute()();
      };
   }-*/;

   private static final native void addClickCallback(CodeMirror editor,
                                                     CommandWithNativeEvent command) /*-{
      if (editor.win.document.body.addEventListener)
      {
         editor.win.document.body.addEventListener("click", function(evt) {
            command.@org.rstudio.codemirror.client.CommandWithNativeEvent::execute(Lcom/google/gwt/dom/client/NativeEvent;)(evt);
         }, false);
      }
      else
      {
         editor.win.document.body.attachEvent("onclick", function(evt) {
            command.@org.rstudio.codemirror.client.CommandWithNativeEvent::execute(Lcom/google/gwt/dom/client/NativeEvent;)(evt);
         }, false);
      }
   }-*/;

   private static final native void addFocusCallback(CodeMirror editor,
                                                     CommandWithNativeEvent command) /*-{
      if (editor.win.addEventListener)
      {
         editor.win.addEventListener("focus", function(evt) {
            command.@org.rstudio.codemirror.client.CommandWithNativeEvent::execute(Lcom/google/gwt/dom/client/NativeEvent;)(evt);
         }, false);
      }
      else
      {
         editor.win.attachEvent("focus", function(evt) {
            command.@org.rstudio.codemirror.client.CommandWithNativeEvent::execute(Lcom/google/gwt/dom/client/NativeEvent;)(evt);
         }, false);
      }
   }-*/;

   public void moveSelectionToStart()
   {
      codeMirror_.jumpToLine(codeMirror_.firstLine());
   }

   public void moveSelectionToEnd()
   {
      codeMirror_.selectLine(codeMirror_.lastLine(),
                             codeMirror_.lineContent(
                                   codeMirror_.lastLine()).length());
   }

   public boolean moveSelectionToNextLine()
   {
      return moveSelectionToNextLine(codeMirror_);
   }

   private static native boolean moveSelectionToNextLine(CodeMirror editor) /*-{
      var pos = editor.cursorPosition(false);
      if (!pos)
         return false;

      var nextLine = editor.nextLine(pos.line);
      while (nextLine) {
         var content = editor.lineContent(nextLine);
         var offset = content.search(/[^\s]/);
         if (offset >= 0) {
            editor.selectLines(nextLine, offset);
            return true;
         }
         nextLine = editor.nextLine(nextLine);
      }

      editor.focus();
   
      return false;
   }-*/;

   public boolean find(CursorPosition pos,
                       String searchString,
                       boolean ignoreCase,
                       boolean reverse)
   {
      if (ignoreCase)
         searchString = searchString.toLowerCase();

      if (!reverse)
      {
         LineHandle lastLine = codeMirror_.lastLine();

         LineHandle line = pos.getLine();
         int offset = pos.getCharacter();

         while (true)
         {
            String lineContent = codeMirror_.lineContent(line);
            if (ignoreCase)
               lineContent = lineContent.toLowerCase();
            int index = lineContent.indexOf(searchString, offset);
            if (index >= 0)
            {
               codeMirror_.selectLines(line, index,
                                       line, index + searchString.length());
               return true;
            }

            if (line == lastLine)
               return false;

            line = codeMirror_.nextLine(line);
            offset = 0;
         }
      }
      else
      {
         LineHandle firstLine = codeMirror_.firstLine();

         LineHandle line = pos.getLine();
         int offset = pos.getCharacter() - 1;

         if (offset < 0)
         {
            if (line == firstLine)
               return false;
            line = codeMirror_.prevLine(line);
            offset = codeMirror_.lineContent(line).length();
         }

         while (true)
         {
            String lineContent = codeMirror_.lineContent(line);
            if (ignoreCase)
               lineContent = lineContent.toLowerCase();
            int index = lineContent.lastIndexOf(searchString, offset);
            if (index >= 0)
            {
               codeMirror_.selectLines(line, index,
                                       line, index + searchString.length());
               return true;
            }

            if (line == firstLine)
               return false;

            line = codeMirror_.prevLine(line);
            offset = codeMirror_.lineContent(line).length();
         }
      }
   }

   public void replaceSelection(String value)
   {
      CursorPosition start = codeMirror_.cursorPosition(true);
      codeMirror_.replaceSelection(value);
      CursorPosition end = codeMirror_.cursorPosition(false);
      codeMirror_.selectLines(start.getLine(), start.getCharacter(),
                              end.getLine(), end.getCharacter());
   }

   public void selectAll()
   {
      LineHandle lastLine = codeMirror_.lastLine();
      codeMirror_.selectLines(
            codeMirror_.firstLine(), 0,
            lastLine, codeMirror_.lineContent(lastLine).length());
   }

   public void markScrollPosition()
   {
      markedScrollPos_ = null;
      WindowEx contentWin = getWindow();
      if (contentWin != null)
         markedScrollPos_ = contentWin.getScrollPosition();
   }

   public void restoreScrollPosition()
   {
      if (markedScrollPos_ != null)
      {
         WindowEx win = getWindow();
         if (win != null)
            win.setScrollPosition(markedScrollPos_);
      }
   }

   public void onResize()
   {
      updateBodyMinHeight();
   }

   public void updateBodyMinHeight()
   {
      // Fixes bug 964, wherein clicking empty areas of a source document don't
      // put the cursor on the last line

      final int TOTAL_MARGIN = 10;

      if (codeMirror_ != null)
      {
         NativeWindow win = codeMirror_.getWin();
         if (win != null)
         {
            Document doc = win.getDocument();
            if (doc != null)
            {
               BodyElement body = doc.getBody();
               if (body != null)
               {
                  int minHeight = Math.max(0, getOffsetHeight() - TOTAL_MARGIN);
                  body.getStyle().setProperty("minHeight", minHeight + "px");
               }
            }
         }
      }
   }

   private WindowEx getWindow()
   {
      if (codeMirror_ == null)
         return null;
      IFrameElementEx frame = (IFrameElementEx) codeMirror_.getFrame();
      if (frame == null)
         return null;
      WindowEx contentWin = frame.getContentWindow();
      return contentWin;
   }

   static
   {
      // Inject codemirror.js into document
      Document doc = Document.get();
      HeadElement head = (HeadElement) doc.getElementsByTagName("head").getItem(0);
      if (head == null)
      {
         head = doc.createHeadElement();
         doc.insertBefore(head, doc.getBody());
      }
      ScriptElement script = doc.createScriptElement(
            CodeMirrorResources.INSTANCE.codemirror().getText());
      script.setType("text/javascript");
      head.appendChild(script);
   }


   private boolean suspendChangeNotification_ = false;

   protected CodeMirror codeMirror_ ;
   protected boolean editorLoaded_ ;
   private final ArrayList<Command> deferredCommands_ = new ArrayList<Command>();
   private final SimplePanel codeMirrorPanel_;
   private Point markedScrollPos_;
}
