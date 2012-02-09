/*
 * ConsolePane.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.shell.Shell;

public class ConsolePane extends WorkbenchPane
   implements Console.Display, CanFocus
{
   @Inject
   public ConsolePane(Provider<Shell> consoleProvider,
                      final EventBus events,
                      Commands commands)
   {
      super("Console");

      consoleProvider_ = consoleProvider ;
      commands_ = commands;

      // console is interacted with immediately so we make sure it
      // is always created during startup
      ensureWidget();

      new Console(this, events, commands);
   }

      commands.goToFileFunction().addHandler(new CommandHandler()
      {
         @Override
         public void onCommand(AppCommand command)
         {
            events.fireEvent(new CompilePdfOutputEvent());
         }
      });
      commands.reflowComment().addHandler(new CommandHandler()
      {
         @Override
         public void onCommand(AppCommand command)
         {
            events.fireEvent(new FindInFilesResultEvent());
         }
      });
   }

   public void setWorkingDirectory(String directory)
   {
      workingDir_.setText(directory);
   }

   public void focus()
   {
      shell_.getDisplay().focus();
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
      toolbar.addRightWidget(commands_.interruptR().createToolbarButton());
      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
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

   private Provider<Shell> consoleProvider_ ;
   private final Commands commands_;
   private Shell shell_;
   private Label workingDir_;
}
