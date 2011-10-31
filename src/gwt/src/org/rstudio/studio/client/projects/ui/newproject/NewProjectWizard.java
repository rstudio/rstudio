/*
 * NewProjectWizard.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.user.client.ui.CheckBox;


public class NewProjectWizard extends Wizard<FileSystemItem,NewProjectResult>
{
   public NewProjectWizard(
         FileSystemItem defaultNewProjectLocation,
         ProgressOperationWithInput<NewProjectResult> operation)
   {
      super("New Project", 
            "Create project from:", 
            defaultNewProjectLocation, 
            operation);
    
      setOkButtonCaption("Create Project");
      
 
      openInNewWindow_ = new CheckBox("Open in new window");
      addLeftWidget(openInNewWindow_);
      openInNewWindow_.setVisible(false);
      
      
      addPage(new NewDirectoryPage());
      addPage(new ExistingDirectoryPage());
      
      if (RStudioGinjector.INSTANCE.getSession()
                                       .getSessionInfo().isVcsAvailable())
      {
         addPage(new VersionControlPage());
      }
   }  
   
   @Override
   protected void onPageActivated(
                     WizardPage<FileSystemItem,NewProjectResult> page)
   {
      openInNewWindow_.setVisible(Desktop.isDesktop());
   }
   
   @Override
   protected void onSelectorActivated()
   {
      openInNewWindow_.setVisible(false);
   }
   
   @Override
   protected NewProjectResult ammendInput(NewProjectResult result)
   {
      result.setOpenInNewWindow(openInNewWindow_.getValue());
      return result;
   }
   
   private final CheckBox openInNewWindow_;
}
