/*
 * MirrorsServerOperations.java
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
package org.rstudio.studio.client.common.mirrors.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JsArray;

public interface MirrorsServerOperations
{
   void setCRANMirror(CRANMirror mirror,
                      ServerRequestCallback<Void> requestCallback);
   
   void getCRANMirrors(
         ServerRequestCallback<JsArray<CRANMirror>> requestCallback);
}
