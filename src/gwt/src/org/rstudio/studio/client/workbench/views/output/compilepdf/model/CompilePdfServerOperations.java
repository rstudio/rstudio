/*
 * CompilePdfServerOperations.java
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

package org.rstudio.studio.client.workbench.views.output.compilepdf.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface CompilePdfServerOperations
{
   // returns true to indicate that the compile has started, returns false
   // to indicate that the compile pdf could not be started because another
   // compile is currently in progress
   void compilePdf(FileSystemItem targetFile, 
                   String completedAction,
                   ServerRequestCallback<Boolean> requestCallback);
}
