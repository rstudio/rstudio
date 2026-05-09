/*
 * RStudioAPIServerOperations.java
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
package org.rstudio.studio.client.common.rstudioapi.model;

import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;

public interface RStudioAPIServerOperations extends CryptoServerOperations
{
   void showDialogCompleted(String prompt,
                            boolean ok,
                            ServerRequestCallback<VoidResponse> callback);

   void showMenuCompleted(Integer selection,
                          ServerRequestCallback<VoidResponse> callback);

   void askSecretCompleted(String value,
                           boolean remember,
                           boolean changed,
                           ServerRequestCallback<VoidResponse> requestCallback);

   void recordCommandExecution(String commandId, ServerRequestCallback<VoidResponse> requestCallback);
}
