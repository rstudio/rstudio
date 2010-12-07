/*
 * PlainTextEditor.java
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
package org.rstudio.studio.client.workbench.views.console.shell.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.inject.Inject;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.NativeWindow;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.events.NativeKeyDownHandler;
import org.rstudio.core.client.events.NativeKeyPressEvent;
import org.rstudio.core.client.events.NativeKeyPressHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.r.RStringToken;
import org.rstudio.studio.client.common.r.RToken;
import org.rstudio.studio.client.common.r.RTokenRange;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.views.console.shell.BraceHighlighter;
import org.rstudio.studio.client.workbench.views.console.shell.BraceHighlighter.BraceHighlighterDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.RTokenizerBraceMatcher;
import org.rstudio.studio.client.workbench.views.console.shell.impl.PlainTextEditorImpl;

public class PlainTextEditor
      extends FocusWidget
      implements InputEditorDisplay, BraceHighlighterDisplay<RToken>
{
   private class PTEPosition extends InputEditorPosition
   {
      public PTEPosition(int position)
      {
         super(null, position);
      }

      @Override
      protected int compareLineTo(Object other)
      {
         return 0;
      }

      @Override
      public InputEditorPosition movePosition(int position, boolean relative)
      {
         return new PTEPosition(relative ? getPosition() + position : position);
      }

      @Override
      public int getLineLength()
      {
         return PlainTextEditor.this.getTextLength();
      }
   }

   @Inject
   public PlainTextEditor(EventBus events,
                          Server server)
   {
      super(DOM.createDiv());
      server_ = server;
      addKeyPressHandler(new KeyPressHandler() {
         public void onKeyPress(KeyPressEvent event)
         {
            event.stopPropagation();
            NativeKeyPressEvent.fire(event.getNativeEvent(),
                                     PlainTextEditor.this);

            impl.poll();
         }
      });

      addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            event.stopPropagation();
            NativeKeyDownEvent.fire(event.getNativeEvent(),
                                    PlainTextEditor.this);
            impl.poll();
         }
      });

      // BraceHighlighter causes browser crashes in IE8 in hosted mode.
      // Update 5/3/2010: Also screws up cursor movement in IE8 script mode.
      if (//GWT.isScript() ||
          !BrowseCap.INSTANCE.suppressConsoleBraceHighlightingInHostedMode())
      {
         new BraceHighlighter<RToken>(this, 20) ;
      }
      
      events.addHandler(SessionInitEvent.TYPE, new SessionInitHandler() {
         public void onSessionInit(SessionInitEvent sie)
         {
            DeferredCommand.addCommand(new Command() {
               public void execute()
               {
                  forceFocus_Hack() ;
               }
            }) ;
         }
      }) ;
   }

   public boolean isSelectionCollapsed()
   {
      return DomUtils.isSelectionCollapsed() ;
   }

   @Override
   public void setFocus(boolean focused)
   {
      NativeWindow.get().focus();
      textContainer_.focus() ;
   }

   /**
    * Deal with Firefox bug that stops the cursor from appearing
    * if you quit the workbench and then start a new session (in 
    * the same browser window).
    */
   private void forceFocus_Hack()
   {
      try
      {
         if (getText().length() > 0)
            return ;
         
         setText("\u200B") ;
         beginSetSelection(new InputEditorSelection(
               new PTEPosition(0),
               new PTEPosition(1)), new Command() {
            public void execute()
            {
               setText("") ;
               NativeWindow.get().focus();
               setFocus(true);
            }
         }) ;
      }
      catch (Exception e)
      {
         Debug.log(e.toString()) ;
      }
   }

   public void setText(String text)
   {
      boolean focused = DomUtils.hasFocus(textContainer_);
      normalize() ;

      DomUtils.setInnerText(textContainer_, text);

      impl.poll();
      if (focused)
      {
         DeferredCommand.addCommand(new Command() {
            public void execute()
            {
               DomUtils.focus(textContainer_, true);
               DomUtils.collapseSelection(false);
            }
         });
      }
   }

   public String getText()
   {
      // Remove non-breaking space
      String innerText = DomUtils.getInnerText(textContainer_);
      return innerText
            .replace('\u00A0', ' ')
            .replace("\u200B", "");
   }

   public RToken[] getTokensToHighlight(InputEditorSelection selection,
                                        boolean forward)
   {
      String text = getText() ;
      int index = selection.getStart().getPosition();
      RTokenRange range = new RTokenRange(text) ;

      // Find the starting token
      RToken orig = null ;
      while (orig == null && range.next() != null
            && range.currentToken().getOffset() <= index)
      {
         RToken t = range.currentToken() ;
         if ((forward && t.getOffset() == index)
               || (!forward && t.getOffset() + t.getLength() == index))
            orig = t ;
      }

      if (orig == null)
         return null;

      if (orig.getTokenType() == RToken.STRING)
      {
         if (((RStringToken)orig).isWellFormed())
            return new RToken[] {orig};
      }

      RTokenizerBraceMatcher matcher = RTokenizerBraceMatcher.createForToken(
            orig.getTokenType());
      if (matcher == null)
         return null;

      RToken matched = matcher.match(range);
      if (matched == null)
         return null;

      return new RToken[] {orig, matched};
   }

   /**
    * Insert a span around the specified range. If the range spans
    * more than one text node, you'll probably get an exception or something.
    */
   public Object attachStyle(RToken token, String style)
   {
      // Splitting text can cause the selection to move to the wrong
      // place, so save it now and we'll restore it at the end.
      InputEditorSelection origSelection = getSelection() ;

      normalize() ;

      Text newText = DomUtils.splitTextNodeAt(textContainer_, token.getOffset());
      DomUtils.splitTextNodeAt(textContainer_,
                               token.getOffset() + token.getLength());

      final SpanElement span = textContainer_.getOwnerDocument().createSpanElement() ;
      span.setInnerText(newText.getNodeValue()) ;
      newText.getParentElement().insertBefore(span, newText) ;
      newText.getParentElement().removeChild(newText) ;

      // Restore original selection
      if (origSelection != null)
         beginSetSelection(origSelection, null) ;

      span.setClassName(style);
      return new Command()
      {
         public void execute()
         {
            stripElement(span);
         }
      };
   }

   public void unattachStyle(Object cookie)
   {
      ((Command)cookie).execute();
   }

   private void stripElement(Element el)
   {
      // Special case where the el was removed from the document already
      if (el.getParentElement() == null)
         return ;

      if (!DomUtils.isDescendant(el, textContainer_))
         throw new IllegalArgumentException(
               "The specified element does not belong to this editor") ;

      InputEditorSelection sel = getSelection() ;

      for (Node child = el.getFirstChild();
           child != null;
           child = child.getNextSibling())
      {
         el.getParentElement().insertBefore(child.cloneNode(true), el);
      }
      el.getParentElement().removeChild(el) ;

      normalize() ;

      if (sel != null)
         beginSetSelection(sel, null) ;
   }


   public int getTextLength()
   {
      return getText().length();
   }
   
   public InputEditorSelection getSelection()
   {
      normalize() ;

      int[] offsets = DomUtils.getSelectionOffsets(textContainer_);
      if (offsets == null)
         return null;

      return new InputEditorSelection(new PTEPosition(offsets[0]),
                                      new PTEPosition(offsets[1]));
   }
   
   public void beginSetSelection(InputEditorSelection selection, Command callback)
   {
      normalize() ;
      
      int start = selection.getStart().getPosition();
      int end = selection.getEnd().getPosition();
      DomUtils.setSelectionOffsets(textContainer_, start, end);

      if (callback != null)
         callback.execute();
   }
   
   public boolean hasSelection()
   {
      return DomUtils.isSelectionInElement(textContainer_);
   }

   public String replaceSelection(String text, final boolean collapseSelection)
   {
      normalize() ;
      if (!hasSelection())
         throw new IllegalStateException("Selection not active") ;

      String orig = DomUtils.replaceSelection(textContainer_.getOwnerDocument(),
                                              text);
      if (collapseSelection)
         collapseSelection(false);
      return orig;
   }

   public void clear()
   {
      setText("");
   }

   public void collapseSelection(boolean collapseToStart)
   {
      DomUtils.collapseSelection(collapseToStart);
   }

   public InputEditorSelection getStart()
   {
      return new InputEditorSelection(
            new PTEPosition(0),
            new PTEPosition(0));
   }

   public InputEditorSelection getEnd()
   {
      int textLength = getTextLength();
      return new InputEditorSelection(
            new PTEPosition(textLength),
            new PTEPosition(textLength));
   }

   public Rectangle getCursorBounds()
   {
      normalize() ;
      if (!hasSelection())
         return null;
      
      return DomUtils.getCursorBounds() ;
   }

   public Rectangle getBounds()
   {
      return new Rectangle(getAbsoluteLeft(), getAbsoluteTop(),
                           getOffsetWidth(), getOffsetHeight());
   }

   @Override
   protected void onLoad()
   {
      super.onLoad() ;
      
      textContainer_ = impl.setupTextContainer(getElement()) ;
      
      addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            if (!event.getNativeEvent().getEventTarget().equals(textContainer_))
            {
               textContainer_.focus() ;
               event.preventDefault() ;
            }
         }
      }) ;

      pasteStrategy_.initialize(this, textContainer_);
   }

   private void normalize()
   {
      textContainer_.normalize() ;
      if (textContainer_.getFirstChild() == null)
      {
         textContainer_.appendChild(
               textContainer_.getOwnerDocument().createTextNode("")) ;
      }
   }

   public HandlerRegistration addNativeKeyDownHandler(NativeKeyDownHandler handler)
   {
      return addHandler(handler, NativeKeyDownEvent.TYPE);
   }

   public HandlerRegistration addNativeKeyPressHandler(NativeKeyPressHandler handler)
   {
      return addHandler(handler, NativeKeyPressEvent.TYPE);
   }

   public Element getKeyEventTarget()
   {
      return textContainer_;
   }

   private final PasteStrategy pasteStrategy_ = GWT.create(PasteStrategy.class);
   private final PlainTextEditorImpl impl = GWT.create(PlainTextEditorImpl.class);
   private ElementEx textContainer_;
   @SuppressWarnings("unused")
   private final Server server_;
}
