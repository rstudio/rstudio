/*
 * WebCRANChooser.java
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
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.packages.CRANChooser;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;

public class WebCRANChooser implements CRANChooser
{
   public void chooseCRANMirror(PackagesServerOperations server,
                                final EventBus eventBus,
                                GlobalDisplay globalDisplay,
                                Command onConfigured)
   {
      globalDisplay.showErrorMessage(
            "CRAN Mirror Not Configured",
            "Please choose a CRAN mirror, then try again.",
            new Operation()
            {
               public void execute()
               {
                  eventBus.fireEvent(
                        new SendToConsoleEvent("chooseCRANmirror()", true));
               }
            });
   }
}
