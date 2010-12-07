/*
 * BraceHighlighter.java
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
package org.rstudio.studio.client.workbench.views.console.shell ;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Timer;
import org.rstudio.core.client.events.*;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;

import java.util.ArrayList;

/**
 * Adds highlighting of braces, brackets, and parens to PlainTextEditor.
 */
public class BraceHighlighter<T>
{
   public interface BraceHighlighterDisplay<T> extends HasNativeKeyHandlers,
                                                       HasClickHandlers
   {
      public InputEditorSelection getSelection() ;

      public String getText() ;

      public T[] getTokensToHighlight(InputEditorSelection selection,
                                      boolean forward);
      
      public Object attachStyle(T token, String style);
      public void unattachStyle(Object cookie);
   }

   public BraceHighlighter(BraceHighlighterDisplay<T> editor, int delayMillis)
   {
      styles_ = ConsoleResources.INSTANCE.consoleStyles();
      editor_ = editor ;
      delayMillis_ = delayMillis;

      editor.addNativeKeyDownHandler(new NativeKeyDownHandler()
      {
         public void onKeyDown(NativeKeyDownEvent event)
         {
            // quickly dismiss any existing highlight
            clearHighlights() ;

            switch (event.getEvent().getKeyCode())
            {
            case KeyCodes.KEY_LEFT:
            case KeyCodes.KEY_HOME:
            case KeyCodes.KEY_DELETE:
               delayedMatch(true) ;
               return ;
            case KeyCodes.KEY_RIGHT:
            case KeyCodes.KEY_BACKSPACE:
            case KeyCodes.KEY_END:
               delayedMatch(false) ;
               return ;
            }
         }
      }) ;

      editor.addNativeKeyPressHandler(new NativeKeyPressHandler()
      {
         public void onKeyPress(NativeKeyPressEvent event)
         {
            //To change body of implemented methods use File | Settings | File Templates.
            // quickly dismiss any existing highlight
            switch (event.getCharCode())
            {
            case '(':
            case ')':
            case '{':
            case '}':
            case '[':
            case ']':
            case '\'':
            case '"':
               clearHighlights() ;
               delayedMatch(false) ;
               break ;
            }
         }
      }) ;

      editor.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            clearHighlights() ;
            delayedMatch(false) ;
         }
      }) ;
   }

   /**
    * Do matching in a little while--after the current keypress has been
    * processed (hopefully).
    * 
    * @param forward
    *           If true, the character to match against is the one immediately
    *           AFTER the selection caret. If false, the character to match
    *           against is the one immediately BEFORE.
    */
   public void delayedMatch(final boolean forward)
   {
      assert pendingHighlight_ == null : "pendingHighlight_ wasn't null--forgot a call to clearHighlights()?" ;
      assert pendingUnhighlight_ == null : "pendingUnhighlight_ wasn't null--forgot a call to clearHighlights()?" ;

      pendingHighlight_ = new Timer() {
         @Override
         public void run()
         {
            InputEditorSelection sel = editor_.getSelection() ;
            if (sel != null && sel.isEmpty())
               match(sel, forward);
         }
      } ;
      pendingHighlight_.schedule(delayMillis_) ;
   }

   /**
    * Do the actual match/highlight.
    */
   private void match(InputEditorSelection selection, boolean forward)
   {
      T[] tokens = editor_.getTokensToHighlight(selection, forward);
      if (tokens != null)
         highlightRanges(tokens);
   }
   
   private void highlightRanges(T[] ranges)
   {
      if (ranges.length == 0)
         return ;
      
      final ArrayList<Object> cookies = new ArrayList<Object>();
      
      for (T range : ranges)
      {
         assert range != null : "Highlight range was null";
         cookies.add(editor_.attachStyle(range, "highlight"));
      }

      pendingUnhighlight_ = new Timer() {
         @Override
         public void run()
         {
            pendingUnhighlight_ = null ;
            for (Object cookie : cookies)
               editor_.unattachStyle(cookie); ;
         }
      } ;
      pendingUnhighlight_.schedule(HIGHLIGHT_MILLIS) ;
   }

   /**
    * Stop any pending highlight operation, and remove any existing highlight.
    */
   public void clearHighlights()
   {
      if (pendingHighlight_ != null)
      {
         pendingHighlight_.cancel() ;
         pendingHighlight_ = null ;
      }

      if (pendingUnhighlight_ != null)
      {
         pendingUnhighlight_.cancel() ;
         pendingUnhighlight_.run() ;
         pendingUnhighlight_ = null ;
      }
   }

   private final static int HIGHLIGHT_MILLIS = 350 ;

   private final int delayMillis_ ;
   private final BraceHighlighterDisplay<T> editor_ ;
   private Timer pendingHighlight_ ;
   private Timer pendingUnhighlight_ ;
   @SuppressWarnings("unused")
   private final ConsoleResources.ConsoleStyles styles_;
}
