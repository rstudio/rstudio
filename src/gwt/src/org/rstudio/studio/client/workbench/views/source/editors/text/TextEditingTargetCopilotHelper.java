/*
 * TextEditingTargetCopilotHelper.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponse.CopilotCodeCompletionResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotCompletion;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

import jsinterop.base.JsArrayLike;

public class TextEditingTargetCopilotHelper
{
   public TextEditingTargetCopilotHelper(TextEditingTarget target)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      target_ = target;
      display_ = target.getDocDisplay();
      
      // TODO: The handlers below need their HandlerRegistrations recorded
      // and cleaned up when the widget is removed.
      
      // Listen for document change events, and use that to trigger
      // the display of ghost text in the editor.
      docChangedTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            requestCompletions();
         }
      };
      
      display_.addCursorChangedHandler((event) ->
      {
         // Delay handler so we can handle a Tab keypress
         Timers.singleShot(0, () -> {
            activeCompletion_ = null;
            display_.removeGhostText();
         });
      });
      
      display_.addDocumentChangedHandler((event) ->
      {
         // TODO: Make configurable?
         // TODO: Request completions eagerly, but display them less eagerly?
         docChangedTimer_.schedule(700);
      });
      
      display_.addCapturingKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent keyEvent)
         {
            // If ghost text is being displayed, accept it on a Tab key press.
            if (activeCompletion_ == null)
               return;
            
            // TODO: Let user choose keybinding for accepting ghost text?
            NativeEvent event = keyEvent.getNativeEvent();
            if (event.getKeyCode() == KeyCodes.KEY_TAB)
            {
               event.stopPropagation();
               event.preventDefault();
               
               Range aceRange = Range.create(
                     activeCompletion_.range.start.line,
                     activeCompletion_.range.start.character,
                     activeCompletion_.range.end.line,
                     activeCompletion_.range.end.character);
               display_.replaceRange(aceRange, activeCompletion_.text);
               
               activeCompletion_ = null;
            }
         }
      });
      
   }
   
   private void requestCompletions()
   {
      final Position oldPosition = display_.getCursorPosition();
      target_.withSavedDoc(() ->
      {
         server_.copilotCodeCompletion(
               target_.getId(),
               display_.getCursorRow(),
               display_.getCursorColumn(),
               new ServerRequestCallback<CopilotCodeCompletionResponse>()
               {
                  @Override
                  public void onResponseReceived(CopilotCodeCompletionResponse response)
                  {
                     Position newPosition = display_.getCursorPosition();
                     if (!oldPosition.isEqualTo(newPosition))
                        return;
                     
                     JsArrayLike<CopilotCompletion> completions = response.result.completions;
                     if (completions.getLength() == 0)
                        return;

                     CopilotCompletion completion = completions.getAt(0);
                     
                     // Copilot includes trailing '```' for some reason in some cases,
                     // remove those if we're inserting in an R document.
                     if (completion.text.endsWith("\n```"))
                        completion.text = completion.text.substring(0, completion.text.length() - 3);
                     
                     if (completion.displayText.endsWith("\n```"))
                        completion.text = completion.text.substring(0, completion.text.length() - 3);
                     
                     activeCompletion_ = completion;
                     display_.setGhostText(activeCompletion_.displayText);
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                  }
               });
      });
   }
   
   @Inject
   private void initialize(Copilot copilot,
                           CopilotServerOperations server)
   {
      copilot_ = copilot;
      server_ = server;
   }
   
   private final TextEditingTarget target_;
   private final DocDisplay display_;
   private final Timer docChangedTimer_;
   
   private CopilotCompletion activeCompletion_;
   
   
   // Injected ----
   private Copilot copilot_;
   private CopilotServerOperations server_;
}
