/*
 * Shell.java
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

package org.rstudio.studio.client.workbench.views.console.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.CommandLineHistory;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.debugging.ErrorManager;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.events.*;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.HistoryCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;

import java.util.ArrayList;

public class Shell implements ConsoleInputHandler,
                              ConsoleWriteOutputHandler,
                              ConsoleWriteErrorHandler,
                              ConsoleWritePromptHandler,
                              ConsoleWriteInputHandler,
                              ConsolePromptHandler,
                              ConsoleResetHistoryHandler,
                              ConsoleRestartRCompletedEvent.Handler,
                              ConsoleExecutePendingInputEvent.Handler,
                              SendToConsoleHandler,
                              DebugModeChangedEvent.Handler
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
                Display display,
                Session session,
                GlobalDisplay globalDisplay,
                Commands commands,
                UIPrefs uiPrefs, 
                ErrorManager errorManager)
   {
      super() ;

      ((Binder)GWT.create(Binder.class)).bind(commands, this);
      
      server_ = server ;
      eventBus_ = eventBus ;
      view_ = display ;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      errorManager_ = errorManager;
      input_ = view_.getInputEditorDisplay() ;
      historyManager_ = new CommandLineHistory(input_);

      inputAnimator_ = new ShellInputAnimator(view_.getInputEditorDisplay());
      
      view_.setMaxOutputLines(session.getSessionInfo().getConsoleActionsLimit());

      keyDownPreviewHandlers_ = new ArrayList<KeyDownPreviewHandler>() ;
      keyPressPreviewHandlers_ = new ArrayList<KeyPressPreviewHandler>() ;

      InputKeyDownHandler handler = new InputKeyDownHandler() ;
      // This needs to be a capturing key down handler or else Ace will have
      // handled the event before we had a chance to prevent it
      view_.addCapturingKeyDownHandler(handler) ;
      view_.addKeyPressHandler(handler) ;
      
      eventBus.addHandler(ConsoleInputEvent.TYPE, this); 
      eventBus.addHandler(ConsoleWriteOutputEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteErrorEvent.TYPE, this);
      eventBus.addHandler(ConsoleWritePromptEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteInputEvent.TYPE, this);
      eventBus.addHandler(ConsolePromptEvent.TYPE, this);
      eventBus.addHandler(ConsoleResetHistoryEvent.TYPE, this);
      eventBus.addHandler(ConsoleRestartRCompletedEvent.TYPE, this);
      eventBus.addHandler(ConsoleExecutePendingInputEvent.TYPE, this);
      eventBus.addHandler(SendToConsoleEvent.TYPE, this);
      eventBus.addHandler(DebugModeChangedEvent.TYPE, this);
      
      final CompletionManager completionManager
                  = new RCompletionManager(view_.getInputEditorDisplay(),
                                          null,
                                          new CompletionPopupPanel(), 
                                          server, 
                                          null,
                                          null) ;
      addKeyDownPreviewHandler(completionManager) ;
      addKeyPressPreviewHandler(completionManager) ;
      
      addKeyDownPreviewHandler(new HistoryCompletionManager(
            view_.getInputEditorDisplay(), server));

      uiPrefs.insertMatching().bind(new CommandWithArg<Boolean>() {
         public void execute(Boolean arg) {
            AceEditorNative.setInsertMatching(arg);
         }});

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
      return view_ ;
   }

   @Handler
   void onConsoleClear()
   {
      // clear output
      view_.clearOutput();
      
      // notify server
      server_.resetConsoleActions(new VoidServerRequestCallback());
      
      // if we don't bounce setFocus the menu retains focus
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
         public void execute()
         {
            view_.getInputEditorDisplay().setFocus(true);
         }
      });

   }

   public void addKeyDownPreviewHandler(KeyDownPreviewHandler handler)
   {
      keyDownPreviewHandlers_.add(handler) ;
   }
   
   public void addKeyPressPreviewHandler(KeyPressPreviewHandler handler)
   {
      keyPressPreviewHandlers_.add(handler) ;
   }
   
   public void onConsoleInput(final ConsoleInputEvent event)
   {
      server_.consoleInput(event.getInput(), 
                           new ServerRequestCallback<Void>() {
         @Override
         public void onError(ServerError error) 
         {
            // show the error in the console then re-prompt
            view_.consoleWriteError(
                  "Error: " + error.getUserMessage() + "\n");
            if (lastPromptText_ != null)
               consolePrompt(lastPromptText_, false);
         }
      });
   }

   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      view_.consoleWriteOutput(event.getOutput()) ;
   }

   public void onConsoleWriteError(ConsoleWriteErrorEvent event)
   {
      UnhandledError err = errorManager_.getLastError();
      if (err.getErrorMessage().equals(event.getError()))
      {
         view_.consoleWriteExtendedError(event.getError(), err);
      }
      else
      {
         view_.consoleWriteError(event.getError());
      }
   }
   
   public void onConsoleWriteInput(ConsoleWriteInputEvent event)
   {
      view_.consoleWriteInput(event.getInput());
   }

   public void onConsoleWritePrompt(ConsoleWritePromptEvent event)
   {
      view_.consoleWritePrompt(event.getPrompt());
   }

   public void onConsolePrompt(ConsolePromptEvent event)
   {
      String prompt = event.getPrompt().getPromptText() ;
      boolean addToHistory = event.getPrompt().getAddToHistory() ;
      
      consolePrompt(prompt, addToHistory) ;
   }

   private void consolePrompt(String prompt, boolean addToHistory)
   {
      view_.consolePrompt(prompt, true) ;

      if (lastPromptText_ == null
            && initialInput_ != null
            && initialInput_.length() > 0)
      {
         view_.getInputEditorDisplay().setText(initialInput_);
         view_.ensureInputVisible();
      }

      addToHistory_ = addToHistory;
      historyManager_.resetPosition();
      lastPromptText_ = prompt ;

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
      String commandText = view_.processCommandEntry() ;
      if (addToHistory_ && (commandText.length() > 0))
         historyManager_.addToHistory(commandText);

      // fire event 
      eventBus_.fireEvent(new ConsoleInputEvent(commandText));
   }

   public void onSendToConsole(final SendToConsoleEvent event)
   {  
      final InputEditorDisplay display = view_.getInputEditorDisplay();
      
      // define code block we execute at finish
      Command finishSendToConsole = new Command() {
         @Override
         public void execute()
         {
            if (event.shouldExecute())
               processCommandEntry();
            
            if (!event.shouldExecute() || event.shouldFocus())
            {
               display.setFocus(true);
               display.collapseSelection(false);
            }  
         }
      };
      
      // do standrd finish if we aren't animating
      if (!event.shouldAnimate())
      {
         display.clear();
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
         commands_.executeCodeWithoutFocus().execute();
      }
   }
   
   @Override
   public void onDebugModeChanged(DebugModeChangedEvent event)
   {
      if (event.debugging())
      {
         view_.ensureInputVisible();
      }
   }

   private final class InputKeyDownHandler implements KeyDownHandler,
                                                      KeyPressHandler
   {
      public void onKeyDown(KeyDownEvent event)
      {
         int keyCode = event.getNativeKeyCode();

         for (KeyDownPreviewHandler handler : keyDownPreviewHandlers_)
         {
            if (handler.previewKeyDown(event.getNativeEvent()))
            {
               event.preventDefault() ;
               event.stopPropagation() ;
               return;
            }
         }
         
         if (event.getNativeKeyCode() == KeyCodes.KEY_TAB)
            event.preventDefault();

         int modifiers = KeyboardShortcut.getModifierValue(event.getNativeEvent());

         if (event.isUpArrow() && modifiers == 0)
         {
            if ((input_.getCurrentLineNum() == 0) || input_.isCursorAtEnd())
            {
               event.preventDefault();
               event.stopPropagation();

               navigateHistory(-1);
            }
         }
         else if (event.isDownArrow() && modifiers == 0)
         {
            if ((input_.getCurrentLineNum() == input_.getCurrentLineCount() - 1)
                || input_.isCursorAtEnd())
            {
               event.preventDefault();
               event.stopPropagation();

               navigateHistory(1);
            }
         }
         else if (keyCode == KeyCodes.KEY_ENTER && modifiers == 0)
         {
            event.preventDefault();
            event.stopPropagation();

            restoreFocus_ = true;
            processCommandEntry();
         }
         else if (keyCode == KeyCodes.KEY_ESCAPE && modifiers == 0)
         {
            event.preventDefault();

            if (input_.getText().length() == 0)
            {
               // view_.isPromptEmpty() is to check for cases where the
               // server is prompting but not at the top level. Escape
               // needs to send null in those cases.
               // For example, try "scan()" function
               if (view_.isPromptEmpty())
               {
                  // interrupt server
                  server_.interrupt(new VoidServerRequestCallback() {
                     @Override
                     public void onError(ServerError error)
                     {
                        super.onError(error);
                        globalDisplay_.showErrorMessage(
                              "Error Interrupting Server",
                              error.getUserMessage());
                     }
                  });
               }
               else
               {
                  // if the input is already empty then send a console reset
                  // which will jump us back to the main prompt
                  eventBus_.fireEvent(new ConsoleInputEvent(null));
               }
            }
             
            input_.clear();
         }
         else
         {
            int mod = KeyboardShortcut.getModifierValue(event.getNativeEvent());
            if (mod == KeyboardShortcut.CTRL)
            {
               switch (keyCode)
               {
                  case 'L':
                     Shell.this.onConsoleClear() ;
                     event.preventDefault() ;
                     break;
               }
            }
            else if (mod == KeyboardShortcut.ALT)
            {
               switch (keyCode)
               {
                  case 189: // hyphen
                     event.preventDefault();
                     event.stopPropagation();
                     input_.replaceSelection(" <- ", true);
                     break;
               }
            }
         }
      }

      public void onKeyPress(KeyPressEvent event)
      {
         for (KeyPressPreviewHandler handler : keyPressPreviewHandlers_)
         {
            if (handler.previewKeyPress(event.getCharCode()))
            {
               event.preventDefault() ;
               event.stopPropagation() ;
               return;
            }
         }
      }

      @SuppressWarnings("unused")
      private boolean lastKeyCodeWasZero_;
   }

   private void navigateHistory(int offset)
   {
      historyManager_.navigateHistory(offset);
      view_.ensureInputVisible();
   }

   public void focus()
   {
      input_.setFocus(true);
   }
   
   private void setHistory(JsArrayString history)
   {
      ArrayList<String> historyList = new ArrayList<String>(history.length());
      for (int i = 0; i < history.length(); i++)
         historyList.add(history.get(i));
      historyManager_.setHistory(historyList);
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

   private final ConsoleServerOperations server_ ;
   private final EventBus eventBus_ ;
   private final Display view_ ;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final ErrorManager errorManager_;
   private final InputEditorDisplay input_ ;
   private final ArrayList<KeyDownPreviewHandler> keyDownPreviewHandlers_ ;
   private final ArrayList<KeyPressPreviewHandler> keyPressPreviewHandlers_ ;
   // indicates whether the next command should be added to history
   private boolean addToHistory_ ;
   private String lastPromptText_ ;

   private final CommandLineHistory historyManager_;
   
   private final ShellInputAnimator inputAnimator_;

   private String initialInput_ ;

   private static final String GROUP_CONSOLE = "console";
   private static final String STATE_INPUT = "input";

   private boolean restoreFocus_ = true;
}
