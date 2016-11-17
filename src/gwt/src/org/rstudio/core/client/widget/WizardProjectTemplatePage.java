/*
 * WizardProjectTemplatePage.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.core.client.widget;

import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.ProjectTemplateDescription;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class WizardProjectTemplatePage
      extends WizardPage<NewProjectInput, NewProjectResult>
{
   public WizardProjectTemplatePage(ProjectTemplateDescription description)
   {
      super(description.getTitle(),
            description.getDescription(),
            description.getBinding(),
            null,
            null);
   }

   @Override
   public void focus()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   protected Widget createWidget()
   {
      return new Label("Hello Friend!");
   }

   @Override
   protected void initialize(NewProjectInput initData)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   protected NewProjectResult collectInput()
   {
      // TODO Auto-generated method stub
      return null;
   }

}
