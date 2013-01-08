/*
 * SimpleRequestCallback.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.common;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public class SimpleRequestCallback<T> extends ServerRequestCallback<T>
{
   public SimpleRequestCallback()
   {
      this("Error");
   }

   public SimpleRequestCallback(String caption)
   {
      caption_ = caption;
   }

   @Override
   public void onError(ServerError error)
   {
      Debug.logError(error);
      RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
            caption_,
            error.getUserMessage());
   }

   private String caption_;
}
