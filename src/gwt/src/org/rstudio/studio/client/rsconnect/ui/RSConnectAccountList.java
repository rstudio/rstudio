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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Cursor;
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
               new ImageResource2x(RSConnectResources.INSTANCE.cloudAccountIconSmall2x()) : 
               new ImageResource2x(RSConnectResources.INSTANCE.localAccountIconSmall2x()));
         icon.getElement().getStyle().setMarginRight(2, Unit.PX);
         panel.add(icon);

         Label nameLabel = new Label(account.getName() + ":");
         nameLabel.getElement().getStyle().setCursor(Cursor.POINTER);
         nameLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
         nameLabel.getElement().getStyle().setMarginRight(4, Unit.PX);
         panel.add(nameLabel);

         Label serverLabel = new Label(account.getServer());
         serverLabel.getElement().getStyle().setCursor(Cursor.POINTER);
         panel.add(serverLabel);

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
         boolean refreshImmediately, 
         boolean showCloudAccounts)
   {
      server_ = server;
      display_ = display;
      showCloudAccounts_ = showCloudAccounts;
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
   
   public int setAccountList(JsArray<RSConnectAccount> accounts)
   {
      accounts_.clear();
      accountList_.clearItems();
      for (int i = 0; i < accounts.length(); i++)
      {
         if (showCloudAccounts_ || !accounts.get(i).isCloudAccount())
         {
            accounts_.add(accounts.get(i));
            accountList_.addItem(new AccountEntry(accounts.get(i)));
         }
      }
      if (onRefreshCompleted_ != null)
      {
         onRefreshCompleted_.execute();
      }
      return accounts_.size();
   }
   
   public RSConnectAccount getSelectedAccount()
   {
      if (accountList_ == null || accounts_ == null)
      {
         return null;
      }

      int idx = accountList_.getSelectedIndex();
      if (idx < accounts_.size()) 
      {
         return accounts_.get(idx);
      }
      return null;
   }
   
   public void selectAccount(RSConnectAccount account)
   {
      for (int i = 0; i < accounts_.size(); i ++)
      {
         if (accounts_.get(i).equals(account))
         {
            // extract the list of accounts, sort the desired account to the
            // top, and put them back
            List<AccountEntry> entries = new ArrayList<AccountEntry>();
            entries.addAll(accountList_.getItems());
            if (entries.size() <= i)
               return;
            accountList_.clearItems();
            AccountEntry entry = entries.get(i);
            entries.remove(i);
            entries.add(0, entry);
            for (int j = 0; j < entries.size(); j++) {
               accountList_.addItem(entries.get(j));
            }

            // synchronize the backing array
            accounts_.remove(i);
            accounts_.add(0, account);
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
         return accounts_.size();
   }
   
   public void setShowCloudAccounts(boolean show)
   {
      showCloudAccounts_ = show;
   }
   
   public boolean getShowCloudAccounts()
   {
      return showCloudAccounts_;
   }
   
   private final WidgetListBox<AccountEntry> accountList_;
   private final RSConnectServerOperations server_; 
   private final GlobalDisplay display_;
   
   private boolean showCloudAccounts_;
   
   private ArrayList<RSConnectAccount> accounts_ = 
         new ArrayList<RSConnectAccount>();
   private Operation onRefreshCompleted_ = null;
}
