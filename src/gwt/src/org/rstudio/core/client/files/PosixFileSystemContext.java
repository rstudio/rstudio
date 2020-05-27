/*
 * PosixFileSystemContext.java
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
package org.rstudio.core.client.files;

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

import java.util.ArrayList;

public abstract class PosixFileSystemContext implements FileSystemContext
{
   public PosixFileSystemContext()
   {
      workingDir_ = "~";
   }

   public void setCallbacks(Callbacks callbacks)
   {
      callbacks_ = callbacks;
   }

   public String combine(String root, String name)
   {
      if (name == null || name.length() == 0)
            return root;

      // Is it absolute?
      if (isAbsolute(name))
         return name;

      if (root == null || root.length() == 0)
         return name;

      if (root.endsWith("/"))
         return root + name;
      else
         return root + "/" + name;
   }

   public FileSystemItem[] parseDir(String dirPath)
   {
      ArrayList<FileSystemItem> results = new ArrayList<FileSystemItem>();

      if (dirPath.startsWith("/"))
         results.add(FileSystemItem.createDir("/"));

      Pattern pattern = Pattern.create("[^/]+");
      Match m = pattern.match(dirPath, 0);
      while (m != null)
      {
         results.add(FileSystemItem.createDir(
               dirPath.substring(0, m.getIndex() + m.getValue().length())));

         m = m.nextMatch();
      }

      return results.toArray(new FileSystemItem[0]);
   }

   public boolean isAbsolute(String path)
   {
      if (path.startsWith("/") || path.startsWith("~/") || path.equals("~"))
         return true;

      // Detect if this is a Windows root--necessary for Windows RDesktop.
      if (path.length() >= 2
          && isAsciiLetter(path.charAt(0))
          && path.charAt(1) == ':')
      {
         return true;
      }

      return false;
   }

   private boolean isAsciiLetter(char c)
   {
      return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
   }

   public String pwd()
   {
      return workingDir_;
   }

   public FileSystemItem pwdItem()
   {
      return FileSystemItem.createDir(workingDir_);
   }

   public FileSystemItem[] ls()
   {
      return contents_;
   }

   public String validatePathElement(String name, boolean forCreation)
   {
      if (name == null || name.length() == 0)
         return "Name is empty";
      if (name.startsWith(" ") || name.endsWith(" "))
         return "Names should not start or end with spaces";
      if (name.contains("/"))
         return "Illegal character: /";
      if (forCreation && (name.equals(".") || name.equals("..")))
         return "Illegal name";

      return null;
   }

   public FileSystemItem itemForName(String name,
                                     boolean onlyIfExists,
                                     boolean createAsDirectory)
   {
      assert validatePathElement(name, true) == null;

      if (contents_ == null)
         return null;
      for (FileSystemItem fsi : contents_)
         if (fsi.getName().equalsIgnoreCase(name))
            return fsi;

      if (onlyIfExists)
         return null;
      else
      {
         String path = combine(workingDir_, name);
         if (createAsDirectory)
            return FileSystemItem.createDir(path);
         else
            return FileSystemItem.createFile(path);
      }
   }

   public boolean isRoot(FileSystemItem item)
   {
      return item.isDirectory() && item.getPath().equals("~");
   }

   public boolean isCloudRoot(FileSystemItem item)
   {
      return false;
   }

   protected String workingDir_;
   protected FileSystemItem[] contents_;
   protected Callbacks callbacks_;
}
