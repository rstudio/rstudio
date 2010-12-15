/*
 * REditor.java
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
package org.rstudio.studio.client.common.reditor;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.event.dom.client.KeyCodeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import org.rstudio.codemirror.client.CodeMirror.CursorPosition;
import org.rstudio.codemirror.client.CodeMirror.LineHandle;
import org.rstudio.codemirror.client.CodeMirrorConfig;
import org.rstudio.codemirror.client.CodeMirrorEditor;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.events.NativeKeyDownHandler;
import org.rstudio.core.client.events.NativeKeyPressEvent;
import org.rstudio.core.client.events.NativeKeyPressHandler;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.reditor.model.REditorServerOperations;
import org.rstudio.studio.client.common.reditor.resources.REditorResources;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.views.console.shell.BraceHighlighter;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager.InitCompletionFilter;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.NullCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;
import org.rstudio.studio.client.workbench.views.source.codemirror.CodeMirrorToInputEditorDisplayAdapter;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileHandler;

public class REditor extends CodeMirrorEditor
   implements HasValueChangeHandlers<Void>
{
   public static Size measureText(String text)
   {
      Size textSize = DomMetrics.measureHTML(
                              "<pre>" + text + "</pre>",
                               REditorResources.INSTANCE.styles().editbox());
      
      return textSize;
   }
   
   public class REditorChangeTracker implements ChangeTracker
   {
      public REditorChangeTracker()
      {
         thisVersion_ = version_;
      }

      public boolean hasChanged()
      {
         return version_ != thisVersion_;
      }

      public void reset()
      {
         thisVersion_ = version_;
      }

      public ChangeTracker fork()
      {
         REditorChangeTracker clone = new REditorChangeTracker();
         clone.thisVersion_ = thisVersion_;
         return clone;
      }

      private int thisVersion_;
   }

   @Inject
   public REditor(REditorServerOperations server)
   {
      server_ = server ;
      setStylePrimaryName("rCodeEditor");
   }

   /**
    * Turns line numbers on or off. Defaults to off. MUST BE SET
    * BEFORE LOAD. CodeMirror lets you change this dynamically but
    * this class doesn't support that (cause we don't need it right
    * now).
    */
   public void setLineNumbers(boolean on)
   {
      lineNumbers_ = on;
   }

   public EditorLanguage getLanguage()
   {
      return language_;
   }
   
   public void setLanguage(EditorLanguage language)
   {
      language_ = language;
      if (editorLoaded_)
      {
         codeMirror_.setParserByName(language.getParserName());
         updateCompletionManager();
      }
   }

   public HandlerRegistration addSaveFileHandler(SaveFileHandler handler)
   {
      return addHandler(handler, SaveFileEvent.TYPE);
   }

   public ChangeTracker getChangeTracker()
   {
      return new REditorChangeTracker();
   }
   
   @Override
   protected void configure(CodeMirrorConfig config)
   {
      config.setParserfiles(language_.getAllParserUrlsWithThisOneLast());
      config.setStylesheetPath(EditorLanguage.STYLES_URL);
      config.setTextWrapping(false);
      config.setTabMode("shift");
      config.setLineNumbers(lineNumbers_);
      
      config.setPassDelay(50);
      config.setPassTime(25);
   }

   @Override
   protected void onChange()
   {
      version_++;
      super.onChange();
      ValueChangeEvent.fire(this, null);
   }

   @Override
   protected void onSave()
   {
      this.fireEvent(new SaveFileEvent());
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      if (completionManager_ != null)
         completionManager_.close();
   }

   @Override
   protected void onEditorLoaded()
   {
      super.onEditorLoaded() ;

      if (Desktop.isDesktop())
      {
         Document editorDoc = codeMirror_.getWin().getDocument();
         StyleElement styleEl = editorDoc.createStyleElement();
         styleEl.setInnerText(".editbox {" +
                              "font-family: " + ThemeFonts.getFixedWidthFont() +
                              " !important}");
         editorDoc.getElementsByTagName("head").getItem(0).appendChild(styleEl);
      }

      inputEditorDisplay_ = new CodeMirrorToInputEditorDisplayAdapter(this);
      updateCompletionManager();
      braceHighlighter_ = new BraceHighlighter<Node>(inputEditorDisplay_, 300);

      addNativeKeyDownHandler(new NativeKeyDownHandler()
      {
         public void onKeyDown(NativeKeyDownEvent evt)
         {
            if (evt.isCanceled())
               return;

            ShortcutManager.INSTANCE.onKeyDown(evt);
            if (evt.isCanceled())
               return;

            FakeKeyCodeEvent event = new FakeKeyCodeEvent(evt.getEvent());
            if (completionManager_.previewKeyDown(event))
            {
               evt.cancel();
               return;
            }

            boolean handled = false;
            if (!event.isAltKeyDown()
                && !event.isShiftKeyDown()
                && !event.isMetaKeyDown()
                && event.isControlKeyDown())
            {
               switch (event.getNativeKeyCode())
               {
                  case 'U':
                     handled = true;
                     InputEditorUtil.yankBeforeCursor(inputEditorDisplay_,
                                                      true);
                     break;
                  case 'K':
                     handled = true;
                     InputEditorUtil.yankAfterCursor(inputEditorDisplay_,
                                                     true);
                     break;
                  case 'Y':
                     handled = true;
                     InputEditorUtil.pasteYanked(inputEditorDisplay_);
                     break;
               }
            }

            if (handled)
               evt.cancel();
         }
      });
      addNativeKeyPressHandler(new NativeKeyPressHandler()
      {
         public void onKeyPress(NativeKeyPressEvent evt)
         {
            if (evt.isCanceled())
               return;
            if (completionManager_.previewKeyPress(evt.getCharCode()))
            {
               evt.cancel();
               return;
            }
         }
      });
   }

   public void fitSelectionToLines(boolean expand)
   {
      CursorPosition start = codeMirror_.cursorPosition(true);
      CursorPosition newStart = start;
      if (start.getCharacter() > 0)
      {
         if (expand)
         {
            newStart = CursorPosition.create(start.getLine(), 0);
         }
         else
         {
            String firstLine = codeMirror_.lineContent(start.getLine());
            if (firstLine.substring(0, start.getCharacter()).trim().length() == 0)
               newStart = CursorPosition.create(start.getLine(), 0);
         }
      }

      CursorPosition end = codeMirror_.cursorPosition(false);
      CursorPosition newEnd = end;
      if (expand)
      {
         LineHandle endLine = end.getLine();
         if (endLine == newStart.getLine() || end.getCharacter() > 0)
         {
            // If selection ends at the start of a line, keep the selection
            // there--unless that means less than one line will be selected
            // in total.
            newEnd = CursorPosition.create(
                  endLine, codeMirror_.lineContent(endLine).length());
         }
      }
      else
      {
         while (newEnd.getLine() != newStart.getLine())
         {
            String line = codeMirror_.lineContent(newEnd.getLine());
            if (line.substring(0, newEnd.getCharacter()).trim().length() != 0)
               break;

            LineHandle prevLine = codeMirror_.prevLine(newEnd.getLine());
            int len = codeMirror_.lineContent(prevLine).length();
            newEnd = CursorPosition.create(prevLine, len);
         }
      }

      codeMirror_.selectLines(newStart.getLine(), newStart.getCharacter(),
                              newEnd.getLine(), newEnd.getCharacter());
   }

   public Object getSelectionLine(boolean start)
   {
      return codeMirror_.cursorPosition(start).getLine();
   }

   public int getSelectionOffset(boolean start)
   {
      return codeMirror_.cursorPosition(start).getCharacter();
   }

   private void updateCompletionManager()
   {
      if (language_.useRCompletion())
      {
         completionManager_ = new RCompletionManager(
               inputEditorDisplay_,
               new CompletionPopupPanel(),
               server_,
               new Filter());
      }
      else
         completionManager_ = new NullCompletionManager();
   }

   /**
    * jcheng 2010-03-09: This exists merely to let me use the nice accessors on
    * KeyCodeEvent that decode NativeEvent info. Previously I would use
    * DomEvent.fireNativeEvent but that causes assertions sometimes under
    * OOPHM. 
    */
   private class FakeKeyCodeEvent extends KeyCodeEvent<EventHandler>
   {
      public FakeKeyCodeEvent(NativeEvent nativeEvent)
      {
         super();
         setNativeEvent(nativeEvent);
      }

      @Override
      public Type<EventHandler> getAssociatedType()
      {
         assert false;
         return null;
      }

      @Override
      protected void dispatch(EventHandler handler)
      {
         assert false;
      }
   }
   
   private class Filter implements InitCompletionFilter
   {
      public boolean shouldComplete(KeyCodeEvent<?> event)
      {
         if (event.getNativeKeyCode() != KeyCodes.KEY_TAB)
            return true ;
         
         // When hitting Tab, only do completion if we're not at
         // the start of a line
         
         CursorPosition pos = codeMirror_.cursorPosition(true) ;
         if (pos == null)
            return false ;
         String lineContent = codeMirror_.lineContent(pos.getLine()) ;
         String content = lineContent.substring(0, pos.getCharacter()) ;
         return content.trim().length() != 0 ;
      }
   }

   public HandlerRegistration addValueChangeHandler(
         ValueChangeHandler<Void> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   public void fireValueChanged()
   {
      ValueChangeEvent.fire(this, null);
   }

   private int version_ = (int) Math.random();
   private final REditorServerOperations server_ ;
   private CodeMirrorToInputEditorDisplayAdapter inputEditorDisplay_;
   @SuppressWarnings("unused")
   private BraceHighlighter<Node> braceHighlighter_;
   private boolean lineNumbers_;
   private EditorLanguage language_ = EditorLanguage.LANG_PLAIN;
   private CompletionManager completionManager_;
}