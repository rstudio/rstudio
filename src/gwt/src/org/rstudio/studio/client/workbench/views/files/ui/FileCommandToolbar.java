/*
 * FileCommandToolbar.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.files.ui;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.UserPrefMenuItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.files.FilesConstants;

public class FileCommandToolbar extends Toolbar
{
   @Inject
   public FileCommandToolbar(Commands commands, UserPrefs prefs)
   {
      super(constants_.fileCommandsLabel());
      StandardIcons icons = StandardIcons.INSTANCE;

      newFolderButton_ = commands.newFolder().createToolbarButton();
      addLeftWidget(newFolderButton_);
      addLeftSeparator();
      
      // New Blank File Menu
      ToolbarPopupMenu newFileMenu = new ToolbarPopupMenu();
      newFileMenu.addItem(commands.touchSourceDoc().createMenuItem(false));
      newFileMenu.addSeparator();
      newFileMenu.addItem(commands.touchRMarkdownDoc().createMenuItem(false));
      // if quarto is disabled, remove Quarto file from the new blank file dropdown
      AppCommand touchQuarto = commands.touchQuartoDoc();
      QuartoConfig quartoConfig = RStudioGinjector.INSTANCE.getSession().getSessionInfo().getQuartoConfig();
      touchQuarto.setVisible(quartoConfig.enabled);
      newFileMenu.addItem(touchQuarto.createMenuItem(false));
      newFileMenu.addSeparator();
      // these two commands will automatically set up files and folders, so they do not need touch commands
      newFileMenu.addItem(commands.newRShinyApp().createMenuItem(false));
      newFileMenu.addItem(commands.newRPlumberDoc().createMenuItem(false));
      newFileMenu.addSeparator();
      newFileMenu.addItem(commands.touchTextDoc().createMenuItem(false));
      newFileMenu.addItem(commands.touchCppDoc().createMenuItem(false));
      newFileMenu.addItem(commands.touchPythonDoc().createMenuItem(false));
      newFileMenu.addItem(commands.touchSqlDoc().createMenuItem(false));
      newFileMenu.addItem(commands.touchStanDoc().createMenuItem(false));
      newFileMenu.addItem(commands.touchD3Doc().createMenuItem(false));
      newFileMenu.addSeparator();
      newFileMenu.addItem(commands.touchSweaveDoc().createMenuItem(false));
      newFileMenu.addItem(commands.touchRHTMLDoc().createMenuItem(false));

      newFileButton_ = new ToolbarMenuButton(
            constants_.newBlankFileText(),
            constants_.createNewBlankFileText(),
            new ImageResource2x(icons.stock_new2x()),
            newFileMenu);
      ElementIds.assignElementId(newFileButton_, ElementIds.MB_FILES_TOUCH_FILE);
      addLeftWidget(newFileButton_);
      addLeftSeparator();
      
      uploadButton_ = commands.uploadFile().createToolbarButton();
      addLeftWidget(uploadButton_);

      addLeftSeparator();

      deleteButton_ = commands.deleteFiles().createToolbarButton();
      addLeftWidget(deleteButton_);

      renameButton_ = commands.renameFile().createToolbarButton();
      addLeftWidget(renameButton_);

      addLeftSeparator();

      // More
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
      moreMenu.addItem(commands.copyFile().createMenuItem(false));
      moreMenu.addItem(commands.copyFileTo().createMenuItem(false));
      moreMenu.addItem(commands.moveFiles().createMenuItem(false));
      moreMenu.addItem(commands.copyFilesPaneCurrentDirectory().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.openFilesInSinglePane().createMenuItem(false));
      moreMenu.addItem(commands.openEachFileInColumns().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.exportFiles().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.setAsWorkingDir().createMenuItem(false));
      moreMenu.addItem(commands.goToWorkingDir().createMenuItem(false));
      moreMenu.addItem(new UserPrefMenuItem<>(prefs.syncFilesPaneWorkingDir(), true,
         constants_.synchronizeWorkingDirectoryLabel(), prefs));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.openNewTerminalAtFilePaneLocation().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands.showFolder().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(new UserPrefMenuItem<>(prefs.showHiddenFiles(), true, constants_.showHiddenFilesLabel(), prefs));

      moreButton_ = new ToolbarMenuButton(
            constants_.moreText(),
            constants_.moreFileCommandsLabel(),
            new ImageResource2x(icons.more_actions2x()),
            moreMenu);
      ElementIds.assignElementId(moreButton_, ElementIds.MB_FILES_MORE);
      addLeftWidget(moreButton_);

      // Refresh
      ToolbarButton refreshButton = commands.refreshFiles().createToolbarButton();
      refreshButton.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());
      addRightWidget(refreshButton);

      this.addHandler(new ResizeHandler()
      {
         @Override
         public void onResize(ResizeEvent event)
         {
            manageToolbarSizes(event.getWidth());
         }
      }, ResizeEvent.getType());
   }

   private void manageToolbarSizes(int width) 
   {
      if (width < 450) 
      {
         newFolderButton_.setText("");
         newFileButton_.setText("");
         uploadButton_.setText("");
         deleteButton_.setText("");
         renameButton_.setText("");
         moreButton_.setText("");
      }
      else if (width < 540)
      {
         newFolderButton_.setText(constants_.folderText());
         newFileButton_.setText(constants_.blankFileText());
         uploadButton_.setText(constants_.uploadText());
         deleteButton_.setText(constants_.deleteText());
         renameButton_.setText(constants_.renameText());
         moreButton_.setText("");
      }
      else
      {
         newFolderButton_.setText(constants_.newFolderText());
         newFileButton_.setText(constants_.newBlankFileText());
         uploadButton_.setText(constants_.uploadText());
         deleteButton_.setText(constants_.deleteText());
         renameButton_.setText(constants_.renameText());
         moreButton_.setText(constants_.moreText());
      }
   }

   private ToolbarButton newFolderButton_;
   private ToolbarMenuButton newFileButton_;
   private ToolbarButton uploadButton_;
   private ToolbarButton deleteButton_;
   private ToolbarButton renameButton_;
   private ToolbarMenuButton moreButton_;
   private static final FilesConstants constants_ = GWT.create(FilesConstants.class);
}
