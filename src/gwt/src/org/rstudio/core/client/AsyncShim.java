/*
 * AsyncShim.java
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
package org.rstudio.core.client;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * AsyncShim makes it easy to sequester a class behind GWT.runAsync()
 * and cause it to be lazy-loaded, while still making it convenient to
 * call (some types of) methods on it from other parts of the code that
 * are not in the same fragment.
 *
 * For each type that needs to be sequestered, create an *abstract*
 * AsyncShim subclass with the TTarget type parameter set to the
 * sequestered type. (The AsyncShim subclass must be at package-level
 * or public visibility.)
 *
 * In your AsyncShim subclass, you'll want to override onDelayLoadSuccess
 * and onDelayLoadFailure to do whatever your app needs. onDelayLoadSuccess
 * will be called only once, when the instance of TTarget is first created.
 *
 * You can also add any other fields, methods, etc. to your subclass that
 * are necessary to give onDelayLoadSuccess and/or onDelayLoadFailure any
 * contextual state they need.
 *
 * As mentioned, AsyncShim can make some kinds of methods easy to call from
 * other fragments. Specifically, any method that returns void and
 * semantically makes sense to run asynchronously, can be easily passed
 * through AsyncShim. Just stub out an abstract version of the method on
 * your AsyncShim subclass, and deferred binding will take care of wiring
 * everything up. Or, you can have your AsyncShim subclass "implement" an
 * interface but not actually provide implementations--if these methods
 * return void, they will also be automatically wired up.
 * 
 * @param <TTarget> The type to be sequestered
 */
public abstract class AsyncShim<TTarget>
{
   /**
    * [DON'T override this, it will be overridden by the code generator]
    *
    * This method must be called before the target object is needed. 
    */
   @Inject
   public void initialize(Provider<TTarget> provider)
   {
   }

   public final void forceLoad(boolean downloadCodeOnly)
   {
      forceLoad(downloadCodeOnly, null);
   }

   /**
    * [DON'T override this, it will be overridden by the code generator]
    *
    * Call this to force the code to be downloaded, and optionally, for
    * the provider to be invoked
    *
    * @param downloadCodeOnly If true, the code will be downloaded but
    *    the provider will not be invoked
    * @param continuation A command to invoke after the load is complete
    *    (regardless of whether the effort was successful or not). Can be
    *    null.
    */
   public void forceLoad(boolean downloadCodeOnly, Command continuation)
   {
   }

   /**
    * You can override this to do something asynchronous between when the
    * code loads and when the instance is created.
    * @param continuation You MUST call this (eventually) to allow execution
    *    to proceed
    */
   protected void preInstantiationHook(Command continuation)
   {
      continuation.execute();
   }

   /**
    * You can override this to do something with the delayed type once the
    * code loads and the instance is created.
    * @param obj
    */
   protected void onDelayLoadSuccess(TTarget obj)
   {
   }

   /**
    * You can (should!) override this to deal with failure cases.
    * @param reason
    */
   protected void onDelayLoadFailure(Throwable reason)
   {
   }
}
