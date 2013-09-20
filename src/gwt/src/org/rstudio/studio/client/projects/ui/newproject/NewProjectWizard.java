/*
 * NewProjectWizard.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import com.google.gwt.user.client.ui.CheckBox;


public class NewProjectWizard extends Wizard<NewProjectInput,NewProjectResult>
{
   public NewProjectWizard(
         SessionInfo sessionInfo,
         UIPrefs uiPrefs,
         NewProjectInput input,
         ProgressOperationWithInput<NewProjectResult> operation)
   {
      super("New Project", 
            "Create project from:", 
            input, 
            operation);
    
      setOkButtonCaption("Create Project");
      
 
      openInNewWindow_ = new CheckBox("Open in new window");
      addLeftWidget(openInNewWindow_);
      openInNewWindow_.setVisible(false);
      
      
      addPage(new NewDirectoryPage());
      addPage(new ExistingDirectoryPage());

      if (sessionInfo.getAllowVcs())
         addPage(new VersionControlNavigationPage(sessionInfo));
   }  
   
   @Override
   protected void onPageActivated(
                     WizardPage<NewProjectInput,NewProjectResult> page,
                     boolean okButtonVisible)
   {
      openInNewWindow_.setVisible(Desktop.isDesktop() && okButtonVisible);
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
