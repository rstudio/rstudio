/*
 * RSConnectCloudAccount.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

public class PositCloudAccount extends Composite
{
   private static PositCloudAccountUiBinder uiBinder = GWT
         .create(PositCloudAccountUiBinder.class);

   interface PositCloudAccountUiBinder extends
         UiBinder<Widget, PositCloudAccount>
   {
   }

   public PositCloudAccount()
   {
      initWidget(uiBinder.createAndBindUi(this));
      accountInfo.addKeyUpHandler(new KeyUpHandler()
      {
         @Override
         public void onKeyUp(KeyUpEvent event)
         {
            if (onAccountInfoChanged_ != null)
            {
               onAccountInfoChanged_.execute();
            }
         }
      });
   }
   
   public void focus()
   {
      accountInfo.setFocus(true);
   }
   
   public void setOnAccountInfoChanged(Command cmd)
   {
      onAccountInfoChanged_ = cmd;
   }
   
   public String getAccountInfo()
   {
      return accountInfo.getText();
   }
   
   public RSConnectCloudAccount.ConnectStyle getStyle()
   {
      return style;
   }

   @UiField TextArea accountInfo;
   @UiField
   RSConnectCloudAccount.ConnectStyle style;
   
   private Command onAccountInfoChanged_;
}
