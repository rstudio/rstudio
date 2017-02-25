/*
 * FileBrowserWidget.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.core.client.files.filedialog;

import org.rstudio.core.client.FocusTransitionManager;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class FileBrowserWidget extends Composite
                 implements FileSystemContext.Callbacks
{
   public interface Host extends SelectionCommitHandler<FileSystemItem>,
                                 SelectionHandler<FileSystemItem>
   {
      String getFilenameLabel();
      void onError(String errorMessage);
      void maybeAccept();
      FileSystemItem[] ls();
   }

   public FileBrowserWidget(FileSystemContext context,
         Host host)
   {
      context_ = context;
      host_ = host;
      
      breadcrumb_ = new PathBreadcrumbWidget(context_);
      breadcrumb_.addSelectionCommitHandler(host);

      directory_ = new DirectoryContentsWidget(context_);
      directory_.addSelectionHandler(host);
      directory_.addSelectionCommitHandler(host);
      directory_.showProgress(true);

      DockPanel dockPanel = new DockPanel();
      Widget topWidget = createTopWidget();
      if (topWidget != null)
         dockPanel.add(topWidget, DockPanel.NORTH);
      dockPanel.add(breadcrumb_, DockPanel.NORTH);
      dockPanel.add(directory_, DockPanel.CENTER);

      initWidget(dockPanel);
   }

   // Public methods ----------------------------------------------------------
   
   public void setFilename(String filename)
   {
      if (filename_ != null)
         filename_.setText(filename);
      else
         initialFilename_ = filename;
   }
   
   public String getFilename()
   {
      return filename_.getText();
   }
   
   public void setFilenameEnabled(boolean enabled)
   {
      filename_.setEnabled(enabled);
   }
   
   public void selectFilename()
   {
      filename_.selectAll();
   }
   
   public void setFilenameFocus(boolean focus)
   {
      filename_.setFocus(focus);
   }
   
   public Style getFilenameStyle()
   {
     return filename_.getElement().getStyle(); 
   }

   public void onNavigated()
   {
      String dir = context_.pwd();

      final FileSystemItem[] parsedDir = context_.parseDir(dir);
      breadcrumb_.setDirectory(parsedDir, null);
      directory_.setContents(
            host_.ls(),
            parsedDir.length > 1 ? parsedDir[parsedDir.length-2] : null);
      setDirectoryFocus(true);
   }

   public void cd(String path)
   {
      directory_.clearContents();
      directory_.showProgress(true);
      context_.cd(path);
   }

   public void cd(FileSystemItem dir)
   {
      assert dir.isDirectory();
      cd(dir.getPath());
   }
   
   public void addKeyUpHandler(KeyUpHandler handler)
   {
      filename_.addKeyUpHandler(handler);
   }

   public void addKeyPressHandler(KeyPressHandler handler)
   {
      filename_.addKeyPressHandler(handler);
   }
   
   public void setSelectedRow(Integer row)
   {
      directory_.setSelectedRow(row);
   }
   
   public void setDirectoryFocus(boolean focus)
   {
      directory_.setFocus(focus);
   }

   @Override
   public void onError(String errorMessage)
   {
      onNavigated();
   }

   @Override
   public void onDirectoryCreated(FileSystemItem directory)
   {
      directory_.addDirectory(directory);
   }

   public String getSelectedValue()
   {
      return directory_.getSelectedValue();
   }
   
   public FileSystemItem getSelectedItem()
   {
      return directory_.getSelectedItem();
   }
   
   public FileSystemItem getCurrentDirectory()
   {
      return context_.pwdItem();
   }
   
   // Private methods ---------------------------------------------------------

   private Widget createTopWidget()
   {
      String nameLabel = host_.getFilenameLabel();
      if (nameLabel == null)
         return null;

      HorizontalPanel filenamePanel = new HorizontalPanel();
      FileDialogStyles styles = FileDialogResources.INSTANCE.styles();
      filenamePanel.setStylePrimaryName(styles.filenamePanel());
      filenamePanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

      Label filenameLabel = new Label(nameLabel + ":", false);
      filenameLabel.setStylePrimaryName(styles.filenameLabel());
      filenamePanel.add(filenameLabel);
      
      filename_ = new TextBox();
      if (initialFilename_ != null)
         filename_.setText(initialFilename_);
      filename_.setStylePrimaryName(styles.filename());
      filenamePanel.add(filename_);
      filenamePanel.setCellWidth(filename_, "100%");

      ftm_ = new FocusTransitionManager();
      ftm_.add(filename_, directory_);

      return filenamePanel;
   }
   
   private PathBreadcrumbWidget breadcrumb_;
   private DirectoryContentsWidget directory_;
   private TextBox filename_;
   private FileSystemContext context_;
   private String initialFilename_;
   private Host host_;
   private FocusTransitionManager ftm_;
}
