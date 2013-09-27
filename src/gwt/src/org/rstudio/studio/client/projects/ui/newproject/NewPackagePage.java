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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.projects.model.NewPackageOptions;
import org.rstudio.studio.client.projects.model.NewProjectInput;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
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
      
      // workaround qt crash on mac desktop
      if (BrowseCap.isMacintoshDesktop())
      {
         DomEvent.fireNativeEvent(Document.get().createChangeEvent(), 
                                  listProjectType_.getListBox());
      }
   }
   
   @Override
   protected NewPackageOptions getNewPackageOptions()
   {
      return NewPackageOptions.create(
            listProjectType_.getValue().equals("package-rcpp"),  
            JsUtil.toJsArrayString(listCodeFiles_.getCodeFiles()));
   }
   
   private SelectWidget listProjectType_;
   private CodeFilesList listCodeFiles_;
}
