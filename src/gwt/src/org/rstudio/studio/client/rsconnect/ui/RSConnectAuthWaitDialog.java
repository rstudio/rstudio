/*
 * RSConnectAuthWaitDialog.java
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

import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.rsconnect.model.RSConnectPreAuthToken;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerInfo;

import com.google.gwt.user.client.ui.Widget;

public class RSConnectAuthWaitDialog extends ModalDialogBase
{
   public RSConnectAuthWaitDialog(RSConnectServerInfo serverInfo,
                                  RSConnectPreAuthToken token)
   {
      serverInfo_ = serverInfo;
      token_ = token;
      setText("Waiting for authentication");
      addCancelButton();
   }

   @Override
   protected Widget createMainWidget()
   {
      RSConnectAuthWait contents = new RSConnectAuthWait();
      contents.setClaimLink(serverInfo_.getName(), 
                            token_.getClaimUrl());
      return contents;
   }
   
   private RSConnectServerInfo serverInfo_;
   private RSConnectPreAuthToken token_;
}
