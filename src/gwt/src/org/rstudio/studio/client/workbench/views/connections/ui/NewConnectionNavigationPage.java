/*
 * NewConnectionNavigationPage.java
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
package org.rstudio.studio.client.workbench.views.connections.ui;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.DecorativeImage;
import org.rstudio.core.client.widget.HasWizardPageSelectionHandler;
import org.rstudio.core.client.widget.WizardNavigationPage;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.core.client.widget.WizardResources;
import org.rstudio.core.client.widget.events.ButtonClickManager;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewConnectionNavigationPage 
   extends WizardNavigationPage<NewConnectionContext, ConnectionOptions>
{
   NewConnectionNavigationPage(String title,
                                      String subTitle,
                                      ImageResource icon,
                                      NewConnectionContext context,
                                      String warning)
   {
      super(title, 
            subTitle,
            "Connect to Existing Data Sources",
            icon,
            null, 
            createPages(context),
            (p) -> createWidget(p, warning)
      );
   }
   
   private static ArrayList<WizardPage<NewConnectionContext, ConnectionOptions>>
           createPages(NewConnectionContext context)
   {
      ArrayList<WizardPage<NewConnectionContext, ConnectionOptions>> pages = new ArrayList<>();

      for(NewConnectionInfo connectionInfo: context.getConnectionsList()) {
         if (!connectionInfo.getLicensed() || 
             RStudioGinjector.INSTANCE.getSession().getSessionInfo().getSupportDriverLicensing()) {

            String subTitle = connectionInfo.getName() + " via " + connectionInfo.getSource();

            if (connectionInfo.getType() == "Shiny") {
               pages.add(new NewConnectionShinyPage(connectionInfo, subTitle));
            }
            else if (connectionInfo.getType() == "Snippet") {
               pages.add(new NewConnectionSnippetPage(connectionInfo, subTitle));
            }
            else if (connectionInfo.getType() == "Install" && connectionInfo.getSubtype() == "Package") {
               pages.add(new NewConnectionInstallPackagePage(connectionInfo));
            }
            else if (connectionInfo.getType() == "Install" && connectionInfo.getSubtype() == "Odbc") {
               pages.add(new NewConnectionPreInstallOdbcPage(connectionInfo, subTitle));
            }
         }
      }

      return pages;
   }

   private static Widget createWidget(ArrayList<WizardPage<NewConnectionContext, ConnectionOptions>> pages,
                                      String warning)
   {
      return new Selector(pages, warning);
   }

   private static class Selector
         extends Composite
         implements HasWizardPageSelectionHandler<NewConnectionContext, ConnectionOptions>
   {
      Selector(final ArrayList<WizardPage<NewConnectionContext, ConnectionOptions>> pages,
                      String warning)
      {
         WizardResources.Styles styles = WizardResources.INSTANCE.styles();
         
         VerticalPanel rootPanel = new VerticalPanel();

         if (!StringUtil.isNullOrEmpty(warning)) {
            HorizontalPanel warningPanel = new HorizontalPanel();
            
            warningPanel.addStyleName(RES.styles().wizardPageWarningPanel());
            Image warningImage = new Image(new ImageResource2x(ThemeResources.INSTANCE.warningSmall2x()));
            warningImage.addStyleName(RES.styles().wizardPageWarningImage());
            warningImage.setAltText(MessageDialogImages.DIALOG_WARNING_TEXT);
            warningPanel.add(warningImage);
            
            Label label = new Label();
            label.setText(warning);
            label.addStyleName(RES.styles().wizardPageWarningLabel());
            warningPanel.add(label);
            warningPanel.setCellWidth(label, "100%");

            rootPanel.add(warningPanel);
            rootPanel.setCellHeight(warningPanel,"25px");
         }

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
                  if (page instanceof NewConnectionPreInstallOdbcPage)
                  {
                     RStudioGinjector.INSTANCE.getDependencyManager().withOdbc(
                        new Command()
                        {
                           @Override
                           public void execute()
                           {
                              onSelected_.execute(page);
                           }
                        },
                        page.getTitle()
                     );
                  }
                  else
                  {
                     onSelected_.execute(page);
                  }
               }
            });
            verticalPanel.add(item);
         }

         scrollPanel.add(verticalPanel);
         rootPanel.add(scrollPanel);

         initWidget(rootPanel);
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
         Roles.getButtonRole().set(panel.getElement());
         panel.getElement().setTabIndex(0);
         panel.getElement().setId(ElementIds.idFromLabel(page.getTitle() + "_wizard_page"));

         DecorativeImage rightArrow = new DecorativeImage(new ImageResource2x(WizardResources.INSTANCE.wizardDisclosureArrow2x()));
         rightArrow.addStyleName(styles.wizardPageSelectorItemRightArrow());
         panel.addEast(rightArrow, 28);
         
         if (page.getImage() == null)
         {
            panel.addWest(new Label(""), 28);
         }
         else
         {  
            DecorativeImage icon = new DecorativeImage(page.getImage());
            icon.addStyleName(RES.styles().wizardPageConnectionSelectorItemLeftIcon());
            
            panel.addWest(icon, 28);
         }
         
         Label mainLabel = new Label(page.getTitle());
         mainLabel.addStyleName(WizardResources.INSTANCE.styles().wizardPageSelectorItemLabel());
         mainLabel.getElement().setAttribute("title", page.getSubTitle());
         panel.add(mainLabel);

         clickManager_ = new ButtonClickManager(panel, handler);
         initWidget(panel);
      }

      @SuppressWarnings("unused")
      private ButtonClickManager clickManager_;
   }

   public interface Styles extends CssResource
   {
      String wizardPageSelector();
      String wizardPageConnectionSelectorItemLeftIcon();
      String wizardPageWarningPanel();
      String wizardPageWarningImage();
      String wizardPageWarningLabel();
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
