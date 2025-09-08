/*
 * AddinsServerOperations.java
 *
 * Copyright (C) 2015 by Posit Software, PBC
 *
 * Unless you have received this program directly from Addins pursuant
 * to the terms of a commercial license agreement with Addins, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.addins;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;

public interface AddinsServerOperations
{
   void getRAddins(boolean reindex,
                   ServerRequestCallback<RAddins> requestCallback);
   
   void prepareForAddin(ServerRequestCallback<Void> requestCallback);
   
   void executeRAddinNonInteractively(String commandId,
                                      ServerRequestCallback<Void> requestCallback);
}
