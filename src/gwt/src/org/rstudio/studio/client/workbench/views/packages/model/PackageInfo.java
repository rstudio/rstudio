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
      return this["name"];
   }-*/;
   
   public final native String getDesc() /*-{
      return this["desc"] || "";
   }-*/;
   
   public final native String getVersion() /*-{
      return this["version"] || "";
   }-*/;

   public final native String getPackageSource() /*-{
      return this["source"] || "";
   }-*/;
   
   public final native String getLibrary() /*-{
      return this["library"] || "";
   }-*/;
   
   public final native String getLibraryAbsolute() /*-{
      return this["library_absolute"] || "";
   }-*/;
   
   public final native int getLibraryIndex() /*-{
      return this["library_index"] || 0;
   }-*/;

   public final native String getHelpUrl() /*-{
      return "help/library/" + this["name"] + "/html/00Index.html";
   }-*/;

   public final native String getPackageUrl() /*-{
      return this["package_url"] || "";
   }-*/;

   public final native String getBrowseUrl() /*-{
      return this["browse_url"] || "";
   }-*/;

   public final native boolean isAttached() /*-{
      return this["attached"] || false;
   }-*/;

   public final native String getMetadata() /*-{
      return this["metadata"] || "";
   }-*/;

   public final native void setAttached(boolean attached) /*-{
      this["attached"] = attached;
   }-*/;
   
   public final native boolean isFirstInLibrary() /*-{
      return this.first_in_library || false;
   }-*/;
   
   public final native boolean setFirstInLibrary(boolean isFirst) /*-{
      this.first_in_library = isFirst;
   }-*/;
   
   public final native String getPackratStringField(String name) /*-{
      return this[name] || "";
   }-*/;
   
   public final native boolean getPackratBoolField(String name) /*-{
      return this[name] || false;
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
