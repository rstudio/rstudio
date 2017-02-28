/*
 * ProfilerEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.profiler;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;

public class ProfilerEditingTargetWidget extends Composite
                                         implements ProfilerPresenter.Display
              
{
   private RStudioFrame profilePage_;
   
   public ProfilerEditingTargetWidget(Commands commands, PublishHtmlSource publishHtmlSource)
   {
      VerticalPanel panel = new VerticalPanel();


      PanelWithToolbars mainPanel = new PanelWithToolbars(
                                          createToolbar(commands, publishHtmlSource), 
                                          panel);

      profilePage_ = new RStudioFrame();
      profilePage_.setWidth("100%");
      profilePage_.setHeight("100%");
      
      panel.add(profilePage_);
      panel.setWidth("100%");
      panel.setHeight("100%");
      
      // needed for Firefox
      if (BrowseCap.isFirefox())
         profilePage_.getElement().getParentElement().setAttribute("height", "100%");
      
      initWidget(mainPanel);
   }

   public void print()
   {
      WindowEx window = profilePage_.getWindow();
      window.focus();
      window.print();
   }

   private Toolbar createToolbar(Commands commands, PublishHtmlSource publishHtmlSource)
   {
      Toolbar toolbar = new EditingTargetToolbar(commands, true);
      
      toolbar.addLeftWidget(commands.gotoProfileSource().createToolbarButton());
      toolbar.addLeftWidget(commands.saveProfileAs().createToolbarButton());
      
      toolbar.addRightWidget(
            publishButton_ = new RSConnectPublishButton(
                  RSConnect.CONTENT_TYPE_DOCUMENT, true, null));
      
      publishButton_.setPublishHtmlSource(publishHtmlSource);
      publishButton_.setContentType(RSConnect.CONTENT_TYPE_HTML);
      
      return toolbar;
   }
   
   public Widget asWidget()
   {
      return this;
   }
   
   public void showProfilePage(String path)
   {
      profilePage_.setUrl(path);
   }
   
   public String getUrl()
   {
      return profilePage_.getUrl();
   }
   
   private RSConnectPublishButton publishButton_;
}
