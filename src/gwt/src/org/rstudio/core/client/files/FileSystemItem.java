/*
 * FileSystemItem.java
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
package org.rstudio.core.client.files;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.filetypes.FileIconResources;

import java.util.Date;

// NOTE: this class is represented as a native JavaScriptObject for
// straightforward RPC handling

public class FileSystemItem extends JavaScriptObject 
{
   protected FileSystemItem()
   {
   }
   
   public static FileSystemItem createDir(String path)
   {
      return create(path, true, 0, 0);
   }
   
   public static FileSystemItem createFile(String path)
   {
      return create(path, false, 0, 0);
   }
      
   private static final native FileSystemItem create(String path, 
                                                     boolean dir,
                                                     double length,
                                                     double lastModified) /*-{
      // Boost "complete" function crashes rsession if it
      // sees e.g. "C:" as opposed to "C:/"
      if (path.match(/^[a-z]:$/im))
         path = path + "/";

      var fileEntry = new Object();
      fileEntry.path = path ;
      fileEntry.dir = dir ;
      fileEntry.length = length;
      fileEntry.lastModified = lastModified;
      return fileEntry ;
   }-*/;
      
   public final native String getPath() /*-{
      return this.path;
   }-*/;

   private final native String getRawPath() /*-{
      return this.raw_path || this.path;
   }-*/;

   public final String getName()
   {
      return getNameFromPath(getRawPath());
   }
   
   public final String getExtension()
   {
      return getExtensionFromPath(getName());
   }
   
   public final String getStem()
   {
      String name = getName();
      int extensionLength = getExtension().length();
      return name.substring(0, name.length() - extensionLength);
   }

   public static String getExtensionFromPath(String path)
   {
      String filename = getNameFromPath(path);
      int lastDotIndex = filename.lastIndexOf('.');
      if (lastDotIndex != -1)
         return filename.substring(lastDotIndex);
      else
         return "";
   }

   public final native boolean isDirectory() /*-{
      return this.dir;
   }-*/;

   public final long getLength()
   { 
      return new Double(getLengthNative()).longValue();
   }

   public final Date getLastModified() 
   { 
      Double lastModified = new Double(getLastModifiedNative());
      return new Date(lastModified.longValue());
   }
   
   public final FileSystemItem getParentPath()
   {
      String parentPath ;
      String path = getPath();
      int lastSlash = path.lastIndexOf('/');
      if (lastSlash <= 0)
      {
         return null;
      }
      else
      {
         parentPath = path.substring(0, lastSlash);
         return FileSystemItem.createDir(parentPath);
      }
   }
   
   public final String getParentPathString()
   {
      FileSystemItem parentPath = getParentPath();
      if (parentPath != null)
         return getParentPath().getPath();
      else
         return "";
   }
 
   public final String completePath(String name)
   {
      String path = getPath();
      if (path.length() == 0)
         return name ;
      else
         return path + "/" + name;
   } 
   
   public final boolean isWithinHome()
   {
      String path = getPath();
      return path.startsWith(HOME_PREFIX) && 
             path.length() > HOME_PREFIX.length();
   }
   
   public final String homeRelativePath()
   {
      if (isWithinHome())
      {
         return getPath().substring(HOME_PREFIX.length());
      }
      else
      {
         return null;
      }
   }
   
   public final boolean isPublicFolder()
   {
      return isDirectory() && PUBLIC.equals(homeRelativePath());
   }

   // RStudio-specific code should use FileTypeRegistry.getIconForFile() instead
   public final ImageResource getIcon()
   {
      if (isDirectory())
      {
         if (isPublicFolder())
            return RES.iconPublicFolder();
         else
            return RES.iconFolder();
      }

      Match m = EXT_PATTERN.match(getName(), 0);
      if (m == null)
         return RES.iconText();

      String lowerExt = m.getValue().toLowerCase();
      if (lowerExt.equals(".csv"))
      {
         return RES.iconCsv();
      }
      else if (lowerExt.equals(".pdf"))
      {
         return RES.iconPdf();
      }
      else if (lowerExt.equals(".jpg") || lowerExt.equals(".jpeg") || 
               lowerExt.equals(".gif") || lowerExt.equals(".bmp")  ||
               lowerExt.equals(".tiff")   || lowerExt.equals(".tif") ||
               lowerExt.equals(".png"))
      {
         return RES.iconPng();
      }
      else
      {
         return RES.iconText();
      }
   }
   
   public final boolean equalTo(FileSystemItem other)
   {
      if (other==null)
         return false;
      
      return compareTo(other) == 0;
   }

   public final int compareTo(FileSystemItem other)
   {
      // If we ever need to compare files that don't share the same
      // parent, then maybe we would need to compare parent directory
      // before anything else.

      if (isDirectory() ^ other.isDirectory())
         return isDirectory() ? -1 : 1;
      return String.CASE_INSENSITIVE_ORDER.compare(getPath(),
                                                   other.getPath());

   }

   public final static FileSystemItem home()
   {
      return createDir(HOME_PATH);
   }
   
   public final static String getNameFromPath(String path)
   {
      if (path.equals(""))
         return "";
      
      if (path.equals("/"))
         return "/";

      while (path.endsWith("/"))
         path = path.substring(0, path.length() - 1);

      return path.substring(Math.max(0, path.lastIndexOf('/') + 1));
   }
   
   private final native double getLengthNative() /*-{
      return this.length;
   }-*/;

   private final native double getLastModifiedNative() /*-{
      return this.lastModified;
   }-*/;

   private static final Pattern EXT_PATTERN = Pattern.create("\\.[^.]+$");
   private static final FileIconResources RES = FileIconResources.INSTANCE;
   
   public static final String HOME_PATH = "~";
   private static final String HOME_PREFIX = HOME_PATH + "/";
   private static final String PUBLIC = "Public";
}
