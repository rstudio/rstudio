/*
 * TerminalPopupMenu.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.views.terminal.events.SwitchToTerminalEvent;
import org.rstudio.studio.client.server.ErrorLoggingServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

/**
 * Drop-down menu used in terminal pane. Has commands, and a list of
 * terminal sessions.
 */
public class TerminalPopupMenu extends ToolbarPopupMenu
{
   public TerminalPopupMenu(TerminalList terminals)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      terminals_ = terminals;
   }

   @Inject
   private void initialize(Commands commands,
                           EventBus events,
                           WorkbenchServerOperations server)
   {
      commands_ = commands;
      eventBus_ = events;
      server_ = server;
   }

   @Override
   public void getDynamicPopupMenu(final DynamicPopupMenuCallback callback)
   {
      // clean out existing entries
      clearItems();
      addItem(commands_.newTerminal().createMenuItem(false));
      addSeparator();

      if (terminals_.terminalCount() > 0)
      {
         for (final String handle : terminals_)
         {
            Scheduler.ScheduledCommand cmd = () ->
                  eventBus_.fireEvent(new SwitchToTerminalEvent(handle, null));

            String caption = trimCaption(terminals_.getCaption(handle));
            if (caption == null)
            {
               continue;
            }

            // visual indicator that terminal has processes running
            caption = addBusyIndicator(caption, terminals_.getHasSubprocs(handle));

            String menuHtml = AppCommand.formatMenuLabel(
                  null,              /*icon*/
                  caption,           /*label*/
                  false,             /*html*/
                  null,              /*shortcut*/
                  null,              /*rightImage*/
                  null);             /*rightImageDesc*/
            addItem(new MenuItem(menuHtml, true, cmd));
         }
         addSeparator();
         addItem(commands_.setTerminalToCurrentDirectory().createMenuItem(false));
         addItem(commands_.renameTerminal().createMenuItem(false));
         addItem(commands_.sendTerminalToEditor().createMenuItem(false));
         addSeparator();
         addItem(commands_.previousTerminal().createMenuItem(false));
         addItem(commands_.nextTerminal().createMenuItem(false));
         addSeparator();
         addItem(commands_.interruptTerminal().createMenuItem(false));
         addItem(commands_.clearTerminalScrollbackBuffer().createMenuItem(false));
         addItem(commands_.closeTerminal().createMenuItem(false));
         addSeparator();
         addItem(commands_.closeAllTerminals().createMenuItem(false));
         addSeparator();
      }

      addItem(commands_.showTerminalOptions().createMenuItem(false));
      callback.onPopupMenu(this);
   }

   public ToolbarButton getToolbarButton()
   {
      if (toolbarButton_ == null)
      {
         String buttonText = "Terminal";

         toolbarButton_ = new ToolbarMenuButton(
                buttonText,
                ToolbarButton.NoTitle,
                StandardIcons.INSTANCE.empty_command(),
                this,
                false);

         ElementIds.assignElementId(toolbarButton_, ElementIds.TERMINAL_DROPDOWN_MENUBUTTON);

         setNoActiveTerminal();
      }
      return toolbarButton_;
   }

   /**
    * @param caption caption of the active terminal
    * @param handle handle of the active terminal
    */
   public void setActiveTerminal(String caption, String handle)
   {
      activeTerminalHandle_ = handle;
      String trimmed = trimCaption(caption);
      if (handle != null)
      {
         trimmed = addBusyIndicator(trimmed, terminals_.getHasSubprocs(handle));
      }
      toolbarButton_.setText(trimmed);

      updateTerminalCommands();

      // inform server of the selection
      server_.processNotifyVisible(
            activeTerminalHandle_,
            new ErrorLoggingServerRequestCallback<Void>());
   }

   /**
    * Update terminal commands based on current selection
    */
   public void updateTerminalCommands()
   {
      boolean haveActiveTerminal = activeTerminalHandle_ != null;
      commands_.setTerminalToCurrentDirectory().setEnabled(haveActiveTerminal);
      commands_.closeTerminal().setEnabled(haveActiveTerminal);
      commands_.renameTerminal().setEnabled(haveActiveTerminal);
      commands_.clearTerminalScrollbackBuffer().setEnabled(haveActiveTerminal);
      commands_.interruptTerminal().setEnabled(haveActiveTerminal);
      commands_.previousTerminal().setEnabled(getPreviousTerminalHandle() != null);
      commands_.nextTerminal().setEnabled(getNextTerminalHandle() != null);
      commands_.sendTerminalToEditor().setEnabled(haveActiveTerminal);
   }

   public void setActiveTerminalByCaption(String caption, boolean createdByApi)
   {
      String handle = terminals_.handleForCaption(caption);
      if (StringUtil.isNullOrEmpty(handle))
         return;
      eventBus_.fireEvent(new SwitchToTerminalEvent(handle, null, createdByApi));
   }

   /**
    * Refresh caption of active terminal based on busy status.
    */
   public void refreshActiveTerminal()
   {
      if (toolbarButton_ == null || activeTerminalHandle_ == null)
         return;

      String caption = terminals_.getCaption(activeTerminalHandle_);
      if (caption == null)
         return;

      toolbarButton_.setText(addBusyIndicator(trimCaption(caption),
            terminals_.getHasSubprocs(activeTerminalHandle_)));
   }

   /**
    * set state to indicate no active terminals
    */
   public void setNoActiveTerminal()
   {
      setActiveTerminal("Terminal", null);
   }

   /**
    * @return Handle of active terminal, or null if no active terminal.
    */
   public String getActiveTerminalHandle()
   {
      return activeTerminalHandle_;
   }

   /**
    * Switch to previous terminal tab.
    */
   public void previousTerminal()
   {
      String prevHandle = getPreviousTerminalHandle();
      if (prevHandle != null)
      {
         eventBus_.fireEvent(new SwitchToTerminalEvent(prevHandle, null));
      }
   }

   /**
    * Switch to next terminal tab.
    */
   public void nextTerminal()
   {
      String nextHandle = getNextTerminalHandle();
      if (nextHandle != null)
      {
         eventBus_.fireEvent(new SwitchToTerminalEvent(nextHandle, null));
      }
   }

   /**
    * Add indicator of busy status to a caption.
    * @param caption
    * @return Caption with busy indicator added.
    */
   private String addBusyIndicator(String caption, boolean busy)
   {
      if (busy)
         return caption + " (busy)";
      else
         return caption;
   }

   private String trimCaption(String caption)
   {
      // TODO (gary) look at doing this via css text-overflow
      // when I do the theming work

      // Enforce a sane visual limit on terminal captions
      if (caption.length() > 32)
      {
         caption = caption.substring(0, 31) + "...";
      }
      return caption;
   }

   /**
    * @return handle of previous terminal or null if there is no previous
    * terminal
    */
   private String getPreviousTerminalHandle()
   {
      if (terminals_.terminalCount() > 0 && activeTerminalHandle_ != null)
      {
         String prevHandle = null;
         for (final String handle : terminals_)
         {
            if (StringUtil.equals(activeTerminalHandle_, handle))
            {
               return prevHandle;
            }
            else
            {
               prevHandle = handle;
            }
         }
      }
      return null;
   }

   /**
    * @return handle of next terminal or null if no next terminal
    */
   private String getNextTerminalHandle()
   {
      if (terminals_.terminalCount() > 0 && activeTerminalHandle_ != null)
      {
         boolean foundCurrent = false;
         for (final String handle : terminals_)
         {
            if (foundCurrent)
            {
               return handle;
            }
            if (StringUtil.equals(activeTerminalHandle_, handle))
            {
               foundCurrent = true;
            }
         }
      }
      return null;
   }

   private ToolbarMenuButton toolbarButton_;
   private String activeTerminalHandle_;
   private final TerminalList terminals_;

   // Injected ----
   private Commands commands_;
   private EventBus eventBus_;
   private WorkbenchServerOperations server_;
}
