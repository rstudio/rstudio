package org.rstudio.studio.client.application.ui;

import java.util.ArrayList;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;


public class GlobalToolbar extends Toolbar
{
   public GlobalToolbar(Commands commands, FileTypeCommands fileTypeCommands)
   {
      super();
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles(); 
      addStyleName(styles.globalToolbar());
      
      
      // add newSourceDoc command
      ToolbarPopupMenu newMenu = new ToolbarPopupMenu();
      newMenu.addItem(commands.newSourceDoc().createMenuItem(false));
      
      // dynamically add other commands
      ArrayList<FileTypeCommands.CommandWithId> fileNewCommands = 
         fileTypeCommands.commandsWithIds(FileTypeRegistry.R);
      for (FileTypeCommands.CommandWithId cmd : fileNewCommands)
         newMenu.addItem(cmd.command.createMenuItem(false));
      
      // create and add new menu
      StandardIcons icons = StandardIcons.INSTANCE;
      ToolbarButton newButton = new ToolbarButton("",
                                                  icons.stock_new(),
                                                  newMenu);
      addLeftWidget(newButton);
      
      addLeftSeparator();
      addLeftWidget(commands.openSourceDoc().createToolbarButton());
      addLeftSeparator();
      addLeftWidget(commands.saveSourceDoc().createToolbarButton());
      addLeftSeparator();
      addLeftWidget(commands.printSourceDoc().createToolbarButton());
   }

   @Override
   public int getHeight()
   {
      return 27;
   }
}
