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

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.shell.ShellSecureInput;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.terminal.events.SwitchToTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalCaptionEvent;

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
 * zero or more Terminal instances.
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
   protected TerminalPane(EventBus events)
   {
      super("Terminal");
      events.addHandler(TerminalSessionStartedEvent.TYPE, this);
      events.addHandler(TerminalSessionStoppedEvent.TYPE, this);
      events.addHandler(SwitchToTerminalEvent.TYPE, this);
      events.addHandler(TerminalCaptionEvent.TYPE, this);
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
      activeTerminalToolbarButton_ = new TerminalPopupMenu();
      toolbar.addRightWidget(activeTerminalToolbarButton_.getToolbarButton());
      return toolbar;
   }
  
   @Override
   public void onSelected()
   {
      super.onSelected();
      activateTerminal();
      ensureTerminal();
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
      if (getTerminalCount() == 0)
      {
         createTerminal();
      }
      setFocusOnVisible();
   }
   
   @Override
   public void createTerminal()
   {
      if (secureInput_ == null)
      {
         secureInput_ = new ShellSecureInput();  
      }
      
      TerminalSession newSession = new TerminalSession(secureInput_,
                                                       nextTerminalSequence());
      newSession.connect();
   }

   @Override
   public void onTerminalSessionStarted(TerminalSessionStartedEvent event)
   {
      TerminalSession terminal = event.getTerminalWidget();
      
      terminalSessionsPanel_.add(terminal);
      terminalSessionsPanel_.showWidget(terminal);
      setFocusOnVisible();
   }
   
   @Override
   public void onTerminalSessionStopped(TerminalSessionStoppedEvent event)
   {
      TerminalSession currentTerminal = event.getTerminalWidget();
      int currentIndex = terminalSessionsPanel_.getWidgetIndex(currentTerminal);
      int newIndex = terminalToShowWhenClosing(currentIndex);
      if (newIndex >= 0)
      {
         terminalSessionsPanel_.showWidget(newIndex);
         setFocusOnVisible();
      }
      terminalSessionsPanel_.remove(currentTerminal);

      if (terminalSessionsPanel_.getWidgetCount() < 1)
      {
         activeTerminalToolbarButton_.setNoActiveTerminal();
         setTerminalCaption("");
      }
   }

   @Override
   public void onSwitchToTerminal(SwitchToTerminalEvent event)
   {
      TerminalSession terminal = terminalWithHandle(event.getTerminalHandle());
      if (terminal != null)
      {
         terminalSessionsPanel_.showWidget(terminal);
         setFocusOnVisible();
      }
   }
   
   @Override
   public void onTerminalCaption(TerminalCaptionEvent event)
   {
      TerminalSession visibleTerm = getVisibleTerminal();
      TerminalSession captionTerm = event.getTerminalSession();
      if (visibleTerm != null && visibleTerm.getHandle().equals(
            captionTerm.getHandle()))
      {
         setTerminalCaption(captionTerm.getCaption());
      }
   }
   
   /**
    * @return number of terminals hosted by the pane
    */
   public int getTerminalCount()
   {
      return terminalSessionsPanel_.getWidgetCount();
   }
   
   /**
    * @param i index of terminal to return
    * @return terminal at index, or null
    */
   public TerminalSession getTerminalAtIndex(int i)
   {
      Widget widget = terminalSessionsPanel_.getWidget(i);
      if (widget instanceof TerminalSession)
      {
         return (TerminalSession)widget;
      }
      return null;
   }
   
   /**
    * Find terminal session for a given handle
    * @param handle of TerminalSession to return
    * @return TerminalSession with that handle, or null
    */
   public TerminalSession terminalWithHandle(String handle)
   {
      int total = getTerminalCount();
      for (int i = 0; i < total; i++)
      {
         TerminalSession t = getTerminalAtIndex(i);
         if (t != null && t.getHandle().equals(handle))
         {
            return t;
         }
      }
      return null;
   }

   /**
    * @return Visible terminal, or null if there is no visible terminal.
    */
   public TerminalSession getVisibleTerminal()
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
               TerminalSession visibleTerminal = getVisibleTerminal();
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
    * Choose a 1-based sequence number one higher than the highest currently opened
    * terminal number. We don't try to fill gaps if terminals are closed in
    * the middle of the opened tabs.
    * @return Highest currently opened terminal plus one
    */
   private int nextTerminalSequence()
   {
      int maxNum = 0;
      for (int i = 0; i < terminalSessionsPanel_.getWidgetCount(); i++)
      {
         maxNum = Math.max(maxNum, getTerminalAtIndex(i).getSequence());
      }
      return maxNum + 1;
   }
   
   /**
    * Index of terminal to show after closing indicated terminal index
    * @param terminalClosing index of terminal being closed
    * @return index of terminal to show next, or -1 if none available
    */
   private int terminalToShowWhenClosing(int terminalClosing)
   {
      if (terminalClosing > 0)
         return terminalClosing - 1;
      else if (terminalClosing + 1 < getTerminalCount())
         return terminalClosing + 1;
      else
         return -1;
   }
   
   private void setTerminalCaption(String caption)
   {
      terminalCaption_.setText(caption);
   }
   
   private DeckLayoutPanel terminalSessionsPanel_;
   private TerminalPopupMenu activeTerminalToolbarButton_;
   private Label terminalCaption_;
   private ShellSecureInput secureInput_;

}