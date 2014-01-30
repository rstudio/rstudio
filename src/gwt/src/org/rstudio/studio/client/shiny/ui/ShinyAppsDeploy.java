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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
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

   public ShinyAppsDeploy()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public void setSourceDir(String dir)
   {
      sourceDir.setText(dir);
      appName.setText(FilePathUtils.friendlyFileName(dir));
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
      return idx >= 0 ? 
            accountList.getItemText(idx) :
            null;
   }
   
   public void setAppList(List<String> apps)
   {
      appList.clear();
      if (apps != null)
      {
         for (int i = 0; i < apps.size(); i++)
         {
            appList.addItem(apps.get(i));
         }
      }
      appList.addItem("Create New");

      // TODO: Don't always select the last item
      appList.setSelectedIndex(appList.getItemCount() - 1);
   }

   @UiField Label sourceDir;
   @UiField ListBox accountList;
   @UiField ListBox appList;
   @UiField TextBox appName;
}
