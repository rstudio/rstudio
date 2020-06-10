/*
 * NewConnectionWizard.java
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
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.connections.events.NewConnectionWizardRequestCloseEvent;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.inject.Inject;

public class NewConnectionWizard extends Wizard<NewConnectionContext, ConnectionOptions>
{
   @Inject
   private void initialize(UserPrefs uiPrefs,
                           EventBus events,
                           GlobalDisplay globalDisplay,
                           ConnectionsServerOperations server)
   {
      events.addHandler(NewConnectionWizardRequestCloseEvent.TYPE, (event) -> closeDialog());
   }

   public NewConnectionWizard(NewConnectionContext context,
                              ProgressOperationWithInput<ConnectionOptions> operation,
                              String warning)
   {
      super(
         "New Connection",
         "OK",
            Roles.getDialogRole(),
         context,
         createFirstPage(context, warning),
         operation
      );

      mainHelpLink_ = new HelpLink(
         "Using RStudio Connections",
         "rstudio_connections",
         false,
         true);
      setHelpLink(mainHelpLink_);

      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Override
   protected void onPageActivated(
                     WizardPage<NewConnectionContext, ConnectionOptions> page,
                     boolean okButtonVisible)
   {
      setHelpLink(page.getHelpLink());
   }

   @Override
   protected void onPageDeactivated(
                     WizardPage<NewConnectionContext, ConnectionOptions> page)
   {
      setHelpLink(mainHelpLink_);
   }

   @Override
   protected ArrayList<String> getWizardBodyStyles()
   {
      ArrayList<String> classes = new ArrayList<>();
      classes.add(RES.styles().wizardBodyPanel());
      return classes;
   }

   @Override
   protected String getMainWidgetStyle()
   {
      return RES.styles().mainWidget();
   }
   
   private static WizardPage<NewConnectionContext, ConnectionOptions>
      createFirstPage(NewConnectionContext input, String warning)
   {
      return new NewConnectionNavigationPage("New Connection", "OK", null, input, warning);
   }

   public interface Styles extends CssResource
   {
      String mainWidget();
      String wizardBodyPanel();
      String newConnectionWizardBackground();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewConnectionWizard.css")
      Styles styles();

      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource newConnectionWizardBackground();
   }
   
   public static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RES.styles().ensureInjected();
   }
   
   private HelpLink mainHelpLink_;
}
