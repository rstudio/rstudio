/*
 * VCSServerOperations.java
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
package org.rstudio.studio.client.common.vcs;

import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface VCSServerOperations extends CryptoServerOperations
{
   void askpassCompleted(String value, boolean remember,
         ServerRequestCallback<Void> requestCallback);
   
   void createSshKey(CreateKeyOptions options,
                     ServerRequestCallback<CreateKeyResult> requestCallback);
   
   void vcsClone(VcsCloneOptions options,
                 ServerRequestCallback<ConsoleProcess> requestCallback);

}
