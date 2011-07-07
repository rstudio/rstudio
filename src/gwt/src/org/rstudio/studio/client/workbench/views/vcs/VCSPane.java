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

import java.util.ArrayList;

public class VCSPane extends WorkbenchPane implements Display
{
   @Inject
   public VCSPane(Session session, Commands commands)
   {
      super(session.getSessionInfo().getVcsName());
      commands_ = commands;
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

      toolbar.addRightWidget(commands_.vcsRefresh().createToolbarButton());
      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      table_ = new ChangelistTable();
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

   private final Commands commands_;
   private ChangelistTable table_;
   private ArrayList<StatusAndPath> items_;
}
