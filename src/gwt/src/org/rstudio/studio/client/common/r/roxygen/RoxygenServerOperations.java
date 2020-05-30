/*
 * RoxygenServerOperations.java
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
package org.rstudio.studio.client.common.r.roxygen;

import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper.SetClassCall;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper.SetGenericCall;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper.SetMethodCall;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper.SetRefClassCall;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface RoxygenServerOperations
{
   void getSetClassCall(
         String call,
         ServerRequestCallback<SetClassCall> requestCallback);
   
   void getSetGenericCall(
         String call,
         ServerRequestCallback<SetGenericCall> requestCallback);
   
   void getSetMethodCall(
         String call,
         ServerRequestCallback<SetMethodCall> requestCallback);
   
   void getSetRefClassCall(
         String call,
         ServerRequestCallback<SetRefClassCall> requestCallback);

}
