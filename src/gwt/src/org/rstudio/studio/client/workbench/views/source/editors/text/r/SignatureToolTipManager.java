/*
 * SignatureToolTipManager.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.r;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.MouseDragHandler.MouseCoordinates;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Inject;

public class SignatureToolTipManager
{
   public SignatureToolTipManager(DocDisplay docDisplay)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      toolTip_ = new RCompletionToolTip(docDisplay);
      docDisplay_ = docDisplay;
      handlers_ = new HandlerRegistrations();
      
      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            // Bail if we don't ever show tooltips
            if (!uiPrefs_.showSignatureTooltips().getGlobalValue())
               return;
            
            // Bail if this is a cursor-activated timer and we
            // don't want idle tooltips
            if (coordinates_ == null && !uiPrefs_.showFunctionTooltipOnIdle().getGlobalValue())
               return;
            
            // Bail if the tooltip is already showing from a non-mouse event.
            if (!isMouseDrivenEvent() && toolTip_.isShowing())
               return;
            
            resolveActiveFunctionAndDisplayToolTip();
         }
      };
      
      monitor_ = new Timer()
      {
         @Override
         public void run()
         {
            if (ready_)
            {
               timer_.schedule(TIMER_DELAY_MS);
               ready_ = false;
            }
         }
      };
      
      handlers_.add(docDisplay_.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            beginMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            timer_.cancel();
            toolTip_.hide();
            endMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(final CursorChangedEvent event)
         {
            beginMonitoring();
               
            // Defer so that anchors can update
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  // Check to see if the cursor has moved outside of the anchored region.
                  if (anchor_ != null && toolTip_.isShowing())
                  {
                     Position position = event.getPosition();
                     if (anchor_.getRange().contains(position))
                     {
                        // Update the tooltip position if the cursor changes rows.
                        if (position.getRow() > tooltipPosition_.getRow())
                        {
                           // Allow tooltip to nudge right (but not left)
                           int newColumn = Math.max(
                                 tooltipPosition_.getColumn(),
                                 position.getColumn());
                           
                           setTooltipPosition(Position.create(
                                 position.getRow(),
                                 newColumn));
                        }
                     }
                     else
                     {
                        anchor_ = null;
                        toolTip_.hide();
                     }
                  }
               }
            });
         }
      }));
      
      handlers_.add(toolTip_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (!event.isAttached())
            {
               coordinates_ = null;
               completionPosition_ = null;
               anchor_ = null;
            }
         }
      }));
      
      handlers_.add(events_.addHandler(ConsoleWriteInputEvent.TYPE, new ConsoleWriteInputHandler()
      {
         @Override
         public void onConsoleWriteInput(ConsoleWriteInputEvent event)
         {
            anchor_ = null;
            toolTip_.hide();
         }
      }));
   }
   
   @Inject
   public void initialize(UIPrefs uiPrefs,
                          EventBus events,
                          CodeToolsServerOperations server)
   {
      uiPrefs_ = uiPrefs;
      events_ = events;
      server_ = server;
   }
   
   private void attachPreviewHandler()
   {
      detachPreviewHandler();
      preview_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            if (preview.getTypeInt() == Event.ONMOUSEMOVE)
            {
               NativeEvent event = preview.getNativeEvent();
               coordinates_ = new MouseCoordinates(
                     event.getClientX(),
                     event.getClientY());
               ready_ = true;
            }
            else if (preview.getTypeInt() == Event.ONKEYDOWN)
            {
               coordinates_ = null;
               ready_ = true;
               if (preview.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE)
                  suppressed_ = true;
            }
         }
      });
   }
   
   private void detachPreviewHandler()
   {
      if (preview_ != null)
      {
         preview_.removeHandler();
         preview_ = null;
      }
   }
   
   private void beginMonitoring()
   {
      if (monitoring_)
         return;
      
      attachPreviewHandler();
      monitor_.scheduleRepeating(MONITOR_DELAY_MS);
      monitoring_ = true;
   }
   
   private void endMonitoring()
   {
      detachPreviewHandler();
      monitor_.cancel();
      monitoring_ = false;
   }
   
   public void detach()
   {
      endMonitoring();
      timer_.cancel();
      handlers_.removeHandler();
   }
   
   public RCompletionToolTip getToolTip()
   {
      return toolTip_;
   }
   
   public boolean previewKeyDown(NativeEvent event)
   {
      return toolTip_.previewKeyDown(event);
   }
   
   private final native boolean isBoringFunction(String name) /*-{
      var boring = ["c", "list", ".rs.addFunction", ".rs.addJsonRpcHandler"];
      return boring.some(function(x) { return x === name; });
   }-*/;
   
   public void displayToolTip(final String name, 
                              final String source,
                              String helpHandler)
   {
      if (isBoringFunction(name))
         return;
      
      server_.getArgs(name, source, helpHandler, new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String response)
         {
            if (StringUtil.isNullOrEmpty(response))
               return;
            
            toolTip_.resolvePositionAndShow(name + response);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   boolean isMouseDrivenEvent()
   {
      return coordinates_ != null;
   }
   
   Position getLookupPosition()
   {
      if (coordinates_ == null)
      {
         Position position = docDisplay_.getCursorPosition();
         
         // Nudge the cursor column if the cursor currently lies
         // upon a closing paren.
         if (docDisplay_.getCharacterAtCursor() == ')' &&
             position.getColumn() != 0)
         {
            position = Position.create(
                  position.getRow(),
                  position.getColumn() - 1);
         }
         
         return position;
      }
      
      return docDisplay_.screenCoordinatesToDocumentPosition(
            coordinates_.getMouseX(),
            coordinates_.getMouseY());
   }
   
   // Sets an anchored range for a cursor currently lying
   // on an identifier before a '(' (a function call).
   private void setAnchor(TokenCursor cursor)
   {
      if (anchor_ != null)
      {
         anchor_.detach();
         anchor_ = null;
      }
      
      TokenCursor endCursor = cursor.cloneCursor();
      if (!endCursor.moveToNextToken())
         return;
      
      if (!endCursor.valueEquals("("))
         return;
      
      // TODO: How to anchor if there is no matching ')' for
      // this function?
      if (!endCursor.fwdToMatchingToken())
         return;
      
      Position endPos = endCursor.currentPosition();
      TokenCursor startCursor = cursor.cloneCursor();
      Token lookbehind = startCursor.peekBwd(1);
      if (lookbehind.valueEquals("::") || lookbehind.valueEquals(":::"))
      {
         if (!startCursor.moveToPreviousToken())
            return;
         
         if (!startCursor.moveToPreviousToken())
            return;
      }
      
      Position startPos = startCursor.currentPosition();
      anchor_ = docDisplay_.createAnchoredSelection(startPos, endPos);
   }
   
   public void resolveActiveFunctionAndDisplayToolTip()
   {
      if (suppressed_)
      {
         suppressed_ = false;
         return;
      }
      
      if (docDisplay_.isPopupVisible())
         return;
      
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor == null)
         return;
      
      final Position position = getLookupPosition();
      final boolean isMouseEvent = isMouseDrivenEvent();
      
      // Ensure that the mouse target is actually the active editor
      if (isMouseEvent)
      {
         Element el = DomUtils.elementFromPoint(
               coordinates_.getMouseX(),
               coordinates_.getMouseY());
         
         AceEditorNative nativeEditor = AceEditorNative.getEditor(el);
         if (nativeEditor != null && nativeEditor != editor.getWidget().getEditor())
            return;
      }
      
      // Hide an older tooltip if this was a mouse move (this allows
      // the popup to hide if the user hovers over a function name,
      // and then later moves the mouse away)
      if (isMouseEvent)
         toolTip_.hide();
      
      TokenCursor cursor = editor.getSession().getMode().getRCodeModel().getTokenCursor();
      if (!cursor.moveToPosition(position, true))
         return;
      
      // If this is a cursor-idle event and the user has opted into
      // cursor-idle tooltips, then do some extra work to resolve
      // the location of that function. It's okay if this fails
      // (implies that perhaps the cursor is already on a function name,
      // rather than within a function). Note that on failure the cursor
      // position is not changed.
      if (!isMouseEvent &&
          uiPrefs_.showFunctionTooltipOnIdle().getGlobalValue() &&
          !cursor.valueEquals("("))
      {
         cursor.findOpeningBracket("(", false);
      }
      
      Token lookahead = cursor.peekFwd(1);
      if (lookahead.valueEquals("::") || lookahead.valueEquals(":::"))
         if (!cursor.moveToNextToken())
            return;
      
      if (cursor.valueEquals("::") || cursor.valueEquals(":::"))
         if (!cursor.moveToNextToken())
            return;
      
      if (cursor.valueEquals("("))
         if (!cursor.moveToPreviousToken())
            return;
      
      // Cache the cursor position -- this should be the first
      // token prior to a '(', forming the active call.
      // If we already have an active tooltip for the current position,
      // then bail.
      if (toolTip_.isShowing() &&
          cursor.currentPosition().isEqualTo(completionPosition_))
      {
         return;
      }
      completionPosition_ = cursor.currentPosition();
      
      // Double check that we're in the correct spot for a function call.
      // The cursor should lie upon an identifier, and the next token should
      // be an opening paren.
      if (!cursor.hasType("identifier"))
         return;

      if (!cursor.nextValue().equals("("))
         return;
      
      String callString = cursor.currentValue();
      if (isBoringFunction(callString))
         return;
      
      // If this is a namespaced function call, then append that context.
      Token lookbehind = cursor.peekBwd(1);
      if (lookbehind.valueEquals("::") || lookbehind.valueEquals(":::"))
      {
         // Do-while loop just to allow 'break' for control flow
         do
         {
            TokenCursor clone = cursor.cloneCursor();
            if (!clone.moveToPreviousToken())
               break;
            if (!clone.moveToPreviousToken())
               break;
            if (!clone.hasType("identifier"))
               break;
            callString = clone.currentValue() + "::" + callString;
         } while (false);
            
      }
      
      // Set anchor (so we can dismiss popup when cursor moves outside
      // of anchored region)
      setAnchor(cursor.cloneCursor());
      
      final String fnString = callString;
      server_.getArgs(fnString, "", "", new ServerRequestCallback<String>() {
         
         @Override
         public void onResponseReceived(String arguments)
         {
            if (StringUtil.isNullOrEmpty(arguments))
               return;
            
            final String signature = fnString + arguments;
              
            resolvePositionAndShow(signature, position);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   private void resolvePositionAndShow(String signature, Position position)
   {
      // Default to displaying before cursor; however, display above
      // if doing so would place the tooltip too close (or off) of
      // the window.
      toolTip_.setText(signature);
      setTooltipPosition(position);
   }
   
   private void setTooltipPosition(Position position)
   {
      final Rectangle bounds = docDisplay_.getPositionBounds(position);
      tooltipPosition_ = position;
      toolTip_.setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            int left = bounds.getLeft();
            
            boolean verticalOverflow = bounds.getBottom() + offsetHeight >= Window.getClientHeight() - 20;
            int top = verticalOverflow
                  ? bounds.getTop() - offsetHeight - 10
                  : bounds.getBottom() + 10;
                  
            toolTip_.setPopupPosition(left, top);
         }
      });
   }
   
   private final RCompletionToolTip toolTip_;
   private final DocDisplay docDisplay_;
   private final HandlerRegistrations handlers_;
   private final Timer timer_;
   
   private final Timer monitor_;
   private boolean monitoring_;
   private boolean suppressed_;
   
   private HandlerRegistration preview_;
   private MouseCoordinates coordinates_;
   private Position completionPosition_;
   private Position tooltipPosition_;
   private AnchoredSelection anchor_;
   private boolean ready_;

   private UIPrefs uiPrefs_;
   private EventBus events_;
   private CodeToolsServerOperations server_;
   
   private static final int MONITOR_DELAY_MS = 200;
   private static final int TIMER_DELAY_MS   = 900;
}

