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
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;

import com.google.gwt.resources.client.ImageResource;

public class NewConnectionNavigationPage 
   extends WizardNavigationPage<NewConnectionContext, ConnectionOptions>
{
   public NewConnectionNavigationPage(String title,
                                      String subTitle,
                                      ImageResource icon,
                                      NewConnectionContext context)
   {
      super(title, subTitle, "Create Connection", 
            icon, null, createPages(context));
   }
   
   private static ArrayList<WizardPage<NewConnectionContext, 
                                       ConnectionOptions>> 
           createPages(NewConnectionContext context)
   {
      ArrayList<WizardPage<NewConnectionContext, 
                           ConnectionOptions>> pages =
                           new ArrayList<WizardPage<NewConnectionContext, 
                                                    ConnectionOptions>>();

      for(NewConnectionInfo connectionInfo: context.getConnectionsList()) {
         if (connectionInfo.getType() == "Shiny") {
            pages.add(new NewConnectionShinyPage(connectionInfo));
         }
      }

      return pages;
   }
}
