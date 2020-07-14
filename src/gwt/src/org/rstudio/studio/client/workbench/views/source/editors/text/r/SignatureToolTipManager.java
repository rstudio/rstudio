/*
 * SignatureToolTipManager.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.core.client.CommandWithArg;
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
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
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
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Inject;

public abstract class SignatureToolTipManager
{
   // Sub-classes should override this to indicate
   // when the tooltip monitor is enabled / disabled.
   protected abstract boolean isEnabled(Position position);
   
   // Subclasses should override this for their own
   // argument-retrieving behaviors.
   protected void getFunctionArguments(final String name,
                                       final String source,
                                       final String helpHandler,
                                       final CommandWithArg<String> onReady)
   {
      server_.getArgs(name, source, helpHandler, new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String response)
         {
            if (StringUtil.isNullOrEmpty(response))
               return;
            
            onReady.execute(response);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
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
            // Bail if we don't have a lookup position
            Position position = getLookupPosition();
            if (position == null)
               return;
            
            // Bail if this signature tooltip manager isn't relevant
            // for this particular cursor position
            if (!isEnabled(position))
               return;
            
            // Bail if we don't ever show tooltips
            if (!userPrefs_.showFunctionSignatureTooltips().getGlobalValue())
               return;
            
            // Bail if this is a cursor-activated timer and we
            // don't want idle tooltips
            if (coordinates_ == null && !userPrefs_.showHelpTooltipOnIdle().getGlobalValue())
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
            if (!monitoring_)
               return;
            
            // Defer so that anchors can update
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  if (anchor_ == null || !toolTip_.isShowing())
                     return;
                  
                  // re-request cursor position in case it's changed since the
                  // last cursor change without signaling event handlers
                  Position position = docDisplay_.getCursorPosition();
                  
                  if (anchor_.getRange().contains(position))
                  {
                     // Update the tooltip position if the cursor changes rows.
                     if (tooltipPosition_ != null &&
                           position.getRow() > tooltipPosition_.getRow())
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
                     detachAnchor();
                     toolTip_.hide();
                  }
               }
            });
         }
      }));
      
      handlers_.add(events_.addHandler(ConsoleWriteInputEvent.TYPE, new ConsoleWriteInputEvent.Handler()
      {
         @Override
         public void onConsoleWriteInput(ConsoleWriteInputEvent event)
         {
            detachAnchor();
            toolTip_.hide();
         }
      }));
   }
   
   @Inject
   public void initialize(UserPrefs uiPrefs,
                          EventBus events,
                          CodeToolsServerOperations server)
   {
      userPrefs_ = uiPrefs;
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
               if (toolTip_.isShowing() && preview.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE)
               {
                  toolTip_.hide();
                  preview.cancel();
                  return;
               }
               
               coordinates_ = null;
               ready_ = true;
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
      detachAnchor();
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
      if (!userPrefs_.showFunctionSignatureTooltips().getGlobalValue())
         return;
      
      if (isBoringFunction(name))
         return;
      
      getFunctionArguments(name, source, helpHandler, (String response) ->
      {
         toolTip_.resolvePositionAndShow(name + response);
      });
   }
   
   boolean isMouseDrivenEvent()
   {
      return coordinates_ != null;
   }
   
   private Position getLookupPosition()
   {
      if (coordinates_ == null)
      {
         return getLookupPositionCursor();
      }
      else
      {
         return getLookupPositionMouseCoordinates();
      }
   }
   
   private Position getLookupPositionCursor()
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
   
   private Position getLookupPositionMouseCoordinates()
   {
      return docDisplay_.screenCoordinatesToDocumentPosition(
            coordinates_.getMouseX(),
            coordinates_.getMouseY());
   }
   
   // Sets an anchored range for a cursor currently lying
   // on an identifier before a '(' (a function call).
   private void setAnchor(TokenIterator cursor)
   {
      TokenIterator endCursor = cursor.clone();
      if (!endCursor.moveToNextToken())
         return;
      
      if (!endCursor.valueEquals("("))
         return;
      
      // TODO: How to anchor if there is no matching ')' for
      // this function?
      if (!endCursor.fwdToMatchingToken())
         return;
      
      Position endPos = endCursor.getCurrentTokenPosition();
      TokenIterator startCursor = cursor.clone();
      Token lookbehind = startCursor.peekBwd(1);
      if (lookbehind != null && (
            lookbehind.valueEquals("::") ||
            lookbehind.valueEquals(":::")))
      {
         if (!startCursor.moveToPreviousToken())
            return;
         
         if (!startCursor.moveToPreviousToken())
            return;
      }
      
      Position startPos = startCursor.getCurrentTokenPosition();
      
      detachAnchor();
      anchor_ = docDisplay_.createAnchoredSelection(startPos, endPos);
   }
   
   public void resolveActiveFunctionAndDisplayToolTip()
   {
      if (!userPrefs_.showFunctionSignatureTooltips().getGlobalValue())
         return;
      
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
         if (nativeEditor == null)
            return;
         
         if (nativeEditor != editor.getWidget().getEditor())
            return;
      }
      
      // Hide an older tooltip if this was a mouse move (this allows
      // the popup to hide if the user hovers over a function name,
      // and then later moves the mouse away)
      if (isMouseEvent)
         toolTip_.hide();
      
      TokenIterator cursor = docDisplay_.createTokenIterator();
      Token token = cursor.moveToPosition(position, true);
      if (token == null)
         return;
      
      // If this is a cursor-idle event and the user has opted into
      // cursor-idle tooltips, then do some extra work to resolve
      // the location of that function. It's okay if this fails
      // (implies that perhaps the cursor is already on a function name,
      // rather than within a function). Note that on failure the cursor
      // position is not changed.
      if (!isMouseEvent &&
          userPrefs_.showFunctionSignatureTooltips().getGlobalValue() &&
          !cursor.valueEquals("("))
      {
         cursor.findTokenValueBwd("(", true);
      }
      
      Token lookahead = cursor.peekFwd(1);
      if (lookahead != null)
      {
         if (lookahead.valueEquals("::") || lookahead.valueEquals(":::"))
            if (!cursor.moveToNextToken())
               return;
      }
      
      if (cursor.valueEquals("::") || cursor.valueEquals(":::"))
         if (!cursor.moveToNextToken())
            return;
      
      if (cursor.valueEquals("("))
         if (!cursor.moveToPreviousToken())
            return;
      
      // Now, double-check that the bounding box associated with
      // the document row actually correspond to the mouse position.
      // This is necessary as Ace will 'clamp' the screen row to
      // actual positions available in the document.
      if (isMouseDrivenEvent())
      {
         Position cursorPos = cursor.getCurrentTokenPosition();
         ScreenCoordinates coordinates =
               editor.documentPositionToScreenCoordinates(cursorPos);

         int rowTop = coordinates.getPageY();
         int rowBottom = coordinates.getPageY() +
               editor.getWidget().getEditor().getRenderer().getLineHeight();

         if (coordinates_.getMouseY() < rowTop ||
             coordinates_.getMouseY() > rowBottom)
         {
            toolTip_.hide();
            return;
         }
      }
      
      // Cache the cursor position -- this should be the first
      // token prior to a '(', forming the active call.
      // If we already have an active tooltip for the current position,
      // then bail.
      if (toolTip_.isShowing() &&
          cursor.getCurrentTokenPosition().isEqualTo(completionPosition_))
      {
         return;
      }
      completionPosition_ = cursor.getCurrentTokenPosition();
      
      // Double check that we're in the correct spot for a function call.
      // The cursor should lie upon an identifier, and the next token should
      // be an opening paren.
      Token currentToken = cursor.getCurrentToken();
      if (currentToken == null)
         return;
      
      if (!currentToken.hasType("identifier", "function"))
         return;
      
      Token nextToken = cursor.peekFwd();
      if (nextToken == null)
         return;
      
      if (!nextToken.getValue().equals("("))
         return;
      
      String callString = currentToken.getValue();
      if (isBoringFunction(callString))
         return;
      
      // If this is a namespaced function call, then append that context.
      Token lookbehind = cursor.peekBwd(1);
      if (lookbehind != null && (
            lookbehind.valueEquals("::") ||
            lookbehind.valueEquals(":::")))
      {
         // Do-while loop just to allow 'break' for control flow
         do
         {
            TokenIterator clone = cursor.clone();
            if (!clone.moveToPreviousToken())
               break;
            if (!clone.moveToPreviousToken())
               break;
            
            if (!clone.getCurrentToken().hasType("identifier"))
               break;
            
            callString = clone.getCurrentToken().getValue() + "::" + callString;
         } while (false);
            
      }
      
      // Set anchor (so we can dismiss popup when cursor moves outside
      // of anchored region)
      setAnchor(cursor.clone());
      
      final String fnString = callString;
      getFunctionArguments(fnString, "", "", (String response) -> {
         resolvePositionAndShow(fnString + response, position);
      });
   }
   
   private void resolvePositionAndShow(String signature, Position position)
   {
      toolTip_.resolvePositionAndShow(signature, position);
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
   
   private void detachAnchor()
   {
      if (anchor_ != null)
      {
         anchor_.detach();
         anchor_ = null;
      }
   }
   
   private final RCompletionToolTip toolTip_;
   private final DocDisplay docDisplay_;
   private final HandlerRegistrations handlers_;
   private final Timer timer_;
   
   private final Timer monitor_;
   private boolean monitoring_;
   
   private HandlerRegistration preview_;
   private MouseCoordinates coordinates_;
   private Position completionPosition_;
   private Position tooltipPosition_;
   private AnchoredSelection anchor_;
   private boolean ready_;

   private UserPrefs userPrefs_;
   private EventBus events_;
   private CodeToolsServerOperations server_;
   
   private static final int MONITOR_DELAY_MS = 200;
   private static final int TIMER_DELAY_MS   = 900;
}

