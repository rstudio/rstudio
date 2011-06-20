package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;


public class GlobalToolbar extends Toolbar
{
   public GlobalToolbar(Commands commands)
   {
      super();
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles(); 
      addStyleName(styles.globalToolbar());
      
      StandardIcons icons = StandardIcons.INSTANCE;
      
      ToolbarPopupMenu newMenu = new ToolbarPopupMenu();
      newMenu.addItem(commands.newSourceDoc().createMenuItem(false));
      

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
