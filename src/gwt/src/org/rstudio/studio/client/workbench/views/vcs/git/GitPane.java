/*
 * GitPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.core.client.GWT;
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
import org.rstudio.studio.client.workbench.views.vcs.ViewVcsConstants;
import org.rstudio.studio.client.workbench.views.vcs.common.events.BranchCaptionChangedEvent;
import org.rstudio.studio.client.workbench.views.vcs.git.GitPresenter.Display;

import java.util.ArrayList;

public class GitPane extends WorkbenchPane implements Display, BranchCaptionChangedEvent.Handler
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
      switchBranchToolbarButton.addBranchCaptionChangedHandler(this);
      createBranchToolbarButton_ = createBranchToolbarButton;

      table_ = changelistTablePresenter.getView();
      table_.addStyleName("ace_editor_theme");
      
      presenter_ = changelistTablePresenter;

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
      moreMenu.addItem(commands_.newTerminal().createMenuItem(false));

      ToolbarPopupMenu pullMoreMenu = new ToolbarPopupMenu();
      pullMoreMenu.addItem(commands_.vcsPullRebase().createMenuItem(false));

      Toolbar toolbar = new Toolbar(constants_.gitTabCapitalized());
      toolbar.addLeftWidget(diffButton_ = commands_.vcsDiff().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commitButton_ = commands_.vcsCommit().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(pullButton_ = commands_.vcsPull().createToolbarButton());
      toolbar.addLeftWidget(new ToolbarMenuButton(ToolbarButton.NoText, constants_.pullOptions(), pullMoreMenu, true));
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(pushButton_ = commands_.vcsPush().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(historyButton_ = commands_.vcsShowHistory().createToolbarButton());
      toolbar.addLeftSeparator();

      moreButton_ = new ToolbarMenuButton(
            constants_.moreCapitalized(),
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
            AppCommand.formatMenuLabel(null, constants_.refreshNowCapitalized(), null),
            true, // as HTML
            () -> commands_.vcsRefresh().execute()));

      toolbar.addRightWidget(new ToolbarMenuButton(
            ToolbarButton.NoText,
            constants_.refreshOptions(),
            refreshMenu,
            false));

      return toolbar;
   }
   
   @Override
   public void onBeforeSelected()
   {
      boolean minimal = !prefs_.vcsAutorefresh().getValue();
      presenter_.refresh(minimal);
   }

   @Override
   public void onSelected()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            manageToolbarSizes();
         }
      });
   }

   @Override
   public void onBranchCaptionChanged(BranchCaptionChangedEvent event) 
   {
      manageToolbarSizes();
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

      width = width - switchBranchToolbarButton_.getOffsetWidth() ;
      
      diffButton_.setText(width > 360, constants_.diffCapitalized());
      commitButton_.setText(width > 360, constants_.commitCapitalized());
      pullButton_.setText(width > 400, constants_.pullCapitalized());
      pushButton_.setText(width > 440, constants_.pushCapitalized());
      historyButton_.setText(width > 470, constants_.historyCapitalized());
      moreButton_.setText(width > 500, constants_.moreCapitalized());
      createBranchToolbarButton_.setText(width > 580, constants_.newBranchCapitalized());
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

      menu.showRelativeTo(clientX, clientY, ElementIds.GIT_TAB_CONTEXT);
   }

   private ToolbarButton diffButton_;
   private ToolbarButton commitButton_;
   private ToolbarButton historyButton_;
   private ToolbarMenuButton moreButton_;
   private ToolbarButton pullButton_;
   private ToolbarButton pushButton_;
   private ToolbarButton refreshButton_;

   private final GitServerOperations server_;
   private final Commands commands_;
   @SuppressWarnings("unused")
   private final GlobalDisplay display_;
   private final UserPrefs prefs_;

   private final GitChangelistTable table_;
   private final GitChangelistTablePresenter presenter_;
   private final CheckoutBranchToolbarButton switchBranchToolbarButton_;
   private final CreateBranchToolbarButton createBranchToolbarButton_;
   
   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}
