/*
 * PackageStatus.java
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
package org.rstudio.studio.client.workbench.views.packages.model;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;



public class PackageStatus extends JavaScriptObject
{
   protected PackageStatus()
   {
   }
   
   public static final native PackageStatus create(String name,
                                                   String lib,
                                                   boolean loaded) /*-{
      var status = new Object();
      status.name = name;
      status.lib = lib;
      status.loaded = loaded;
      return status;
   }-*/;
   
   public final native String getName() /*-{
      return this.name[0];
   }-*/;

   public final String getLib() 
   {
      String lib = getLibNative();
      if (lib != null)
      {
         return lib;
      }
      else
      {
         FileSystemItem path = FileSystemItem.createDir(getPathNative());
         return path.getParentPathString();
      }
   }   
   
   public final native boolean isLoaded() /*-{
      return this.loaded[0];
   }-*/;
   
   private final native String getPathNative() /*-{
      return this.path[0];
   }-*/;
   
   private final native String getLibNative() /*-{
      return this.lib;
   }-*/;
}
