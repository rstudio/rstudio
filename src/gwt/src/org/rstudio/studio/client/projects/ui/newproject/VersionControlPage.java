/*
 * VersionControlPage.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.vcs.VCSHelpLink;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class VersionControlPage extends NewProjectWizardPage
{

   public VersionControlPage(String title, 
                            String subTitle, 
                            String pageCaption, 
                            ImageResource image,
                            ImageResource largeImage,
                            boolean vcsIsAvailable)
   {
      super(title, subTitle, pageCaption, image, largeImage);
      vcsIsAvailable_ = vcsIsAvailable;
      
   }
   
   @Override
   protected boolean acceptNavigation()
   {
      if (!vcsIsAvailable_)
      {         
         NewProjectResources.Styles styles = 
                                 NewProjectResources.INSTANCE.styles();   
         
         VerticalPanel verticalPanel = new VerticalPanel();
         verticalPanel.addStyleName(styles.vcsNotInstalledWidget());
         
         if (Desktop.isDesktop())
         {
            HTML msg = new HTML(
               "<p>" + getTitle() + " was not detected " +
               "on the system path.</p>" +
               "<p>To create projects from " + getTitle() + " " + 
               "repositories you should install " + getTitle() + " " +
               "and then restart RStudio.</p>" +
               "<p>Note that if " + getTitle() + " is installed " +
               "and not on the path, then you can specify its location using " +
               "the " + (BrowseCap.isMacintosh() ? "Preferences" : "Options") +
               " dialog.</p>");
            msg.setWidth("100%");
            
            verticalPanel.add(msg);
            
            VCSHelpLink vcsHelpLink = new VCSHelpLink();
            vcsHelpLink.setCaption("Using " + getTitle() + " with RStudio");
            vcsHelpLink.addStyleName(styles.vcsHelpLink());
            verticalPanel.add(vcsHelpLink);
         }
         else
         {
            HTML msg = new HTML(
                  "<p>An installation of " + getTitle() + " was not detected " +
                  "on this system.</p>" +
                  "<p>To create projects from " + getTitle() + " " + 
                  "repositories you should request that your server " +
                  "administrator install the " + getTitle() + " package.</p>");
               msg.setWidth("100%");
               
               verticalPanel.add(msg);
         }
         
         MessageDialog dlg = new MessageDialog(MessageDialog.INFO,
                                               getTitle() + " Not Found",
                                               verticalPanel);
         
         
         dlg.addButton("OK", (Operation)null, true, false);
         dlg.showModal();
         
         return false;
      }
      else
      {
         return true;
      }
   }
   
   
   private final boolean vcsIsAvailable_;
}
