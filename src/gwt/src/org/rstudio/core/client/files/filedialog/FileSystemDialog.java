/*
 * FileSystemDialog.java
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
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public abstract class FileSystemDialog extends ModalDialogBase
      implements SelectionCommitHandler<FileSystemItem>, FileSystemContext.Callbacks,
                 SelectionHandler<FileSystemItem>,
                 ProgressIndicator
{
   private class NewFolderHandler implements ClickHandler,
                                             ProgressOperationWithInput<String>
   {
      public void onClick(ClickEvent event)
      {
         context_.messageDisplay().promptForText("New Folder",
                                                 "Folder name",
                                                 null,
                                                 this);
      }

      public void execute(final String input, final ProgressIndicator progress)
      {
         context_.mkdir(input, new ProgressIndicator()
         {
            public void onProgress(String message)
            {
               progress.onProgress(message);
            }

            public void onCompleted()
            {
               progress.onCompleted();
               context_.cd(input);
            }

            public void onError(String message)
            {
               progress.onError(message);
            }
         });
      }
   }

   static
   {
      FileDialogResources.INSTANCE.styles().ensureInjected();
   }

   public FileSystemDialog(String title,
                           String caption,
                           String buttonName,
                           FileSystemContext context,
                           ProgressOperationWithInput<FileSystemItem> operation)
   {
      context_ = context;
      context_.setCallbacks(this);
      operation_ = operation;

      setTitle(caption);
      setText(title);

      addLeftButton(new ThemedButton("New Folder", new NewFolderHandler()));

      addOkButton(new ThemedButton(buttonName, new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            maybeAccept();
         }
      }));
      addCancelButton(new ThemedButton("Cancel", new ClickHandler() {
         public void onClick(ClickEvent event) {
            if (invokeOperationEvenOnCancel_)
            {
               operation_.execute(null, FileSystemDialog.this);
            }
            closeDialog();
         }
      }));

      addDomHandler(new KeyDownHandler() {
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.stopPropagation();
               event.preventDefault();
               maybeAccept();
            }
         }
      }, KeyDownEvent.getType());

      progress_ = addProgressIndicator();
   }

   /**
    * Subclasses that pass true for the promptOnOverwrite constructor arg
    * must override this method. If the user elects to overwrite, the
    * subclass should call accept() directly (this will cause all validation
    * to be bypassed).
    */
   protected void showOverwritePrompt()
   {
      assert false :
            "Subclasses should override showOvewritePrompt() " +
            "if promptOnOverwrite is true";
   }

   /**
    * Accept if validation passes
    */
   protected final void maybeAccept()
   {
      if (shouldAccept())
         accept();
   }

   protected boolean shouldAccept()
   {
      return true;
   }

   protected void accept()
   {
      operation_.execute(getSelectedItem(), this);
   }

   protected abstract FileSystemItem getSelectedItem();

   @Override
   public void showModal()
   {
      super.showModal();
      DeferredCommand.addCommand(new Command()
      {
         public void execute()
         {
            center();
         }
      });
   }

   @Override
   protected Widget createMainWidget()
   {
      breadcrumb_ = new PathBreadcrumbWidget(context_, false);
      breadcrumb_.addSelectionCommitHandler(this);

      directory_ = new DirectoryContentsWidget(context_);
      directory_.addSelectionHandler(this);
      directory_.addSelectionCommitHandler(this);
      directory_.showProgress(true);

      DockPanel dockPanel = new DockPanel();
      Widget topWidget = createTopWidget();
      if (topWidget != null)
         dockPanel.add(topWidget, DockPanel.NORTH);
      dockPanel.add(breadcrumb_, DockPanel.NORTH);
      dockPanel.add(directory_, DockPanel.CENTER);

      return dockPanel;
   }

   protected Widget createTopWidget()
   {
      String nameLabel = getFilenameLabel();
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

      return filenamePanel;
   }

   /**
    * If non-null, the filename textbox will be inserted at the top
    * of the dialog with the given label shown next to it.
    *
    * If null, the filename textbox will not be created.
    */
   protected abstract String getFilenameLabel();

   /**
    * Set the contents of the filename box
    */
   public void setFilename(String filename)
   {
      if (filename_ != null)
         filename_.setText(filename);
      else
         initialFilename_ = filename;
   }

   @Override
   protected void onDialogShown()
   {
   }

   @Override
   public void onPreviewNativeEvent(Event.NativePreviewEvent event)
   {
      if (event.getTypeInt() == Event.ONKEYDOWN
          && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER)
      {
         // Let directory browser handle its own Enter
         return;
      }
      super.onPreviewNativeEvent(event);    //To change body of overridden methods use File | Settings | File Templates.
   }

   public void onSelectionCommit(SelectionCommitEvent<FileSystemItem> event)
   {
      FileSystemItem item = event.getSelectedItem();
      if (item != null && item.isDirectory())
         cd(item);
      else
         maybeAccept();
   }

   protected void cd(String path)
   {
      if (REMEMBER_SCROLL_POSITION)
      {
         if (currentDir_ != null)
            scrollPositions_.put(currentDir_, directory_.getScrollPosition());
      }

      directory_.clearContents();
      directory_.showProgress(true);
      context_.cd(path);
   }

   protected void cd(FileSystemItem dir)
   {
      assert dir.isDirectory();
      cd(dir.getPath());
   }

   public void onSelection(SelectionEvent<FileSystemItem> event)
   {
   }

   public void onNavigated()
   {
      String dir = context_.pwd();

      final FileSystemItem[] parsedDir = context_.parseDir(dir);
      breadcrumb_.setDirectory(parsedDir);
      directory_.setContents(
            ls(),
            parsedDir.length > 1 ? parsedDir[parsedDir.length-2] : null);
      
      if (REMEMBER_SCROLL_POSITION)
      {
         if (scrollPositions_.containsKey(dir))
            directory_.setScrollPosition(scrollPositions_.get(dir));
      }
      currentDir_ = dir;
   }

   protected FileSystemItem[] ls()
   {
      FileSystemItem[] items = context_.ls();
      if (items == null)
         return new FileSystemItem[0];
      
      FileSystemItem[] clone = new FileSystemItem[items.length];
      for (int i = 0; i < items.length; i++)
         clone[i] = items[i];
      Arrays.sort(clone, new Comparator<FileSystemItem>() {
         public int compare(FileSystemItem o1, FileSystemItem o2)
         {
            return o1.compareTo(o2);
         }
      });
      return clone;
   }

   public void onProgress(String message)
   {
      progress_.onProgress(message);
   }

   public void onCompleted()
   {
      progress_.onCompleted();
   }

   public void onError(String errorMessage)
   {
      progress_.onError(errorMessage);
      onNavigated();
   }

   protected void showError(String errorMessage)
   {
      context_.messageDisplay().showErrorMessage("Error", errorMessage);
   }

   public void onDirectoryCreated(FileSystemItem directory)
   {
      directory_.addDirectory(directory);
   }

   /**
    * If true, hitting the Cancel button will result in the action operation
    * (passed in the constructor) to be invoked with a null value, rather
    * than having the dialog just close.
    */
   public void setInvokeOperationEvenOnCancel(boolean invoke)
   {
      invokeOperationEvenOnCancel_ = invoke;
   }


   private final HashMap<String, Point> scrollPositions_ =
                                                   new HashMap<String, Point>();
   private String currentDir_; 
   protected final FileSystemContext context_;
   private final ProgressOperationWithInput<FileSystemItem> operation_;
   private PathBreadcrumbWidget breadcrumb_;
   protected DirectoryContentsWidget directory_;
   private static final boolean REMEMBER_SCROLL_POSITION = false;
   protected TextBox filename_;
   private String initialFilename_;
   private boolean invokeOperationEvenOnCancel_;
   private final ProgressIndicator progress_;
}
