/*
 * ActiveTerminalToolbarButton.java
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

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.terminal.events.SwitchToTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

public class TerminalPopupMenu extends ToolbarPopupMenu
                               implements TerminalSessionStartedEvent.Handler,
                                          TerminalSessionStoppedEvent.Handler
{
   public TerminalPopupMenu()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      eventBus_.addHandler(TerminalSessionStartedEvent.TYPE, this);
      eventBus_.addHandler(TerminalSessionStoppedEvent.TYPE, this);
   }

   @Inject
   private void initialize(Commands commands,
                           EventBus events)
   {
      commands_ = commands;
      eventBus_ = events;
   }
   
   @Override
   public void getDynamicPopupMenu(final DynamicPopupMenuCallback callback)
   { 
      // clean out existing entries
      clearItems(); 
      addItem(commands_.newTerminal().createMenuItem(false));
      addSeparator();

      if (terminals_.size() > 0)
      {
         for (final java.util.Map.Entry<String, String> item : terminals_.entrySet())
         {
            Scheduler.ScheduledCommand cmd = new Scheduler.ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  eventBus_.fireEvent(new SwitchToTerminalEvent(item.getKey()));
               }
            };

            String menuHtml = AppCommand.formatMenuLabel(
                  null,            /*icon*/
                  item.getValue(), /*label*/
                  false,           /*html*/
                  null,            /*shortcut*/
                  null,            /*rightImage*/
                  null);           /*rightImageDesc*/
            addItem(new MenuItem(menuHtml, true, cmd));
         }
         addSeparator();
         addItem(commands_.closeTerminal().createMenuItem(false));
      }

      // TODO (gary) put these back once implemented
      // addItem(commands_.renameTerminal().createMenuItem(false));
      // addItem(commands_.clearTerminalScrollbackBuffer().createMenuItem(false));
      
      callback.onPopupMenu(this);
   }
   
   public ToolbarButton getToolbarButton()
   {
      if (toolbarButton_ == null)
      {
         String buttonText = "Terminal";
         
         toolbarButton_ = new ToolbarButton(
                buttonText, 
                StandardIcons.INSTANCE.empty_command(),
                this, 
                false);

         setNoActiveTerminal();
      }
      return toolbarButton_;
   }
   
   /**
    *       
    * @param title title of the active terminal
    * @param handle handle of the active terminal
    */
   public void setActiveTerminal(String title, String handle)
   {
      activeTerminalTitle_ = title;
      activeTerminalHandle_ = handle;
      toolbarButton_.setText(activeTerminalTitle_);
   }
   
   /**
    * set state to indicate no active terminals
    */
   public void setNoActiveTerminal()
   {
      setActiveTerminal("Terminal", null);
   }

   private void addTerminal(String title, String handle)
   {
      terminals_.put(handle, title);
   }
   
   private void removeTerminal(String handle)
   {
      terminals_.remove(handle);
   }
   
   @Override
   public void onTerminalSessionStarted(TerminalSessionStartedEvent event)
   {
      addTerminal(event.getTerminalWidget().getTitle(),
            event.getTerminalWidget().getHandle());
   }
    
   @Override
   public void onTerminalSessionStopped(TerminalSessionStoppedEvent event)
   {
      removeTerminal(event.getTerminalWidget().getHandle());
   }
   
   /**
    * @return Handle of active terminal, or null if no active terminal.
    */
   public String getActiveTerminalHandle()
   {
      return activeTerminalHandle_;
   }
   
   private ToolbarButton toolbarButton_;
   private Commands commands_;
   private EventBus eventBus_;
   private String activeTerminalTitle_;
   private String activeTerminalHandle_;
   
   /**
    * map of terminal handles to titles
    */
   private HashMap<String, String> terminals_ = new HashMap<String, String>();
}
