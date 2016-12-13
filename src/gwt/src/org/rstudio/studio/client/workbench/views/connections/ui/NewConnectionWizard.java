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

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.ui.NewConnectionDialog.Resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionWizard extends ModalDialog<ConnectionOptions>
{
   @Inject
   private void initialize(UIPrefs uiPrefs,
                           EventBus events,
                           GlobalDisplay globalDisplay,
                           ConnectionsServerOperations server)
   {
   }
   
   public NewConnectionWizard(NewConnectionContext context,
                              OperationWithInput<ConnectionOptions> operation,
                              Operation cancelOperation)
   {
      super("New Connection", operation, cancelOperation);
      RStudioGinjector.INSTANCE.injectMembers(this);
           
      HelpLink helpLink = new HelpLink(
            "Creating New Connections",
            "new_connections",
            false);
      helpLink.addStyleName(RES.styles().helpLink());
      addLeftWidget(helpLink);   
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
   }
   
   @Override
   protected boolean validate(ConnectionOptions result)
   {
      return true;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel container = new VerticalPanel();    
     
      return container;
   }

   @Override
   protected ConnectionOptions collectInput()
   {
      return null;
   }
   
   public static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RES.styles().ensureInjected();
   }
}
