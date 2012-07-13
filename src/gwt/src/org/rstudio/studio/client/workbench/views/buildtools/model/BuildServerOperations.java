/*
 * BuildServerOperations.java
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

package org.rstudio.studio.client.workbench.views.buildtools.model;

import org.rstudio.studio.client.server.ServerRequestCallback;

public interface BuildServerOperations
{
   // returns true to indicate that the build has started, returns false
   // to indicate that the build could not be started because another
   // build is currently in progress
   void startBuild(String type,
                   ServerRequestCallback<Boolean> requestCallback);
   
   // terminate any running build
   void terminateBuild(ServerRequestCallback<Boolean> requestCallback);
   
   
   // validate the build configuration
   void validateBuildTargetPath(String buildType,
                                String targetPath, 
                                ServerRequestCallback<String> requestCallback);
   
   // get the devtools::load_all path
   void devtoolsLoadAllPath(ServerRequestCallback<String> requestCallback);
}
