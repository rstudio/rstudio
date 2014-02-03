/*
 * ShinyAppsDeploy.java
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

import java.util.List;

import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.shiny.model.ShinyAppsApplicationInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class ShinyAppsDeploy extends Composite
{
   private static ShinyAppsDeployUiBinder uiBinder = GWT
         .create(ShinyAppsDeployUiBinder.class);

   interface ShinyAppsDeployUiBinder extends UiBinder<Widget, ShinyAppsDeploy>
   {
   }
   
   public interface DeployStyle extends CssResource
   {
      String statusLabel();
      String normalStatus();
      String otherStatus();
   }

   public ShinyAppsDeploy()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public void setSourceDir(String dir)
   {
      sourceDir.setText(dir);
      appName.setText(FilePathUtils.friendlyFileName(dir));
   }
   
   public void setAccountList(JsArrayString accounts, String selected)
   {
      accountList.clear();
      int selectedIdx = 0;
      for (int i = 0; i < accounts.length(); i++)
      {
         String account = accounts.get(i);
         accountList.addItem(account);
         if (account.equals(selected))
            selectedIdx = i;
      }
      accountList.setSelectedIndex(selectedIdx);
   }
   
   public String getSelectedAccount()
   {
      int idx = accountList.getSelectedIndex();
      return idx >= 0 ? 
            accountList.getItemText(idx) :
            null;
   }
   
   public String getSelectedApp()
   {
      int idx = appList.getSelectedIndex();
      return idx >= 0 ? 
            appList.getItemText(idx) :
            null;
   }
   
   public void setAppList(List<String> apps, String selected)
   {
      appList.clear();
      int selectedIdx = apps.size();
      if (apps != null)
      {
         for (int i = 0; i < apps.size(); i++)
         {
            String app = apps.get(i);
            appList.addItem(app);
            if (app.equals(selected))
            {
               selectedIdx = i;
            }
         }
      }
      appList.addItem("Create New");
      appList.setSelectedIndex(selectedIdx);
   }
   
   public String getNewAppName()
   {
      return appName.getText();
   }
   
   public void showAppInfo(ShinyAppsApplicationInfo info)
   {
      if (info == null)
      {
         appInfoPanel.setVisible(false);
         nameLabel.setVisible(true);
         appName.setVisible(true);
         return;
      }

      urlAnchor.setText(info.getUrl());
      urlAnchor.setHref(info.getUrl());
      String status = info.getStatus();
      statusLabel.setText(status);
      statusLabel.setStyleName(style.statusLabel() + " " + 
              (status.equals("running") ?
                    style.normalStatus() :
                    style.otherStatus()));

      appInfoPanel.setVisible(true);
      nameLabel.setVisible(false);
      appName.setVisible(false);
   }
   
   public HandlerRegistration addAccountChangeHandler(ChangeHandler handler)
   {
      return accountList.addChangeHandler(handler);
   }

   public HandlerRegistration addAppChangeHandler(ChangeHandler handler)
   {
      return appList.addChangeHandler(handler);
   }

   @UiField Anchor urlAnchor;
   @UiField Label sourceDir;
   @UiField Label nameLabel;
   @UiField Label statusLabel;
   @UiField ListBox accountList;
   @UiField ListBox appList;
   @UiField TextBox appName;
   @UiField HTMLPanel appInfoPanel;
   @UiField DeployStyle style;
}
