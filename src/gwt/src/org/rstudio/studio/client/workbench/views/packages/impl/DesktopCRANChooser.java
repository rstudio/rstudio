/*
 * DesktopCRANChooser.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.impl;

import com.google.gwt.user.client.Command;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.packages.CRANChooser;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;

public class DesktopCRANChooser implements CRANChooser
{
   public void chooseCRANMirror(PackagesServerOperations server,
                                EventBus eventBus,
                                GlobalDisplay globalDisplay,
                                final Command onConfigured)
   {
      String reposUrl = Desktop.getFrame().chooseCRANmirror();
      if (StringUtil.isNullOrEmpty(reposUrl))
         return;  // user cancelled

      // Fire and forget
      server.setCRANReposUrl(
            reposUrl,
            new SimpleRequestCallback<Void>(
                  "Error Setting CRAN Mirror") {
               @Override
               public void onResponseReceived(Void response)
               {
                  onConfigured.execute();
               }
            });

   }
}
