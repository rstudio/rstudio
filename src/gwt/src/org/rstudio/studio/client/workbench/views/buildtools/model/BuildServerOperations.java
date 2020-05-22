/*
 * BuildServerOperations.java
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

package org.rstudio.studio.client.workbench.views.buildtools.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.model.CppCapabilities;

public interface BuildServerOperations
{
   // check if we can build C/C++ code
   void getCppCapabilities(
                     ServerRequestCallback<CppCapabilities> requestCallback);
   
   // prompted install of build tools
   void installBuildTools(
                     String action, 
                     ServerRequestCallback<Boolean> callback);
   
   // returns true to indicate that the build has started, returns false
   // to indicate that the build could not be started because another
   // build is currently in progress
   void startBuild(String type,
                   String subType,
                   ServerRequestCallback<Boolean> requestCallback);
   
   // terminate any running build
   void terminateBuild(ServerRequestCallback<Boolean> requestCallback);
   
   
   // get the devtools::load_all path
   void devtoolsLoadAllPath(ServerRequestCallback<String> requestCallback);
   
   // get bookdown output formats
   void getBookdownFormats(ServerRequestCallback<BookdownFormats> requestCallback);
}
