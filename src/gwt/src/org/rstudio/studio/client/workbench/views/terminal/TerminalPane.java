/*
 * TerminalPane.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.shell.ShellSecureInput;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.terminal.TerminalList.TerminalMetadata;
import org.rstudio.studio.client.workbench.views.terminal.events.SwitchToTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalCaptionEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
                          implements ClickHandler,
                                     TerminalTabPresenter.Display,
                                     TerminalSessionStartedEvent.Handler,
                                     TerminalSessionStoppedEvent.Handler,
                                     SwitchToTerminalEvent.Handler,
                                     TerminalCaptionEvent.Handler
{
   @Inject
   protected TerminalPane(EventBus events, GlobalDisplay globalDisplay)
   {
      super("Terminal");
      globalDisplay_ = globalDisplay;
      events_ = events;
      events_.addHandler(TerminalSessionStartedEvent.TYPE, this);
      events_.addHandler(TerminalSessionStoppedEvent.TYPE, this);
      events_.addHandler(SwitchToTerminalEvent.TYPE, this);
      events_.addHandler(TerminalCaptionEvent.TYPE, this);
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      terminalSessionsPanel_ = new DeckLayoutPanel();
      return terminalSessionsPanel_;
   }

   @Override
   public void onClick(ClickEvent event)
   {
      // TODO (gary) implement
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();

      terminalCaption_ = new Label();
      terminalCaption_.setStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar.addLeftWidget(terminalCaption_);
      activeTerminalToolbarButton_ = new TerminalPopupMenu(terminals_);
      toolbar.addRightWidget(activeTerminalToolbarButton_.getToolbarButton());
      return toolbar;
   }
  
   @Override
   public void onSelected()
   {
      // terminal tab was selected
      super.onSelected();
      activateTerminal();
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
   
   @Override
   public void ensureTerminal()
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
     startTerminal(terminals_.nextTerminalSequence(), null);
   }

   /**
    * Start a terminal
    * @param sequence sequence number to track relative order of terminal creation
    * @param terminalHandle handle for terminal, or null if starting a new terminal
    */
   public void startTerminal(int sequence, String terminalHandle)
   {
      TerminalSession newSession = new TerminalSession(
            getSecureInput(), sequence, terminalHandle);
      newSession.connect();
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
      // loaded until selected via the dropdown
      for (ConsoleProcessInfo procInfo : procList)
      {
         terminals_.addTerminal(new TerminalMetadata(procInfo.getHandle(),
                                          procInfo.getCaption(), 
                                          procInfo.getTerminalSequence()));
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
               visibleTerminal.getTitle() + "\"? Any running jobs will be terminated.",
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
   public void onTerminalSessionStarted(TerminalSessionStartedEvent event)
   {
      TerminalSession terminal = event.getTerminalWidget();
      
      terminals_.addTerminal(new TerminalMetadata(
            terminal.getHandle(),
            terminal.getTitle(),
            terminal.getSequence()));   

      terminalSessionsPanel_.add(terminal);
      terminalSessionsPanel_.showWidget(terminal);
      setFocusOnVisible();
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
         setTerminalCaption("");
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
         return;
      }

      // Reconnect to server?
      TerminalMetadata existingTerminal = terminals_.getMetadataForHandle(handle);
      if (existingTerminal != null)
      {
         startTerminal(existingTerminal.getSequence(), handle);
         return;
      }

      Debug.logWarning("Tried to switch to unknown terminal handle");
   }
   
   @Override
   public void onTerminalCaption(TerminalCaptionEvent event)
   {
      TerminalSession visibleTerm = getSelectedTerminal();
      TerminalSession captionTerm = event.getTerminalSession();
      if (visibleTerm != null && visibleTerm.getHandle().equals(
            captionTerm.getHandle()))
      {
         setTerminalCaption(captionTerm.getCaption());
      }
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
                        visibleTerminal.getTitle(), visibleTerminal.getHandle());
                  setTerminalCaption(visibleTerminal.getCaption());
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
   
   private void setTerminalCaption(String caption)
   {
      terminalCaption_.setText(caption);
   }
   
   private ShellSecureInput getSecureInput()
   {
      if (secureInput_ == null)
      {
         secureInput_ = new ShellSecureInput();  
      }
      return secureInput_;
   }
   
   private DeckLayoutPanel terminalSessionsPanel_;
   private TerminalPopupMenu activeTerminalToolbarButton_;
   private final TerminalList terminals_ = new TerminalList();
   private Label terminalCaption_;
   private ShellSecureInput secureInput_;

   // Injected ----  
   private GlobalDisplay globalDisplay_;
   private EventBus events_;
}