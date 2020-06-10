/*
 * PackageInstallOptions.java
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

import com.google.gwt.core.client.JavaScriptObject;

public class PackageInstallOptions extends JavaScriptObject
{
   protected PackageInstallOptions()
   {
      
   }
   
   public static final native PackageInstallOptions create(
                                          boolean installFromRepository,
                                          String libraryPath, 
                                          boolean installDependencies) /*-{
      var options = new Object();
      options.installFromRepository = installFromRepository;
      options.libraryPath = libraryPath;
      options.installDependencies = installDependencies;
      return options;
   }-*/;


   public final native boolean getInstallFromRepository() /*-{
      if (typeof this.installFromRepository  != 'undefined')
         return this.installFromRepository;
      else
         return true;
   }-*/;
   
   public final native String getLibraryPath() /*-{
      return this.libraryPath;
   }-*/;
   
   public final native boolean getInstallDependencies() /*-{
      return this.installDependencies;
   }-*/;
   
   public static native boolean areEqual(PackageInstallOptions a, 
                                         PackageInstallOptions b) /*-{
      if (a === null ^ b === null)
         return false;
      if (a === null)
         return true;
      return a.libraryPath === b.libraryPath &&
             a.installDependencies === b.installDependencies &&
             a.installFromRepository === b.installFromRepository;     
   }-*/;

   
}
