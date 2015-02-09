/*
 * RSConnectAccountList.java
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

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class RSConnectAccountList extends Composite
{
   public class AccountEntry extends Composite
   {
      public AccountEntry(RSConnectAccount account)
      {
         account_ = account;
         HorizontalPanel panel = new HorizontalPanel();
         Image icon = new Image(account.isCloudAccount() ? 
               RSConnectAccountResources.INSTANCE.cloudAccountIconSmall() : 
               RSConnectAccountResources.INSTANCE.localAccountIconSmall());
         icon.getElement().getStyle().setMarginRight(2, Unit.PX);
         panel.add(icon);
         Label serverLabel = new Label(account.getServer() + ": ");
         serverLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
         serverLabel.getElement().getStyle().setMarginRight(5, Unit.PX);
         panel.add(serverLabel);

         Label nameLabel = new Label(account.getName());
         panel.add(nameLabel);

         initWidget(panel);
      }
      
      public RSConnectAccount getAccount()
      {
         return account_;
      }
      
      final RSConnectAccount account_;
   }
   
   public RSConnectAccountList(RSConnectServerOperations server, 
         GlobalDisplay display,
         boolean refreshImmediately)
   {
      server_ = server;
      display_ = display;
      accountList_ = new WidgetListBox<AccountEntry>();
      accountList_.setEmptyText("No accounts connected.");
      if (refreshImmediately)
         refreshAccountList();
      initWidget(accountList_);
   }
   
   public void setOnRefreshCompleted(Operation operation)
   {
      onRefreshCompleted_ = operation;
   }
   
   public void refreshAccountList()
   {
      server_.getRSConnectAccountList(
            new ServerRequestCallback<JsArray<RSConnectAccount>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectAccount> accounts)
         {
            setAccountList(accounts);
         }

         @Override
         public void onError(ServerError error)
         {
            display_.showErrorMessage("Error retrieving accounts", 
                                     error.getMessage());
         }
      });
   }
   
   public void setAccountList(JsArray<RSConnectAccount> accounts)
   {
      accounts_ = accounts;
      accountList_.clearItems();
      for (int i = 0; i < accounts.length(); i++)
      {
         accountList_.addItem(new AccountEntry(accounts.get(i)));
      }
      if (onRefreshCompleted_ != null)
      {
         onRefreshCompleted_.execute();
      }
   }
   
   public RSConnectAccount getSelectedAccount()
   {
      if (accountList_ == null || accounts_ == null)
      {
         return null;
      }

      int idx = accountList_.getSelectedIndex();
      if (idx < accounts_.length()) 
      {
         return accounts_.get(idx);
      }
      return null;
   }
   
   public void selectAccount(RSConnectAccount account)
   {
      for (int i = 0; i < accounts_.length(); i ++)
      {
         if (accounts_.get(i).equals(account))
         {
            accountList_.setSelectedIndex(i);
            break;
         }
      }
   }
   
   public HandlerRegistration addChangeHandler(ChangeHandler handler)
   {
      return accountList_.addChangeHandler(handler);
   }
   
   public int getAccountCount() 
   {
      if (accounts_ == null)
         return 0;
      else
         return accounts_.length();
   }
   
   private final WidgetListBox<AccountEntry> accountList_;
   private final RSConnectServerOperations server_; 
   private final GlobalDisplay display_;
   private JsArray<RSConnectAccount> accounts_;
   private Operation onRefreshCompleted_ = null;
}
