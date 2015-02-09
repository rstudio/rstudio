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

import java.util.ArrayList;

import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.core.client.widget.WizardNavigationPage;
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
            "Create Project",
            input, 
            createFirstPage(sessionInfo),
            operation);
    
      openInNewWindow_ = new CheckBox("Open in new window");
      addLeftWidget(openInNewWindow_);
      openInNewWindow_.setVisible(false);
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
      if (result != null)
      {
         result.setOpenInNewWindow(openInNewWindow_.getValue());
         return result;
      }
      else
      {
         return null;
      }
   }
   
   private static WizardPage<NewProjectInput, NewProjectResult> createFirstPage(
         SessionInfo sessionInfo)
   {
      return new WizardNavigationPage<NewProjectInput, NewProjectResult>(
            "New Project", "Create project from:", "Create Project", 
            null, null, createSubPages(sessionInfo));
   }
   
   private static ArrayList<WizardPage<NewProjectInput, NewProjectResult>> createSubPages(
         SessionInfo sessionInfo)
   {
      ArrayList<WizardPage<NewProjectInput, NewProjectResult>> subPages = 
            new ArrayList<WizardPage<NewProjectInput, NewProjectResult>>();
      subPages.add(new NewDirectoryNavigationPage(sessionInfo));
      subPages.add(new ExistingDirectoryPage());

      if (sessionInfo.getAllowVcs())
         subPages.add(new VersionControlNavigationPage(sessionInfo));

      return subPages;
   }
   
   private final CheckBox openInNewWindow_;
}
