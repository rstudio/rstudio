/*
 * PosixFileSystemContext.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.files;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.StringUtil;
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
         return normalizePath(name);

      if (root == null || root.length() == 0)
         return normalizePath(name);

      if (root.endsWith("/"))
         return normalizePath(root + name);
      else
         return normalizePath(root + "/" + name);
   }

   /**
    * Collapses `.` and `..` segments lexically, so a typed `../sub` lists the
    * sibling under its own path rather than as `<dir>/../sub`: the server
    * lists that fine, but the breadcrumb and every path taken from the
    * listing would carry the `..` along. `..` never climbs above the root
    * (`/`, `~`, a Windows drive, or a UNC host); with no root to anchor it,
    * a leading `..` is kept.
    */
   public static String normalizePath(String path)
   {
      String root = "";
      String rest = path;
      if (path.startsWith("//"))
      {
         // a Windows UNC path, in the `//server/share` form the server
         // reports it in; the host is the root
         int slash = path.indexOf('/', 2);
         root = slash == -1 ? path : path.substring(0, slash);
         rest = slash == -1 ? "" : path.substring(slash);
      }
      else if (path.startsWith("/"))
      {
         root = "/";
         rest = path.substring(1);
      }
      else if (path.equals("~") || path.startsWith("~/"))
      {
         root = "~";
         rest = path.substring(1);
      }
      else if (path.length() >= 2
               && isAsciiLetter(path.charAt(0))
               && path.charAt(1) == ':')
      {
         root = path.substring(0, 2);
         rest = path.substring(2);
      }

      ArrayList<String> parts = new ArrayList<>();
      for (String part : rest.split("/"))
      {
         if (part.isEmpty() || part.equals("."))
            continue;

         if (!part.equals(".."))
            parts.add(part);
         else if (!parts.isEmpty() && !parts.get(parts.size() - 1).equals(".."))
            parts.remove(parts.size() - 1);
         else if (root.isEmpty())
            parts.add("..");
      }

      String joined = StringUtil.join(parts, "/");
      if (root.isEmpty())
         return joined;
      else if (root.equals("/"))
         return "/" + joined;
      else if (joined.isEmpty())
         return root.equals("~") ? "~" : root + "/";
      else
         return root + "/" + joined;
   }

   public FileSystemItem[] parseDir(String dirPath)
   {
      ArrayList<FileSystemItem> results = new ArrayList<>();

      if (dirPath.startsWith("/"))
         results.add(FileSystemItem.createDir("/"));

      Pattern pattern = Pattern.create("[^/]+");
      Match m = pattern.match(dirPath, 0);
      while (m != null)
      {
         results.add(FileSystemItem.createDir(
               StringUtil.substring(dirPath, 0, m.getIndex() + m.getValue().length())));

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

   private static boolean isAsciiLetter(char c)
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
         return constants_.nameEmptyMessage();
      if (name.startsWith(" ") || name.endsWith(" "))
         return constants_.nameStartWithMessage();
      if (name.contains("/"))
         return constants_.nameIllegalCharacterMessage();
      if (forCreation && (name.equals(".") || name.equals("..")))
         return constants_.illegalNameMessage();

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
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
