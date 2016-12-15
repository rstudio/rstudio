/*
 * NewConnectionShinyPage.java
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

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;

import com.google.gwt.user.client.ui.Widget;

public class NewConnectionShinyPage 
   extends WizardPage<NewConnectionContext, ConnectionOptions>
{
   public NewConnectionShinyPage(NewConnectionInfo info)
   {
      super(info.getName(), "", info.getName() + " Connection", null, null);
      info_ = info;
   }

   @Override
   public void focus()
   {
   }

   @Override
   public void onBeforeActivate(Operation operation)
   {
      contents_.onBeforeActivate(operation, info_);
   }
   
   @Override
   public void onActivate(ProgressIndicator indicator)
   {
   }

   @Override
   public void onDeactivate(Operation operation)
   {
      contents_.onDeactivate(operation);
   }
   
   @Override
   protected Widget createWidget()
   {
      contents_ = new NewConnectionShinyHost();

      return contents_;
   }

   @Override
   protected void initialize(NewConnectionContext initData)
   {
   }

   @Override
   protected ConnectionOptions collectInput()
   {
      return contents_.collectInput();
   }
   
   private NewConnectionShinyHost contents_;
   private NewConnectionInfo info_;
}
