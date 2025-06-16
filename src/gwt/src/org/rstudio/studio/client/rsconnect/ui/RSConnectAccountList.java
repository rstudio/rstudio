/*
 * RSConnectAccountList.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.widget.CanSetControlId;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.RsconnectConstants;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;

public class RSConnectAccountList extends Composite implements CanSetControlId
{
   public RSConnectAccountList(RSConnectServerOperations server, 
         GlobalDisplay display,
         boolean refreshImmediately,
         boolean showShinyAppsAccounts,
         boolean showConnectAccounts,
         String ariaLabel)
   {
      server_ = server;
      display_ = display;
      showShinyAppsAccounts_ = showShinyAppsAccounts;
      showConnectAccounts_ = showConnectAccounts;
      accountList_ = new WidgetListBox<>();
      accountList_.setEmptyText(constants_.noAccountsConnected());
      if (refreshImmediately)
         refreshAccountList();
      initWidget(accountList_);
      accountList_.setAriaLabel(ariaLabel);
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
            display_.showErrorMessage(constants_.errorRetrievingAccounts(),
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
         RSConnectAccount account = accounts.get(i);
         if (account.isShinyAppsAccount() && showShinyAppsAccounts_)
         {
            accounts_.add(account);
            accountList_.addItem(new RSConnectAccountEntry(account));
         }
         else if (!account.isShinyAppsAccount() && showConnectAccounts_)
         {
            accounts_.add(account);
            accountList_.addItem(new RSConnectAccountEntry(account));
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
            List<RSConnectAccountEntry> entries = new ArrayList<>();
            entries.addAll(accountList_.getItems());
            if (entries.size() <= i)
               return;
            accountList_.clearItems();
            RSConnectAccountEntry entry = entries.get(i);
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
   
   public void setShowShinyAppsAccounts(boolean show) { showShinyAppsAccounts_ = show; }
   
   public boolean getShowShinyAppsAccounts() { return showShinyAppsAccounts_; }

   public void setShowConnectAccounts(boolean show)
   {
      showConnectAccounts_ = show;
   }

   public boolean getShowConnectAccounts()
   {
      return showConnectAccounts_;
   }
   
   private final WidgetListBox<RSConnectAccountEntry> accountList_;
   private final RSConnectServerOperations server_; 
   private final GlobalDisplay display_;
   
   private boolean showShinyAppsAccounts_;
   private boolean showConnectAccounts_;
   
   private ArrayList<RSConnectAccount> accounts_ = new ArrayList<>();
   private Operation onRefreshCompleted_ = null;

   @Override
   public void setElementId(String id)
   {
      accountList_.getElement().setId(id);
   }
   
   public void setLabelledBy(Element describedBy)
   {
      Roles.getListboxRole().setAriaLabelledbyProperty(accountList_.getElement(), Id.of(describedBy));
   }
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
