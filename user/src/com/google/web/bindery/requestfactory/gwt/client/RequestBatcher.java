/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.web.bindery.requestfactory.gwt.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestContext;

/**
 * A RequestBatcher is a convenience class that allows RequestFactory operations
 * to be aggregated over a single tick of the event loop and sent as one HTTP
 * request. Instances of RequestBatcher are reusable, so they may be used as
 * application-wide singleton objects or within a particular subsystem or UI.
 * <p>
 * Subclasses need only to provide the instance of the RequestFactory used by
 * the application and a method to provide a "default" RequestContext from the
 * RequestFactory.
 * 
 * <pre>
 * public class MyRequestBatcher extends RequestBatcher<MyRequestFactory, MyRequestContext> {
 *   public MyRequestBatcher() {
 *     // MyRequestFactory could also be injected
 *     super(GWT.create(MyRequestFactory.class));
 *   }
 *   
 *   protected MyRequestContext createContext(MyRequestFactory factory) {
 *     return factory.myRequestContext();
 *   }
 * }
 * </pre>
 * A singleton or otherwise scoped instance of RequestBatcher should be injected
 * into consuming classes. The {@link RequestContext#fire()} and
 * {@link com.google.web.bindery.requestfactory.shared.Request#fire()
 * Request.fire()} methods reachable from the RequestContext returned from
 * {@link #get()} will not trigger an HTTP request. The
 * {@link RequestContext#fire(Receiver)} and
 * {@link com.google.web.bindery.requestfactory.shared.Request#fire(Receiver)
 * Request.fire(Receiver)} methods will register their associated Receivers as
 * usual. This allows consuming code to be written that can be used with or
 * without a RequestBatcher.
 * <p>
 * When an application uses multiple RequestContext types, the
 * {@link RequestContext#append(RequestContext)} method can be used to chain
 * multiple RequestContext objects together:
 * 
 * <pre>
 * class MyRequestBatcher {
 *   public OtherRequestContext otherContext() {
 *     return get().append(getFactory().otherContext());
 *   }
 * }
 * </pre>
 * 
 * @param <F> the type of RequestFactory
 * @param <C> any RequestContext type
 * @see Scheduler#scheduleFinally(ScheduledCommand)
 */
public abstract class RequestBatcher<F extends RequestFactory, C extends RequestContext> {
  private C openContext;
  private AbstractRequestContext openContextImpl;
  private final F requestFactory;

  protected RequestBatcher(F requestFactory) {
    this.requestFactory = requestFactory;
  }

  /**
   * Returns a mutable {@link RequestContext}.
   */
  public C get() {
    return get(null);
  }

  /**
   * Returns a mutable {@link RequestContext} and enqueues the given receiver to
   * be called as though it had been passed directly to
   * {@link RequestContext#fire(Receiver)}.
   */
  public C get(Receiver<Void> receiver) {
    if (openContext == null) {
      openContext = createContext(requestFactory);
      openContextImpl = (AbstractRequestContext) openContext;
      openContextImpl.setFireDisabled(true);
      getScheduler().scheduleFinally(new ScheduledCommand() {
        @Override
        public void execute() {
          assert !openContextImpl.isLocked() : "AbstractRequestContext.fire() should have been a no-op";
          openContextImpl.setFireDisabled(false);
          openContext.fire();
          openContext = null;
          openContextImpl = null;
        }
      });
    }
    if (receiver != null) {
      // Queue a final callback receiver
      openContextImpl.fire(receiver);
    }
    return openContext;
  }

  /**
   * Convenience access to the RequestFactory instance to aid developers using
   * multiple RequestContext types.
   * 
   * <pre>
   * RequestBatcher{@literal <MyRequestFactory, MyRequestContext>} batcher;
   * 
   * public void useOtherRequestContext() {
   *   OtherRequestContext ctx = batcher.get().append(batcher.getFactory().otherContext());
   *   ctx.someOtherMethod().to(someReceiver);
   * }
   * </pre>
   */
  public F getRequestFactory() {
    return requestFactory;
  }

  /**
   * Subclasses must implement this method to return an instance of a
   * RequestContext.
   */
  protected abstract C createContext(F requestFactory);

  /**
   * Returns {@link Scheduler#get()}, but may be overridden for testing
   * purposes.
   */
  protected Scheduler getScheduler() {
    return Scheduler.get();
  }
}
