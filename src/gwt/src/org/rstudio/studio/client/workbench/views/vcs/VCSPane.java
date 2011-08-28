/*
 * VCSPane.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.vcs.VCS.Display;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarFramePanel;

import java.util.ArrayList;

public class VCSPane extends WorkbenchPane implements Display
{
   private class HasValueChangeHandlersImpl<T> implements HasValueChangeHandlers<T>
   {
      @Override
      public HandlerRegistration addValueChangeHandler(ValueChangeHandler<T> handler)
      {
         return handlers_.addHandler(ValueChangeEvent.getType(), handler);
      }

      @Override
      public void fireEvent(GwtEvent<?> event)
      {
         handlers_.fireEvent(event);
      }

      private final HandlerManager handlers_ = new HandlerManager(this);
   }

   @Inject
   public VCSPane(ConsoleBarFramePanel consoleBarFrame,
                  ChangelistTablePresenter changelistTablePresenter,
                  Session session,
                  Commands commands,
                  BranchToolbarButton branchToolbarButton)
   {
      super(session.getSessionInfo().getVcsName());
      consoleBarFrame_ = consoleBarFrame;
      commands_ = commands;
      branchToolbarButton_ = branchToolbarButton;

      changelistTablePresenter.initializeClientState();
      table_ = changelistTablePresenter.getView();
      consoleBarFrame_.setWidget(table_);
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
      moreMenu.addItem(commands_.vcsIgnore().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.vcsPull().createMenuItem(false));
      moreMenu.addItem(commands_.vcsPush().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.vcsShowHistory().createMenuItem(false));

      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.vcsDiff().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsRevert().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsCommit().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(new ToolbarButton(
            "More",
            StandardIcons.INSTANCE.more_actions(),
            moreMenu));

      toolbar.addRightWidget(branchToolbarButton_);
      toolbar.addRightWidget(commands_.vcsRefresh().createToolbarButton());
      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      return consoleBarFrame_;
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

   private final Commands commands_;
   private final BranchToolbarButton branchToolbarButton_;
   private ChangelistTable table_;
   private ConsoleBarFramePanel consoleBarFrame_;
}
