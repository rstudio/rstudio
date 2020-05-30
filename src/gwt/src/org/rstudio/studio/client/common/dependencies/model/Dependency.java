/*
 * Dependency.java
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

package org.rstudio.studio.client.common.dependencies.model;


import com.google.gwt.core.client.JavaScriptObject;

public class Dependency extends JavaScriptObject
{
   protected Dependency()
   {
   }

   public static Dependency cranPackage(String name)
   {
      // use version '0.0.0' just to indicate that any currently installed
      // version of the requested package should suffice
      return cranPackage(name, "0.0.0");
   }

   public final native static Dependency cranPackage(String name, String version) /*-{
      return {
         "name": name,
         "version": version,
         "location": "cran",
         "source": false
      };
   }-*/;
   
   public final native String getLocation() /*-{
      return this.location;
   }-*/;
   
   public final native String setName(String name) /*-{
      this.name = name;
   }-*/;
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getVersion() /*-{
      return this.version;
   }-*/;
   
   public final native boolean getSource() /*-{
      return this.source;
   }-*/;

   public final native String getAvailableVersion() /*-{
      return this.available_version;
   }-*/;

   public final native boolean getVersionSatisfied() /*-{
      return this.version_satisfied;
   }-*/;
   
   public final boolean isEqualTo(Dependency other)
   {
      return (getLocation()  == other.getLocation() &&
              getName()      == other.getName() &&
              getVersion()   == other.getVersion() &&
              getSource()    == other.getSource());
   }
}
