/*
 * Server.java
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

package org.rstudio.studio.client.server;

import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.plumber.model.PlumberServerOperations;
import org.rstudio.studio.client.common.rstudioapi.model.RStudioAPIServerOperations;
import org.rstudio.studio.client.common.shiny.model.ShinyServerOperations;
import org.rstudio.studio.client.sql.model.SqlServerOperations;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;

public interface Server extends ApplicationServerOperations,
                                WorkbenchServerOperations,
                                SqlServerOperations,
                                ShinyServerOperations,
                                RStudioAPIServerOperations,
                                PlumberServerOperations
{     
   /**
    * Add an entry to the server's log. 
    * 
    * @param logEntryType     see LogEntryType.ERROR, ec.
    * @param logEntry         log entry (should not include newlines)
    * @param requestCallback  handle errors
    */
   void log(int logEntryType, 
            String logEntry,
            ServerRequestCallback<Void> requestCallback);
   
   void logException(ClientException e,
                     ServerRequestCallback<Void> requestCallback);
}
