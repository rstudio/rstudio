/*
 * SourceAppCommand.java
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.EnabledChangedEvent;
import org.rstudio.core.client.command.EnabledChangedHandler;
import org.rstudio.core.client.command.ImageResourceProvider;
import org.rstudio.core.client.command.VisibleChangedEvent;
import org.rstudio.core.client.command.VisibleChangedHandler;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;

public class SourceAppCommand
{
   private class CommandSourceColumnToolbarButton extends ToolbarButton implements
                                                                        EnabledChangedHandler, VisibleChangedHandler
   {
      public CommandSourceColumnToolbarButton(SourceAppCommand sourceCmd,
                                              String buttonLabel,
                                              String buttonTitle,
                                              ImageResourceProvider imageResourceProvider,
                                              ClickHandler clickHandler,
                                              /* false when button is managed elsewhere */
                                              boolean synced)
      {
         super(buttonLabel, buttonTitle, imageResourceProvider, clickHandler);
         sourceCmd_ = sourceCmd;
         synced_ = synced;
      }

      @Override
      protected void onAttach()
      {
         super.onAttach();

         if (synced_)
         {
            setEnabled(buttonEnabled_);
            setVisible(buttonVisible_);
         }

         parentToolbar_ = getParentToolbar();
         assert sourceCmd_ != null;
         if (synced_)
         {
            handlers_.add(sourceCmd_.addEnabledChangedHandler(this));
            handlers_.add(sourceCmd_.addVisibleChangedHandler(this));
         }

         if (isVisible())
            setEnabled(true);
      }

      @Override
      protected void onDetach()
      {
         super.onDetach();
         if (synced_)
            handlers_.removeHandler();
      }

      @Override
      public void onEnabledChanged(EnabledChangedEvent event)
      {
         if (StringUtil.equals(column_, event.getColumnName()))
            setEnabled(event.getButtonEnabled());
      }

      @Override
      public void onVisibleChanged(VisibleChangedEvent event)
      {
         if (StringUtil.equals(column_, event.getColumnName()))
         {
            setVisible(event.getButtonVisible());
            parentToolbar_.invalidateSeparators();
         }
      }

      private final boolean synced_;
      private final SourceAppCommand sourceCmd_;
      private final HandlerRegistrations handlers_ = new HandlerRegistrations();
      private Toolbar parentToolbar_;
   }

   private class CommandSourceColumnMenuItem extends AppMenuItem
   {

      public CommandSourceColumnMenuItem(Command wrapper)
      {
         super(command_, false, wrapper);
      }
   }

   public SourceAppCommand(AppCommand command,
                           String column,
                           SourceColumnManager manager)
   {
      command_ = command;
      column_ = column;
      columnManager_ = manager;
   }

   public AppCommand getCommand()
   {
      return command_;
   }

   public String getColumn()
   {
      return column_;
   }

   public HandlerRegistration addEnabledChangedHandler(
      EnabledChangedHandler handler)
   {
      return handlers_.addHandler(EnabledChangedEvent.TYPE, handler);
   }

   public HandlerRegistration addVisibleChangedHandler(
      VisibleChangedHandler handler)
   {
      return handlers_.addHandler(VisibleChangedEvent.TYPE, handler);
   }

   public ToolbarButton createToolbarButton()
   {
      return createToolbarButton(true);
   }

   public ToolbarButton createUnsyncedToolbarButton()
   {
      return createToolbarButton(false);
   }

   public MenuItem createMenuItem()
   {
      return new CommandSourceColumnMenuItem(
         () -> {
            columnManager_.setActive(column_);
            command_.execute();
         });
   }

   public void setVisible(boolean visible)
   {
      setVisible(true, visible, visible);
   }

   public void setVisible(boolean setCommand, boolean commandVisible, boolean buttonVisible)
   {
      buttonVisible_ = buttonVisible;
      if (setCommand)
         command_.setVisible(commandVisible);
      handlers_.fireEvent(new VisibleChangedEvent(command_, column_, buttonVisible));
   }

   public void setEnabled(boolean enabled)
   {
      setEnabled(true, enabled, enabled);
   }

   public void setEnabled(boolean setCommand, boolean commandEnabled, boolean buttonEnabled)
   {
      buttonEnabled_ = buttonEnabled;
      if (setCommand)
         command_.setEnabled(commandEnabled);
      handlers_.fireEvent((new EnabledChangedEvent(command_, column_, buttonEnabled)));
   }

   private ToolbarButton createToolbarButton(boolean synced)
   {
      CommandSourceColumnToolbarButton button =
         new CommandSourceColumnToolbarButton(
            this,
            command_.getButtonLabel(),
            command_.getDesc(),
            command_,
            event -> {
               columnManager_.setActive(column_);
               command_.execute();
            },
            synced);
      if (command_.getTooltip() != null)
         button.setTitle(command_.getTooltip());
      return button;
   }

   private boolean buttonVisible_ = false;
   private boolean buttonEnabled_ = false;
   private final AppCommand command_;
   private final String column_;
   private final SourceColumnManager columnManager_;
   private final HandlerManager handlers_ = new HandlerManager(this);
}
