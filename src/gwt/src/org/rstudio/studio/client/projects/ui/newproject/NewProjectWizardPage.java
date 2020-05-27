/*
 * NewProjectWizardPage.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public abstract class NewProjectWizardPage 
                     extends WizardPage<NewProjectInput,NewProjectResult>
{
   public NewProjectWizardPage(String title, 
                               String subTitle, 
                               String pageCaption, 
                               ImageResource image,
                               ImageResource largeImage)
   {
      super(title, subTitle, pageCaption, image, largeImage);
      
   }
   
   @Override
   protected Widget createWidget()
   {
      flowPanel_ = new FlowPanel();
      flowPanel_.setWidth("100%");
      
      onAddWidgets();
      
      return flowPanel_;
   }
   
   protected abstract void onAddWidgets();
   
   
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      defaultNewProjectLocation_ = input.getDefaultNewProjectLocation();
   }
   
   protected void addWidget(Widget widget)
   {
      widget.addStyleName(NewProjectResources.INSTANCE.styles().wizardWidget());
      flowPanel_.add(widget);
   }
   
   protected SessionInfo getSessionInfo()
   {
      return  RStudioGinjector.INSTANCE.getSession().getSessionInfo();
   }
   
   protected void addSpacer()
   {
      Label spacerLabel = new Label();
      spacerLabel.addStyleName(
                     NewProjectResources.INSTANCE.styles().wizardSpacer());
      flowPanel_.add(spacerLabel);
   }
   
   protected FileSystemItem defaultNewProjectLocation_;
   
   protected final GlobalDisplay globalDisplay_ = 
                           RStudioGinjector.INSTANCE.getGlobalDisplay();
   
   private FlowPanel flowPanel_;
}
