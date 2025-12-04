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

import java.util.Objects;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.MathUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.core.client.dom.EventProperty;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.projects.ui.prefs.events.ProjectOptionsChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.model.CopilotConstants;
import org.rstudio.studio.client.workbench.copilot.model.CopilotEvent;
import org.rstudio.studio.client.workbench.copilot.model.CopilotEvent.CopilotEventType;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotGenerateCompletionsResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotNextEditSuggestionsResponse;
import org.rstudio.studio.client.workbench.copilot.model.CopilotResponseTypes.CopilotNextEditSuggestionsResultEntry;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotCompletion;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotError;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.InsertionBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

import jsinterop.base.Any;
import jsinterop.base.Js;
import jsinterop.base.JsArrayLike;

public class TextEditingTargetCopilotHelper
{
   interface CopilotCommandBinder extends CommandBinder<Commands, TextEditingTargetCopilotHelper>
   {
   }

   // A wrapper class for Copilot Completions, which is used to track partially-accepted completions.
   private static class Completion
   {
      public Completion(CopilotCompletion originalCompletion)
      {
         // Copilot includes trailing '```' for some reason in some cases,
         // remove those if we're inserting in an R document.
         this.insertText = postProcessCompletion(originalCompletion.insertText);
         this.displayText = this.insertText;

         this.startLine = originalCompletion.range.start.line;
         this.startCharacter = originalCompletion.range.start.character;
         this.endLine = originalCompletion.range.end.line;
         this.endCharacter = originalCompletion.range.end.character;

         this.originalCompletion = originalCompletion;
         this.partialAcceptedLength = 0;
      }

      public String insertText;
      public String displayText;
      public int startLine;
      public int startCharacter;
      public int endLine;
      public int endCharacter;
      public CopilotCompletion originalCompletion;
      public int partialAcceptedLength;
   }

   /**
    * Shows an inline edit suggestion diff view for the given completion.
    */
   private void showEditSuggestion(CopilotCompletion completion)
   {
      // Note that we can accept the diff suggestion with Tab
      Scheduler.get().scheduleDeferred(() ->
      {
         canAcceptSuggestionWithTab_ = true;
      });

      // Highlight the range in the document associated with
      // the edit suggestion
      Range editRange = Range.create(
         completion.range.start.line,
         completion.range.start.character,
         completion.range.end.line,
         completion.range.end.character);

      diffMarkerId_ = display_.addHighlight(editRange, "ace_next-edit-suggestion-highlight", "text");

      // Get the original text from the document at the edit range
      String originalText = display_.getCode(
         Position.create(completion.range.start.line,
                         completion.range.start.character),
         Position.create(completion.range.end.line,
                         completion.range.end.character));

      // Get the replacement text from the completion
      String replacementText = completion.insertText;

      // Create the diff view widget
      diffView_ = new AceEditorDiffView(originalText, replacementText, display_.getFileType())
      {
         @Override
         protected void apply()
         {
            // Get edit range
            Range range = Range.create(
               completion.range.start.line,
               completion.range.start.character,
               completion.range.end.line,
               completion.range.end.character);

            // Move cursor to end of edit range
            display_.setCursorPosition(range.getEnd());

            // Perform the actual replacement
            display_.replaceRange(range, completion.insertText);

            // Reset and schedule another suggestion
            reset();
            nesTimer_.schedule(20);
         }

         @Override
         protected void discard()
         {
            reset();
         }

         @Override
         public double getLineHeight()
         {
            return display_.getLineHeight();
         }
      };

      // Insert as line widget at the end row of the completion
      int row = completion.range.end.line;

      diffWidget_ = new PinnedLineWidget(
         "copilot-diff",
         display_,
         diffView_.getWidget(),
         row,
         null,
         null);
   }

