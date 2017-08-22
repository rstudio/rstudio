/*
 * TerminalPane.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.terminal;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.events.SessionSerializationHandler;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.console.ServerProcessExitEvent;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.NewWorkingCopyEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.SwitchToTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSubprocEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalTitleEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * Holds the contents of the Terminal pane, including the toolbar and
 * zero or more Terminal instances, of which only one is visible at a time.
 * 
 * The toolbar has a dropdown menu, which owns the list of terminals and
 * their handles, and other metadata.
 * 
 * As each terminal is selected from the dropdown, a new TerminalSession
 * widget is created, hooked to a server-side terminal via ConsoleProcess,
 * and added to the pane via a DeckLayoutPanel. Once loaded the widgets
 * stay loaded and are shown/hidden by the DeskLayoutPanel.
 */
public class TerminalPane extends WorkbenchPane
                          implements TerminalTabPresenter.Display,
                                     TerminalSessionStartedEvent.Handler,
                                     TerminalSessionStoppedEvent.Handler,
                                     ServerProcessExitEvent.Handler,
                                     SwitchToTerminalEvent.Handler,
                                     TerminalTitleEvent.Handler,
                                     SessionSerializationHandler,
                                     TerminalSubprocEvent.Handler
{
   @Inject
   protected TerminalPane(EventBus events,
                          GlobalDisplay globalDisplay,
                          Commands commands,
                          UIPrefs uiPrefs,
                          WorkbenchServerOperations server)
   {
      super("Terminal");
      events_ = events;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      uiPrefs_ = uiPrefs;
      server_ = server;
      events_.addHandler(TerminalSessionStartedEvent.TYPE, this);
      events_.addHandler(TerminalSessionStoppedEvent.TYPE, this);
      events_.addHandler(ServerProcessExitEvent.TYPE, this);
      events_.addHandler(SwitchToTerminalEvent.TYPE, this);
      events_.addHandler(TerminalTitleEvent.TYPE, this);
      events_.addHandler(SessionSerializationEvent.TYPE, this);
      events_.addHandler(TerminalSubprocEvent.TYPE, this);

      events.addHandler(RestartStatusEvent.TYPE, 
                          new RestartStatusEvent.Handler()
      {
         @Override
         public void onRestartStatus(RestartStatusEvent event)
         {
            if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED)
            {
               isRestartInProgress_ = true;
            }
            else if (event.getStatus() == RestartStatusEvent.RESTART_COMPLETED)
            {
               isRestartInProgress_ = false;
            }
         }
      });
 
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      terminalSessionsPanel_ = new DeckLayoutPanel();
      terminalSessionsPanel_.getElement().addClassName("ace_editor");
      return terminalSessionsPanel_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();

      toolbar.addLeftWidget(commands_.previousTerminal().createToolbarButton());
      toolbar.addLeftWidget(commands_.nextTerminal().createToolbarButton());
      toolbar.addLeftSeparator();
      activeTerminalToolbarButton_ = new TerminalPopupMenu(terminals_);
      toolbar.addLeftWidget(activeTerminalToolbarButton_.getToolbarButton());
      toolbar.addLeftSeparator();
      terminalTitle_ = new Label();
      terminalTitle_.setStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar.addLeftWidget(terminalTitle_);

      clearButton_ = commands_.clearTerminalScrollbackBuffer().createToolbarButton();
      clearButton_.addStyleName(ThemeStyles.INSTANCE.terminalClearButton());
      toolbar.addRightWidget(clearButton_);

      interruptButton_ = commands_.interruptTerminal().createToolbarButton();
      toolbar.addRightWidget(interruptButton_);

      closeButton_ = commands_.closeTerminal().createToolbarButton();
      closeButton_.addStyleName("rstudio-themes-inverts");
      toolbar.addRightWidget(closeButton_);

      updateTerminalToolbar();
      commands_.previousTerminal().setEnabled(false);
      commands_.nextTerminal().setEnabled(false);
      commands_.closeTerminal().setEnabled(false);
      commands_.renameTerminal().setEnabled(false);
      commands_.clearTerminalScrollbackBuffer().setEnabled(false);
      commands_.showTerminalInfo().setEnabled(true);
      commands_.interruptTerminal().setEnabled(false);
      commands_.sendTerminalToEditor().setEnabled(false);

      return toolbar;
   }
   
   private void updateTerminalToolbar()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            boolean interruptable = false;
            boolean closable = false;
            boolean clearable = false;

            final TerminalSession visibleTerminal = getSelectedTerminal();
            if (visibleTerminal != null)
            {
               clearable = true;
               if (!visibleTerminal.getHasChildProcs())
               {
                  // nothing running in current terminal
                  closable = true;
               }
               else
               {
                  if (!visibleTerminal.xtermAltBufferActive())
                  {
                     // not running a full-screen program
                     interruptable = true;
                     closable = false;
                  }
                  else
                  {
                     // running a full-screen program
                     closable = true;
                     interruptable = false;
                     clearable = false;
                  }
               }
            }
            interruptButton_.setVisible(interruptable);
            closeButton_.setVisible(closable);
            clearButton_.setVisible(clearable);
         }
      });

    }

   @Override
   public void onSelected()
   {
      // terminal tab was selected
      super.onSelected();
      
      // if terminal is not selected, and the tab "X" is clicked, the tab receives
      // onSelected but in this case we don't want to create a new terminal
      if (!closingAll_)
         ensureTerminal(null);

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            suppressAutoFocus_ = false;
         }
      });
   }

   @Override
   public void onBeforeUnselected()
   {
      // terminal tab being unselected
      super.onBeforeUnselected();

      // current terminal needs to know it's not visible so it doesn't
      // respond to resize requests (which will cause xterm.js to lose its
      // mind)
      TerminalSession currentTerminal = getSelectedTerminal();
      if (currentTerminal != null)
      {
         currentTerminal.setVisible(false);
      }
   }

   @Override
   public void onBeforeSelected()
   {
      // terminal tab is about to become visible
      super.onBeforeSelected();

      // make sure a previously hidden terminal is visible
      TerminalSession currentTerminal = getSelectedTerminal();
      if (currentTerminal != null)
      {
         currentTerminal.setVisible(true);
      }
   }

   @Override
   public void activateTerminal()
   {
      closingAll_ = false;
      bringToFront();
   }

   /**
    * Ensure there's a terminal available, and optionally send text to it when ready. 
    * @param postCreateText text to send, may be null
    */
   private void ensureTerminal(String postCreateText)
   {
      if (terminals_.terminalCount() == 0)
      {
         // No terminals at all, create a new one
         createTerminal(postCreateText);
         return;
      }

      TerminalSession terminal = getSelectedTerminal();
      if (terminal == null)
      {
         // No terminal loaded, load the first terminal in the list
         String handle = terminals_.terminalHandleAtIndex(0);
         if (handle == null)
         {
            Debug.log("Unexpected null terminal handle");
            return;
         }
         events_.fireEvent(new SwitchToTerminalEvent(handle, postCreateText));
         return;
      }
      else
      {
         setFocusOnVisible();
         terminal.receivedSendToTerminal(postCreateText);
      }
   }

   @Override
   public void createTerminal(String postCreateText)
   {
      if (creatingTerminal_)
         return;

      creatingTerminal_ = true;
      postCreateText_ = postCreateText;
      terminals_.createNewTerminal(new ResultCallback<Boolean, String>()
      {
         @Override
         public void onSuccess(Boolean connected)
         {
         }
         
         @Override
         public void onFailure(String msg)
         {
            Debug.log(msg);
            creatingTerminal_ = false;
            postCreateText_ = null;
         }
      });
   }

   @Override
   public void addTerminal(ConsoleProcessInfo cpi, boolean hasSession)
   {
      terminals_.addTerminal(cpi, hasSession);
   }

   @Override
   public void removeTerminal(String handle)
   {
      // rstudioapi::terminalKill has already killed the process and reaped it.

      // If the terminal being removed was loaded in the UI, and is currently selected,
      // figure out which terminal to switch to.
      String newTerminalHandle = null;
      boolean didCloseCurrent = false;
      TerminalSession visibleTerminalWidget = getSelectedTerminal();
      TerminalSession killedTerminalWidget = loadedTerminalWithHandle(handle);
      if (visibleTerminalWidget != null && killedTerminalWidget != null && 
            (visibleTerminalWidget == killedTerminalWidget))
      {
         didCloseCurrent = true;
         newTerminalHandle = terminalToShowWhenClosing(handle);
         if (newTerminalHandle != null)
         {
            events_.fireEvent(new SwitchToTerminalEvent(newTerminalHandle, null));
         }
      }

      // Remove terminated terminal from dropdown
      terminals_.removeTerminal(handle);

      // If terminal was loaded, remove its pane.
      if (killedTerminalWidget != null)
      {
         terminalSessionsPanel_.remove(killedTerminalWidget);
      }

      if (newTerminalHandle == null && didCloseCurrent)
      {
         activeTerminalToolbarButton_.setNoActiveTerminal();
         setTerminalTitle("");
      }
      updateTerminalToolbar();
   }

   @Override
   public void activateNamedTerminal(String caption)
   {
      if (StringUtil.isNullOrEmpty(caption))
         return;
      
      activeTerminalToolbarButton_.setActiveTerminalByCaption(caption);
   }

   @Override
   public void repopulateTerminals(ArrayList<ConsoleProcessInfo> procList)
   {
      // Expect to receive this after a browser reset, so if we already have
      // terminals in the cache, something is, to be technical, busted.
      if (terminals_.terminalCount() > 0 || getLoadedTerminalCount() > 0 )
      {
         Debug.logWarning("Received terminal list from server when terminals " + 
                          "already loaded. Ignoring.");
         return;
      }
      
      // add terminal to the dropdown's cache; terminals aren't actually
      // connected until selected via the dropdown
      for (ConsoleProcessInfo procInfo : procList)
      {
         terminals_.addTerminal(procInfo, false /*hasSession*/);
      }
   }

   @Override
   public void terminateCurrentTerminal()
   {
      final TerminalSession visibleTerminal = getSelectedTerminal();
      if (visibleTerminal != null)
      {
         if (visibleTerminal.getHasChildProcs())
         {
            globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
                  "Close " + visibleTerminal.getTitle(),
                  "Are you sure you want to exit the terminal named \"" +
                        visibleTerminal.getCaption() + "\"? Any running jobs will be terminated.",
                        false,
                        new Operation()
            {
               @Override
               public void execute()
               {
                  visibleTerminal.terminate();
               }
            },
            new Operation()
            {
               @Override
               public void execute()
               {
                  setFocusOnVisible();
               }
            },
            new Operation()
            {
               @Override
               public void execute()
               {
                  setFocusOnVisible();
               }
            }, "Terminate", "Cancel", true);
         }
         else
         {
            visibleTerminal.terminate();
         }
      }
   }

   @Override
   public boolean activeTerminals()
   {
      // We treat shells that have child processes as active. If a shell is
      // not running anything, it is fair game for being killed via a
      // session suspend.
      return terminals_.haveSubprocs();
   }

   @Override
   public void terminateAllTerminals()
   {
      closingAll_ = true;
      
      // kill any terminal server processes, and remove them from the server-
      // side list of known processes, and client-side list
      terminals_.terminateAll();

      // set client state back to startup values
      creatingTerminal_ = false;
      postCreateText_ = null;
      activeTerminalToolbarButton_.setNoActiveTerminal();
      setTerminalTitle("");

      // remove all widgets
      while (terminalSessionsPanel_.getWidgetCount() > 0)
      {
         terminalSessionsPanel_.remove(0);
      }
      updateTerminalToolbar();
   }

   @Override
   public void renameTerminal()
   {
      final TerminalSession visibleTerminal = getSelectedTerminal();
      if (visibleTerminal == null)
      {
         return;
      }
      final String origCaption = visibleTerminal.getCaption();

      globalDisplay_.promptForText("Rename Terminal",
            "Please enter the new terminal name:",
            origCaption,
            new OperationWithInput<String>()
            {
               @Override
               public void execute(final String newCaption)
               {
                  // rename in the UI
                  renameVisibleTerminalInClient(newCaption);

                  // rename on the server
                  server_.processSetCaption(visibleTerminal.getHandle(), 
                        newCaption,
                        new ServerRequestCallback<Boolean>()
                        {
                           @Override
                           public void onResponseReceived(Boolean result)
                           {
                              if (result == false)
                              {
                                 globalDisplay_.showMessage(GlobalDisplay.MSG_INFO, 
                                       "Name already in use",
                                       "Please enter a unique name.");
                                 // failed, put back original caption on client
                                 renameVisibleTerminalInClient(origCaption);
                              }
                           }
                           
                           @Override
                           public void onError(ServerError error)
                           {
                              // failed, put back original caption on client
                              renameVisibleTerminalInClient(origCaption);
                           }
                        });
               }
            });
   }

   @Override
   public void clearTerminalScrollbackBuffer(String caption)
   {
      final TerminalSession terminalToClear = getTerminalWithCaption(caption);
      if (terminalToClear == null)
         return;

      terminalToClear.clearBuffer();
   }

   @Override
   public void sendToTerminal(String text, boolean setFocus)
   {
      if (StringUtil.isNullOrEmpty(text))
         return;

      suppressAutoFocus_ = !setFocus;
      ensureTerminal(text);
      activateTerminal();
   }

   @Override
   public void previousTerminal()
   {
      activeTerminalToolbarButton_.previousTerminal();
   }

   @Override
   public void nextTerminal()
   {
      activeTerminalToolbarButton_.nextTerminal();
   }

   @Override
   public void showTerminalInfo()
   {
      new TerminalInfoDialog(debug_dumpTerminalContext(), getSelectedTerminal()).showModal();
   }
   
   @Override
   public void interruptTerminal()
   {
      final TerminalSession visibleTerminal = getSelectedTerminal();
      if (visibleTerminal == null)
      {
         return;
      }
      visibleTerminal.interruptTerminal();
   }
   
   @Override
   public void sendTerminalToEditor()
   {
      final TerminalSession visibleTerminal = getSelectedTerminal();
      if (visibleTerminal == null)
         return;
 
      visibleTerminal.getBuffer(true /*stripAnsi*/, new ResultCallback<String, String>()
      {
         @Override
         public void onSuccess(String buffer)
         {
            if (buffer.isEmpty())
               return;

            // open a new tab for the terminal buffer copy
            events_.fireEvent(new NewWorkingCopyEvent(FileTypeRegistry.TEXT,
                  null /*path*/,
                  buffer));
         }
         
         @Override
         public void onFailure(String msg)
         {
            Debug.log(msg);
         }
      });
   }
   
   public String debug_dumpTerminalContext()
   {
      StringBuilder dump = new StringBuilder();

      if (terminalSessionsPanel_ != null)
      {
         int total = getLoadedTerminalCount();
         dump.append("Loaded TerminalSessions: ");
         dump.append(total);
         dump.append("\n");
         for (int i = 0; i < total; i++)
         {
            TerminalSession session = getLoadedTerminalAtIndex(i);
            if (session == null)
            {
               dump.append("null\n");
            }
            else
            {
               dump.append("Handle: '");
               String handle = session.getHandle();
               if (handle == null)
               {
                  dump.append("null");
               }
               else
               {
                  dump.append(handle);
               }
               dump.append("' Caption: '");
               String caption = session.getCaption();
               if (caption == null)
               {
                  dump.append("null");
               }
               else
               {
                  dump.append(caption);
               }
               dump.append("'\n");
            }
         }
         
         dump.append("\n");
         dump.append(terminals_.debug_dumpTerminalList());
      }
      
      return dump.toString();
   }

   /**
    * Rename the currently visible terminal (client-side only).
    * 
    */
   private void renameVisibleTerminalInClient(String newCaption)
   {
      TerminalSession visibleTerminal = getSelectedTerminal();
      if (visibleTerminal == null)
      {
         return;
      }

      // The caption lives in multiple places:
      // (1) the dropdown menu label
      // (2) the ConsoleProcessInfo held in terminals_
      activeTerminalToolbarButton_.setActiveTerminal(
            newCaption, visibleTerminal.getHandle());
      terminals_.setCaption(visibleTerminal.getHandle(), newCaption);
   }

   @Override
   public void onTerminalSessionStarted(TerminalSessionStartedEvent event)
   {
      TerminalSession terminal = event.getTerminalWidget();

      terminals_.addTerminal(terminal);

      // Check if this is a reconnect of an already displayed terminal, such
      // as after a session suspend/resume.
      if (terminalSessionsPanel_.getWidgetIndex(terminal) == -1)
      {
         terminalSessionsPanel_.add(terminal);
         showTerminalWidget(terminal);
         setFocusOnVisible();
      }
      else
      {
         terminal.writeRestartSequence();
      }
      terminal.receivedSendToTerminal(postCreateText_);
      creatingTerminal_ = false;
      postCreateText_ = null;
      updateTerminalToolbar();
   }
   
   /**
    * Cleanup after process with given handle has terminated.
    * @param handle identifier for process that exited
    * @param processExited true if process exited on server; false if client-side is forcing exit
    * @param exitCode exit code if known (-1 if not known)
    */
   private void cleanupAfterTerminate(String handle, boolean processExited, int exitCode)
   {
      if (terminals_.indexOfTerminal(handle) == -1)
      {
         // Don't know about this process handle, nothing to do on client related
         // to the Terminal pane.
         return;
      }

      if (isRestartInProgress_)
      {
         return;
      }

      // determine if terminal should remain loaded in UI even though process
      // has exited
      if (processExited)
      {
         int autoCloseMode = terminals_.autoCloseForHandle(handle);
         if (autoCloseMode == ConsoleProcessInfo.AUTOCLOSE_DEFAULT)
         {
            if (uiPrefs_.terminalAutoClose().getValue())
               autoCloseMode = ConsoleProcessInfo.AUTOCLOSE_ALWAYS;
            else
               autoCloseMode = ConsoleProcessInfo.AUTOCLOSE_NEVER;
         }

         if (autoCloseMode == ConsoleProcessInfo.AUTOCLOSE_NEVER)
         {
            terminals_.setExitCode(handle, exitCode);
            terminals_.setZombie(handle, true);
            TerminalSession terminal = loadedTerminalWithHandle(handle);
            if (terminal != null)
            {
               terminal.showZombieMessage();
               terminal.disconnect(true /*permanent*/);
            }

            events_.fireEvent(new TerminalSubprocEvent(handle, false));
            return;
         }
      }
      
      // Figure out which terminal to switch to, send message to do so.
      String newTerminalHandle = terminalToShowWhenClosing(handle);
      if (newTerminalHandle != null)
      {
         events_.fireEvent(new SwitchToTerminalEvent(newTerminalHandle, null));
      }
      
      // Remove terminated terminal from dropdown
      terminals_.removeTerminal(handle);
      server_.processReap(handle, new VoidServerRequestCallback());

      // If terminal was loaded, remove its pane.
      TerminalSession currentTerminal = loadedTerminalWithHandle(handle);
      if (currentTerminal != null)
      {
         terminalSessionsPanel_.remove(currentTerminal);
      }

      if (newTerminalHandle == null)
      {
         activeTerminalToolbarButton_.setNoActiveTerminal();
         setTerminalTitle("");
      }
      updateTerminalToolbar();
   }

   @Override
   public void onServerProcessExit(ServerProcessExitEvent event)
   {
      // Notification from server that a process exited.
      cleanupAfterTerminate(event.getProcessHandle(), true, event.getExitCode());
   }

   @Override
   public void onTerminalSessionStopped(TerminalSessionStoppedEvent event)
   {
      // Notification from a TerminalSession that it is being forcibly closed.
      cleanupAfterTerminate(event.getTerminalWidget().getHandle(), false, -1);
   }

   @Override
   public void onSwitchToTerminal(SwitchToTerminalEvent event)
   {
      String handle = event.getTerminalHandle();

      // If terminal was already loaded, just make it visible
      TerminalSession terminal = loadedTerminalWithHandle(handle);
      if (terminal != null)
      {
         showTerminalWidget(terminal);
         setFocusOnVisible();
         ensureConnected(terminal); // needed after session suspend/resume
         terminal.receivedSendToTerminal(event.getInputText());
         return;
      }

      // Reconnect to server?
      if (terminals_.reconnectTerminal(handle))
      {
         return;
      }

      Debug.logWarning("Tried to switch to unknown terminal handle");
   }

   @Override
   public void onTerminalTitle(TerminalTitleEvent event)
   {
      // Notification from a TerminalSession that it changed its title
      TerminalSession visibleTerm = getSelectedTerminal();
      TerminalSession retitledTerm = event.getTerminalSession();
      if (visibleTerm != null && visibleTerm.getHandle().equals(
            retitledTerm.getHandle()))
      {
         // update the toolbar label if currently displayed terminal has changed
         // its title
         setTerminalTitle(retitledTerm.getTitle());
      }

      // Update local metadata
      if (!terminals_.retitleTerminal(retitledTerm.getHandle(),
                                      retitledTerm.getTitle()))
      {
         return; // title unchanged
      }

      // update server
      server_.processSetTitle(
            retitledTerm.getHandle(), 
            retitledTerm.getTitle(),
            new VoidServerRequestCallback()
            {
               @Override
               public void onError(ServerError error)
               {
                  // failed; this might mean we show the wrong title after
                  // a reset, but it will update itself fairly quickly
                  Debug.logError(error);
               }
            });
   }

   /**
    * @return number of terminals loaded into panes
    */
   public int getLoadedTerminalCount()
   {
      return terminalSessionsPanel_.getWidgetCount();
   }

   /**
    * @param i index of terminal to return
    * @return terminal at index, or null
    */
   public TerminalSession getLoadedTerminalAtIndex(int i)
   {
      Widget widget = terminalSessionsPanel_.getWidget(i);
      if (widget instanceof TerminalSession)
      {
         return (TerminalSession)widget;
      }
      return null;
   }

   /**
    * Find loaded terminal session for a given handle
    * @param handle of TerminalSession to return
    * @return TerminalSession with that handle, or null
    */
   private TerminalSession loadedTerminalWithHandle(String handle)
   {
      int total = getLoadedTerminalCount();
      for (int i = 0; i < total; i++)
      {
         TerminalSession t = getLoadedTerminalAtIndex(i);
         if (t != null && t.getHandle().equals(handle))
         {
            return t;
         }
      }
      return null;
   }

   /**
    * Find loaded terminal session for a given caption
    * @param caption of TerminalSession to return
    * @return TerminalSession with that caption, or null
    */
   private TerminalSession loadedTerminalWithCaption(String caption)
   {
      int total = getLoadedTerminalCount();
      for (int i = 0; i < total; i++)
      {
         TerminalSession t = getLoadedTerminalAtIndex(i);
         if (t != null && t.getCaption().equals(caption))
         {
            return t;
         }
      }
      return null;
   }


   /**
    * @return Selected terminal, or null if there is no selected terminal.
    */
   public TerminalSession getSelectedTerminal()
   {
      return getTerminalWithCaption(null);
   }

   /**
    * Return a terminal by caption.
    * @param caption caption to find; if null or empty, returns currently
    * selected terminal; null if no terminals open.
    * @return
    */
   public TerminalSession getTerminalWithCaption(String caption)
   {
      if (StringUtil.isNullOrEmpty(caption))
      {
         if (terminalSessionsPanel_ == null)
            return null;
         
         Widget visibleWidget = terminalSessionsPanel_.getVisibleWidget();
         if (visibleWidget instanceof TerminalSession)
         {
            return (TerminalSession)visibleWidget;
         }
         return null;
      }
      
      return loadedTerminalWithCaption(caption);
   }


   /**
    * If a terminal is visible give it focus and update dropdown selection.
    */
   public void setFocusOnVisible()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            TerminalSession visibleTerminal = getSelectedTerminal();
            if (visibleTerminal != null)
            {
               if (!suppressAutoFocus_)
                  visibleTerminal.setFocus(true);
               activeTerminalToolbarButton_.setActiveTerminal(
                     visibleTerminal.getCaption(), visibleTerminal.getHandle());
               setTerminalTitle(visibleTerminal.getTitle());
            }
         }
      });
   }

   /**
    * Handle of terminal to show after closing indicated terminal.
    * @param handle terminal being closed
    * @return handle of terminal to show next, or null if none to show
    */
   private String terminalToShowWhenClosing(String handle)
   {
      int terminalClosing = terminals_.indexOfTerminal(handle);
      if (terminalClosing > 0)
         return terminals_.terminalHandleAtIndex(terminalClosing - 1);
      else if (terminalClosing + 1 < terminals_.terminalCount())
         return terminals_.terminalHandleAtIndex(terminalClosing + 1);
      else
         return null;
   }

   private void setTerminalTitle(String title)
   {
      terminalTitle_.setText(title);
   }

   @Override
   public void onSessionSerialization(SessionSerializationEvent event)
   {
      switch(event.getAction().getType())
      {
      case SessionSerializationAction.RESUME_SESSION:
         final TerminalSession currentTerminal = getSelectedTerminal();
         if (currentTerminal != null)
         {
            ensureConnected(currentTerminal);
         }
         break;
      }
   }

   /**
    * Reconnect an existing terminal, if currently disconnected
    * @param terminal terminal to reconnect
    */
   private void ensureConnected(final TerminalSession terminal)
   {
      if (terminal.isConnected())
      {
         return;
      }

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            terminal.connect(new ResultCallback<Boolean, String>()
            {
               @Override
               public void onSuccess(Boolean connected) 
               {
               }

               @Override
               public void onFailure(String msg)
               {
                  Debug.log(msg);
               }
            });
         }
      });
   }

   @Override
   public void onTerminalSubprocs(TerminalSubprocEvent event)
   {
      TerminalSession terminal = loadedTerminalWithHandle(event.getHandle());
      if (terminal != null)
      {
         terminal.setHasChildProcs(event.hasSubprocs());
      }
      updateTerminalToolbar();
   }

   private void showTerminalWidget(TerminalSession terminal)
   {
      registerChildProcsHandler(terminal);
      terminalSessionsPanel_.showWidget(terminal);
      updateTerminalToolbar();
   }
   
   private void registerChildProcsHandler(TerminalSession terminal)
   {
      unregisterChildProcsHandler();
      if (terminal != null)
      {
         terminalHasChildProcsHandler_ = terminal.addHasChildProcsChangeHandler(
               new ValueChangeHandler<Boolean>() 
         {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event)
            {
               // When terminal reports that there are child procs, there can
               // be a lag before it (potentially) enters a full-screen program, 
               // and our toolbar controls use both bits of information to 
               // set state. We don't get a notification on full-screen terminal
               // mode, we can only poll it. So, delay just a bit to improve
               // chances of it being current.
               Timer timer = new Timer()
               {
                  public void run() 
                  {
                     updateTerminalToolbar();
                  }
               };
               timer.schedule(200);
            }
         });
      }
   }
   
   private void unregisterChildProcsHandler()
   {
      if (terminalHasChildProcsHandler_ != null)
      {
         terminalHasChildProcsHandler_.removeHandler();
         terminalHasChildProcsHandler_ = null;
      }
   }

   private DeckLayoutPanel terminalSessionsPanel_;
   private TerminalPopupMenu activeTerminalToolbarButton_;
   private final TerminalList terminals_ = new TerminalList();
   private Label terminalTitle_;
   private boolean creatingTerminal_;
   private String postCreateText_;
   private ToolbarButton interruptButton_;
   private ToolbarButton closeButton_;
   ToolbarButton clearButton_;
   private HandlerRegistration terminalHasChildProcsHandler_;
   private boolean isRestartInProgress_;
   private boolean closingAll_;
   private boolean suppressAutoFocus_;
   
   // Injected ----  
   private GlobalDisplay globalDisplay_;
   private EventBus events_;
   private Commands commands_;
   private WorkbenchServerOperations server_;
   private UIPrefs uiPrefs_;
}