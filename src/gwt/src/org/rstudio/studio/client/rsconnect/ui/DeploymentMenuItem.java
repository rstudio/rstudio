/*
 * DeploymentMenuItem.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CheckableMenuItem;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;

public class DeploymentMenuItem extends CheckableMenuItem
{
   public DeploymentMenuItem(RSConnectDeploymentRecord record, 
         boolean isChecked,
         Command onInvoked)
   {
      super(getHTML(isChecked, record), true);
      
      isChecked_ = isChecked;
      onInvoked_ = onInvoked;
      record_ = record;
      onStateChanged();
   }

   @Override
   public String getLabel()
   {
      return getText();
   }

   @Override
   public boolean isChecked()
   {
      return isChecked_;
   }
   
   @Override
   public void onInvoked()
   {
      onInvoked_.execute();
   }
   
   @Override
   public String getHTMLContent()
   {
      return getHTML(isChecked(), record_);
   }
   
   private static String getHTML(boolean isChecked, RSConnectDeploymentRecord record)
   {
      // build title; we don't know the name of RPubs-deployed content, so don't show it
      String title = record.getServer() == "rpubs.com" || 
               StringUtil.isNullOrEmpty(record.getDisplayName()) ? 
            record.getServer() : 
            record.getDisplayName();

      // build subtitle
      String subtitle = "";
      if (!StringUtil.isNullOrEmpty(record.getUsername()))
         subtitle = record.getUsername();
      else
         subtitle = record.getAccountName();
      if (!StringUtil.isNullOrEmpty(subtitle))
         subtitle += "@";
      if (record.getHostUrl() != null)
         subtitle += StringUtil.getAuthorityFromUrl(record.getHostUrl());
      else
         subtitle += record.getServer();
      
      String label = 
            "<div>" + SafeHtmlUtils.htmlEscape(title) + "</div>" +
            "<div class=\"" + ThemeStyles.INSTANCE.menuItemSubtitle() + "\">" + 
                  SafeHtmlUtils.htmlEscape(subtitle) + "</div>";

      return AppCommand.formatMenuLabel(
            // icon
            isChecked ? 
                  new ImageResource2x(ThemeResources.INSTANCE.menuCheck2x()) :
                  null,
            label,   // label
            true,    // label is HTML
            null,    // no shortcut
            null,    // no icon offset
            null,    // no right image
            null,    // no right image description
            ThemeStyles.INSTANCE.menuCheckable());
   }
   
   private final RSConnectDeploymentRecord record_;
   private boolean isChecked_;
   private final Command onInvoked_;
}
