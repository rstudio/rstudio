/*
 * HTMLPreviewServerOperations.java
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
package org.rstudio.studio.client.htmlpreview.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.rpubs.model.RPubsServerOperations;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;

public interface HTMLPreviewServerOperations extends RPubsServerOperations
{
   void previewHTML(HTMLPreviewParams params, 
                    ServerRequestCallback<Boolean> requestCallback);

   void terminatePreviewHTML(ServerRequestCallback<Void> requestCallback);

   void getHTMLCapabilities(ServerRequestCallback<HTMLCapabilities> callback);

   // copy file
   void copyFile(FileSystemItem sourceFile, 
                 FileSystemItem targetFile,
                 boolean overwrite, 
                 ServerRequestCallback<Void> requestCallback);

   String getApplicationURL(String pathName);
   String getFileUrl(FileSystemItem file);
}
