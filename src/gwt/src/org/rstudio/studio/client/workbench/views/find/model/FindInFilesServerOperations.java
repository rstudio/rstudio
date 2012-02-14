/*
 * FindInFilesServerOperations.java
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
package org.rstudio.studio.client.workbench.views.find.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

public interface FindInFilesServerOperations
{
   void beginFind(String searchString,
                  boolean regex,
                  boolean ignoreCase,
                  FileSystemItem directory,
                  String filePattern,
                  ServerRequestCallback<String> requestCallback);

   void stopFind(String findOperationHandle,
                 ServerRequestCallback<Void> requestCallback);
}
