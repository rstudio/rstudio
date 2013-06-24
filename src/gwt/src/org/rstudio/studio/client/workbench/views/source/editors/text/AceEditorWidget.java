/*
 * AceEditorWidget.java
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

import java.util.ArrayList;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.environment.events.DebugBreakpointSetEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceClickEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceDocumentChangeEventNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceMouseEventNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FoldChangeEvent.Handler;

public class AceEditorWidget extends Composite
      implements RequiresResize,
                 HasValueChangeHandlers<Void>,
                 HasFoldChangeHandlers,
                 HasKeyDownHandlers,
                 HasKeyPressHandlers
{
   public AceEditorWidget()
   {
      initWidget(new HTML());
      FontSizer.applyNormalFontSize(this);
      setSize("100%", "100%");

      capturingHandlers_ = new HandlerManager(this);
      addEventListener(getElement(), "keydown", capturingHandlers_);
      addEventListener(getElement(), "keyup", capturingHandlers_);
      addEventListener(getElement(), "keypress", capturingHandlers_);

      addStyleName("loading");

      editor_ = AceEditorNative.createEditor(getElement());
      editor_.getRenderer().setHScrollBarAlwaysVisible(false);
      editor_.setShowPrintMargin(false);
      editor_.setPrintMarginColumn(0);
      editor_.setHighlightActiveLine(false);
      editor_.setHighlightGutterLine(false);
      editor_.delegateEventsTo(AceEditorWidget.this);
      editor_.onChange(new CommandWithArg<AceDocumentChangeEventNative>()
      {
         public void execute(AceDocumentChangeEventNative changeEvent)
         {
            ValueChangeEvent.fire(AceEditorWidget.this, null);            
            updateBreakpoints(changeEvent);
         }

      });
      editor_.onChangeFold(new Command()
      {
         @Override
         public void execute()
         {
            fireEvent(new FoldChangeEvent());
         }
      });
      editor_.onGutterMouseDown(new CommandWithArg<AceMouseEventNative>()
      {
        @Override
        public void execute(AceMouseEventNative arg)
        {
           int lineNumber = arg.getDocumentPosition().getRow();
           int breakpointIdx = breakpointLines_.indexOf(lineNumber);
           if (breakpointIdx >= 0)
           {
              editor_.getSession().clearBreakpoint(lineNumber);
              breakpointLines_.remove(breakpointIdx);
           }
           else
           {
              editor_.getSession().setBreakpoint(lineNumber);
              breakpointLines_.add(lineNumber);
              fireEvent(new BreakpointSetEvent(lineNumber));
           }
        }
      });
      editor_.getSession().getSelection().addCursorChangeHandler(new CommandWithArg<Position>()
      {
         public void execute(Position arg)
         {
            AceEditorWidget.this.fireEvent(new CursorChangedEvent(arg));
         }
      });
      AceEditorNative.addEventListener(
                  editor_,
                  "undo",
                  new CommandWithArg<Void>()
                  {
                     public void execute(Void arg)
                     {
                        fireEvent(new UndoRedoEvent(false));
                     }
                  });
      AceEditorNative.addEventListener(
                  editor_,
                  "redo",
                  new CommandWithArg<Void>()
                  {
                     public void execute(Void arg)
                     {
                        fireEvent(new UndoRedoEvent(true));
                     }
                  });
      AceEditorNative.addEventListener(
                  editor_,
                  "paste",
                  new CommandWithArg<String>()
                  {
                     public void execute(String text)
                     {
                        fireEvent(new PasteEvent(text));
                     }
                  });
      AceEditorNative.addEventListener(
                  editor_,
                  "mousedown",
                  new CommandWithArg<AceMouseEventNative>()
                  {
                     @Override
                     public void execute(AceMouseEventNative event)
                     {
                        fireEvent(new AceClickEvent(event));
                     }
                  });
   }

   public HandlerRegistration addCursorChangedHandler(
         CursorChangedHandler handler)
   {
      return addHandler(handler, CursorChangedEvent.TYPE);
   }

   @Override
   public HandlerRegistration addFoldChangeHandler(Handler handler)
   {
      return addHandler(handler, FoldChangeEvent.TYPE);
   }
   
   public HandlerRegistration addBreakpointSetHandler
      (BreakpointSetEvent.Handler handler)
   {
      return addHandler(handler, BreakpointSetEvent.TYPE);
   }

   public AceEditorNative getEditor() {
      return editor_;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      editor_.getRenderer().updateFontSize();
      onResize();
      fireEvent(new EditorLoadedEvent());

      int delayMs = initToEmptyString_ ? 100 : 500;

      // On Windows desktop sometimes we inexplicably end up at the wrong size
      // if the editor is being resized while it's loading (such as when a new
      // document is created while the source pane is hidden)
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         public boolean execute()
         {
            if (isAttached())
               onResize();
            removeStyleName("loading");
            return false;
         }
      }, delayMs);
   }

   public void onResize()
   {
      editor_.resize();
   }

   public void onActivate()
   {
      if (editor_ != null)
      {
         if (BrowseCap.INSTANCE.aceVerticalScrollBarIssue())
            editor_.getRenderer().forceScrollbarUpdate();
         editor_.getRenderer().updateFontSize();
         editor_.getRenderer().forceImmediateRender();
      }
   }

   public void setCode(String code)
   {
      code = StringUtil.notNull(code);
      initToEmptyString_ = code.length() == 0;
      editor_.getSession().setValue(code);
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Void> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return addHandler(handler, FocusEvent.getType());
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return addHandler(handler, BlurEvent.getType());
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   public HandlerRegistration addEditorLoadedHandler(EditorLoadedHandler handler)
   {
      return addHandler(handler, EditorLoadedEvent.TYPE);
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return addHandler(handler, KeyDownEvent.getType());
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return addHandler(handler, KeyPressEvent.getType());
   }

   public HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler)
   {
      return capturingHandlers_.addHandler(KeyDownEvent.getType(), handler);
   }

   public HandlerRegistration addCapturingKeyPressHandler(KeyPressHandler handler)
   {
      return capturingHandlers_.addHandler(KeyPressEvent.getType(), handler);
   }

   public HandlerRegistration addCapturingKeyUpHandler(KeyUpHandler handler)
   {
      return capturingHandlers_.addHandler(KeyUpEvent.getType(), handler);
   }

   private static native void addEventListener(Element element,
                                        String event,
                                        HasHandlers handlers) /*-{
      var listener = $entry(function(e) {
         @com.google.gwt.event.dom.client.DomEvent::fireNativeEvent(Lcom/google/gwt/dom/client/NativeEvent;Lcom/google/gwt/event/shared/HasHandlers;Lcom/google/gwt/dom/client/Element;)(e, handlers, element);
      });
      element.addEventListener(event, listener, true);

   }-*/;

   public HandlerRegistration addUndoRedoHandler(UndoRedoHandler handler)
   {
      return addHandler(handler, UndoRedoEvent.TYPE);
   }

   public HandlerRegistration addPasteHandler(PasteEvent.Handler handler)
   {
      return addHandler(handler, PasteEvent.TYPE);
   }

   public HandlerRegistration addAceClickHandler(AceClickEvent.Handler handler)
   {
      return addHandler(handler, AceClickEvent.TYPE);
   }
   
   public HandlerRegistration addSetBreakpointHandler(DebugBreakpointSetEvent.Handler handler)
   {
      return addHandler(handler, DebugBreakpointSetEvent.TYPE);
   }

   public void forceResize()
   {
      editor_.getRenderer().onResize(true);
   }

   public void autoHeight()
   {
      editor_.autoHeight();
   }

   public void forceCursorChange()
   {
      editor_.onCursorChange();
   }
   
   private void updateBreakpoints(AceDocumentChangeEventNative changeEvent)
   {
      // if there are no breakpoints, don't do any work to move them about
      if (breakpointLines_.size() == 0)
      {
         return;
      }
      
      // see if we need to move any breakpoints around in response to 
      // this change to the document's text
      String action = changeEvent.getAction();
      Range range = changeEvent.getRange();
      Position start = range.getStart();
      Position end = range.getEnd();
      
      // if the edit was all on one line or the action didn't change text
      // in a way that could change lines, we can't have moved anything
      if (start.getRow() == end.getRow() || 
          (!action.equals("insertText") &&
           !action.equals("insertLines") &&
           !action.equals("removeText") &&
           !action.equals("removeLines")))
      {
         return;
      }
      
      int shiftedBy = 0;
      int shiftStartRow = 0;
      
      // compute how many rows to shift
      if (action == "insertText" || 
          action == "insertLines")
      {
         shiftedBy = end.getRow() - start.getRow();
      } 
      else
      {
         shiftedBy = start.getRow() - end.getRow();
      }
      
      // compute where to start shifting
      shiftStartRow = start.getRow() + 
            ((action == "insertText" && start.getColumn() > 0) ? 
                  1 : 0);
      
      // make a pass through the breakpoints and move them as appropriate:
      // remove all the breakpoints after the row where the change
      // happened, and add them back at their new position if they were
      // not part of a deleted range. 
      //
      // this must be done in two passes, so we can distinguish
      // breakpoints that haven't been processed yet from those that have.
      // (an alternative would be to keep the set of breakpoints sorted.)
      ArrayList<Integer> oldBreakpoints = new ArrayList<Integer>();
     
      for (int idx = 0; idx < breakpointLines_.size(); idx++)
      {
         Integer breakpointLine = breakpointLines_.get(idx);
         if (breakpointLine >= shiftStartRow)
         {
            // remove the breakpoint from its old position
            breakpointLines_.remove(idx);
            oldBreakpoints.add(breakpointLine);
            editor_.getSession().clearBreakpoint(breakpointLine);
            idx--;
         }
      }
      for (Integer breakpointLine: oldBreakpoints)
      {
         // calculate the new position of the breakpoint
         Integer newBreakpointPosition = 
               new Integer(breakpointLine + shiftedBy);
         
         // add a breakpoint in this new position only if it wasn't 
         // in a deleted range, and if we don't already have a
         // breakpoint there
         if (breakpointLine >= end.getRow() &&
             !(breakpointLine == end.getRow() && shiftedBy < 0) &&
             !breakpointLines_.contains(newBreakpointPosition))
         {
            breakpointLines_.add(newBreakpointPosition);
            editor_.getSession().setBreakpoint(newBreakpointPosition);
         }
      }
   }
  
   private final AceEditorNative editor_;
   private final HandlerManager capturingHandlers_;
   private boolean initToEmptyString_ = true;
   private ArrayList<Integer> breakpointLines_ = new ArrayList<Integer>();
}