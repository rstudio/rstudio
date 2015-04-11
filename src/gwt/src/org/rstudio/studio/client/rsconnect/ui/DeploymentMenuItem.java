/*
 * DeploymentMenuItem.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.CheckableMenuItem;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;

import com.google.gwt.user.client.Command;

public class DeploymentMenuItem extends CheckableMenuItem
{
   public DeploymentMenuItem(RSConnectDeploymentRecord record, 
         boolean isChecked,
         Command onInvoked)
   {
      // we don't know the name of RPubs-deployed content, so don't show it
      super(record.getServer() == "rpubs.com" || 
               StringUtil.isNullOrEmpty(record.getName()) ? 
            record.getServer() : 
            record.getName() + " (" + record.getServer() + ")");
      isChecked_ = isChecked;
      onInvoked_ = onInvoked;
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
   
   private boolean isChecked_;
   private final Command onInvoked_;
}
