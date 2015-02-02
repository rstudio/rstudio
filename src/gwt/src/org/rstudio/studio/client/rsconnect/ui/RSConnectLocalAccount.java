/*
 * RSConnectLocalAccount.java
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

import org.rstudio.core.client.widget.TextBoxWithCue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class RSConnectLocalAccount extends Composite
{
   private static RSConnectLocalAccountUiBinder uiBinder = GWT
         .create(RSConnectLocalAccountUiBinder.class);

   interface RSConnectLocalAccountUiBinder extends
         UiBinder<Widget, RSConnectLocalAccount>
   {
   }

   public RSConnectLocalAccount()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public String getServerUrl() 
   {
      return serverUrl_.getText();
   }

   public String getAccountName() 
   {
      return accountName_.getText();
   }
   
   public void focus()
   {
      serverUrl_.setFocus(true);
   }
   
   @UiField TextBoxWithCue serverUrl_;
   @UiField TextBoxWithCue accountName_;
}
