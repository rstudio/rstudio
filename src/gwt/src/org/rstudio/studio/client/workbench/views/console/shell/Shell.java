/*
 * Shell.java
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

package org.rstudio.studio.client.workbench.views.console.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.inject.Inject;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.views.console.events.*;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.HistoryCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorUtil;

import java.util.ArrayList;

public class Shell implements ConsoleInputHandler,
                              ConsoleWriteOutputHandler,
                              ConsoleWriteErrorHandler,
                              ConsolePromptHandler,
                              ConsoleResetHistoryHandler,
                              SendToConsoleHandler,
                              BusyHandler
{
   static interface Binder extends CommandBinder<Commands, Shell>
   {
   }

   public interface Display extends HasKeyDownHandlers, 
                                    HasKeyPressHandlers
   {
      void consoleError(String string) ;
      void consoleOutput(String output) ;
      void consolePrompt(String prompt) ;
      void ensureInputVisible() ;
      InputEditorDisplay getInputEditorDisplay() ;
      void clearOutput() ;
      String processCommandEntry() ;
      void setFocus(boolean focused) ;
      int getCharacterWidth() ;
      boolean isPromptEmpty();

      void playbackActions(RpcObjectList<ConsoleAction> actions);

      void setMaxOutputLines(int maxLines);
   }

   @Inject
   public Shell(ConsoleServerOperations server, 
                EventBus eventBus,
                Display display,
                Session session,
                GlobalDisplay globalDisplay,
                Commands commands)
   {
      super() ;

      ((Binder)GWT.create(Binder.class)).bind(commands, this);
      
      server_ = server ;
      eventBus_ = eventBus ;
      view_ = display ;
      globalDisplay_ = globalDisplay;
      input_ = view_.getInputEditorDisplay() ;

      view_.setMaxOutputLines(session.getSessionInfo().getConsoleActionsLimit());

      keyDownPreviewHandlers_ = new ArrayList<KeyDownPreviewHandler>() ;
      keyPressPreviewHandlers_ = new ArrayList<KeyPressPreviewHandler>() ;

      InputKeyDownHandler handler = new InputKeyDownHandler() ;
      view_.addKeyDownHandler(handler) ;
      view_.addKeyPressHandler(handler) ;
      
      eventBus.addHandler(ConsoleInputEvent.TYPE, this); 
      eventBus.addHandler(ConsoleWriteOutputEvent.TYPE, this);
      eventBus.addHandler(ConsoleWriteErrorEvent.TYPE, this);
      eventBus.addHandler(ConsolePromptEvent.TYPE, this);
      eventBus.addHandler(ConsoleResetHistoryEvent.TYPE, this);
      eventBus.addHandler(SendToConsoleEvent.TYPE, this);
      eventBus.addHandler(BusyEvent.TYPE, this);
      
      final CompletionManager completionManager
                  = new RCompletionManager(view_.getInputEditorDisplay(),
                                          new CompletionPopupPanel(), 
                                          server, 
                                          null) ;
      addKeyDownPreviewHandler(completionManager) ;
      addKeyPressPreviewHandler(completionManager) ;

      addKeyDownPreviewHandler(new HistoryCompletionManager(
            view_.getInputEditorDisplay(), server));

      sessionInit(session);
   }
   
   private void sessionInit(Session session)
   {
      SessionInfo sessionInfo = session.getSessionInfo();
      ClientInitState clientState = sessionInfo.getClientState();

      new StringStateValue(GROUP_CONSOLE, STATE_INPUT, false, clientState) {
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
      DeferredCommand.addCommand(new Command() {
         public void execute()
         {
            view_.getInputEditorDisplay().setFocus(true);
         }
      });

   }

   
   @Handler
   void onInterruptR()
   {
      server_.interrupt(new VoidServerRequestCallback());
   }

   public void addKeyDownPreviewHandler(KeyDownPreviewHandler handler)
   {
      keyDownPreviewHandlers_.add(handler) ;
   }
   
   public void addKeyPressPreviewHandler(KeyPressPreviewHandler handler)
   {
      keyPressPreviewHandlers_.add(handler) ;
   }
   
   public void onConsoleInput(ConsoleInputEvent event)
   {
      server_.consoleInput(event.getInput(), 
                           new ServerRequestCallback<Void>() { 
         public void onError(ServerError error) 
         {
            // show the error in the console then re-prompt
            view_.consoleError("Error: " + error.getUserMessage() + "\n");
            if (lastPromptText_ != null)
               consolePrompt(lastPromptText_, false);
         }
      });
   }

   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      view_.consoleOutput(event.getOutput()) ;
   }

   public void onConsoleWriteError(ConsoleWriteErrorEvent event)
   {
      view_.consoleError(event.getError()) ;
   }
   
   public void onConsolePrompt(ConsolePromptEvent event)
   {
      String prompt = event.getPrompt().getPromptText() ;
      boolean addToHistory = event.getPrompt().getAddToHistory() ;
      
      consolePrompt(prompt, addToHistory) ;
   }

   private void consolePrompt(String prompt, boolean addToHistory)
   {
      view_.consolePrompt(prompt) ;

      if (lastPromptText_ == null
            && initialInput_ != null
            && initialInput_.length() > 0)
      {
         view_.getInputEditorDisplay().setText(initialInput_);
         view_.ensureInputVisible();
      }

      addToHistory_ = addToHistory;
      historyPos_ = history_.size();
      historyTail_ = "";
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

   private void processCommandEntry()
   {
      String commandText = view_.processCommandEntry() ;
      if (addToHistory_ && (commandText.length() > 0))
         addToHistory(commandText);

      // fire event 
      eventBus_.fireEvent(new ConsoleInputEvent(commandText));
   }

   private void addToHistory(String command)
   {
      if (command == null)
         return;

      if (history_.size() > 0
          && command.equals(history_.get(history_.size() - 1)))
      {
         // do not allow dupes
         return;
      }

      history_.add(command);
   }

   public void onSendToConsole(SendToConsoleEvent event)
   {
      InputEditorDisplay display = view_.getInputEditorDisplay();
      display.clear();
      display.setText(event.getCode());
      if (event.shouldExecute())
         processCommandEntry();
      else
      {
         display.setFocus(true);
         display.collapseSelection(false);
      }
   }

   public void onBusy(BusyEvent event)
   {
      serverIsBusy_ = event.isBusy();
   }

   private final class InputKeyDownHandler implements KeyDownHandler,
                                                      KeyPressHandler
   {
      public void onKeyDown(KeyDownEvent event)
      {
         int keyCode = event.getNativeKeyCode();

         for (KeyDownPreviewHandler handler : keyDownPreviewHandlers_)
         {
            if (handler.previewKeyDown(event))
            {
               event.preventDefault() ;
               event.stopPropagation() ;
               return;
            }
         }
         
         if (event.getNativeKeyCode() == KeyCodes.KEY_TAB)
            event.preventDefault();

         int modifiers = KeyboardShortcut.getModifierValue(event.getNativeEvent());
         
         if (event.isUpArrow())
         {
            event.preventDefault();
            event.stopPropagation();

            navigateHistory(-1);
         }
         else if (event.isDownArrow())
         {
            event.preventDefault();
            event.stopPropagation();
            
            navigateHistory(1);
         }
         else if (keyCode == KeyCodes.KEY_ENTER)
         {
            event.preventDefault();
            event.stopPropagation();

            restoreFocus_ = true;
            processCommandEntry();
         }
         else if (keyCode == KeyCodes.KEY_ESCAPE)
         {
            if (input_.getText().length() == 0)
            {
               // view_.isPromptEmpty() is to check for cases where the
               // server is prompting but not at the top level. Escape
               // needs to send null in those cases.
               // For example, try "scan()" function
               if (serverIsBusy_ && view_.isPromptEmpty())
               {
                  // interrupt server
                  server_.interrupt(new VoidServerRequestCallback() {
                     @Override
                     public void onError(ServerError error)
                     {
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
         else if (BrowseCap.INSTANCE.emulatedHomeAndEnd()
                  && keyCode == KeyCodes.KEY_HOME
                  && modifiers == KeyboardShortcut.NONE)
         {
            InputEditorUtil.moveSelectionToLineStart(input_);
         }
         else if (BrowseCap.INSTANCE.emulatedHomeAndEnd()
                  && keyCode == KeyCodes.KEY_HOME
                  && modifiers == KeyboardShortcut.SHIFT)
         {
            InputEditorUtil.extendSelectionToLineStart(input_);
         }
         else if (BrowseCap.INSTANCE.emulatedHomeAndEnd()
                  && keyCode == KeyCodes.KEY_END
                  && modifiers == KeyboardShortcut.NONE)
         {
            InputEditorUtil.moveSelectionToLineEnd(input_);
         }
         else if (BrowseCap.INSTANCE.emulatedHomeAndEnd()
                  && keyCode == KeyCodes.KEY_END
                  && modifiers == KeyboardShortcut.SHIFT)
         {
            InputEditorUtil.extendSelectionToLineEnd(input_);
         }
         else if (!event.isAltKeyDown()
               && !event.isShiftKeyDown() 
               && !event.isMetaKeyDown()
               && event.isControlKeyDown())
         {
            switch (keyCode)
            {
            case 'L':
               Shell.this.onConsoleClear() ;
               event.preventDefault() ;
               break;
            case 'U':
               event.preventDefault() ;
               InputEditorUtil.yankBeforeCursor(input_, true);
               break;
            case 'K':
               event.preventDefault();
               InputEditorUtil.yankAfterCursor(input_, true);
               break;
            case 'Y':
               event.preventDefault();
               InputEditorUtil.pasteYanked(input_);
               break;
            case 'A':
               event.preventDefault();
               InputEditorUtil.moveSelectionToLineStart(input_);
               break;
            case 'E':
               event.preventDefault();
               InputEditorUtil.moveSelectionToLineEnd(input_);
               break;
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
   }

   private void navigateHistory(int offset)
   {
      int newPos = historyPos_ + offset;

      newPos = Math.max(0, Math.min(newPos, history_.size()));

      if (newPos == historyPos_)
         return; // no-op due to boundary limits

      if (historyPos_ == history_.size())
      {
         historyTail_ = input_.getText();
      }

      input_.setText(newPos < history_.size() ? history_.get(newPos)
                     : historyTail_ != null ? historyTail_
                     : "");
      historyPos_ = newPos;

      view_.ensureInputVisible();
   }

   public void focus()
   {
      input_.setFocus(true);
   }
   
   private void setHistory(JsArrayString history)
   {
      history_.clear();
      for (int i = 0; i < history.length(); i++)
         addToHistory(history.get(i));
      historyPos_ = history_.size();
      historyTail_ = "";
   }

   private final ConsoleServerOperations server_ ;
   private final EventBus eventBus_ ;
   private final Display view_ ;
   private final GlobalDisplay globalDisplay_;
   private final InputEditorDisplay input_ ;
   private final ArrayList<KeyDownPreviewHandler> keyDownPreviewHandlers_ ;
   private final ArrayList<KeyPressPreviewHandler> keyPressPreviewHandlers_ ;
   // indicates whether the next command should be added to history
   private boolean addToHistory_ ;
   private final ArrayList<String> history_ = new ArrayList<String>() ;
   private int historyPos_ ;
   // If you start typing a command, then go up in history, then go down,
   // then what you had previously typed should still be there. This is
   // that value--it is loaded/saved whenever history nagivation takes you
   // into/out of that final history position (history_.size()).
   private String historyTail_;
   private String lastPromptText_ ;

   private String initialInput_ ;

   private boolean serverIsBusy_ ;

   private static final String GROUP_CONSOLE = "console";
   private static final String STATE_INPUT = "input";

   private boolean restoreFocus_ = true;
}
