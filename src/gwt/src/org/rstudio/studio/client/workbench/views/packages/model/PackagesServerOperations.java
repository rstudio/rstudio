/*
 * PackagesServerOperations.java
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
package org.rstudio.studio.client.workbench.views.packages.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface PackagesServerOperations
{
   // list installed packages
   void listPackages(
         ServerRequestCallback<JsArray<PackageInfo>> requestCallback);
   
   void availablePackages(
         String repository,
         ServerRequestCallback<JsArrayString> requestCallback);
   
   // load a package
   void loadPackage(String packageName,
                    ServerRequestCallback<Void> requestCallback);
   
   // unload a package
   void unloadPackage(String packageName,
                      ServerRequestCallback<Void> requestCallback);

   void isCRANConfigured(ServerRequestCallback<Boolean> requestCallback);

   void setCRANReposUrl(String reposUrl,
                        ServerRequestCallback<Void> requestCallback);
}
