/*
 * Shell.java
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

package org.rstudio.studio.client.workbench.views.console.shell;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.CommandLineHistory;
import org.rstudio.studio.client.common.debugging.ErrorManager;
import org.rstudio.studio.client.common.debugging.events.UnhandledErrorEvent;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.rmarkdown.events.ChunkExecStateChangedEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookDocQueue;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.ConsoleEditorProvider;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleExecutePendingInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleHistoryAddedEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleResetHistoryEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleRestartRCompletedEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWritePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.RunCommandWithDebugEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.HistoryCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.events.SuppressNextShellFocusEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.history.events.HistoryEntriesAddedEvent;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.source.SourceSatellite;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.EditorBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class Shell implements ConsoleHistoryAddedEvent.Handler,
                              ConsoleInputEvent.Handler,
                              ConsoleWriteOutputEvent.Handler,
                              ConsoleWriteErrorEvent.Handler,
                              ConsoleWritePromptEvent.Handler,
                              ConsoleWriteInputEvent.Handler,
                              ConsolePromptEvent.Handler,
                              ConsoleResetHistoryEvent.Handler,
                              ConsoleRestartRCompletedEvent.Handler,
                              ConsoleExecutePendingInputEvent.Handler,
                              ChunkExecStateChangedEvent.Handler,
                              SendToConsoleEvent.Handler,
                              DebugModeChangedEvent.Handler,
                              RunCommandWithDebugEvent.Handler,
                              UnhandledErrorEvent.Handler,
                              SuppressNextShellFocusEvent.Handler,
                              RestartStatusEvent.Handler,
                              HistoryEntriesAddedEvent.Handler
                              
{
   static interface Binder extends CommandBinder<Commands, Shell>
   {
   }

   public interface Display extends ShellDisplay
   {
      void onBeforeUnselected();
      void onBeforeSelected();
      void onSelected();
   }

   @Inject
   public Shell(ConsoleServerOperations server,
                EventBus eventBus,
                AriaLiveService ariaLive,
                Display display,
                Session session,
                EventBus events,
                Commands commands,
                UserPrefs uiPrefs,
                ErrorManager errorManager,
                DependencyManager dependencyManager,
                ConsoleEditorProvider editorProvider,
                ConsoleLanguageTracker languageTracker)
   {
      super();

      ((Binder)GWT.create(Binder.class)).bind(commands, this);

      server_ = server;
      eventBus_ = eventBus;
      session_ = session;
      ariaLive_ = ariaLive;
      view_ = display;
      commands_ = commands;
      errorManager_ = errorManager;
      dependencyManager_ = dependencyManager;
      input_ = view_.getInputEditorDisplay();
      historyManager_ = new CommandLineHistory(input_);
      browseHistoryManager_ = new CommandLineHistory(input_);
      prefs_ = uiPrefs;
      languageTracker_ = languageTracker;

      editorProvider.setConsoleEditor(input_);

      configureLiveAnnouncements();

      prefs_.surroundSelection().bind(new CommandWithArg<String>()
      {
         @Override
         public void execute(String value)
         {
            ((DocDisplay) input_).setSurroundSelectionPref(value);
         }
      });

      inputAnimator_ = new ShellInputAnimator(view_.getInputEditorDisplay());

      view_.setMaxOutputLines(session.getSessionInfo().getConsoleActionsLimit());

      keyDownPreviewHandlers_ = new ArrayList<>();
      keyPressPreviewHandlers_ = new ArrayList<>();

      InputKeyHandler handler = new InputKeyHandler();

      // This needs to be a capturing key down handler or else Ace will have
      // handled the event before we had a chance to prevent it
      view_.addCapturingKeyDownHandler(handler);
      view_.addKeyPressHandler(handler);
      view_.addCapturingKeyUpHandler(handler);

      eventBus.addHandler(ConsoleHistoryAddedEvent.TYPE, this);
      eventBus.addHandler(ConsoleInputEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteOutputEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteErrorEvent.TYPE, this);
      eventBus.addHandler(ConsoleWritePromptEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteInputEvent.TYPE, this);
      eventBus.addHandler(ConsolePromptEvent.TYPE, this);
      eventBus.addHandler(ConsoleResetHistoryEvent.TYPE, this);
      eventBus.addHandler(ConsoleRestartRCompletedEvent.TYPE, this);
      eventBus.addHandler(ConsoleExecutePendingInputEvent.TYPE, this);
      eventBus.addHandler(ChunkExecStateChangedEvent.TYPE, this);
      eventBus.addHandler(SendToConsoleEvent.TYPE, this);
      eventBus.addHandler(DebugModeChangedEvent.TYPE, this);
      eventBus.addHandler(RunCommandWithDebugEvent.TYPE, this);
      eventBus.addHandler(UnhandledErrorEvent.TYPE, this);
      eventBus.addHandler(SuppressNextShellFocusEvent.TYPE, this);
      eventBus.addHandler(RestartStatusEvent.TYPE, this);
      eventBus.addHandler(HistoryEntriesAddedEvent.TYPE, this);

      final CompletionManager completionManager = new RCompletionManager(
            view_.getInputEditorDisplay(),
            null,
            new CompletionPopupPanel(),
            server,
            null,
            null,
            null,
            (DocDisplay) view_.getInputEditorDisplay(),
            EditorBehavior.AceBehaviorConsole);
      
      addKeyDownPreviewHandler(completionManager);
      addKeyPressPreviewHandler(completionManager);

      historyCompletion_ = new HistoryCompletionManager(
            view_.getInputEditorDisplay(),
            server);
      
      addKeyDownPreviewHandler(historyCompletion_);

      // we need to explicitly connect a paste handler on Desktop
      // to ensure the completion popup is dismissed in shell on paste
      if (Desktop.isDesktop())
      {
         view_.getInputEditorDisplay().addPasteHandler(new PasteEvent.Handler()
         {
            @Override
            public void onPaste(PasteEvent event)
            {
               completionManager.onPaste(event);
            }
         });
      }

      AceEditorNative.syncUiPrefs(uiPrefs);

      sessionInit(session);
   }

   private void sessionInit(Session session)
   {
      SessionInfo sessionInfo = session.getSessionInfo();
      ClientInitState clientState = sessionInfo.getClientState();

      new StringStateValue(GROUP_CONSOLE, STATE_INPUT, ClientState.TEMPORARY, clientState) {
         @Override
         protected void onInit(String value)
         {
            initialInput_ = value;
         }
         @Override
         protected String getValue()
         {
            return view_.getInputEditorDisplay().getText();
         }
      };

      JsArrayString history = sessionInfo.getConsoleHistory();
      if (history != null)
         setHistory(history);

      RpcObjectList<ConsoleAction> actions = sessionInfo.getConsoleActions();
      if (actions != null)
      {
         view_.playbackActions(actions);
      }

      if (sessionInfo.getResumed())
      {
         // no special UI for this (resuming session with all console
         // history and other UI state preserved deemed adequate feedback)
      }
   }

   public Display getDisplay()
   {
      return view_;
   }

   @Handler
   void onConsoleClear()
   {
      // clear output
      view_.clearOutput();

      ariaLive_.announce(AriaLiveService.CONSOLE_CLEARED, constants_.consoleClearedMessage(),
            Timing.IMMEDIATE, Severity.STATUS);

      // notify server
      server_.resetConsoleActions(new VoidServerRequestCallback());

      // if we don't bounce setFocus the menu retains focus
      Scheduler.get().scheduleDeferred(() -> view_.getInputEditorDisplay().setFocus(true));

   }

   @Handler
   void onFocusConsoleOutputEnd()
   {
      if (prefs_.limitVisibleConsole().getValue())
      {
         ariaLive_.announce(AriaLiveService.INACCESSIBLE_FEATURE,
            constants_.focusConsoleWarningMessage(prefs_.limitVisibleConsole().getTitle()),
            Timing.IMMEDIATE, Severity.STATUS);
         return;
      }
      view_.getConsoleOutputWriter().focusEnd();
   }

   public void addKeyDownPreviewHandler(KeyDownPreviewHandler handler)
   {
      keyDownPreviewHandlers_.add(handler);
   }

   public void addKeyPressPreviewHandler(KeyPressPreviewHandler handler)
   {
      keyPressPreviewHandlers_.add(handler);
   }

   public void onConsoleInput(final ConsoleInputEvent event)
   {
      view_.setBusy(true);
      view_.clearLiveRegion();
      server_.consoleInput(event.getInput(),
                           event.getConsole(),
                           event.getFlags(),
                           new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void response)
         {
         }

         @Override
         public void onError(ServerError error)
         {
            // show the error in the console then re-prompt
            view_.consoleWriteError(constants_.errorString(error.getUserMessage()));
            if (lastPromptText_ != null)
               consolePrompt(lastPromptText_, false);
         }
      });
   }

   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      view_.consoleWriteOutput(event.getOutput());
   }

   public void onConsoleWriteError(final ConsoleWriteErrorEvent event)
   {
      view_.consoleWriteError(event.getError());
   }

   public void onUnhandledError(UnhandledErrorEvent event)
   {
      if (!debugging_)
      {
         view_.consoleWriteExtendedError(
               event.getError().getErrorMessage(),
               event.getError(),
               prefs_.autoExpandErrorTracebacks().getValue(),
               getHistoryEntry(0));
      }
   }

   public void onConsoleWriteInput(ConsoleWriteInputEvent event)
   {
      view_.consoleWriteInput(event.getInput(), event.getConsole());
   }

   public void onConsoleWritePrompt(ConsoleWritePromptEvent event)
   {
      view_.consoleWritePrompt(event.getPrompt());
   }

   public void onConsolePrompt(ConsolePromptEvent event)
   {
      historyCompletion_.resetOffset();
      String prompt = event.getPrompt().getPromptText();
      boolean addToHistory = event.getPrompt().getAddToHistory();
      consolePrompt(prompt, addToHistory);
   }
   
   // NOTE: 'addToHistory()' flag controls whether the next-processed
   // command entry should be added to the console history; normally,
   // it's only set to false for commands synthesized by the IDE (and
   // so not explicitly entered by the user)
   private void consolePrompt(String prompt, boolean addToHistory)
   {
      view_.setBusy(false);
      view_.consolePrompt(prompt, true);

      if (lastPromptText_ == null
            && initialInput_ != null
            && initialInput_.length() > 0)
      {
         view_.getInputEditorDisplay().setText(initialInput_);
         view_.ensureInputVisible();
      }

      addToHistory_ = addToHistory;
      resetHistoryPosition();
      lastPromptText_ = prompt;

      if (restoreFocus_)
      {
         restoreFocus_ = false;
         view_.getInputEditorDisplay().setFocus(true);
      }
   }

   public void onConsoleResetHistory(ConsoleResetHistoryEvent event)
   {
      setHistory(event.getHistory());
   }

   @Override
   public void onRestartRCompleted(ConsoleRestartRCompletedEvent event)
   {
      if (view_.isPromptEmpty())
         eventBus_.fireEvent(new SendToConsoleEvent("", true));

      focus();
   }

   private void processCommandEntry()
   {
      processCommandEntry(view_.processCommandEntry(), true);
   }

   private void processCommandEntry(String commandText, boolean echo)
   {
      // add to console history
      if (addToHistory_ && commandText.length() > 0)
         eventBus_.fireEvent(new ConsoleHistoryAddedEvent(commandText));

      // fire event
      eventBus_.fireEvent(new ConsoleInputEvent(commandText, "", echo ? 0 : ConsoleInputEvent.FLAG_NO_ECHO));
   }

   public void onSendToConsole(final SendToConsoleEvent event)
   {
      if (StringUtil.equals(event.getLanguage(), "Python"))
      {
         dependencyManager_.withReticulate(
               constants_.executingPythonCodeProgressCaption(),
               constants_.executingPythonCodeProgressCaption(),
               () -> {
                  onSendToConsoleImpl(event);
               });
      }
      else
      {
         onSendToConsoleImpl(event);
      }
   }

   private void onSendToConsoleImpl(final SendToConsoleEvent event)
   {
      String language = event.getLanguage();
      
      if (StringUtil.isNullOrEmpty(language))
      {
         sendToConsoleImpl(event);
      }
      else
      {
         languageTracker_.adaptToLanguage(
               language,
               () -> sendToConsoleImpl(event));
      }
   }

   private void sendToConsoleImpl(final SendToConsoleEvent event)
   {
      final InputEditorDisplay display = view_.getInputEditorDisplay();

      // get anything already at the console
      final String previousInput = StringUtil.notNull(display.getText());

      // define code block we execute at finish
      Command finishSendToConsole = new Command()
      {
         @Override
         public void execute()
         {
            if (event.shouldExecute())
            {
               String commandText = event.shouldEcho() ? view_.processCommandEntry() : event.getCode();
               processCommandEntry(commandText, event.shouldEcho());
               display.setText(previousInput);
            }

            if (!event.shouldExecute() || event.shouldFocus())
            {
               display.setFocus(true);
               display.collapseSelection(false);
            }
         }
      };

      // do standard finish if we aren't animating
      if (!event.shouldAnimate())
      {
         display.clear();
         if (event.shouldEcho()) 
            display.setText(event.getCode());
         finishSendToConsole.execute();
      }
      else
      {
         inputAnimator_.enque(event.getCode(), finishSendToConsole);
      }
   }

   @Override
   public void onExecutePendingInput(ConsoleExecutePendingInputEvent event)
   {
      // if the source view is delegating a Cmd+Enter to us then
      // take it if we are focused and we have a command to enter
      if (view_.getInputEditorDisplay().isFocused() &&
         (view_.getInputEditorDisplay().getText().length() > 0))
      {
         processCommandEntry();
      }
      // otherwise delegate back to the source view. we do this via
      // executing a command which is a bit of hack but it's a clean
      // way to call code within the "current editor" (an event would
      // go to all editors). another alternative would be to
      // call a method on the SourceShim
      else
      {
         AppCommand command = commands_.getCommandById(event.getCommandId());

         // the current editor may be in another window; if one of our source
         // windows was last focused, use that one instead
         SourceWindowManager manager =
               RStudioGinjector.INSTANCE.getSourceWindowManager();
         if (!StringUtil.isNullOrEmpty(manager.getLastFocusedSourceWindowId()))
         {
            RStudioGinjector.INSTANCE.getSatelliteManager().dispatchCommand(
                  command, SourceSatellite.NAME_PREFIX +
                           manager.getLastFocusedSourceWindowId());
         }
         else
         {
            command.execute();
         }
      }
   }

   @Override
   public void onDebugModeChanged(DebugModeChangedEvent event)
   {
      if (event.debugging())
      {
         view_.ensureInputVisible();
      }
      debugging_ = event.debugging();
   }

   @Override
   public void onRunCommandWithDebug(final RunCommandWithDebugEvent event)
   {
      // Invoked from the "Rerun with Debug" command in the ConsoleError widget.
      errorManager_.setDebugSessionHandlerType(
            UserState.ERROR_HANDLER_TYPE_BREAK,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void v)
               {
                  eventBus_.fireEvent(new SendToConsoleEvent(
                        event.getCommand(), true));
               }

               @Override
               public void onError(ServerError error)
               {
                  // if we failed to set debug mode, don't rerun the command
               }
            });
   }

   @Override
   public void onConsoleHistoryAdded(ConsoleHistoryAddedEvent event)
   {
      if (isBrowsePrompt())
      {
         browseHistoryManager_.addToHistory(event.getCode());
         return;
      }
      
      buffer_.add(event.getCode());
      if (bufferingInput_)
         return;
      
      flush(false);
   }
   
   private void flush(boolean resetPosition)
   {
      // extract all buffered code
      String code = StringUtil.join(buffer_, "\n");
      buffer_.clear();
      
      // add to history, and reset history position if requested
      historyManager_.addToHistory(code);
      if (resetPosition)
         historyManager_.resetPosition();
   }
   
   @Override
   public void onChunkExecStateChanged(ChunkExecStateChangedEvent event)
   {
      switch (event.getExecState())
      {
      
      case NotebookDocQueue.CHUNK_EXEC_STARTED:
      {
         bufferingInput_ = true;
         break;
      }
      
      case NotebookDocQueue.CHUNK_EXEC_FINISHED:
      case NotebookDocQueue.CHUNK_EXEC_CANCELLED:
      {
         bufferingInput_ = false;
         flush(true);
         break;
      }
      
      }
   }

   private final class InputKeyHandler implements KeyDownHandler,
                                                  KeyPressHandler,
                                                  KeyUpHandler
   {
      @Override
      public void onKeyDown(KeyDownEvent event)
      {
         int keyCode = event.getNativeKeyCode();

         // typically we allow all the handlers to process the key; however,
         // this behavior is suppressed when we're incrementally searching the
         // history so we don't stack two kinds of completion popups
         ArrayList<KeyDownPreviewHandler> handlers = keyDownPreviewHandlers_;
         if (historyCompletion_.getMode() ==
               HistoryCompletionManager.PopupMode.PopupIncremental)
         {
            handlers = new ArrayList<>();
            handlers.add(historyCompletion_);
         }

         for (KeyDownPreviewHandler handler : handlers)
         {
            if (handler.previewKeyDown(event.getNativeEvent()))
            {
               event.preventDefault();
               event.stopPropagation();
               return;
            }
         }

         if (event.getNativeKeyCode() == KeyCodes.KEY_TAB)
         {
            if (prefs_.tabKeyMoveFocus().getValue())
            {
               // allow tab to change focus instead of letting editor use for indenting
               event.stopPropagation();
               return;
            }
            else
               event.preventDefault();
         }

         int modifiers = KeyboardShortcut.getModifierValue(event.getNativeEvent());

         if ((event.isUpArrow() && modifiers == 0) ||
             (keyCode == 'P'    && modifiers == KeyboardShortcut.CTRL))
         {
            if ((input_.getCurrentLineNum() == 0) || input_.isCursorAtEnd())
            {
               event.preventDefault();
               event.stopPropagation();

               navigateHistory(-1);
            }
         }
         else if ((event.isDownArrow() && modifiers == 0) ||
                  (keyCode == 'N'      && modifiers == KeyboardShortcut.CTRL))
         {
            if ((input_.getCurrentLineNum() == input_.getCurrentLineCount() - 1)
                || input_.isCursorAtEnd())
            {
               event.preventDefault();
               event.stopPropagation();

               navigateHistory(1);
            }
         }
         else if (keyCode == KeyCodes.KEY_ENTER && (
                     modifiers == 0 ||
                     modifiers == KeyboardShortcut.CTRL ||
                     modifiers == KeyboardShortcut.META))
         {
            event.preventDefault();
            event.stopPropagation();

            restoreFocus_ = true;
            processCommandEntry();
         }
         else if (
               (keyCode == KeyCodes.KEY_ESCAPE &&
                     (modifiers == 0 || modifiers == KeyboardShortcut.CTRL /*iPadOS 13.1*/)) ||
               (BrowseCap.isMacintoshDesktop() && (
                     modifiers == KeyboardShortcut.CTRL &&
                     keyCode == KeyCodes.KEY_C)))
         {
            event.preventDefault();

            if (input_.getText().length() == 0)
            {
               // interrupt server
               server_.interrupt(new ServerRequestCallback<Boolean>()
               {

                  @Override
                  public void onResponseReceived(Boolean busy)
                  {
                     // if the session was not busy, then we should
                     // send a console cancel
                     if (!busy)
                     {
                        eventBus_.fireEvent(
                              new ConsoleInputEvent(ConsoleInputEvent.FLAG_CANCEL));
                     }
                        
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                  }
               });
            }

            input_.clear();
         }
         else if (keyCode == KeyCodes.KEY_D &&
                  modifiers == KeyboardShortcut.CTRL &&
                  input_.getText().length() == 0)
         {
            event.stopPropagation();
            event.preventDefault();
            
            eventBus_.fireEvent(
                  new ConsoleInputEvent(ConsoleInputEvent.FLAG_EOF));
         }
         else if (keyCode == KeyCodes.KEY_L &&
                  modifiers == KeyboardShortcut.CTRL)
         {
            event.stopPropagation();
            event.preventDefault();
            
            Shell.this.onConsoleClear();
         }
      }

      @Override
      public void onKeyPress(KeyPressEvent event)
      {
         // typically we allow all the handlers to process the key; however,
         // this behavior is suppressed when we're incrementally searching the
         // history so we don't stack two kinds of completion popups
         ArrayList<KeyPressPreviewHandler> handlers = keyPressPreviewHandlers_;
         if (historyCompletion_.getMode() ==
               HistoryCompletionManager.PopupMode.PopupIncremental)
         {
            handlers = new ArrayList<>();
            handlers.add(historyCompletion_);
         }

         for (KeyPressPreviewHandler handler : handlers)
         {
            if (handler.previewKeyPress(event.getCharCode()))
            {
               event.preventDefault();
               event.stopPropagation();
               return;
            }
         }
      }

      @Override
      public void onKeyUp(KeyUpEvent event)
      {
         if (event.isAnyModifierKeyDown())
            return;
         if (KeyCodeEvent.isArrow(event.getNativeKeyCode()))
            return;
         if (historyCompletion_.getMode() ==
               HistoryCompletionManager.PopupMode.PopupIncremental)
         {
            historyCompletion_.beginSearch();
         }
      }

      @SuppressWarnings("unused")
      private boolean lastKeyCodeWasZero_;
   }
   
   @Override
   public void onSuppressNextShellFocus(SuppressNextShellFocusEvent event)
   {
      restoreFocus_ = false;
   }
   
   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      if (event.getStatus() == RestartStatusEvent.RESTART_COMPLETED)
      {
         SessionInfo info = session_.getSessionInfo();
         String prompt = info.getPrompt();
         consolePrompt(prompt, true);
      }
   }
   
   private boolean isBrowsePrompt()
   {
      return lastPromptText_ != null && (lastPromptText_.startsWith("Browse"));
   }
   
   public void onHistoryEntriesAdded(HistoryEntriesAddedEvent event)
   {
      if (event.update())
      {
         RpcObjectList<HistoryEntry> entries = event.getEntries();
         for (HistoryEntry entry : entries.toArrayList())
         {
            historyManager_.addToHistory(entry.getCommand());
         }
      }
   }

   private void resetHistoryPosition()
   {
      historyManager_.resetPosition();
      browseHistoryManager_.resetPosition();
   }

   private String getHistoryEntry(int offset)
   {
      if (isBrowsePrompt())
         return browseHistoryManager_.getHistoryEntry(offset);
      else
         return historyManager_.getHistoryEntry(offset);
   }

   private void navigateHistory(int offset)
   {
      if (isBrowsePrompt())
      {
         browseHistoryManager_.navigateHistory(offset);
      }
      else
      {
         if (input_.isCursorAtEnd())
            historyManager_.navigateHistory(offset);
         else
            historyCompletion_.navigatePrefix(offset);
      }

      view_.ensureInputVisible();
   }

   public void focus()
   {
      input_.setFocus(true);
   }

   private void setHistory(JsArrayString history)
   {
      ArrayList<String> historyList = new ArrayList<>(history.length());
      for (int i = 0; i < history.length(); i++)
         historyList.add(history.get(i));
      historyManager_.setHistory(historyList);
      browseHistoryManager_.resetPosition();
   }

   public void onBeforeUnselected()
   {
      view_.onBeforeUnselected();
   }

   public void onBeforeSelected()
   {
      view_.onBeforeSelected();
   }

   public void onSelected()
   {
      view_.onSelected();
   }

   private void configureLiveAnnouncements()
   {
      if (prefs_.enableScreenReader().getValue() &&
            (!ariaLive_.isDisabled(AriaLiveService.CONSOLE_LOG) ||
             !ariaLive_.isDisabled(AriaLiveService.CONSOLE_COMMAND)))
      {
         view_.enableLiveReporting();
      }
   }
   

   private final ConsoleServerOperations server_;
   private final EventBus eventBus_;
   private final Session session_;
   private final AriaLiveService ariaLive_;
   private final Display view_;
   private final Commands commands_;
   private final ErrorManager errorManager_;
   private final DependencyManager dependencyManager_;
   private final InputEditorDisplay input_;
   private final ArrayList<KeyDownPreviewHandler> keyDownPreviewHandlers_;
   private final ArrayList<KeyPressPreviewHandler> keyPressPreviewHandlers_;
   private final HistoryCompletionManager historyCompletion_;

   // indicates whether the next command should be added to history
   private boolean addToHistory_;
   private String lastPromptText_;
   private final UserPrefs prefs_;

   private final ConsoleLanguageTracker languageTracker_;

   private final CommandLineHistory historyManager_;
   private final CommandLineHistory browseHistoryManager_;

   private final ShellInputAnimator inputAnimator_;

   private String initialInput_;

   private static final String GROUP_CONSOLE = "console";
   private static final String STATE_INPUT = "input";

   private List<String> buffer_ = new ArrayList<String>();
   private boolean bufferingInput_ = false;
   private boolean restoreFocus_ = true;
   private boolean debugging_ = false;
   private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);
}
