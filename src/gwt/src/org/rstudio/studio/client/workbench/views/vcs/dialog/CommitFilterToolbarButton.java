/*
 * CommitFilterToolbarButton.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

public class CommitFilterToolbarButton extends ToolbarMenuButton
                                       implements HasValue<FileSystemItem>
{
   @Inject
   public CommitFilterToolbarButton(FileTypeRegistry fileTypeRegistry,
                                    FileDialogs fileDialogs,
                                    RemoteFileSystemContext fileContext,
                                    Session session)
   {
      super(ALL_COMMITS,
            ALL_COMMITS_TITLE,
            StandardIcons.INSTANCE.empty_command(),
            new ToolbarPopupMenu());
      
      fileTypeRegistry_ = fileTypeRegistry;
      fileDialogs_ = fileDialogs;
      fileContext_ = fileContext;
      session_ = session;
      
      ToolbarPopupMenu menu = getMenu();
      
      menu.addItem(new MenuItem("(all commits)", new Command() {
         @Override
         public void execute()
         {
            setValue(null);
         } 
      }));
      
      menu.addItem(new MenuItem("Filter by File...", new Command() {
         @Override
         public void execute()
         {
            fileDialogs_.openFile("Choose File",
                                  fileContext_, 
                                  getInitialChooserPath(),
                                  chooserOperation());
         } 
      }));
      
      menu.addItem(new MenuItem("Filter by Directory...", new Command() {
         @Override
         public void execute()
         {
            fileDialogs_.chooseFolder("Choose Folder", 
                                       fileContext_, 
                                       getInitialChooserPath(),
                                       chooserOperation());
            
         } 
      }));
      
   }

   @Override
   public HandlerRegistration addValueChangeHandler(
                                    ValueChangeHandler<FileSystemItem> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   @Override
   public FileSystemItem getValue()
   {
      return value_;
   }

   @Override
   public void setValue(FileSystemItem value)
   {
      setValue(value, true);
      
   }

   @Override
   public void setValue(FileSystemItem value, boolean fireEvents)
   {
      if (!FileSystemItem.areEqual(value, value_))
      {
         value_ = value;
         
         if (value_ == null)
         {
            setLeftImage(StandardIcons.INSTANCE.empty_command());
            setText(ALL_COMMITS);
            setTitle(ALL_COMMITS_TITLE);
         }
         else
         {
            if (value_.isDirectory())
               setLeftImage(value_.getIcon().getImageResource());
            else
               setLeftImage(fileTypeRegistry_.getIconForFile(value_).getImageResource());
            setText(value_.getName());
            setTitle("Filter: " + value_.getPath());
         }
         
         if (fireEvents)
            ValueChangeEvent.fire(CommitFilterToolbarButton.this, value_);
      }
      
   }
   
   private FileSystemItem getInitialChooserPath()
   {
      return  value_ != null ? value_.getParentPath() : 
                               session_.getSessionInfo().getActiveProjectDir(); 
   }
   
   private ProgressOperationWithInput<FileSystemItem> chooserOperation()
   {
      return new ProgressOperationWithInput<FileSystemItem>() {

         @Override
         public void execute(FileSystemItem input,
                             ProgressIndicator indicator)
         {
            indicator.onCompleted();
            
            if (input != null)
               setValue(input);
         }
       };
   }

   
   private FileSystemItem value_ = null;
   private final FileTypeRegistry fileTypeRegistry_;
   private final RemoteFileSystemContext fileContext_;
   private final FileDialogs fileDialogs_;
   private final Session session_;
   
   private static final String ALL_COMMITS = "(all commits)";
   private static final String ALL_COMMITS_TITLE = "Filter: (None)";
}
