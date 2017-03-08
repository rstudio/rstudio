/*
 * SVNPane.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
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
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNPresenter.Display;

import java.util.ArrayList;

public class SVNPane extends WorkbenchPane implements Display
{
   @Inject
   public SVNPane(SVNChangelistTablePresenter changelistTablePresenter,
                  Session session,
                  Commands commands)
   {
      super(session.getSessionInfo().getVcsName());

      changelistTablePresenter_ = changelistTablePresenter;
      commands_ = commands;
   }

   @Override
   public void bringToFront()
   {
   }

   @Override
   public void setProgress(boolean showProgress)
   {
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();

      toolbar.addLeftWidget(commands_.vcsDiff().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsAddFiles().createToolbarButton());
      toolbar.addLeftWidget(commands_.vcsRemoveFiles().createToolbarButton());
      toolbar.addLeftSeparator();
      
      toolbar.addLeftWidget(commands_.vcsCommit().createToolbarButton());
      
      toolbar.addLeftSeparator();
      
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();

      moreMenu.addItem(commands_.vcsRevert().createMenuItem(false));
      moreMenu.addItem(commands_.vcsIgnore().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.vcsResolve().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.vcsPull().createMenuItem(false));
      moreMenu.addItem(commands_.vcsCleanup().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.vcsShowHistory().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.showShellDialog().createMenuItem(false));

      toolbar.addLeftWidget(new ToolbarButton(
          "More",
          new ImageResource2x(StandardIcons.INSTANCE.more_actions2x()),
          moreMenu));

      toolbar.addLeftSeparator();
      toolbar.addRightWidget(commands_.vcsRefresh().createToolbarButton());

      return toolbar;
   }
   
   @Override
   public void setItems(ArrayList<StatusAndPath> items)
   {
      getChangelistTable().setItems(items);
   }

   @Override
   public ArrayList<StatusAndPath> getSelectedItems()
   {
      return changelistTablePresenter_.getSelectedItems();
   }
   
   @Override
   public ChangelistTable getChangelistTable()
   {
      return changelistTablePresenter_.getView();
   }
   
   @Override
   public void showContextMenu(final int clientX, final int clientY)
   {
      final ToolbarPopupMenu menu = new ToolbarPopupMenu();
      
      menu.addItem(commands_.vcsDiff().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.vcsAddFiles().createMenuItem(false));
      menu.addItem(commands_.vcsRemoveFiles().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.vcsRevert().createMenuItem(false));
      menu.addItem(commands_.vcsIgnore().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.vcsResolve().createMenuItem(false));
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

   @Override
   protected Widget createMainWidget()
   {
      return changelistTablePresenter_.getView();
   }

   @Override
   public void onBeforeSelected()
   {
   }

   @Override
   public void onSelected()
   {
   }

   private final SVNChangelistTablePresenter changelistTablePresenter_;
   private final Commands commands_;
}
