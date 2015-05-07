/*
 * CompilePdfResult.java
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

package org.rstudio.studio.client.common.dependencies.model;


import com.google.gwt.core.client.JavaScriptObject;

public class Dependency extends JavaScriptObject
{
   public static final int CRAN_PACKAGE = 0;
   public static final int EMBEDDED_PACKAGE = 1;
   
   protected Dependency()
   {
   }
   
   public static Dependency cranPackage(String name, 
                                        String version)
   {
      return cranPackage(name, version, false);
   }
            
            
   
   public native static final Dependency cranPackage(String name, 
                                                     String version,
                                                     boolean source) /*-{
      var dep = new Object();
      dep.type = 0;
      dep.name = name;
      dep.version = version;
      dep.source = source;
      return dep;
   }-*/;
   
   public native static final Dependency embeddedPackage(String name) /*-{
      var dep = new Object();
      dep.type = 1;
      dep.name = name;
      dep.version = "";
      dep.source = true;
      return dep;
   }-*/;
   
   public final native int getType() /*-{
      return this.type;
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
}
