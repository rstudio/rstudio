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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.HasWizardPageSelectionHandler;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.core.client.widget.WizardResources;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewConnectionNavigationPage 
   extends WizardNavigationPage<NewConnectionContext, ConnectionOptions>
{
   public NewConnectionNavigationPage(String title,
                                      String subTitle,
                                      ImageResource icon,
                                      NewConnectionContext context)
   {
      super(title, 
            subTitle,
            "Create Connection",
            icon,
            null, 
            createPages(context),
            new WizardNavigationPageProducer<NewConnectionContext, ConnectionOptions>()
            {
               @Override
               public Widget createMainWidget(ArrayList<WizardPage<NewConnectionContext, ConnectionOptions>> pages)
               {
                  return createWidget(pages);
               }
            });
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
         if (!connectionInfo.getLicensed() || 
             RStudioGinjector.INSTANCE.getSession().getSessionInfo().getSupportDriverLicensing()) {

            if (connectionInfo.getType() == "Shiny") {
               pages.add(new NewConnectionShinyPage(connectionInfo));
            }
            else if (connectionInfo.getType() == "Snippet") {
               pages.add(new NewConnectionSnippetPage(connectionInfo));
            }
            else if (connectionInfo.getType() == "Install") {
               pages.add(new NewConnectionInstallPackagePage(connectionInfo));
            }
         }
      }

      return pages;
   }

   private static Widget createWidget(ArrayList<WizardPage<NewConnectionContext, ConnectionOptions>> pages)
   {
      return new Selector(pages);
   }

   private static class Selector
         extends Composite
         implements HasWizardPageSelectionHandler<NewConnectionContext, ConnectionOptions>
   {
      public Selector(final ArrayList<WizardPage<NewConnectionContext, ConnectionOptions>> pages)
      {
         WizardResources.Styles styles = WizardResources.INSTANCE.styles();
         
         ScrollPanel scrollPanel = new ScrollPanel();
         scrollPanel.setSize("100%", "100%");
         scrollPanel.addStyleName(RES.styles().wizardPageSelector());
         scrollPanel.addStyleName(styles.wizardPageSelector());

         VerticalPanel verticalPanel = new VerticalPanel();
         verticalPanel.setSize("100%", "100%");

         for (int i = 0, n = pages.size(); i < n; i++)
         {
            final WizardPage<NewConnectionContext, ConnectionOptions> page = pages.get(i);
            SelectorItem item = new SelectorItem(page, new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  onSelected_.execute(page);
               }
            });
            verticalPanel.add(item);
         }

         scrollPanel.add(verticalPanel);
         initWidget(scrollPanel);
      }
      
      @Override
      public void setSelectionHandler(CommandWithArg<WizardPage<NewConnectionContext, ConnectionOptions>> onSelected)
      {
         onSelected_ = onSelected;
      }
      
      private CommandWithArg<WizardPage<NewConnectionContext, ConnectionOptions>> onSelected_;
   }
   
   private static class SelectorItem extends Composite
   {
      public SelectorItem(WizardPage<NewConnectionContext, ConnectionOptions> page,
                          ClickHandler handler)
      {
         WizardResources.Styles styles = WizardResources.INSTANCE.styles();
         
         DockLayoutPanel panel = new DockLayoutPanel(Unit.PX);
         panel.addStyleName(styles.wizardPageSelectorItem());
         panel.addStyleName(styles.wizardPageSelectorItemSize());
         
         Image rightArrow = new Image(new ImageResource2x(WizardResources.INSTANCE.wizardDisclosureArrow2x()));
         rightArrow.addStyleName(styles.wizardPageSelectorItemRightArrow());
         panel.addEast(rightArrow, 28);
         
         if (page.getImage() == null)
         {
            panel.addWest(new Label(""), 28);
         }
         else
         {  
            Image icon = new Image(page.getImage());
            icon.addStyleName(RES.styles().wizardPageConnectionSelectorItemLeftIcon());
            
            panel.addWest(icon, 28);
         }
         
         Label mainLabel = new Label(page.getTitle());
         mainLabel.addStyleName(WizardResources.INSTANCE.styles().wizardPageSelectorItemLabel());
         mainLabel.getElement().setAttribute("title", page.getSubTitle());
         panel.add(mainLabel);
         
         panel.addDomHandler(handler, ClickEvent.getType());
         
         initWidget(panel);
      }
   }

   public interface Styles extends CssResource
   {
      String wizardPageSelector();
      String wizardPageConnectionSelectorItemLeftIcon();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("NewConnectionNavigationPage.css")
      Styles styles();
   }

   @Override
   protected String getWizardPageBackgroundStyle()
   {
      return NewConnectionWizard.RES.styles().newConnectionWizardBackground();
   }

   private static Resources RES = GWT.create(Resources.class);
   static { RES.styles().ensureInjected(); }
}
