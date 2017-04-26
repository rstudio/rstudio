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
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.events.SessionSerializationHandler;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.terminal.events.SwitchToTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSubprocEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalTitleEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
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
                                     SwitchToTerminalEvent.Handler,
                                     TerminalTitleEvent.Handler,
                                     SessionSerializationHandler,
                                     TerminalSubprocEvent.Handler
{
   @Inject
   protected TerminalPane(EventBus events,
                          GlobalDisplay globalDisplay,
                          Commands commands,
                          WorkbenchServerOperations server)
   {
      super("Terminal");
      events_ = events;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      server_ = server;
      events_.addHandler(TerminalSessionStartedEvent.TYPE, this);
      events_.addHandler(TerminalSessionStoppedEvent.TYPE, this);
      events_.addHandler(SwitchToTerminalEvent.TYPE, this);
      events_.addHandler(TerminalTitleEvent.TYPE, this);
      events_.addHandler(SessionSerializationEvent.TYPE, this);
      events_.addHandler(TerminalSubprocEvent.TYPE, this);

      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      terminalSessionsPanel_ = new DeckLayoutPanel();
      return terminalSessionsPanel_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();

      activeTerminalToolbarButton_ = new TerminalPopupMenu(terminals_);
      toolbar.addLeftWidget(activeTerminalToolbarButton_.getToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.previousTerminal().createToolbarButton());
      toolbar.addLeftWidget(commands_.nextTerminal().createToolbarButton());
      toolbar.addLeftSeparator();
      terminalTitle_ = new Label();
      terminalTitle_.setStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar.addLeftWidget(terminalTitle_);
      toolbar.addRightWidget(commands_.clearTerminalScrollbackBuffer().createToolbarButton());

      commands_.previousTerminal().setEnabled(false);
      commands_.nextTerminal().setEnabled(false);
      commands_.closeTerminal().setEnabled(false);
      commands_.renameTerminal().setEnabled(false);
      commands_.closeTerminal().setEnabled(false);
      commands_.clearTerminalScrollbackBuffer().setEnabled(false);
      commands_.showTerminalInfo().setEnabled(false);

      return toolbar;
   }

   @Override
   public void onSelected()
   {
      // terminal tab was selected
      super.onSelected();
      ensureTerminal();
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
      ensureVisible();
      bringToFront();
   }

   private void ensureTerminal()
   {
      if (terminals_.terminalCount() == 0)
      {
         // No terminals at all, create a new one
         createTerminal();
      }
      else if (getSelectedTerminal() == null)
      {
         // No terminal loaded, load the first terminal in the list
         String handle = terminals_.terminalHandleAtIndex(0);
         if (handle != null)
         {
            events_.fireEvent(new SwitchToTerminalEvent(handle));
         }
      }
      else
      {
         setFocusOnVisible();
      }
   }

   @Override
   public void createTerminal()
   {
      if (creatingTerminal_)
         return;

      creatingTerminal_ = true;
      terminals_.createNewTerminal();
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
         terminals_.addTerminal(procInfo);
      }
   }

   @Override
   public void terminateCurrentTerminal()
   {
      final TerminalSession visibleTerminal = getSelectedTerminal();
      if (visibleTerminal != null)
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
      // kill any terminal server processes, and remove them from the server-
      // side list of known processes, and client-side list
      terminals_.terminateAll();

      // set client state back to startup values
      creatingTerminal_ = false;
      activeTerminalToolbarButton_.setNoActiveTerminal();
      setTerminalTitle("");

      // remove all widgets
      while (terminalSessionsPanel_.getWidgetCount() > 0)
      {
         terminalSessionsPanel_.remove(0);
      }
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
            0,
            origCaption.length(),
            null,
            new ProgressOperationWithInput<String>()
            {
               @Override
               public void execute(final String newCaption,
                                   final ProgressIndicator progress)
               {
                  progress.onProgress("Renaming terminal...");

                  // rename in the UI
                  renameVisibleTerminalInClient(newCaption);

                  // rename on the server
                  server_.processSetCaption(visibleTerminal.getHandle(), 
                        newCaption,
                        new VoidServerRequestCallback(progress)
                        {
                           @Override
                           public void onError(ServerError error)
                           {
                              // failed, put back original caption on client
                              renameVisibleTerminalInClient(origCaption);
                              Debug.logError(error);
                           }
                        });
               }
            });
   }

   @Override
   public void clearTerminalScrollbackBuffer()
   {
      final TerminalSession visibleTerminal = getSelectedTerminal();
      if (visibleTerminal == null)
      {
         return;
      }

      visibleTerminal.clearBuffer();
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
      final TerminalSession visibleTerminal = getSelectedTerminal();
      if (visibleTerminal == null)
      {
         return;
      }

      visibleTerminal.showTerminalInfo();
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
      // TODO (gary) consolidate ownership or update with event
      // (1) the TerminalSession for connected terminals
      // (2) the dropdown menu label
      // (3) the TerminalMetadata held in terminals_
      visibleTerminal.setCaption(newCaption);
      activeTerminalToolbarButton_.setActiveTerminal(
            newCaption, visibleTerminal.getHandle());
      terminals_.addTerminal(visibleTerminal);
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
         terminalSessionsPanel_.showWidget(terminal);
         setFocusOnVisible();
      }
      creatingTerminal_ = false;
   }

   @Override
   public void onTerminalSessionStopped(TerminalSessionStoppedEvent event)
   {
      // Figure out which terminal to switch to, send message to do so,
      // and remove the stopped terminal.
      TerminalSession currentTerminal = event.getTerminalWidget();
      String handle = currentTerminal.getHandle();

      String newTerminalHandle = terminalToShowWhenClosing(handle);
      if (newTerminalHandle != null)
      {
         events_.fireEvent(new SwitchToTerminalEvent(newTerminalHandle));
      }
      terminals_.removeTerminal(handle);
      terminalSessionsPanel_.remove(currentTerminal);

      if (newTerminalHandle == null)
      {
         activeTerminalToolbarButton_.setNoActiveTerminal();
         setTerminalTitle("");
      }
   }

   @Override
   public void onSwitchToTerminal(SwitchToTerminalEvent event)
   {
      String handle = event.getTerminalHandle();

      // If terminal was already loaded, just make it visible
      TerminalSession terminal = loadedTerminalWithHandle(handle);
      if (terminal != null)
      {
         terminalSessionsPanel_.showWidget(terminal);
         setFocusOnVisible();
         ensureConnected(terminal); // needed after session suspend/resume
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
    * @return Selected terminal, or null if there is no selected terminal.
    */
   public TerminalSession getSelectedTerminal()
   {
      Widget visibleWidget = terminalSessionsPanel_.getVisibleWidget();
      if (visibleWidget instanceof TerminalSession)
      {
         return (TerminalSession)visibleWidget;
      }
      return null;
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
            terminal.connect();
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
   }

   private DeckLayoutPanel terminalSessionsPanel_;
   private TerminalPopupMenu activeTerminalToolbarButton_;
   private final TerminalList terminals_ = new TerminalList();
   private Label terminalTitle_;
   private boolean creatingTerminal_;

   // Injected ----  
   private GlobalDisplay globalDisplay_;
   private EventBus events_;
   private Commands commands_;
   private WorkbenchServerOperations server_;
}