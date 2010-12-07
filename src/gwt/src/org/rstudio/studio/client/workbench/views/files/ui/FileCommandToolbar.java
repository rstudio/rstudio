/*
 * FileCommandToolbar.java
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
package org.rstudio.studio.client.workbench.views.files.ui;

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;

public class FileCommandToolbar extends Toolbar
{
   public FileCommandToolbar(Commands commands)
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
      moreMenu.addItem(commands.moveFiles().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.exportFiles().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.syncWorkingDir().createMenuItem(false));
      moreMenu.addItem(commands.showFolder().createMenuItem(false));

      ToolbarButton moreButton = new ToolbarButton("More",
                                                  icons.more_actions(),
                                                  moreMenu);
      addLeftWidget(moreButton);
      

      // Refresh
      ToolbarButton refreshButton = commands.refreshFiles().createToolbarButton();
      addRightWidget(refreshButton);
   }
}
