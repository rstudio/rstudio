/*
 * PublishCodePage.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PublishCodePage 
   extends WizardPage<RSConnectPublishInput, RSConnectPublishResult>
{
   public PublishCodePage(String title, String subTitle, 
         RSConnectPublishInput input)
   {
      super(title, subTitle, "Publish", null, null);
   }

   @Override
   public void focus()
   {
      
   }
   
   @Override
   public void onActivate()
   {
      contents_.populateAccountList();
   }
   
   @Override
   protected Widget createWidget()
   {
      contents_ = new RSConnectDeploy(true, true);
      return contents_;
   }

   @Override
   protected void initialize(RSConnectPublishInput initData)
   {
   }

   @Override
   protected RSConnectPublishResult collectInput()
   {
      return new RSConnectPublishResult();
   }

   @Override
   protected boolean validate(RSConnectPublishResult input)
   {
      return false;
   }
   
   private RSConnectDeploy contents_;
}
