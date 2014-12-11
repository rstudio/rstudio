/*
 * NewDirectoryPage.java
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

import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectInput;

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
      super("R Package", 
            "Create a new R package",
            "Create R Package",
            NewProjectResources.INSTANCE.packageIcon(),
            NewProjectResources.INSTANCE.packageIconLarge());
      
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
      dirNameLabel_.setText("Package name:");
      
      String[] labels = {"Package"};
      String[] values = {"package"};
      listProjectType_ = new SelectWidget("Type:",
                                          labels,
                                          values,
                                          false);
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
   protected void onAddBodyWidgets()
   {
      // code files panel
      listCodeFiles_ = new CodeFilesList();
      addWidget(listCodeFiles_);
   }
   
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
      
      if (input.getContext().isRcppAvailable())
         listProjectType_.addChoice("Package w/ Rcpp", "package-rcpp");
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
            listProjectType_.getValue().equals("package-rcpp"),  
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
   
   private SelectWidget listProjectType_;
   private CodeFilesList listCodeFiles_;
   private final NewProjectResources.Styles styles_;
}
