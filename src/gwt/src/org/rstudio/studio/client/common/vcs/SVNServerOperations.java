/*
 * SVNServerOperations.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

public interface SVNServerOperations extends CryptoServerOperations
{
   void svnAdd(JsArrayString paths,
               ServerRequestCallback<Void> requestCallback);

   void svnDelete(JsArrayString paths,
                  ServerRequestCallback<Void> requestCallback);

   void svnRevert(JsArrayString paths,
                  ServerRequestCallback<Void> requestCallback);

   void svnStatus(
         ServerRequestCallback<JsArray<StatusAndPathInfo>> requestCallback);

   void svnDiff(String path,
                ServerRequestCallback<String> requestCallback);
}
