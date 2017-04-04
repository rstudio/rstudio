/*
 * ProfilerType.java
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
package org.rstudio.studio.client.common.filetypes;

import java.util.HashSet;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.OpenProfileEvent;

public class ProfilerType extends EditableFileType
{
   public ProfilerType()
   {
      super("r_prof", "R Profiler",
            new ImageResource2x(FileIconResources.INSTANCE.iconRdoc2x()));
   }
   
   @Override
   public void openFile(FileSystemItem file,
                        FilePosition position,
                        int navMethod,
                        EventBus eventBus)
   {
      eventBus.fireEvent(new OpenProfileEvent(
            file.getPath(), null, null, false, null));
   }
   
   @Override
   public void openFile(FileSystemItem file, EventBus eventBus)
   {
      openFile(file, null, NavigationMethods.DEFAULT, eventBus);
   }

   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> results = new HashSet<AppCommand>();
      results.add(commands.saveSourceDoc());
      results.add(commands.saveSourceDocAs());
      results.add(commands.saveProfileAs());

      return results;
   }
   
   public String getDefaultExtension()
   {
      return ".Rprofvis";
   }
}
