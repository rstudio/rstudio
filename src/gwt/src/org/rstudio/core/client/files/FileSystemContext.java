/*
 * FileSystemContext.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.files;

import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.widget.ProgressIndicator;

public interface FileSystemContext
{
   public interface Callbacks
   {
      void onNavigated();
      void onError(String errorMessage);
      void onDirectoryCreated(FileSystemItem directory);
   }

   MessageDisplay messageDisplay();

   void setCallbacks(Callbacks callbacks);

   String combine(String root, String name);
   FileSystemItem[] parseDir(String dirPath);
   boolean isAbsolute(String path);

   /**
    * @return The current directory
    */
   String pwd();
   FileSystemItem pwdItem();

   /**
    * Begin navigating the context to the specified relative or absolute
    * path. An onNavigated() callback will be fired when navigation
    * completes--the results of cd() and ls() are stale until this happens.
    * @param relativeOrAbsolutePath
    */
   void cd(String relativeOrAbsolutePath);

   /**
    * Get the contents of the current directory
    * @return
    */
   FileSystemItem[] ls();

   /**
    * Equivalent to calling cd(".")
    */
   void refresh();

   /**
    * Begin creating a folder with the specified name in the current directory.
    * An onContentsChanged() callback will be fired when it is complete (if
    * successful--otherwise onError).
    * @param folderName
    */
   void mkdir(String folderName, ProgressIndicator progress);

   /**
    * Checks if a name is valid 
    * @param name A name for a file or directory (not a full path).
    * @return An error string if name is NOT valid; otherwise, null.
    */
   String validatePathElement(String name, boolean forCreation);

   /**
    * Finds the item in the current directory for the given name. If no
    * item is found, null is returned if onlyIfExists is true. Otherwise,
    * a new item is returned--either a directory or a file, depending on
    * createAsDirectory. 
    * @param name
    * @param onlyIfExists
    * @param createAsDirectory
    * @return
    */
   FileSystemItem itemForName(String name,
                              boolean onlyIfExists,
                              boolean createAsDirectory);

   ImageResource getIcon(FileSystemItem item);

   boolean isRoot(FileSystemItem item);
}
