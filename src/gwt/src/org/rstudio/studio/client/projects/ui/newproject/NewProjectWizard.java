/*
 * NewProjectWizard.java
 *
 * Copyright (C) 2022 by Posit, PBC
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
package org.rstudio.studio.client.projects.ui.newproject;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.RVersionsInfo;
import org.rstudio.studio.client.application.ui.RVersionSelectWidget;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.CheckBox;


public class NewProjectWizard extends Wizard<NewProjectInput,NewProjectResult>
{
   public NewProjectWizard(
         SessionInfo sessionInfo,
         UserPrefs uiPrefs,
         WorkbenchContext workbenchContext,
         NewProjectInput input,
         boolean allowOpenInNewWindow,
         ProgressOperationWithInput<NewProjectResult> operation)
   {
      super(constants_.newProjectWizardCaption(),
            constants_.createProjectCaption(),
            Roles.getDialogRole(), 
            input, 
            createFirstPage(sessionInfo),
            operation);
    
      sessionInfo_ = sessionInfo;
      allowOpenInNewWindow_ = allowOpenInNewWindow;
      
      RVersionsInfo rVersions = workbenchContext.getRVersionsInfo();
      if (rVersions.isMultiVersion())
      {
         rVersionSelector_ = new RVersionSelectWidget(
           "",
           ElementIds.SelectWidgetId.R_VER_NEW_PROJ_WIZ,
           rVersions.getAvailableRVersions(),
           false,
           false,
           false);
         RVersionSpec rVersion = RVersionSpec.create(
               rVersions.getDefaultRVersion(),
               rVersions.getDefaultRVersionHome(),
               rVersions.getRVersionLabel());
         rVersionSelector_.setRVersion(rVersion);
         addLeftWidget(rVersionSelector_);
         rVersionSelector_.getElement().getStyle().setMarginRight(8, Unit.PX);
         rVersionSelector_.setVisible(false);
      }
      
      openInNewWindow_ = new CheckBox(constants_.openNewSessionLabel());
      ElementIds.assignElementId(openInNewWindow_,
         ElementIds.idWithPrefix(getTitle(), ElementIds.NEW_PROJECT_NEW_SESSION));
      addLeftWidget(openInNewWindow_);
      openInNewWindow_.setVisible(false);
   }  
   
   @Override
   protected void onPageActivated(
                     WizardPage<NewProjectInput,NewProjectResult> page,
                     boolean okButtonVisible)
   {
      openInNewWindow_.setVisible(allowOpenInNewWindow_ &&
                                  sessionInfo_.getMultiSession() && 
                                  okButtonVisible);
      if (rVersionSelector_ != null)
         rVersionSelector_.setVisible(okButtonVisible);
   }
   
   @Override
   protected void onSelectorActivated()
   {
      openInNewWindow_.setVisible(false);
      if (rVersionSelector_ != null)
         rVersionSelector_.setVisible(false);
   }
   
   @Override
   protected NewProjectResult amendInput(NewProjectResult result)
   {
      if (result != null)
      {
         result.setOpenInNewWindow(openInNewWindow_.getValue());
         if (rVersionSelector_ != null)
            result.setRVersion(rVersionSelector_.getRVersion());
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
      return new WizardNavigationPage<>(
            constants_.newProjectTitle(), constants_.createProjectFromLabel(), constants_.createProjectCaption(),
            null, null, createSubPages(sessionInfo));
   }
   
   private static ArrayList<WizardPage<NewProjectInput, NewProjectResult>> createSubPages(
         SessionInfo sessionInfo)
   {
      ArrayList<WizardPage<NewProjectInput, NewProjectResult>> subPages = new ArrayList<>();
      subPages.add(new NewDirectoryNavigationPage(sessionInfo));
      subPages.add(new ExistingDirectoryPage());

      if (sessionInfo.getAllowVcs())
         subPages.add(new VersionControlNavigationPage());

      return subPages;
   }
   
   private final CheckBox openInNewWindow_;
   private RVersionSelectWidget rVersionSelector_ = null;
   private final SessionInfo sessionInfo_;
   private final boolean allowOpenInNewWindow_;
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
}
