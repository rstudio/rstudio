/*
 * PackratServerOperations.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.packrat.model;

import org.rstudio.studio.client.common.packrat.model.PackratContext;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArray;

public interface PackratServerOperations
{
   void getPackratContext(ServerRequestCallback<PackratContext> requestCallback);
   
   void getPackratStatus(String dir,
            ServerRequestCallback<JsArray<PackratStatus>> requestCallback);
   
   void packratBootstrap(String dir,
                         ServerRequestCallback<PackratContext> requestCallback);

   void listPackagesPackrat(String dir,
                            ServerRequestCallback<JsArray<PackratPackageInfo>> requestCallback);
}
