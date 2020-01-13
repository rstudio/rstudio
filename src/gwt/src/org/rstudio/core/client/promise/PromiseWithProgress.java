package org.rstudio.core.client.promise;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;

import elemental2.promise.IThenable;
import elemental2.promise.Promise;
import elemental2.promise.IThenable.ThenOnFulfilledCallbackFn;
import elemental2.promise.IThenable.ThenOnRejectedCallbackFn;


public class PromiseWithProgress<V>
{ 
   public PromiseWithProgress(Promise<V> promise, V errorVal, CommandWithArg<V> completed)
   {
      this(promise, "Working...", errorVal, completed);
   }
   
   public PromiseWithProgress(Promise<V> promise, String progress, V errorVal, CommandWithArg<V> completed)
   {
      // setup progress
      GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
      ProgressIndicator indicator = new GlobalProgressDelayer(globalDisplay, 300, progress).getIndicator();
      
      // execute the promise
      promise.then(new ThenOnFulfilledCallbackFn<V,V>() {
         @Override
         public IThenable<V> onInvoke(V v)
         {
            indicator.onCompleted();
            completed.execute(v);
            return null;
           
         }
      },new ThenOnRejectedCallbackFn<V>() {
      
         @Override
         public IThenable<V> onInvoke(Object error)
         {
            indicator.onError(error.toString());
            completed.execute(errorVal);
            return null;
         }
      });
   }
   
}
