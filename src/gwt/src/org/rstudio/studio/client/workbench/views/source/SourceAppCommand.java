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
import org.rstudio.core.client.widget.ToolbarButton;

public class SourceAppCommand
{
   private class CommandSourceColumnToolbarButton extends ToolbarButton implements
                                                                        EnabledChangedHandler, VisibleChangedHandler
   {
      public CommandSourceColumnToolbarButton(String buttonLabel,
                                              String buttonTitle,
                                              ImageResourceProvider imageResourceProvider,
                                              ClickHandler clickHandler)
      {
         super(buttonLabel, buttonTitle, imageResourceProvider, clickHandler);
      }

      @Override
      protected void onAttach()
      {
         super.onAttach();

         assert command_ != null;
         handlers_.add(command_.addEnabledChangedHandler(this));
         handlers_.add(command_.addVisibleChangedHandler(this));

         if (isVisible())
            setEnabled(true);
      }

      @Override
      protected void onDetach()
      {
         super.onDetach();
         handlers_.removeHandler();
      }

      @Override
      public void onEnabledChanged(EnabledChangedEvent event)
      {
         if (StringUtil.equals(column_.getName(), event.getColumnName()))
            setEnabled(event.getButtonEnabled());
      }

      @Override
      public void onVisibleChanged(VisibleChangedEvent event)
      {
         if (StringUtil.equals(column_.getName(), event.getColumnName()))
            setVisible(event.getButtonVisible());
      }

      private final HandlerRegistrations handlers_ = new HandlerRegistrations();
   }

   private static class CommandSourceColumnMenuItem extends AppMenuItem
   {

      public CommandSourceColumnMenuItem(Command wrapper)
      {
         super(command_, false, wrapper);
      }
   }

   public SourceAppCommand(AppCommand command,
                           SourceColumn column,
                           SourceColumnManager manager)
   {
      command_ = command;
      column_ = column;
      columnManager_ = manager;
   }

   public ToolbarButton createToolbarButton()
   {
      CommandSourceColumnToolbarButton button =
         new CommandSourceColumnToolbarButton(
            command_.getButtonLabel(),
            command_.getDesc(),
            command_,
            event -> {
               columnManager_.setActive(column_.getName());
               command_.execute();
            });
      if (command_.getTooltip() != null)
         button.setTitle(command_.getTooltip());
      return button;
   }

   public MenuItem createMenuItem()
   {
      return new CommandSourceColumnMenuItem(
         () -> {
            columnManager_.setActive(column_.getName());
            command_.execute();
         });
   }

   public void setVisible(boolean commandVisible, boolean buttonVisible)
   {
      command_.setVisible(commandVisible, column_.getName());
      handlers_.fireEvent(new VisibleChangedEvent(command_, column_.getName(), buttonVisible));
   }

   public void setEnabled(boolean commandEnabled, boolean buttonVisible)
   {
      command_.setEnabled(commandEnabled, column_.getName());
      handlers_.fireEvent((new EnabledChangedEvent(command_, column_.getName(), buttonVisible)));
   }

   private static AppCommand command_ = null;
   private final SourceColumn column_;
   private final SourceColumnManager columnManager_;
   private final HandlerManager handlers_ = new HandlerManager(this);
}
