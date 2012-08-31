/*
 * FileSystemItem.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.vcs.StatusAndPathInfo;

import java.util.Date;
import java.util.HashMap;

// NOTE: this class is represented as a native JavaScriptObject for
// straightforward RPC handling

public class FileSystemItem extends JavaScriptObject 
{
   protected FileSystemItem()
   {
   }
   
   public static FileSystemItem createDir(String path)
   {
      return create(path, true, -1, 0);
   }
   
   public static FileSystemItem createFile(String path)
   {
      return create(path, false, -1, 0);
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
   
   public final String mimeType()
   {
      return mimeType("text/plain");
   }
   
   public final String mimeType(String defaultType)
   {
      String ext = getExtension().toLowerCase();
      if (ext.length() > 0)
      {
         String mimeExt = ext.substring(1).toLowerCase();
         String mimeType = MIME_TYPES.get(mimeExt);
         if (mimeType != null)
            return mimeType;
         else
            return defaultType;
      }
      else
      {
         return defaultType;
      }    
   }
   
   public final static boolean areEqual(FileSystemItem a, FileSystemItem b)
   {
      if (a == null && b == null)
         return true;
      else if (a == null && b != null)
         return false;
      else if (a != null && b == null)
         return false;
      else 
         return a.equalTo(b);
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

   public final native StatusAndPathInfo getGitStatus() /*-{
      return this.git_status;
   }-*/;

   public final native StatusAndPathInfo getSVNStatus() /*-{
      return this.svn_status;
   }-*/;

   // NOTE: should be synced with mime type database in FilePath.cpp
   private final static HashMap<String,String> MIME_TYPES = 
                                             new HashMap<String,String>();
   static 
   {
      MIME_TYPES.put( "htm",   "text/html" );
      MIME_TYPES.put( "html",  "text/html" );
      MIME_TYPES.put( "css",   "text/css" );
      MIME_TYPES.put( "gif",   "image/gif" );
      MIME_TYPES.put( "jpg",   "image/jpeg" );
      MIME_TYPES.put( "jpeg",  "image/jpeg" );
      MIME_TYPES.put( "jpe",   "image/jpeg" );
      MIME_TYPES.put( "png",   "image/png" );
      MIME_TYPES.put( "js",    "application/x-javascript" );
      MIME_TYPES.put( "pdf",   "application/pdf" );
      MIME_TYPES.put( "svg",   "image/svg+xml" );
      MIME_TYPES.put( "swf",   "application/x-shockwave-flash" );
      
      // markdown types
      MIME_TYPES.put( "md",       "text/x-markdown" );
      MIME_TYPES.put( "mdtxt",    "text/x-markdown" );
      MIME_TYPES.put( "markdown", "text/x-markdown" );
      
      // programming languages
      MIME_TYPES.put( "f",         "text/x-fortran" );
 
      // other types we are likely to serve
      MIME_TYPES.put( "xml",   "text/xml" );
      MIME_TYPES.put( "csv",   "text/csv" );
      MIME_TYPES.put( "ico",   "image/x-icon" );
      MIME_TYPES.put( "zip",   "application/zip" );
      MIME_TYPES.put( "bz",    "application/x-bzip");
      MIME_TYPES.put( "bz2",   "application/x-bzip2");
      MIME_TYPES.put( "gz",    "application/x-gzip");
      MIME_TYPES.put( "tar",   "application/x-tar");
       
      // yet more types...
      
      MIME_TYPES.put( "shtml", "text/html" );
      MIME_TYPES.put( "tsv",   "text/tab-separated-values" );
      MIME_TYPES.put( "tab",   "text/tab-separated-values" );
      MIME_TYPES.put( "dcf",   "text/debian-control-file" );
      MIME_TYPES.put( "txt",   "text/plain" );
      MIME_TYPES.put( "mml",   "text/mathml" );
      MIME_TYPES.put( "log",   "text/plain");
      MIME_TYPES.put( "out",   "text/plain");
      MIME_TYPES.put( "Rout",  "text/plain");
      MIME_TYPES.put( "po",    "text/plain");
      MIME_TYPES.put( "pot",   "text/plain");
      MIME_TYPES.put( "gitignore",   "text/plain");
      MIME_TYPES.put( "Rbuildignore","text/plain");
     
      MIME_TYPES.put( "tif",   "image/tiff" );
      MIME_TYPES.put( "tiff",  "image/tiff" );
      MIME_TYPES.put( "bmp",   "image/bmp"  );
      MIME_TYPES.put( "ps",    "application/postscript" );
      MIME_TYPES.put( "eps",   "application/postscript" );
      MIME_TYPES.put( "dvi",   "application/x-dvi" );
         
      MIME_TYPES.put( "atom",  "application/atom+xml" );
      MIME_TYPES.put( "rss",   "application/rss+xml" );
      
      MIME_TYPES.put( "doc",   "application/msword" );
      MIME_TYPES.put( "docx",  "application/vnd.openxmlformats-officedocument.wordprocessingml.document" );
      MIME_TYPES.put( "odt",   "application/vnd.oasis.opendocument.text" );
      MIME_TYPES.put( "rtf",   "application/rtf" );
      MIME_TYPES.put( "xls",   "application/vnd.ms-excel" );
      MIME_TYPES.put( "xlsx",  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" );
      MIME_TYPES.put( "ods",   "application/x-vnd.oasis.opendocument.spreadsheet" );
      MIME_TYPES.put( "ppt",   "application/vnd.ms-powerpoint" );
      MIME_TYPES.put( "pps",   "application/vnd.ms-powerpoint" );
      MIME_TYPES.put( "pptx",  "application/vnd.openxmlformats-officedocument.presentationml.presentation" );
    
      MIME_TYPES.put( "sit",   "application/x-stuffit" );
      MIME_TYPES.put( "sxw",   "application/vnd.sun.xml.writer" );
      
      MIME_TYPES.put( "iso",   "application/octet-stream" );
      MIME_TYPES.put( "dmg",   "application/octet-stream" );
      MIME_TYPES.put( "exe",   "application/octet-stream" );
      MIME_TYPES.put( "dll",   "application/octet-stream" );
      MIME_TYPES.put( "deb",   "application/octet-stream" );
      MIME_TYPES.put( "xpi",   "application/x-xpinstall" );
      
      MIME_TYPES.put( "mp2",   "audio/mpeg" );
      MIME_TYPES.put( "mp3",   "audio/mpeg" );
      
      MIME_TYPES.put( "mpg",   "video/mpeg" );
      MIME_TYPES.put( "mpeg",  "video/mpeg" );
      MIME_TYPES.put( "flv",   "video/x-flv" );
   }

   private static final Pattern EXT_PATTERN = Pattern.create("\\.[^.]+$");
   private static final FileIconResources RES = FileIconResources.INSTANCE;
   
   public static final String HOME_PATH = "~";
   private static final String HOME_PREFIX = HOME_PATH + "/";
   private static final String PUBLIC = "Public";
}
