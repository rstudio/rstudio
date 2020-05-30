/*
 * CompilePdfServerOperations.java
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

package org.rstudio.studio.client.common.compilepdf.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface CompilePdfServerOperations
{
   // returns true to indicate that the compile has started, returns false
   // to indicate that the compile pdf could not be started because another
   // compile is currently in progress. pass the terminateExisting flag
   // to terminate a running compile
   void compilePdf(FileSystemItem targetFile,
                   String encoding,
                   SourceLocation sourceLocation,
                   String completedAction,
                   ServerRequestCallback<Boolean> requestCallback);
   
   // check whether compile pdf is running
   void isCompilePdfRunning(ServerRequestCallback<Boolean> requestCallback);
   
   // terminate any running pdf compilation
   void terminateCompilePdf(ServerRequestCallback<Boolean> requestCallback);
   
   // notify the server that the compile pdf tab was closed
   void compilePdfClosed(ServerRequestCallback<Void> requestCallback);
   
   // get a file url (used for showing in external browser)
   String getFileUrl(FileSystemItem file);
}
