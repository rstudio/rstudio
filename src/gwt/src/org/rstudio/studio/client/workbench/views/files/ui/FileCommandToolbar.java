/*
 * FileCommandToolbar.java
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
package org.rstudio.studio.client.workbench.views.files.ui;

import com.google.inject.Inject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;

public class FileCommandToolbar extends Toolbar
{
   @Inject
   public FileCommandToolbar(Commands commands, Session session)
   {
      StandardIcons icons = StandardIcons.INSTANCE;

      addLeftWidget(commands.newFolder().createToolbarButton());
      addLeftSeparator();
      addLeftWidget(commands.uploadFile().createToolbarButton());
      addLeftSeparator();
      addLeftWidget(commands.deleteFiles().createToolbarButton());
      addLeftWidget(commands.renameFile().createToolbarButton());
      addLeftSeparator();

      // More
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
      moreMenu.addItem(commands.copyFile().createMenuItem(false));
      moreMenu.addItem(commands.copyFileTo().createMenuItem(false));
      moreMenu.addItem(commands.moveFiles().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.exportFiles().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.setAsWorkingDir().createMenuItem(false));
      moreMenu.addItem(commands.goToWorkingDir().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.showFolder().createMenuItem(false));

      ToolbarButton moreButton = new ToolbarButton("More",
                                                  new ImageResource2x(icons.more_actions2x()),
                                                  moreMenu);
      addLeftWidget(moreButton);
      

      // Refresh
      ToolbarButton refreshButton = commands.refreshFiles().createToolbarButton();
      addRightWidget(refreshButton);
   }
}
