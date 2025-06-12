/*
 * PackageInfo.java
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
package org.rstudio.studio.client.workbench.views.packages.model;


import com.google.gwt.core.client.JavaScriptObject;

public class PackageInfo extends JavaScriptObject 
{
   protected PackageInfo()
   {
   }
   
   public final native String getName() /*-{
      return this["Package"];
   }-*/;
   
   public final native String getDesc() /*-{
      return this["Title"] || "";
   }-*/;
   
   public final native String getVersion() /*-{
      return this["Version"] || "";
   }-*/;

   public final native String getPackageSource() /*-{
      return this["Source"] || "";
   }-*/;
   
   public final native String getLibrary() /*-{
      return this["Library"] || "";
   }-*/;
   
   public final native String getLibraryAbsolute() /*-{
      return this["LibraryAbsolute"] || "";
   }-*/;
   
   public final native int getLibraryIndex() /*-{
      return this["LibraryIndex"] || 0;
   }-*/;

   public final native String getHelpUrl() /*-{
      return "help/library/" + this["Package"] + "/html/00Index.html";
   }-*/;

   public final native String getPackageUrl() /*-{
      return this["PackageUrl"] || "";
   }-*/;

   public final native String getBrowseUrl() /*-{
      return this["BrowseUrl"] || "";
   }-*/;

   public final native boolean isAttached() /*-{
      return this["Attached"] || false;
   }-*/;

   public final native void setAttached(boolean attached) /*-{
      this["Attached"] = attached;
   }-*/;
   
   public final native boolean isFirstInLibrary() /*-{
      return this.first_in_library || false;
   }-*/;
   
   public final native boolean setFirstInLibrary(boolean isFirst) /*-{
      this.first_in_library = isFirst;
   }-*/;
   
   public final native String getPackratStringField(String name) /*-{
      if (typeof this[name] === "undefined" || this[name] === null)
         return "";
      else
         return this[name];
   }-*/;
   
   public final native boolean getPackratBoolField(String name) /*-{
      if (typeof this[name] === "undefined" || this[name] === null)
         return false;
      else
         return this[name];
   }-*/;
   
   public final String getPackratVersion() 
   {
      return getPackratStringField("packrat.version");
   }
   
   public final String getPackratSource() 
   {
      return getPackratStringField("packrat.source");
   }
   
   public final boolean getCurrentlyUsed() 
   {
      return getPackratBoolField("currently.used");
   }

   public final boolean isInProjectLibrary()
   {
      return getPackratBoolField("in.project.library");
   }
   
   public final String getSourceLibrary()
   {
      String sourceLibrary = getPackratStringField("source.library");
      return sourceLibrary.length() == 0 ? getLibrary() : sourceLibrary;
   }
}
