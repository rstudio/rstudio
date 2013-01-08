/*
 * FindInFilesServerOperations.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.find.model;

import com.google.gwt.core.client.JsArrayString;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

public interface FindInFilesServerOperations
{
   void beginFind(String searchString,
                  boolean regex,
                  boolean ignoreCase,
                  FileSystemItem directory,
                  JsArrayString filePatterns,
                  ServerRequestCallback<String> requestCallback);

   void stopFind(String findOperationHandle,
                 ServerRequestCallback<Void> requestCallback);

   void clearFindResults(ServerRequestCallback<Void> requestCallback);
}
