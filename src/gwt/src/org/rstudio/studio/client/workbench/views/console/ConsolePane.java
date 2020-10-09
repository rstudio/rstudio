/*
 * ConsolePane.java
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
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Stack;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.shell.Shell;
import org.rstudio.studio.client.workbench.views.jobs.JobProgressPresenter;
import org.rstudio.studio.client.workbench.views.jobs.model.LocalJobProgress;

public class ConsolePane extends WorkbenchPane
   implements Console.Display, CanFocus
{
   enum ConsoleMode
   {
      Normal,     // typical R console mode
      Debug,      // ongoing debugging session
      Profiler,   // ongoing profile session
      Job         // ongoing asynchronous job
   }

   @Inject
   public ConsolePane(Provider<Shell> consoleProvider,
                      Provider<JobProgressPresenter> progressProvider,
                      final EventBus events,
                      Commands commands,
                      Session session)
   {
      // We pass null in place of events here to prevent ActivePaneEvent from being called.
      // ActivatePaneEvent isn't necessary and causes an exception for the Console Pane.
      super("Console", null);

      consoleProvider_ = consoleProvider;
      progressProvider_ = progressProvider;
      commands_ = commands;
      session_ = session;

      // the secondary toolbar can have several possible states that obscure
      // each other; we keep track of the stack here
      mode_ = new Stack<>();

      ElementIds.assignElementId(this, ElementIds.WORKBENCH_PANEL + "_console");

      // technically this is only playing aria-tabpanel role when the console pane
      // has tabs (e.g. at least one other pane is being shown, such as Terminal), but
      // having it always marked with that role is fine
      Roles.getTabpanelRole().set(this.getElement());
      Roles.getTabpanelRole().setAriaLabelProperty(this.getElement(), "Console");

      // console is interacted with immediately so we make sure it
      // is always created during startup
      ensureWidget();

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
   public void ensureCursorVisible()
   {
      shell_.getDisplay().ensureInputVisible();
   }

   @Override
   public IsWidget getConsoleInterruptButton()
   {
      return consoleInterruptButton_;
   }

   @Override
   public IsWidget getConsoleClearButton()
   {
      return consoleClearButton_;
   }

   @Override
   public IsWidget getProfilerInterruptButton()
   {
      return profilerInterruptButton_;
   }

   public int getCharacterWidth()
   {
      return shell_.getDisplay().getCharacterWidth();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar("Console Tab");
      workingDir_ = new Label();
      workingDir_.setStyleName(ThemeStyles.INSTANCE.subtitle());
      toolbar.addLeftWidget(workingDir_);
      toolbar.addLeftWidget(commands_.goToWorkingDir().createToolbarButton());
      consoleInterruptButton_ = commands_.interruptR().createToolbarButton();
      consoleClearButton_ = commands_.consoleClear().createToolbarButton();
      consoleClearButton_.addStyleName(ThemeStyles.INSTANCE.consoleClearButton());
      consoleClearButton_.setVisible(true);

      profilerInterruptButton_ = ConsoleInterruptProfilerButton.CreateProfilerButton();
      profilerInterruptButton_.setVisible(false);

      toolbar.addRightWidget(profilerInterruptButton_);
      toolbar.addRightWidget(consoleInterruptButton_);
      toolbar.addRightWidget(consoleClearButton_);

      return toolbar;
   }

   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      secondaryToolbar_ = new SecondaryToolbar(true, "Console Tab Second");
      secondaryToolbar_.getWrapper().addStyleName(ThemeStyles.INSTANCE.tallerToolbarWrapper());
       
      return secondaryToolbar_;
   }

   @Override
   protected Widget createMainWidget()
   {
      // initialize the debug toolbar--generally this hides it until debug state
      // is entered.
      syncSecondaryToolbar();

      shell_ = consoleProvider_.get();
      shell_.getDisplay().setTextInputAriaLabel("Console");
      return (Widget) shell_.getDisplay();
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
   public void setFocus()
   {
      focus();
   }

   @Override
   public void enterMode(ConsoleMode mode)
   {
      // ignore if this mode is already in the stack
      if (mode_.contains(mode))
         return;

      // add to the node stack
      mode_.add(mode);

      // show the toolbar corresponding to the mode
      syncSecondaryToolbar();

      // if switching into debug mode, sync cursor state too
      if (mode == ConsoleMode.Debug)
      {
         Scheduler.get().scheduleFinally(() -> ensureCursorVisible());
      }
   }

   @Override
   public void leaveMode(ConsoleMode mode)
   {
      ConsoleMode prevMode = mode();

      // the mode may not be at the top of the stack, and at most one mode of
      // each type may be in the stack, so it's safe to just remove all
      // instances of the mode from the queue
      mode_.removeIf((ConsoleMode t) ->
      {
         return t == mode;
      });

      ConsoleMode newMode = mode();

      // this should not happen, but safely ignore it if it does
      if (prevMode == newMode)
         return;

      // clear progress event when exiting job mode
      if (prevMode == ConsoleMode.Job)
         lastProgress_ = null;

      // show the new topmost mode in the stack
      syncSecondaryToolbar();
   }

   @Override
   public ConsoleMode mode()
   {
      if (mode_.isEmpty())
         return ConsoleMode.Normal;
      return mode_.peek();
   }

   @Override
   public void showProgress(LocalJobProgress progress)
   {
      // show progress if we're in progress mode
      if (progress_ != null)
         progress_.showProgress(progress);
      lastProgress_ = progress;
   }

   private void syncSecondaryToolbar()
   {      
      // show the toolbar if we're not in normal mode
      setSecondaryToolbarVisible(mode() != ConsoleMode.Normal);

      // clean up toolbar in preparation for switching modes
      secondaryToolbar_.removeAllWidgets();
      progress_ = null;

      switch(mode())
      {
        case Debug:
           initDebugToolbar();
           break;

        case Profiler:
           initProfilerToolbar();
           break;

        case Job:
           initJobToolbar();
           break;

        case Normal:
           // no work necessary here, we won't show the toolbar
           break;
      }
   }

   private void initDebugToolbar()
   {
      secondaryToolbar_.setLabel("Console Tab Debug");
      secondaryToolbar_.addLeftWidget(commands_.debugStep().createToolbarButton()); 
      if (session_.getSessionInfo().getHaveAdvancedStepCommands())
      {
         secondaryToolbar_.addLeftSeparator();
         secondaryToolbar_.addLeftWidget(commands_.debugStepInto().createToolbarButton());
         secondaryToolbar_.addLeftSeparator();
         secondaryToolbar_.addLeftWidget(commands_.debugFinish().createToolbarButton());
      }
      secondaryToolbar_.addLeftSeparator();
      secondaryToolbar_.addLeftWidget(commands_.debugContinue().createToolbarButton());
      secondaryToolbar_.addLeftSeparator();
      secondaryToolbar_.addLeftWidget(commands_.debugStop().createToolbarButton());
   }

   private void initProfilerToolbar()
   {
      secondaryToolbar_.setLabel("Console Tab Profiler");
      secondaryToolbar_.addLeftWidget(commands_.stopProfiler().createToolbarButton()); 
   }

   private void initJobToolbar()
   {
      progress_ = progressProvider_.get();
      secondaryToolbar_.setLabel("Console Tab Job Progress");
      secondaryToolbar_.addLeftWidget(progress_.asWidget());
      secondaryToolbar_.setLeftWidgetWidth(progress_.asWidget(), "100%");

      if (lastProgress_ != null)
         showProgress(lastProgress_);
   }

   private Provider<Shell> consoleProvider_;
   private Provider<JobProgressPresenter> progressProvider_;
   JobProgressPresenter progress_;
   LocalJobProgress lastProgress_;
   private final Commands commands_;
   private Shell shell_;
   private Session session_;
   private Label workingDir_;
   private ToolbarButton consoleInterruptButton_;
   private ToolbarButton consoleClearButton_;
   private Image profilerInterruptButton_;
   private Stack<ConsoleMode> mode_;
   private SecondaryToolbar secondaryToolbar_;
}
