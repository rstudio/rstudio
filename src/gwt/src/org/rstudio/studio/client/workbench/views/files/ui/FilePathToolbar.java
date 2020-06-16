/*
 * FilePathToolbar.java
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
package org.rstudio.studio.client.workbench.views.files.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemContext.Callbacks;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.PosixFileSystemContext;
import org.rstudio.core.client.files.filedialog.PathBreadcrumbWidget;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CheckBoxHiddenLabel;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.workbench.views.files.Files;

import java.util.ArrayList;
import java.util.Arrays;

public class FilePathToolbar extends Composite
      implements RequiresResize, ProvidesResize
{
   private class FileSystemContextImpl extends PosixFileSystemContext
   {
      public MessageDisplay messageDisplay()
      {
         return RStudioGinjector.INSTANCE.getGlobalDisplay();
      }

      public void cd(String relativeOrAbsolutePath)
      {
         workingDir_ = combine(pwd(), relativeOrAbsolutePath);
         if (callbacks_ != null)
            callbacks_.onNavigated();
      }

      public void refresh()
      {
         throw new UnsupportedOperationException("refresh not supported");
      }

      public void mkdir(String folderName, ProgressIndicator progress)
      {
         throw new UnsupportedOperationException("mkdir not supported");
      }

      public FileIcon getIcon(FileSystemItem item)
      {
         throw new UnsupportedOperationException("getIcon not supported");
      }

      @Override
      public FileSystemItem[] parseDir(String dirPath)
      {
         if (!cloudFolderEnabled_)
            return super.parseDir(dirPath);

         // if path starts with /cloud, eliminate the entry for the root folder; enables
         // display of "/cloud" as a single breadcrumb, similar to how "Home" is displayed
         ArrayList<FileSystemItem> parsedDir = new ArrayList<>(Arrays.asList(super.parseDir(dirPath)));
         if (parsedDir.size() >= 2)
         {
            if (StringUtil.equals(parsedDir.get(1).getPath(), "/cloud"))
            {
               parsedDir.remove(0);
            }
         }
         return parsedDir.toArray(new FileSystemItem[0]);
      }

      @Override
      public boolean isCloudRoot(FileSystemItem item)
      {
         if (cloudFolderEnabled_)
            return item.isDirectory() && item.getPath().equals("/cloud");
         else
            return false;
      }
   }

   /**
    *
    * @param navigationObserver
    * @param cloudFolderEnabled if true, display /cloud folder in similar fashion to Home
    */
   public FilePathToolbar(Files.Display.NavigationObserver navigationObserver, boolean cloudFolderEnabled)
   {
      cloudFolderEnabled_ = cloudFolderEnabled;

      LayoutPanel layout = new LayoutPanel();
      layout.setSize("100%", "21px");

      initWidget(layout);
      addStyleName(ThemeStyles.INSTANCE.rstheme_toolbarWrapper());
      addStyleName(ThemeStyles.INSTANCE.rstheme_secondaryToolbar());

      navigationObserver_ = navigationObserver;

      // select all check box
      CheckBoxHiddenLabel selectAllCheckBox = new CheckBoxHiddenLabel("Select all files");
      selectAllCheckBox.addStyleDependentName("FilesSelectAll");
      selectAllCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>(){

         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            navigationObserver_.onSelectAllValueChanged(
                                    event.getValue().booleanValue());
         }
      });

      layout.add(selectAllCheckBox);
      layout.setWidgetTopBottom(selectAllCheckBox, 0, Unit.PX, 0, Unit.PX);
      layout.setWidgetLeftWidth(selectAllCheckBox, 0, Unit.PX, 20, Unit.PX);

      // breadcrumb widget
      fileSystemContext_ = new FileSystemContextImpl();
      fileSystemContext_.setCallbacks(new Callbacks()
      {
         public void onNavigated()
         {
            navigationObserver_.onFileNavigation(fileSystemContext_.pwdItem());
         }

         public void onError(String errorMessage)
         {
            assert false : "Not implemented";
         }

         public void onDirectoryCreated(FileSystemItem directory)
         {
            assert false : "Not implemented";
         }
      });
      pathBreadcrumbWidget_ = new PathBreadcrumbWidget(fileSystemContext_);
      pathBreadcrumbWidget_.addStyleDependentName("filepane");
      pathBreadcrumbWidget_.addSelectionCommitHandler((SelectionCommitEvent<FileSystemItem> e) ->
      {
         fileSystemContext_.cd(e.getSelectedItem().getPath());
      });

      layout.add(pathBreadcrumbWidget_);
      layout.setWidgetTopBottom(pathBreadcrumbWidget_, 0, Unit.PX, 0, Unit.PX);
      layout.setWidgetLeftRight(pathBreadcrumbWidget_, 21, Unit.PX, 0, Unit.PX);
   }

   public void setPath(String path, String lastBrowseable)
   {
      assert fileSystemContext_.isAbsolute(path);
      pathBreadcrumbWidget_.setDirectory(fileSystemContext_.parseDir(path),
            lastBrowseable);
   }

   public int getHeight()
   {
      return 21;
   }

   public void onResize()
   {
      if (getWidget() instanceof RequiresResize)
         ((RequiresResize)getWidget()).onResize();
   }

   private final Files.Display.NavigationObserver navigationObserver_;
   private final boolean cloudFolderEnabled_;
   private FileSystemContextImpl fileSystemContext_;
   private PathBreadcrumbWidget pathBreadcrumbWidget_;
}
