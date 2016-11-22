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

import java.util.HashMap;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.shell.ShellSecureInput;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
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
                                     TerminalSessionStoppedEvent.Handler
{
   @Inject
    protected TerminalPane(Commands commands,
                           Session session)
   {
      super("Terminal");
      commands_ = commands;
      session_ = session;
      widgetToRegistrations_ = new HashMap<Widget, HandlerRegistrations>();
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

      activeTerminalToolbarButton_ = new TerminalPopupMenu(session_.getSessionInfo(),
                                                           commands_);
      
      toolbar.addLeftWidget(activeTerminalToolbarButton_.getToolbarButton());

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
   }
   
   @Override
   public void createTerminal()
   {
      if (secureInput_ == null)
      {
         secureInput_ = new ShellSecureInput();  
      }
      
      TerminalSession newSession = new TerminalSession(secureInput_);
      
      addHandlerRegistration(newSession, newSession.addTerminalSessionStartedHandler(this));
      addHandlerRegistration(newSession, newSession.addTerminalSessionStoppedHandler(this));
      
      newSession.connect();
   }

   @Override
   public void onTerminalSessionStarted(TerminalSessionStartedEvent event)
   {
      terminalSessionsPanel_.add(event.getTerminalWidget());
      terminalSessionsPanel_.showWidget(event.getTerminalWidget());
      
      // TODO (gary) update dropdown
   } 
   
   @Override
   public void onTerminalSessionStopped(TerminalSessionStoppedEvent event)
   {
      Widget currentTerminal = event.getTerminalWidget();
      unregisterHandlers(currentTerminal);
      int currentIndex = terminalSessionsPanel_.getWidgetIndex(currentTerminal);
      if (currentIndex > 0)
      {
         terminalSessionsPanel_.showWidget(currentIndex - 1);
         // TODO (gary) set focus on the widget
      }
      terminalSessionsPanel_.remove(currentTerminal);
       
      // TODO (gary) update dropdown
   }

   public int getTerminalCount()
   {
      return terminalSessionsPanel_.getWidgetCount();
   }

   @Override
   public void onUnload()
   {
      super.onUnload();
      unregisterAllHandlers();
   }

   /**
    * Track a handler registration with a specific widget, so we can remove
    * handlers for that specific widget when it is removed.
    * @param widget Widget we are registering with
    * @param reg Handler we are tracking
    */
   protected void addHandlerRegistration(Widget widget, HandlerRegistration reg)
   {
      HandlerRegistrations registrations = widgetToRegistrations_.get(widget);
      if (registrations == null)
      {
         registrations = new HandlerRegistrations();
         widgetToRegistrations_.put(widget, registrations);
      }
      registrations.add(reg);
   }
   
   /**
    * Unregister handlers for a specific widget.
    * @param widget Widget to unregister.
    */
   protected void unregisterHandlers(Widget widget)
   {
      HandlerRegistrations registrations = widgetToRegistrations_.get(widget);
      if (registrations != null)
      {
         registrations.removeHandler();
         widgetToRegistrations_.remove(widget);
      }
   }
   
   /**
    * Unregister handlers for all widgets.
    */
   protected void unregisterAllHandlers()
   {
      for (HandlerRegistrations registrations : widgetToRegistrations_.values())
      {
         registrations.removeHandler();
      }
      widgetToRegistrations_.clear();
   }
   
   private DeckLayoutPanel terminalSessionsPanel_;
   private Commands commands_;
   private Session session_;
   private TerminalPopupMenu activeTerminalToolbarButton_;
   private ShellSecureInput secureInput_;
   
   /**
    * Due to dynamic child Widgets, we track registrations by Widget so we 
    * can remove them when the Widget is being closed. 
    */
   private final HashMap<Widget, HandlerRegistrations> widgetToRegistrations_;
}