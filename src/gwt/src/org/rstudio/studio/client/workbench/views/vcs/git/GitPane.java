/*
 * GitPane.java
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.inject.Inject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.vcs.CheckoutBranchToolbarButton;
import org.rstudio.studio.client.workbench.views.vcs.CreateBranchToolbarButton;
import org.rstudio.studio.client.workbench.views.vcs.git.GitPresenter.Display;

import java.util.ArrayList;

public class GitPane extends WorkbenchPane implements Display
{
   @Inject
   public GitPane(GitChangelistTablePresenter changelistTablePresenter,
                  Session session,
                  Commands commands,
                  CheckoutBranchToolbarButton switchBranchToolbarButton,
                  CreateBranchToolbarButton createBranchToolbarButton)
   {
      super(session.getSessionInfo().getVcsName());
      commands_ = commands;
      switchBranchToolbarButton_ = switchBranchToolbarButton;
      createBranchToolbarButton_ = createBranchToolbarButton;     
      
      table_ = changelistTablePresenter.getView();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
      moreMenu.addItem(commands_.vcsRevert().createMenuItem(false));
      moreMenu.addItem(commands_.vcsIgnore().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.showShellDialog().createMenuItem(false));

      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.vcsDiff().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsCommit().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(pullButton_ = commands_.vcsPull().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(pushButton_ = commands_.vcsPush().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(historyButton_ = commands_.vcsShowHistory().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(moreButton_ = new ToolbarButton(
            "More",
            new ImageResource2x(StandardIcons.INSTANCE.more_actions2x()),
            moreMenu));

      toolbar.addRightWidget(createBranchToolbarButton_);
      
      toolbar.addRightSeparator();
      
      toolbar.addRightWidget(switchBranchToolbarButton_);
      
      toolbar.addRightSeparator();
      
      toolbar.addRightWidget(new ToolbarButton(
            commands_.vcsRefresh().getImageResource(),
            new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  table_.showProgress();
                  commands_.vcsRefresh().execute();
               }
            }));
      
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
      
      pullButton_.setText(width > 550 ? "Pull" : "");
      pushButton_.setText(width > 550 ? "Push" : "");
      historyButton_.setText(width > 630 ? "History" : "");
      moreButton_.setText(width > 630 ? "More" : "");
      createBranchToolbarButton_.setText(width > 680 ? "New Branch" : "");
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
    
      menu.setPopupPositionAndShow(new PositionCallback() {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            menu.setPopupPosition(clientX, clientY);     
         }
      });
   }

   private ToolbarButton historyButton_;
   private ToolbarButton moreButton_;
   private ToolbarButton pullButton_;
   private ToolbarButton pushButton_;
   
   private final Commands commands_;
   private final CheckoutBranchToolbarButton switchBranchToolbarButton_;
   private final CreateBranchToolbarButton createBranchToolbarButton_;
   private GitChangelistTable table_;
}
