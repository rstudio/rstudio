/*
 * NewConnectionSelectionPage.java
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

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;

import com.google.gwt.user.client.ui.Widget;

public class NewConnectionSelectionPage 
   extends WizardPage<NewConnectionContext, ConnectionOptions>
{
   public NewConnectionSelectionPage(String connectionName)
   {
      super(connectionName, "", connectionName + " Connection", null, null);
   }

   @Override
   public void focus()
   {
   }
   
   @Override
   public void onActivate(ProgressIndicator indicator)
   {
      contents_.onActivate(indicator);
   }
   
   @Override
   protected Widget createWidget()
   {
      contents_ = new NewConnectionShinyHost(context_);
      
      return contents_;
   }

   @Override
   protected void initialize(NewConnectionContext initData)
   {
      context_ = initData;
   }

   @Override
   protected ConnectionOptions collectInput()
   {
      return null;
   }

   @Override
   protected void validateAsync(ConnectionOptions input,
                                OperationWithInput<Boolean> onValidated)
   {
   }
   
   private NewConnectionShinyHost contents_;
   private NewConnectionContext context_;
}
