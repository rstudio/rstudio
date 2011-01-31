/*
 * WorkspaceTab.java
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
package org.rstudio.studio.client.workbench.views.workspace.table;

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.workspace.Workspace;

public class WorkspaceTab extends DelayLoadWorkbenchTab<Workspace>
{
   public interface Binder extends CommandBinder<Commands, WorkspaceTab.Shim>
   {
   }

   public abstract static class Shim extends
         DelayLoadTabShim<Workspace, WorkspaceTab>
   {
      @Handler
      public abstract void onLoadWorkspace();
      @Handler
      public abstract void onLoadDefaultWorkspace();
      @Handler
      public abstract void onSaveWorkspace();
      @Handler
      public abstract void onSaveDefaultWorkspace();
      @Handler
      public abstract void onImportDatasetFromFile();
      @Handler
      public abstract void onImportDatasetFromURL();
      @Handler
      public abstract void onImportDatasetFromGoogleSpreadsheet();
      @Handler
      public abstract void onClearWorkspace();
   }

   @Inject
   public WorkspaceTab(Shim shim, Binder binder, Commands commands)
   {
      super("Workspace", shim);
      binder.bind(commands, shim);
   }
}
