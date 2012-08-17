/*
 * WorkspacePane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.workspace;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.workspace.table.WorkspaceObjectTable;

public class WorkspacePane extends WorkbenchPane
                           implements Workspace.Display
{
   @Inject
   public WorkspacePane(WorkspaceObjectTable objectsTable, Commands commands)
   {
      super("Workspace");
      objectsTable_ = objectsTable;
      commands_ = commands;

      ensureWidget();
   }


   public WorkspaceObjectTable getWorkspaceObjectTable()
   {
      return objectsTable_;
   }

   @Override
   protected Widget createMainWidget()
   {
      Widget objectsTableView = (Widget) objectsTable_.getView();
      objectsTableView.setSize("100%", "100%");
      objectsTableView.getElement().getStyle().setProperty("overflowX",
                                                           "hidden");
      return objectsTableView;
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      return new Toolbar(
            new Widget[] {
                  createImportMenu(),
                  commands_.clearWorkspace().createToolbarButton()
            },
            new Widget[] {
                  commands_.refreshWorkspace().createToolbarButton()
            }
      );
   }
   
   private Widget createImportMenu()
   {
      ToolbarPopupMenu menu = new ToolbarPopupMenu();
      menu.addItem(commands_.importDatasetFromFile().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromURL().createMenuItem(false));
      return new ToolbarButton("Import Dataset", 
                               StandardIcons.INSTANCE.import_dataset(),
                               menu);
      
   }

   private final WorkspaceObjectTable objectsTable_;
   private final Commands commands_;
}