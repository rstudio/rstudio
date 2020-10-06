/*
 * MarkersServerOperations.java
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
package org.rstudio.studio.client.workbench.views.output.markers.model;

import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

public interface MarkersServerOperations
{
   void updateActiveMarkerSet(String set,
                              ServerRequestCallback<Void> requestCallback);
   
   void clearActiveMarkerSet(ServerRequestCallback<Void> requestCallback);
   
   void markersTabClosed(ServerRequestCallback<Void> requestCallback);
}
