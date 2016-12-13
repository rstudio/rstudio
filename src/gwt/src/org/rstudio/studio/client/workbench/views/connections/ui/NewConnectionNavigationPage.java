/*
 * NewConnectionNavigationPage.java
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
package org.rstudio.studio.client.workbench.views.connections.ui;

import java.util.ArrayList;

import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;

import com.google.gwt.resources.client.ImageResource;

public class NewConnectionNavigationPage 
   extends WizardNavigationPage<NewConnectionContext, ConnectionOptions>
{
   public NewConnectionNavigationPage(String title,
                                      String subTitle,
                                      ImageResource icon,
                                      NewConnectionContext input)
   {
      super(title, subTitle, "Create Connection", 
            icon, null, createPages(input));
   }
   
   private static ArrayList<WizardPage<NewConnectionContext, 
                                       ConnectionOptions>> 
           createPages(NewConnectionContext input)
   {
      ArrayList<WizardPage<NewConnectionContext, 
                           ConnectionOptions>> pages =
                           new ArrayList<WizardPage<NewConnectionContext, 
                                                    ConnectionOptions>>();

      pages.add(new NewConnectionSelectionPage("Spark"));

      return pages;
   }
}
