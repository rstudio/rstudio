/*
 * RSConnectCloudDialog.java
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
package org.rstudio.studio.client.rsconnect.ui;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.rsconnect.model.NewRSConnectAccountResult;

import com.google.gwt.user.client.ui.Widget;

public class RSConnectCloudDialog extends ModalDialog<NewRSConnectAccountResult>
{
   public RSConnectCloudDialog(
         ProgressOperationWithInput<NewRSConnectAccountResult> operation,
         Operation cancelOperation)
   {
      super("Connecting your ShinyApps Account", Roles.getDialogRole(), operation, cancelOperation);
      setWidth("400px");
   }

   @Override
   protected NewRSConnectAccountResult collectInput()
   {
      return new NewRSConnectAccountResult(
            contents_ == null ? "" : contents_.getAccountInfo());
   }

   @Override
   protected boolean validate(NewRSConnectAccountResult input)
   {
      return !input.getCloudSecret().isEmpty();
   }

   @Override
   protected Widget createMainWidget()
   {
      contents_ = new RSConnectCloudAccount();
      return contents_;
   }

   RSConnectCloudAccount contents_;
}
