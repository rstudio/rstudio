/*
 * NewDirectoryPage.java
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;


public class NewPackagePage extends NewDirectoryPage
{
   public NewPackagePage()
   {
      super(constants_.newPackageTitle(),
            constants_.createNewPackageSubTitle(),
            constants_.createRPackagePageCaption(),
            new ImageResource2x(NewProjectResources.INSTANCE.packageIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.packageIconLarge2x()));
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      styles_ = NewProjectResources.INSTANCE.styles();
      
      txtProjectName_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               
               @Override
               public void execute()
               {
                  validatePackageName();
               }
            });
         }
      });
      
   }
   
   protected boolean getOptionsSideBySide()
   {
      return true;
   }
    
   @Override 
   protected void onAddTopPanelWidgets(HorizontalPanel panel)
   {
      String[] labels = {constants_.packageLabel()};
      String[] values = {"package"};
      listProjectType_ = new SelectWidget(constants_.typeLabel(),
                                          labels,
                                          values,
                                          false);
      ElementIds.assignElementId(listProjectType_, ElementIds.NEW_PROJECT_TYPE);
      listProjectType_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            txtProjectName_.setFocus(true);
         }
      });
      panel.add(listProjectType_);
   }

   @Override
   protected String getDirNameLabel()
   {
      return constants_.packageNameLabel();
   }

   @Override
   protected void onAddTopWidgets()
   {
      // code files panel
      listCodeFiles_ = new CodeFilesList();
      ElementIds.assignElementId(listCodeFiles_, ElementIds.NEW_PROJECT_SOURCE_FILES);
      addWidget(listCodeFiles_);
   }
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
      
      if (input.getContext().isRcppAvailable())
         listProjectType_.addChoice(constants_.rcppPackageOption(), "package-rcpp");
   }

   @Override
   public void focus()
   {
      super.focus();
   }
   
   @Override
   protected NewPackageOptions getNewPackageOptions()
   {
      return NewPackageOptions.create(
            getProjectName(),
            listProjectType_.getValue() == "package-rcpp",  
            JsUtil.toJsArrayString(listCodeFiles_.getCodeFiles()));
   }
   
   private void validatePackageName()
   {
      String packageName = txtProjectName_.getText().trim();
      
      // Don't validate if the name is empty
      if (packageName.isEmpty() || isPackageNameValid(packageName))
         txtProjectName_.removeStyleName(styles_.invalidPkgName());
      else
         txtProjectName_.addStyleName(styles_.invalidPkgName());
   }
   
   private boolean isPackageNameValid(String packageName)
   {
      return Projects.PACKAGE_NAME_PATTERN.test(packageName);
   }
   
   @Override
   protected void validateAsync(final NewProjectResult input,
                                final OperationWithInput<Boolean> onValidated)
   {
      // validate package name first
      String packageName = txtProjectName_.getText().trim();
      if (!isPackageNameValid(packageName))
      {
         globalDisplay_.showMessage(
               MessageDialog.WARNING,
               constants_.errorCaption(),
               constants_.validateAsyncMessage(packageName));
         onValidated.execute(false);
         return;
      }

      super.validateAsync(input, onValidated);
   }

   private SelectWidget listProjectType_;
   private CodeFilesList listCodeFiles_;
   private final NewProjectResources.Styles styles_;
   
   // Injected ----
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
}
