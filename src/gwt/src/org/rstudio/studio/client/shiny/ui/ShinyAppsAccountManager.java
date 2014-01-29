/*
 * ShinyAppsAccountManager.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.shiny.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class ShinyAppsAccountManager extends Composite
{

   private static ShinyAppsAccountManagerUiBinder uiBinder = GWT
         .create(ShinyAppsAccountManagerUiBinder.class);

   interface ShinyAppsAccountManagerUiBinder extends
         UiBinder<Widget, ShinyAppsAccountManager>
   {
   }

   public ShinyAppsAccountManager()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public void setAccountList(JsArrayString accounts)
   {
      accountList.clear();
      for (int i = 0; i < accounts.length(); i++)
      {
         accountList.addItem(accounts.get(i));
      }
   }
   
   public String getSelectedAccount()
   {
      int idx = accountList.getSelectedIndex();
      return idx >= 0 ? accountList.getItemText(idx) : null;
   }
   
   public void removeAccount(String accountName)
   {
      for (int i = 0; i < accountList.getItemCount(); i++)
      {
         if (accountList.getItemText(i).equals(accountName))
         {
            accountList.removeItem(i);
            return;
         }
      }
   }
   
   public HandlerRegistration addAccountSelectionChangeHandler(
         ChangeHandler handler)
   {
      return accountList.addChangeHandler(handler);
   }
   
   @UiField
   ListBox accountList;
}
