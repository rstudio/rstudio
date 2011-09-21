/*
 * ConsoleBarPresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs.console;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.studio.client.common.CommandLineHistory;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent.Handler;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

public class ConsoleBarPresenter
{
   public interface Display extends HasText, HasKeyDownHandlers, IsWidget,
                                    HasClickHandlers
   {
      void notifyOutputVisible(boolean visible);
   }

   public interface OutputDisplay extends IsWidget,
                                          HasEnsureVisibleHandlers,
                                          HasEnsureHiddenHandlers
   {
      void addCommand(String command);
      void addOutput(String output);
      void clearOutput();

      void onShow();
      void onBeforeHide();
   }

   @Inject
   public ConsoleBarPresenter(Display consoleBarView,
                              OutputDisplay outputView,
                              VCSServerOperations server)
   {
      consoleBarView_ = consoleBarView;
      outputView_ = outputView;
      server_ = server;

      history_ = new CommandLineHistory(consoleBarView_);

      consoleBarView_.setText(defaultText_);

      consoleBarView_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            int mod = KeyboardShortcut.getModifierValue(event.getNativeEvent());
            if (mod == KeyboardShortcut.NONE)
            {
               if (event.isUpArrow())
               {
                  history_.navigateHistory(-1);
                  event.preventDefault();
                  event.stopPropagation();
               }
               else if (event.isDownArrow())
               {
                  history_.navigateHistory(1);
                  event.preventDefault();
                  event.stopPropagation();
               }
               else if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
               {
                  event.preventDefault();
                  event.stopPropagation();

                  processCommand();
               }
            }
/*
            else if (mod == KeyboardShortcut.CTRL)
            {
               if (event.getNativeKeyCode() == 'L')
               {
                  event.preventDefault();
                  event.stopPropagation();

                  outputView_.clearOutput();
               }
            }
*/
         }
      });
   }

   private void processCommand()
   {
      String command = consoleBarView_.getText();
      history_.addToHistory(command);
      history_.resetPosition();
      consoleBarView_.setText(defaultText_);

      outputView_.addCommand(command);

      server_.vcsExecuteCommand(command, new SimpleRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess response)
         {
            response.addConsoleOutputHandler(new ConsoleOutputEvent.Handler()
            {
               @Override
               public void onConsoleOutput(ConsoleOutputEvent event)
               {
                  outputView_.addOutput(event.getOutput());
               }
            });

            response.start(new SimpleRequestCallback<Void>());
         }
      });
   }

   public OutputDisplay getOutputView()
   {
      return outputView_;
   }

   public Display getConsoleBarView()
   {
      return consoleBarView_;
   }

   public void setOutputVisible(boolean visible)
   {
      consoleBarView_.notifyOutputVisible(visible);
   }

   private final CommandLineHistory history_;
   private final String defaultText_ = "git ";
   private final Display consoleBarView_;
   private final OutputDisplay outputView_;
   private final VCSServerOperations server_;
}
