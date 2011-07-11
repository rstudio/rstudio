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
