/*
 * ConsoleServerOperations.java
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
package org.rstudio.studio.client.workbench.views.console.model;

import org.rstudio.studio.client.common.reditor.model.REditorServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;

public interface ConsoleServerOperations extends REditorServerOperations,
                                                 HistoryServerOperations
{
   // interrupt the current session
   void interrupt(ServerRequestCallback<Void> requestCallback);

   // send console input
   void consoleInput(String consoleInput, 
                     ServerRequestCallback<Void> requestCallback);
   
   void resetConsoleActions(ServerRequestCallback<Void> requestCallback);
}
