/*
 * OpenProjectDialog.java
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
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.aria.client.Roles;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.projects.model.OpenProjectParams;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;

public class OpenProjectDialog extends FileDialog
{
   public OpenProjectDialog(FileSystemContext context,
                int defaultType,
                boolean newSessionOption,
                final ProgressOperationWithInput<OpenProjectParams> operation)
   {
      super("Open Project", null, Roles.getDialogRole(), "Open", false, false, false, context, 
            "R Projects (*.RProj)", 
            new ProgressOperationWithInput<FileSystemItem>()
            {
               @Override
               public void execute(FileSystemItem input,
                     ProgressIndicator indicator)
               {
                  // NOTE: we currently do not expose R version selection
                  // for the open project dialog since projects already
                  // have a pinned R version by default
                  operation.execute(new OpenProjectParams(input, 
                                                          null,
                                                          inNewSession_),
                        indicator);
               }
            });
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // Used to create a project in an existing directory which
      // does not already have a .Rproj file.
      ThemedButton createButton = new ThemedButton("Create", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            server_.createProjectFile(
                  browser_.getCurrentDirectory().getPath(),
                  new ServerRequestCallback<String>()
                  {
                     @Override
                     public void onResponseReceived(String response)
                     {
                        accept(FileSystemItem.createFile(response));
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                        accept();
                     }
                  });
         }
      });
      addButton(createButton, ElementIds.CREATE_BUTTON);
      
      newSessionCheck_ = new CheckBox("Open in new session");
      newSessionCheck_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            inNewSession_ = event.getValue();
         }
      });
      if (newSessionOption)
         addLeftWidget(newSessionCheck_);
   }
   
   @Inject
   private void initialize(ProjectsServerOperations server)
   {
      server_ = server;
   }
   
   private CheckBox newSessionCheck_;
   private static boolean inNewSession_ = false;
   
   // Injected ----
   private ProjectsServerOperations server_;
}
