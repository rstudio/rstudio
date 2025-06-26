/*
 * PackagesServerOperations.java
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

import java.util.List;

import org.rstudio.studio.client.packrat.model.PackratServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public interface PackagesServerOperations extends PackratServerOperations
{
   void getPackageState(
         boolean manual,
         ServerRequestCallback<PackageState> requestCallback);
   
   void availablePackages(
         String repository,
         ServerRequestCallback<JsArrayString> requestCallback);
   
   void isPackageAttached(String packageName,
                          String libName,
                          ServerRequestCallback<Boolean> requestCallback);
   
   void isPackageHyperlinkSafe(String packageName,
                               ServerRequestCallback<Boolean> requestCallback);
   
   void isPackageInstalled(String packageName,
                           String version,
                           ServerRequestCallback<Boolean> requestCallback);
   
   void checkForPackageUpdates(
            ServerRequestCallback<JsArray<PackageUpdate>> requestCallback);

   void getPackageInstallContext(
         ServerRequestCallback<PackageInstallContext> requestCallback);
   
   void initDefaultUserLibrary(ServerRequestCallback<Void> requestCallback);
   
   void loadedPackageUpdatesRequired(
                            List<String> packages,
                            ServerRequestCallback<Boolean> requestCallback);
   
   void ignoreNextLoadedPackageCheck(
                        ServerRequestCallback<Void> requestCallback);
   
   void getPackageNewsUrl(
                        String packageName,
                        String libraryPath,
                        ServerRequestCallback<String> requestCallback);
   
   void getPackageCitations(
      String packageName,
      ServerRequestCallback<JavaScriptObject> requestCallback);

   void getRepositories(
      ServerRequestCallback<JsArray<PackageManagerRepository>> requestCallback);
}
