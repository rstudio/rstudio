/*
 * RSConnectCloudConnectAccount.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.rsconnect.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class RSConnectCloudConnectAccount extends Composite
{
   private static RSConnectCloudConnectAccountUiBinder uiBinder = GWT
         .create(RSConnectCloudConnectAccountUiBinder.class);

   interface RSConnectCloudConnectAccountUiBinder extends
         UiBinder<Widget, RSConnectCloudConnectAccount>
   {
   }

   public RSConnectCloudConnectAccount()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
}