   /**
    * Resets any visible ghost text or inline diff view.
    * Call this before presenting a new suggestion to ensure only one is shown at a time.
    */
   private void reset()
   {
      // Remove ghost text
      display_.removeGhostText();
      completionTimer_.cancel();
      activeCompletion_ = null;

      // Detach inline diff view
      if (diffWidget_ != null)
      {
         diffWidget_.detach();
         diffWidget_ = null;
      }

      if (diffView_ != null)
      {
         diffView_.detach();
         diffView_ = null;
      }

      if (diffMarkerId_ != -1)
      {
         display_.removeHighlight(diffMarkerId_);
         diffMarkerId_ = -1;
      }
   }

   public TextEditingTargetCopilotHelper(TextEditingTarget target)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      binder_.bind(commands_, this);

      target_ = target;
      display_ = target.getDocDisplay();

      registrations_ = new HandlerRegistrations();
      
      completionTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            if (copilotDisabledInThisDocument_)
               return;
            
            target_.withSavedDoc(() ->
            {
               requestId_ += 1;
               final int requestId = requestId_;
               final Position savedCursorPosition = display_.getCursorPosition();
               
               events_.fireEvent(
                     new CopilotEvent(CopilotEventType.COMPLETION_REQUESTED));

               String trigger = prefs_.copilotCompletionsTrigger().getGlobalValue();
               boolean autoInvoked = trigger.equals(UserPrefsAccessor.COPILOT_COMPLETIONS_TRIGGER_AUTO);
               if (completionTriggeredByCommand_)
               {
                  // users can trigger completions manually via command, even if set to auto
                  autoInvoked = false;
                  completionTriggeredByCommand_ = false;
               }
               
               server_.copilotGenerateCompletions(
                     target_.getId(),
                     StringUtil.notNull(target_.getPath()),
                     StringUtil.isNullOrEmpty(target_.getPath()),
                     autoInvoked,
                     display_.getCursorRow(),
                     display_.getCursorColumn(),
                     new ServerRequestCallback<CopilotGenerateCompletionsResponse>()
                     {
                        @Override
                        public void onResponseReceived(CopilotGenerateCompletionsResponse response)
                        {
                           // Check for invalidated request.
                           if (requestId_ != requestId)
                              return;
                           
                           // Check for alternate cursor position.
                           Position currentCursorPosition = display_.getCursorPosition();
                           if (!currentCursorPosition.isEqualTo(savedCursorPosition))
                              return;
                           
                           // Check for null completion results -- this may occur if the Copilot
                           // agent couldn't be started for some reason.
                           if (response == null)
                              return;
                           
                           // Check whether completions are enabled in this document.
                           if (Objects.equals(response.enabled, false))
                           {
                              copilotDisabledInThisDocument_ = true;
                              events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETION_CANCELLED));
                              return;
                           }
                           
                           // Check for error.
                           CopilotError error = response.error;
                           if (error != null)
                           {
                              // Handle 'document could not be found' errors up-front. These errors
                              // will normally self-resolve after the user starts editing the document,
                              // so it should suffice just to indicate that no completions are available.
                              int code = error.code;
                              if (code == CopilotConstants.ErrorCodes.DOCUMENT_NOT_FOUND)
                              {
                                 events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETION_RECEIVED_NONE));
                              }
                              else
                              {
                                 String message = copilot_.messageForError(error);
                                 events_.fireEvent(
                                       new CopilotEvent(
                                             CopilotEventType.COMPLETION_ERROR,
                                             message));
                                 return;
                              }
                           }
                           
                           // Check for null result. This might occur if the completion request
                           // was cancelled by the copilot agent.
                           Any result = response.result;
                           if (result == null)
                           {
                              events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETION_CANCELLED));
                              return;
                           }
                           
                           // Check for a cancellation reason.
                           Object reason = result.asPropertyMap().get("cancellationReason");
                           if (reason != null)
                           {
                              events_.fireEvent(
                                    new CopilotEvent(CopilotEventType.COMPLETION_CANCELLED));
                              return;
                           }
                           
                           // Otherwise, handle the response.
                           JsArrayLike<CopilotCompletion> completions =
                                 Js.cast(result.asPropertyMap().get("items"));
                           
                           events_.fireEvent(new CopilotEvent(
                                 completions.getLength() == 0
                                    ? CopilotEventType.COMPLETION_RECEIVED_NONE
                                    : CopilotEventType.COMPLETION_RECEIVED_SOME));
                           
                           // TODO: If multiple completions are available we should provide a way for 
                           // the user to view/select them. For now, use the last one.
                           // https://github.com/rstudio/rstudio/issues/16055
                           if (completions.getLength() > 0)
                           {
                              CopilotCompletion completion = completions.getAt(completions.getLength() - 1);

                              // The completion data gets modified when doing partial (word-by-word)
                              // completions, so we need to use a copy and preserve the original
                              // (which we need to send back to the server as-is in some language-server methods).
                              CopilotCompletion normalized = normalizeCompletion(completion);

                              reset();
                              activeCompletion_ = new Completion(normalized);
                              display_.setGhostText(activeCompletion_.displayText);
                              server_.copilotDidShowCompletion(completion, new VoidServerRequestCallback());
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                        }
                     });
            });
         }
      };

      suspendTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            completionRequestsSuspended_ = false;
         }
      };

      nesTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            requestNextEditSuggestions();
         }
      };
      
      events_.addHandler(ProjectOptionsChangedEvent.TYPE, (event) ->
      {
         manageHandlers();
      });
      
      prefs_.copilotEnabled().addValueChangeHandler((event) ->
      {
         manageHandlers();
      });
      
      Scheduler.get().scheduleDeferred(() ->
      {
         manageHandlers();
      });
      
   }
   
   private void manageHandlers()
   {
      if (!copilot_.isEnabled())
      {
         display_.removeGhostText();
         registrations_.removeHandler();
         requestId_ = 0;
         completionTimer_.cancel();
         completionTriggeredByCommand_ = false;
         events_.fireEvent(new CopilotEvent(CopilotEventType.COPILOT_DISABLED));
      }
      else
      {
         registrations_.addAll(

               display_.addValueChangeHandler((event) ->
               {
                  nesTimer_.schedule(300);
               }),

               // click handler for next-edit suggestion gutter icon. we use a capturing
               // event handler here so we can intercept the event before Ace does.
               DomUtils.addEventListener(display_.getElement(), "mousedown", true, (event) ->
               {
                  if (event.getButton() != NativeEvent.BUTTON_LEFT)
                     return;

                  Element target = event.getEventTarget().cast();

                  // Check for clicks on the next-edit suggestion gutter icon.
                  Element nesEl = DomUtils.findParentElement(target, true, new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.hasClassName("ace_next-edit-suggestion");
                     }
                  });

                  if (nesEl != null)
                  {
                     event.stopPropagation();
                     event.preventDefault();
                     display_.applyGhostText();
                     return;
                  }
               }),

               display_.addCursorChangedHandler((event) ->
               {
                  // Eagerly reset Tab acceptance flag
                  canAcceptSuggestionWithTab_ = false;

                  // Check if we've been toggled off
                  if (!automaticCodeSuggestionsEnabled_)
                     return;
                  
                  // Check preference value
                  String trigger = prefs_.copilotCompletionsTrigger().getGlobalValue();
                  if (trigger != UserPrefsAccessor.COPILOT_COMPLETIONS_TRIGGER_AUTO)
                     return;
                           
                  // Allow one-time suppression of cursor change handler
                  if (completionRequestsSuspended_)
                     return;
                  
                  // Don't do anything if we have a selection.
                  if (display_.hasSelection())
                  {
                     completionTimer_.cancel();
                     completionTriggeredByCommand_ = false;
                     return;
                  }
                  
                  // Request completions on cursor navigation.
                  int delayMs = MathUtil.clamp(prefs_.copilotCompletionsDelay().getValue(), 10, 5000);
                  completionTimer_.schedule(delayMs);

                  // Delay handler so we can handle a Tab keypress
                  Timers.singleShot(0, () -> {
                     activeCompletion_ = null;
                     display_.removeGhostText();
                  });
               }),

               display_.addCapturingKeyDownHandler(new KeyDownHandler()
               {
                  @Override
                  public void onKeyDown(KeyDownEvent keyEvent)
                  {
                     // Let diff view accept on Tab if applicable
                     if (diffView_ != null)
                     {
                        NativeEvent event = keyEvent.getNativeEvent();
                        if (event.getKeyCode() == KeyCodes.KEY_TAB && canAcceptSuggestionWithTab_)
                        {
                           event.stopPropagation();
                           event.preventDefault();
                           diffView_.apply();
                           return;
                        }
                     }

                     // Respect suppression flag
                     if (completionRequestsSuspended_)
                        return;

                     // If ghost text is being displayed, accept it on a Tab key press.
                     // TODO: Let user choose keybinding for accepting ghost text?
                     if (activeCompletion_ == null)
                        return;

                     // TODO: If we have a completion popup, should that take precedence?
                     if (display_.isPopupVisible())
                        return;
                     
                     // Check if the user just inserted some text matching the current
                     // ghost text. If so, we'll suppress the next cursor change handler,
                     // so we can continue presenting the current ghost text.
                     String key = EventProperty.key(keyEvent.getNativeEvent());
                     if (activeCompletion_.displayText.startsWith(key))
                     {
                        updateCompletion(key);
                        temporarilySuspendCompletionRequests();
                        return;
                     }

                     NativeEvent event = keyEvent.getNativeEvent();
                     if (event.getKeyCode() == KeyCodes.KEY_TAB)
                     {
                        event.stopPropagation();
                        event.preventDefault();

                        // Otherwise, accept the ghost text
                        Range aceRange = Range.create(
                              activeCompletion_.startLine,
                              activeCompletion_.startCharacter,
                              activeCompletion_.endLine,
                              activeCompletion_.endCharacter);
                        display_.replaceRange(aceRange, activeCompletion_.insertText);

                        Position cursorPos = Position.create(
                           activeCompletion_.endLine,
                           activeCompletion_.endCharacter + activeCompletion_.insertText.length());
                        display_.setCursorPosition(cursorPos);

                        server_.copilotDidAcceptCompletion(
                           activeCompletion_.originalCompletion.command,
                           new VoidServerRequestCallback());

                        reset();
                     }
                     else if (event.getKeyCode() == KeyCodes.KEY_BACKSPACE)
                     {
                        display_.removeGhostText();
                     }
                     else if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
                     {
                        // Don't remove ghost text if Ace's autocomplete is active
                        // Let Ace close its popup first
                        if (!display_.hasActiveAceCompleter())
                        {
                           display_.removeGhostText();
                           activeCompletion_ = null;
                        }
                     }
                     else if (display_.hasGhostText() &&
                              event.getKeyCode() == KeyCodes.KEY_RIGHT &&
                              (event.getCtrlKey() || event.getMetaKey()))
                     {
                        event.stopPropagation();
                        event.preventDefault();

                        commands_.copilotAcceptNextWord().execute();
                     }
                     
                  }
               })

         );

      }
   }

   private void requestNextEditSuggestions()
   {
      if (!prefs_.copilotNesEnabled().getGlobalValue())
         return;

      if (completionRequestsSuspended_)
         return;

      target_.withSavedDoc(() ->
      {
         requestNextEditSuggestionsImpl();
      });
   }

   private void requestNextEditSuggestionsImpl()
   {
      // Invalidate any prior requests.
      nesId_ += 1;

      // Save current request ID.
      final int id = nesId_;

      // Make the request.
      server_.copilotNextEditSuggestions(
         target_.getId(),
         StringUtil.notNull(target_.getPath()),
         StringUtil.isNullOrEmpty(target_.getPath()),
         display_.getCursorRow(),
         display_.getCursorColumn(),
         new ServerRequestCallback<CopilotNextEditSuggestionsResponse>()
         {
            @Override
            public void onResponseReceived(CopilotNextEditSuggestionsResponse response)
            {
               // Check for invalidated request.
               if (id != nesId_)
                  return;

               // Check for edits
               boolean hasEdits =
                  response.result != null &&
                  response.result.edits != null &&
                  response.result.edits.getLength() > 0;

               if (!hasEdits)
                  return;

               reset();
               CopilotNextEditSuggestionsResultEntry entry = response.result.edits.getAt(0);

               // Construct a Copilot completion object from the response
               CopilotCompletion completion = new CopilotCompletion();
               completion.insertText = entry.text;
               completion.range = entry.range;
               completion.command = entry.command;

               // The completion data gets modified when doing partial (word-by-word)
               // completions, so we need to use a copy and preserve the original
               // (which we need to send back to the server as-is in some language-server methods).
               CopilotCompletion normalized = normalizeCompletion(completion);
               activeCompletion_ = new Completion(normalized);

               if (normalized.range.start.line == normalized.range.end.line &&
                   normalized.range.start.character == normalized.range.end.character)
               {
                  // If the start position and end position match, then display
                  // the suggestion using ghost text at that position.
                  Position position = Position.create(
                     normalized.range.start.line,
                     normalized.range.start.character);
                  display_.setGhostText(activeCompletion_.displayText, position);

                  server_.copilotDidShowCompletion(completion, new VoidServerRequestCallback());
                  return;
               }
               else
               {
                  // Otherwise, show the suggestion as an inline diff view.
                  showEditSuggestion(completion);
               }
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
   }
   
   @Handler
   public void onCopilotRequestCompletions()
   {
      if (copilot_.isEnabled() && display_.isFocused())
      {
         completionTriggeredByCommand_ = true;
         completionTimer_.schedule(0);
      }
   }
   
   @Handler
   public void onCopilotAcceptNextWord()
   {
      if (!display_.isFocused())
         return;
         
      boolean hasActiveSuggestion = display_.hasGhostText() && activeCompletion_ != null;
      if (!hasActiveSuggestion)
         return;
      
      String text = activeCompletion_.displayText;
      Pattern pattern = Pattern.create("(?:\\b|$)");
      Match match = pattern.match(text, 1);
      if (match == null)
         return;
      
      String insertedWord = StringUtil.substring(text, 0, match.getIndex());
      String leftoverText = StringUtil.substring(text, match.getIndex());
      
      int n = insertedWord.length();
      
      // From the docs: "Note that the acceptedLength includes everything from the start of
      //                 insertText to the end of the accepted text. It is not the length of 
      //                 the accepted text itself."
      activeCompletion_.partialAcceptedLength += n;

      activeCompletion_.displayText = leftoverText;
      activeCompletion_.insertText = leftoverText;
      activeCompletion_.startCharacter += n;
      activeCompletion_.endCharacter += n;
      
      Timers.singleShot(() ->
      {
         temporarilySuspendCompletionRequests();
         display_.insertCode(insertedWord, InsertionBehavior.EditorBehaviorsDisabled);
         display_.setGhostText(activeCompletion_.displayText);
         server_.copilotDidAcceptPartialCompletion(activeCompletion_.originalCompletion, 
                                                   activeCompletion_.partialAcceptedLength,
                                                   new VoidServerRequestCallback());
         
         // Work around issue with ghost text not appearing after inserting
         // a code suggestion containing a new line
         if (insertedWord.indexOf('\n') != -1)
         {
            Timers.singleShot(20, () ->
            {
               display_.setGhostText(activeCompletion_.displayText);
            });
         }
      });
   }
   
   @Handler
   public void onCopilotToggleAutomaticCompletions()
   {
      if (display_.isFocused())
      {
         automaticCodeSuggestionsEnabled_ = !automaticCodeSuggestionsEnabled_;
         
         if (automaticCodeSuggestionsEnabled_)
         {
            events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETIONS_ENABLED));
         }
         else
         {
            events_.fireEvent(new CopilotEvent(CopilotEventType.COMPLETIONS_DISABLED));
         }
      }
   }
   
   private void updateCompletion(String key)
   {
      int n = key.length();
      activeCompletion_.displayText = StringUtil.substring(activeCompletion_.displayText, n);
      activeCompletion_.insertText = StringUtil.substring(activeCompletion_.insertText, n);
      activeCompletion_.startCharacter += n;
      activeCompletion_.endCharacter += n;
      
      // Ace's ghost text uses a custom token appended to the current line,
      // and lines are eagerly re-tokenized when new text is inserted. To
      // dodge this effect, we reset the ghost text at the end of the event loop.
      Timers.singleShot(() ->
      {
         display_.setGhostText(activeCompletion_.displayText);
      });
   }
   
   private static String postProcessCompletion(String text)
   {
      // Exclude chunk markers from completion results
      int endChunkIndex = text.indexOf("\n```");
      if (endChunkIndex != -1)
         text = text.substring(0, endChunkIndex);
      
      return text;
   }
   
   public void onFileTypeChanged()
   {
      copilotDisabledInThisDocument_ = false;
   }

   public boolean isCopilotEnabled()
   {
      return copilot_.isEnabled();
   }
   
   // A Copilot completion will often overlap region(s) of the document.
   // Try to avoid presenting this overlap, so that only the relevant
   // portion of the completed text is presented to the user.
   private CopilotCompletion normalizeCompletion(CopilotCompletion completion)
   {
      try
      {
         return normalizeCompletionImpl(completion);
      }
      catch (Exception e)
      {
         Debug.logException(e);
         return completion;
      }
   }

   private CopilotCompletion normalizeCompletionImpl(CopilotCompletion completion)
   {
      // Remove any overlap from the start of the completion.
      int lhs = 0;
      {
         int row = completion.range.start.line;
         int col = completion.range.start.character;
         String line = display_.getLine(row);

         for (; col + lhs < line.length(); lhs++)
         {
            char clhs = completion.insertText.charAt(lhs);
            char crhs = line.charAt(col + lhs);
            if (clhs != crhs)
               break;
         }
      }

      // Remove any overlap from the end of the completion.
      // Only do this part for single-line completions.
      int rhs = 0;
      if (completion.range.start.line == completion.range.end.line)
      {
         int row = completion.range.end.line;
         int col = completion.range.end.character;
         String line = display_.getLine(row);

         for (; col - rhs > 0; rhs++)
         {
            char clhs = completion.insertText.charAt(completion.insertText.length() - rhs - 1);
            char crhs = line.charAt(col - rhs - 1);
            if (clhs != crhs)
               break;
         }
      }

      CopilotCompletion normalized = JsUtil.clone(completion);
      normalized.insertText = normalized.insertText.substring(lhs, normalized.insertText.length() - rhs);
      normalized.range.start.character += lhs;
      normalized.range.end.character -= rhs;
      return normalized;
   }

   private void temporarilySuspendCompletionRequests()
   {
      completionRequestsSuspended_ = true;
      suspendTimer_.schedule(1200);
   }

   @Inject
   private void initialize(Copilot copilot,
                           EventBus events,
                           UserPrefs prefs,
                           Commands commands,
                           CopilotCommandBinder binder,
                           CopilotServerOperations server)
   {
      copilot_ = copilot;
      events_ = events;
      prefs_ = prefs;
      commands_ = commands;
      binder_ = binder;
      server_ = server;
   }
   
   private final TextEditingTarget target_;
   private final DocDisplay display_;
   private final Timer completionTimer_;
   private final Timer suspendTimer_;
   private final Timer nesTimer_;
   private int nesId_ = 0;
   private boolean completionTriggeredByCommand_ = false;
   private final HandlerRegistrations registrations_;
   
   private int requestId_;
   private boolean completionRequestsSuspended_;
   private boolean copilotDisabledInThisDocument_;

   private AceEditorDiffView diffView_;
   private PinnedLineWidget diffWidget_;
   private int diffMarkerId_ = -1;
   private boolean canAcceptSuggestionWithTab_ = false;
   private Completion activeCompletion_;
   private boolean automaticCodeSuggestionsEnabled_ = true;


   // Injected ----
   private Copilot copilot_;
   private EventBus events_;
   private UserPrefs prefs_;
   private Commands commands_;
   private CopilotCommandBinder binder_;
   private CopilotServerOperations server_;
}
