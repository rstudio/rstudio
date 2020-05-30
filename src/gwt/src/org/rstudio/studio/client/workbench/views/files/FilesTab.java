/*
 * FilesTab.java
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
package org.rstudio.studio.client.workbench.views.files;

import com.google.inject.Inject;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenFileInBrowserHandler;
import org.rstudio.studio.client.common.filetypes.events.RenameSourceFileEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateEvent;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateHandler;

public class FilesTab extends DelayLoadWorkbenchTab<Files>
{
   public interface Binder extends CommandBinder<Commands, FilesTab.Shim> {}
   
   public abstract static class Shim
         extends DelayLoadTabShim<Files, FilesTab>
         implements OpenFileInBrowserHandler, DirectoryNavigateHandler, RenameSourceFileEvent.Handler
   {
      @Handler
      public abstract void onUploadFile();
      @Handler
      public abstract void onSetWorkingDirToFilesPane();
      @Handler
      public abstract void onGoToWorkingDir();
      @Handler
      public abstract void onCopyFilesPaneCurrentDirectory();
   }

   @Inject
   public FilesTab(Shim shim,
                   Binder binder,
                   EventBus events,
                   Commands commands)
   {
      super("Files", shim);
      binder.bind(commands, shim);
      events.addHandler(OpenFileInBrowserEvent.TYPE, shim);
      events.addHandler(DirectoryNavigateEvent.TYPE, shim);
      events.addHandler(RenameSourceFileEvent.TYPE, shim);
   }
}
