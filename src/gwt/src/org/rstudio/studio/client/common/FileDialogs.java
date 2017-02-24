/*
 * FileDialogs.java
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
package org.rstudio.studio.client.common;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

public interface FileDialogs
{
   void openFile(String caption,
                 FileSystemContext fsContext,
                 FileSystemItem initialFilePath,
                 ProgressOperationWithInput<FileSystemItem> operation);
   
   void openFile(String caption,
                 FileSystemContext fsContext,
                 FileSystemItem initialFilePath,
                 String filter,
                 ProgressOperationWithInput<FileSystemItem> operation);
   
   void openFile(String caption,
                 FileSystemContext fsContext,
                 FileSystemItem initialFilePath,
                 String filter,
                 boolean canChooseDirectories,
                 ProgressOperationWithInput<FileSystemItem> operation);
   

   void saveFile(String caption,
                 FileSystemContext fsContext,
                 FileSystemItem initialFilePath,
                 String defaultExtension,
                 boolean forceDefaultExtension,
                 ProgressOperationWithInput<FileSystemItem> operation);

   void chooseFolder(String caption,
                     FileSystemContext fsContext,
                     FileSystemItem initialDir,
                     ProgressOperationWithInput<FileSystemItem> operation);
}
