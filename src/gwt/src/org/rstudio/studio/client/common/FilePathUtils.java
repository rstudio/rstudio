/*
 * FilePathUtils.java
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

import org.rstudio.core.client.files.FileSystemItem;

public class FilePathUtils
{
   public static String friendlyFileName(String unfriendlyFileName)
   {
      int idx = unfriendlyFileName.lastIndexOf("/");
      if (idx < 0)
      {
         idx = unfriendlyFileName.lastIndexOf("\\");
      }
      return unfriendlyFileName.substring(
              idx + 1, unfriendlyFileName.length()).trim();
   }
   
   public static String normalizePath (String path, String workingDirectory)
   {
      // Examine the path to see if it appears to be absolute. An absolute path
	  // - begins with ~ , or 
	  // - begins with / (Unix-like systems), or
	  // - begins with F:/ (Windows systems), where F is an alphabetic drive 
	  //   letter.
      if (path.startsWith(FileSystemItem.HOME_PREFIX) || 
          path.startsWith("/") ||                        
          path.matches("^[a-zA-Z]:\\/.*"))
      {
         return path;
      }

      // if the path appears to be relative, prepend the working directory
      // (consider: should we try to handle ..-style relative notation here?)
      String prefix = new String(workingDirectory + 
            (workingDirectory.endsWith("/") ? "" : "/"));
      String relative = new String(path.startsWith("./") ? 
            path.substring(2, path.length()) : path);
      
      return prefix + relative;
   }
}
