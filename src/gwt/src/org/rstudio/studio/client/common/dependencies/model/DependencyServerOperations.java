/*
 * DependencyServerOperations.java
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

import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArray;

public interface DependencyServerOperations extends CryptoServerOperations
{
   void unsatisfiedDependencies(
       JsArray<Dependency> dependencies,
       boolean silentUpdate,
       ServerRequestCallback<JsArray<Dependency>> requestCallback);
   
   void installDependencies(
       String context,
       JsArray<Dependency> dependencies,
       ServerRequestCallback<String> requestCallback);
}

