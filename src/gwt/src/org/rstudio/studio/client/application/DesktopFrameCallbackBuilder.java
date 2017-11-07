/*
 * DesktopFrameCallbackBuilder.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.application;

public abstract class DesktopFrameCallbackBuilder<T>
{
   public DesktopFrameCallbackBuilder()
   {
   }
   
   public abstract void execute(T result);
   
   public final native DesktopFrameCallback<T> create()
   /*-{
      var self = this;
      return $entry(function(result) {
         self.@org.rstudio.studio.client.application.DesktopFrameCallbackBuilder::forward(Ljava/lang/Object;)(result);
      });
   }-*/;
   
   @SuppressWarnings("unchecked")
   private void forward(Object result)
   {
      execute((T) result);
   }
   
}
