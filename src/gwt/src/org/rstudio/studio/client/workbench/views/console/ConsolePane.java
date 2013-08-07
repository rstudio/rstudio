/*
 * ConsolePane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.theme.ExplicitSizeWidget;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.events.ErrorHandlerChangedEvent;
import org.rstudio.studio.client.common.debugging.model.ErrorHandlerType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.shell.Shell;

public class ConsolePane extends WorkbenchPane
   implements Console.Display, CanFocus, ErrorHandlerChangedEvent.Handler
{
   @Inject
   public ConsolePane(Provider<Shell> consoleProvider,
                      final EventBus events,
                      Commands commands)
   {
      super("Console");

      consoleProvider_ = consoleProvider ;
      commands_ = commands;
      debugMode_ = false;

      // console is interacted with immediately so we make sure it
      // is always created during startup
      ensureWidget();

      errorManagementMenu_ = new ToolbarPopupMenu();      
      errorManagementMenu_.addItem(
            commands_.errorsAutomatic().createMenuItem(false));
      errorManagementMenu_.addItem(
            commands_.errorsBreak().createMenuItem(false));
      errorManagementMenu_.addItem(
            commands_.errorsBreakUser().createMenuItem(false));
      errorManagementMenu_.addItem(
            commands_.errorsIgnore().createMenuItem(false));
      errorManagementButton_ = new ToolbarButton(
            "", null, errorManagementMenu_);
      setErrorToolbarHandlerType(ErrorHandlerType.ERRORS_AUTOMATIC);
      
      events.addHandler(ErrorHandlerChangedEvent.TYPE, this);

      new Console(this, events, commands);
   }

   public void setWorkingDirectory(String directory)
   {
      workingDir_.setText(directory);
   }

   public void focus()
   {
      shell_.getDisplay().focus();
   }

   @Override
   public IsWidget getConsoleInterruptButton()
   {
      return consoleInterruptButton_;
   }
   
   public ExplicitSizeWidget getErrorManagementMenu()
   {
      return new ExplicitSizeWidget(errorManagementButton_, 40, 150);
   }
   
   public int getCharacterWidth()
   {
      return shell_.getDisplay().getCharacterWidth();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      workingDir_ = new Label();
      workingDir_.setStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar.addLeftWidget(workingDir_);
      toolbar.addLeftWidget(commands_.goToWorkingDir().createToolbarButton());
      consoleInterruptButton_ = commands_.interruptR().createToolbarButton();
      toolbar.addRightWidget(consoleInterruptButton_);
      toolbar.addRightPopupMenu(new Label("Errors"), errorManagementMenu_);
      return toolbar;
   }
   
   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      SecondaryToolbar toolbar = new SecondaryToolbar(true);
      toolbar.addLeftWidget(commands_.debugStep().createToolbarButton());  
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.debugContinue().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.debugStop().createToolbarButton());    
      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      // initialize the debug toolbar--generally this hides it until debug state
      // is entered.
      setDebugMode(debugMode_);

      shell_ = consoleProvider_.get() ;
      return (Widget) shell_.getDisplay() ;
   }

   @Override
   public void onBeforeUnselected()
   {
      shell_.onBeforeUnselected();
   }

   @Override
   public void onBeforeSelected()
   {
      shell_.onBeforeSelected();
   }

   @Override
   public void onSelected()
   {
      shell_.onSelected();
   }

   @Override
   public void setDebugMode(boolean debugMode)
   {
      debugMode_ = debugMode;
      setSecondaryToolbarVisible(debugMode_);
      
      if (debugMode_)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute()
            {
               if (shell_ != null)
                  shell_.getDisplay().ensureInputVisible();
            }
         });
      }
   }

   @Override
   public void onErrorHandlerChanged(ErrorHandlerChangedEvent event)
   {
      setErrorToolbarHandlerType(event.getHandlerType().getType());
   }
   
   private void setErrorToolbarHandlerType(int type)
   {
      errorManagementButton_.setText("Errors: " + 
            ErrorHandlerType.getNameOfType(type));
   }
   
   private Provider<Shell> consoleProvider_ ;
   private final Commands commands_;
   private Shell shell_;
   private Label workingDir_;
   private ToolbarButton consoleInterruptButton_;
   private ToolbarButton errorManagementButton_;
   private ToolbarPopupMenu errorManagementMenu_;
   private boolean debugMode_;
}
