/*
 * FileMRUList.java
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
package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileMRUList extends MRUList
{
   @Inject 
   public FileMRUList(Commands commands,
                      final FileTypeRegistry fileTypeRegistry,
                      Session session)
   {
      super(commands, 
            session, 
            "mru",
            new AppCommand[] {
                  commands.mru0(),
                  commands.mru1(),
                  commands.mru2(),
                  commands.mru3(),
                  commands.mru4(),
                  commands.mru5(),
                  commands.mru6(),
                  commands.mru7(),
                  commands.mru8(),
                  commands.mru9()
            },
            commands.clearRecentFiles(),
            true,
            new OperationWithInput<String>() 
            {
               @Override
               public void execute(String file)
               {
                  fileTypeRegistry.editFile(FileSystemItem.createFile(file));   
               }
            });
   }
}
