package org.rstudio.studio.client.workbench.views.vcs.console;

import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.studio.client.common.CommandLineHistory;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.ExecuteCommandResult;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;

public class ConsoleBarPresenter implements IsWidget
{
   public interface Display extends HasText, HasKeyDownHandlers, IsWidget
   {
   }

   @Inject
   public ConsoleBarPresenter(Display consoleBarView,
                              VCSServerOperations server,
                              GlobalDisplay globalDisplay)
   {
      consoleBarView_ = consoleBarView;
      server_ = server;
      globalDisplay_ = globalDisplay;

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
         }
      });

   }

   private void processCommand()
   {
      String command = consoleBarView_.getText();
      history_.addToHistory(command);
      history_.resetPosition();
      consoleBarView_.setText(defaultText_);

      server_.vcsExecuteCommand(command, new SimpleRequestCallback<ExecuteCommandResult>()
      {
         @Override
         public void onResponseReceived(ExecuteCommandResult response)
         {
            globalDisplay_.showMessage(GlobalDisplay.MSG_INFO,
                                       "Output",
                                       response.getOutput());
         }
      });
   }

   @Override
   public Widget asWidget()
   {
      return consoleBarView_.asWidget();
   }

   private CommandLineHistory history_;
   private String defaultText_ = "git ";
   private final Display consoleBarView_;
   private final VCSServerOperations server_;
   private final GlobalDisplay globalDisplay_;
}
