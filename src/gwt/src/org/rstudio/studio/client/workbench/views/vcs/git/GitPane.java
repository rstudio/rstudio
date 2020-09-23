/*
 * GitPane.java
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.inject.Inject;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.MonitoringMenuItem;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.vcs.CheckoutBranchToolbarButton;
import org.rstudio.studio.client.workbench.views.vcs.CreateBranchToolbarButton;
import org.rstudio.studio.client.workbench.views.vcs.git.GitPresenter.Display;

import java.util.ArrayList;

public class GitPane extends WorkbenchPane implements Display
{
   @Inject
   public GitPane(GitChangelistTablePresenter changelistTablePresenter,
                  GitServerOperations server,
                  Session session,
                  Commands commands,
                  GlobalDisplay display,
                  UserPrefs prefs,
                  CheckoutBranchToolbarButton switchBranchToolbarButton,
                  CreateBranchToolbarButton createBranchToolbarButton)
   {
      super(session.getSessionInfo().getVcsName());

      server_ = server;
      commands_ = commands;
      display_ = display;
      prefs_ = prefs;

      switchBranchToolbarButton_ = switchBranchToolbarButton;
      createBranchToolbarButton_ = createBranchToolbarButton;

      table_ = changelistTablePresenter.getView();
      table_.addStyleName("ace_editor_theme");

      if (Desktop.isDesktop())
      {
         WindowEx.addFocusHandler(new FocusHandler()
         {
            @Override
            public void onFocus(FocusEvent event)
            {
               if (prefs_.vcsAutorefresh().getGlobalValue())
               {
                  commands_.vcsRefreshNoError().execute();
               }
            }
         });
      }
   }

   private class GitMonitoringMenuItem extends MonitoringMenuItem
   {
      public GitMonitoringMenuItem(boolean monitoredValue)
      {
         super(
               refreshButton_,
               prefs_.vcsAutorefresh(),
               prefs_.vcsAutorefresh().getGlobalValue(),
               monitoredValue);
      }

      @Override
      public void onInvoked()
      {
         if (prefs_.vcsAutorefresh().getGlobalValue() != monitoredValue_)
         {
            prefs_.vcsAutorefresh().setGlobalValue(monitoredValue_);
            prefs_.writeUserPrefs();
         }
      }
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
      moreMenu.addItem(commands_.vcsRevert().createMenuItem(false));
      moreMenu.addItem(commands_.vcsIgnore().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.showShellDialog().createMenuItem(false));

      ToolbarPopupMenu pullMoreMenu = new ToolbarPopupMenu();
      pullMoreMenu.addItem(commands_.vcsPullRebase().createMenuItem(false));

      Toolbar toolbar = new Toolbar("Git Tab");
      toolbar.addLeftWidget(commands_.vcsDiff().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsCommit().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(pullButton_ = commands_.vcsPull().createToolbarButton());
      toolbar.addLeftWidget(new ToolbarMenuButton(ToolbarButton.NoText, "Pull options", pullMoreMenu, true));
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(pushButton_ = commands_.vcsPush().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(historyButton_ = commands_.vcsShowHistory().createToolbarButton());
      toolbar.addLeftSeparator();

      moreButton_ = new ToolbarMenuButton(
            "More",
            ToolbarButton.NoTitle,
            new ImageResource2x(StandardIcons.INSTANCE.more_actions2x()),
            moreMenu);
      ElementIds.assignElementId(moreButton_, ElementIds.MB_GIT_MORE);
      toolbar.addLeftWidget(moreButton_);

      toolbar.addRightWidget(createBranchToolbarButton_);

      toolbar.addRightSeparator();

      toolbar.addRightWidget(switchBranchToolbarButton_);
      switchBranchToolbarButton_.setRightAlignMenu(true);

      toolbar.addRightSeparator();

      refreshButton_ = new ToolbarButton(
            ToolbarButton.NoText,
            commands_.vcsRefresh().getTooltip(),
            commands_.vcsRefresh().getImageResource(),
            new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  table_.showProgress();
                  commands_.vcsRefresh().execute();
               }
            });
      ElementIds.assignElementId(refreshButton_, ElementIds.TB_GIT_REFRESH);
      toolbar.addRightWidget(refreshButton_);

      ToolbarPopupMenu refreshMenu = new ToolbarPopupMenu();
      refreshMenu.addItem(new GitMonitoringMenuItem(true));
      refreshMenu.addItem(new GitMonitoringMenuItem(false));
      refreshMenu.addSeparator();

      refreshMenu.addItem(new MenuItem(
            AppCommand.formatMenuLabel(null, "Refresh Now", null),
            true, // as HTML
            () -> commands_.vcsRefresh().execute()));

      toolbar.addRightWidget(new ToolbarMenuButton(
            ToolbarButton.NoText,
            "Refresh options",
            refreshMenu,
            false));

      return toolbar;
   }

   @Override
   public void onSelected()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {

         @Override
         public void execute()
         {
            manageToolbarSizes();
         }
      });
   }

   @Override
   public void onResize()
   {
      super.onResize();

      manageToolbarSizes();

   }

   private void manageToolbarSizes()
   {
      // sometimes width is passed in as 0 (not sure why)
      int width = getOffsetWidth();
      if (width == 0)
         return;

      pullButton_.setText(width > 600, "Pull");
      pushButton_.setText(width > 600, "Push");
      historyButton_.setText(width > 680, "History");
      moreButton_.setText(width > 680, "More");
      createBranchToolbarButton_.setText(width > 730, "New Branch");
   }

   @Override
   protected Widget createMainWidget()
   {
      return table_;
   }

   @Override
   public void setItems(ArrayList<StatusAndPath> items)
   {
      table_.setItems(items);
   }

   @Override
   public ArrayList<String> getSelectedPaths()
   {
      return table_.getSelectedPaths();
   }

   @Override
   public ArrayList<StatusAndPath> getSelectedItems()
   {
      return table_.getSelectedItems();
   }

   @Override
   public int getSelectedItemCount()
   {
      return table_.getSelectedItems().size();
   }

   @Override
   public void onRefreshBegin()
   {
      table_.showProgress();
   }

   @Override
   public HandlerRegistration addSelectionChangeHandler(Handler handler)
   {
      return table_.addSelectionChangeHandler(handler);
   }

   @Override
   public GitChangelistTable getChangelistTable()
   {
      return table_;
   }

   @Override
   public void showContextMenu(final int clientX, final int clientY)
   {
      final ToolbarPopupMenu menu = new ToolbarPopupMenu();

      menu.addItem(commands_.vcsDiff().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.vcsRevert().createMenuItem(false));
      menu.addItem(commands_.vcsIgnore().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.vcsOpen().createMenuItem(false));

      menu.showRelativeTo(clientX, clientY);
   }

   private ToolbarButton historyButton_;
   private ToolbarMenuButton moreButton_;
   private ToolbarButton pullButton_;
   private ToolbarButton pushButton_;
   private ToolbarButton refreshButton_;

   @SuppressWarnings("unused")
   private final GitServerOperations server_;
   private final Commands commands_;
   @SuppressWarnings("unused")
   private final GlobalDisplay display_;
   private final UserPrefs prefs_;

   private final CheckoutBranchToolbarButton switchBranchToolbarButton_;
   private final CreateBranchToolbarButton createBranchToolbarButton_;
   private final GitChangelistTable table_;
}
