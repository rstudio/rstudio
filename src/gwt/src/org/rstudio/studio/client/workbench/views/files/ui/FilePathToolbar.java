/*
 * FilePathToolbar.java
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
package org.rstudio.studio.client.workbench.views.files.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemContext.Callbacks;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.PosixFileSystemContext;
import org.rstudio.core.client.files.filedialog.PathBreadcrumbWidget;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.files.Files;

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

      public ImageResource getIcon(FileSystemItem item)
      {
         throw new UnsupportedOperationException("getIcon not supported");
      }
   }

   public FilePathToolbar(Files.Display.NavigationObserver navigationObserver)
   {
      UIPrefs uiPrefs = RStudioGinjector.INSTANCE.getUIPrefs();
      
      LayoutPanel layout = new LayoutPanel();
      String layoutPanelHeight = RStudioThemes.isFlat(uiPrefs) ? "20px" : "21px";
      layout.setSize("100%", layoutPanelHeight);

      initWidget(layout);
      setStyleName(ThemeStyles.INSTANCE.secondaryToolbar());

      navigationObserver_ = navigationObserver;
      
      // select all check box
      CheckBox selectAllCheckBox = new CheckBox();
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
      pathBreadcrumbWidget_.addSelectionCommitHandler(
            new SelectionCommitHandler<FileSystemItem>()
      {
         public void onSelectionCommit(SelectionCommitEvent<FileSystemItem> e)
         {
            fileSystemContext_.cd(e.getSelectedItem().getPath());
         }
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
   private FileSystemContextImpl fileSystemContext_;
   private PathBreadcrumbWidget pathBreadcrumbWidget_;
}
