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
