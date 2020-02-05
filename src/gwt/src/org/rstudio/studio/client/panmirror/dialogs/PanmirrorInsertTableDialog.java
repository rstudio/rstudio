/*
 * PanmirrorInsertTableDialog.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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


package org.rstudio.studio.client.panmirror.dialogs;

import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;
import jsinterop.annotations.JsType;

public class PanmirrorInsertTableDialog
{
   @JsType
   public static class Result 
   {
      public int rows;
      public int cols;
      public boolean header;
      public String caption;
   }
   
   public static Promise<Result> show() {
      return new Promise<Result>((ResolveCallbackFn<Result> resolve, RejectCallbackFn reject) -> {
         Result result = new Result();
         result.rows = 3;
         result.cols = 3;
         result.header = true;
         resolve.onInvoke(result);
      });
   }

}
