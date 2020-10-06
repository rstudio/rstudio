/*
 * FilePathUtils.java
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
   
   public static String dirFromFile(String fileName)
   {
      int idx = fileName.lastIndexOf("/");
      return idx > 0 ?
            fileName.substring(0, idx) :
            fileName;
   }

   public static String parent(String path)
   {
      int idx = path.lastIndexOf("/");
      return idx > 0 ?
            path.substring(0, idx) :
            path;
   }
   
   public static boolean pathIsAbsolute(String path)
   {
      // Examine the path to see if it appears to be absolute. An absolute path
      // - begins with ~ , or 
      // - begins with / (Unix-like systems), or
      // - begins with F:/ (Windows systems), where F is an alphabetic drive 
      //   letter.
      return path.startsWith(FileSystemItem.HOME_PREFIX) || 
             path.startsWith("/") ||                        
             path.matches("^[a-zA-Z]:\\/.*");
   }
   
   public static boolean pathIsRelative(String path)
   {
      return !pathIsAbsolute(path);
   }

   public static String normalizePath (String path, String workingDirectory)
   {
      if (pathIsAbsolute(path))
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
   
   public static String stripExtension(String path)
   {
      int tarGzIndex = path.indexOf(".tar.gz");
      if (tarGzIndex != -1)
         return path.substring(0, tarGzIndex);
      
      return path.substring(0, path.lastIndexOf('.'));
   }
   
   public static String fileNameSansExtension(String path)
   {
      int lastSlashIndex = path.lastIndexOf('/');
      int extensionIndex = getExtensionIndex(path, lastSlashIndex + 1);
      return path.substring(lastSlashIndex + 1, extensionIndex);
   }
   
   public static String filePathSansExtension(String path)
   {
      int lastSlashIndex = path.lastIndexOf('/');
      int extensionIndex = getExtensionIndex(path, lastSlashIndex + 1);
      return path.substring(0, extensionIndex);
   }
   
   @SuppressWarnings("unused")
   private static int getExtensionIndex(String path)
   {
      return getExtensionIndex(path, path.lastIndexOf('/') + 1);
   }
   
   private static int getExtensionIndex(String path,
                                        int fromIndex)
   {
      if (path.endsWith(".tar.gz"))
         return path.length() - 7;
      else if (path.endsWith(".nb.html"))
         return path.length() - 8;
      
      int lastDotIndex = path.lastIndexOf('.');
      if (lastDotIndex == -1 || lastDotIndex < fromIndex)
         return path.length();
      
      return lastDotIndex;
   }
}
