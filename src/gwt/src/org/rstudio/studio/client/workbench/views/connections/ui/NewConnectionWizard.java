/*
 * NewConnectionWizard.java
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

package org.rstudio.studio.client.workbench.views.connections.ui;

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class NewConnectionWizard extends Wizard<NewConnectionContext, ConnectionOptions>
{
   @Inject
   private void initialize(UIPrefs uiPrefs,
                           EventBus events,
                           GlobalDisplay globalDisplay,
                           ConnectionsServerOperations server)
   {
   }

   public NewConnectionWizard(NewConnectionContext context,
                              ProgressOperationWithInput<ConnectionOptions> operation)
   {
      super(
         "New Connection",
         "OK",
         context,
         createFirstPage(context),
         operation
      );

      RStudioGinjector.INSTANCE.injectMembers(this); 
   }
   
   private static WizardPage<NewConnectionContext, ConnectionOptions>
      createFirstPage(NewConnectionContext input)
   {
      return new NewConnectionNavigationPage("New Connection", "OK", null, input);
   }
}
