/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client;

import com.google.gwt.core.client.GWT;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The AsyncProxy type is used to provide a reachability barrier between classes
 * intended to be used with runAsync while maintaining a simple,
 * deferred-synchronous API. The first invocation of an instance method on the
 * AsyncProxy will trigger the instantiation of a concrete object via runAsync.
 * All method invocations on the proxy will be recorded and played back on the
 * newly-instantiated object after the call to runAsync returns.
 * <p>
 * Once method playback has finished, the proxy will continue to forward
 * invocations onto the instantiated object.
 * 
 * <p>
 * Example use:
 * 
 * <pre>
 * interface IFoo {
 *   void doSomething(int a, int b);
 *   void anotherMethad(Object o);
 * }
 * class FooImpl implements IFoo { .... }
 * 
 * {@literal @}ConcreteType(FooImpl.class)
 * interface FooProxy extends AsyncProxy&lt;IFoo&gt;, IFoo {}
 * 
 * class UserOfIFoo {
 *   private IFoo foo = GWT.create(FooProxy.class);
 *   
 *   void makeTrouble() {
 *     // This first call triggers a runAsync load
 *     foo.doSomething(1, 2);
 *     
 *     // and this second will replayed after the call to doSomething()
 *     foo.anotherMethod("A string");
 *   }
 * }
 * </pre>
 * 
 * For cases where dispatch speed is critical, a ProxyCallback can be installed
 * in order to reassign the field containing the AsyncProxy instance with the
 * backing object:
 * 
 * <pre>
 * class UserOfIFoo {
 *   private IFoo fooOrProxy = GWT.create(FooProxy.class);
 *
 *   public UserOfIFoo() {
 *     // When the load, initialization, and playback are done, get rid of the proxy.
 *     ((AsyncProxy&lt;IFoo&gt;) fooOrProxy).setProxyCallback(new ProxyCallback&lt;IFoo&gt;() {
 *       public void onComplete(IFoo instance) {
 *         fooOrProxy = instance;
 *       }
 *     });
 *   }
 *   
 *   void makeTrouble() {
 *     // This first call triggers a runAsync load
 *     fooOrProxy.doSomething(1, 2);
 *     
 *     // and this second will also be replayed before the above onComplete is called
 *     fooOrProxy.anotherMethod("A string");
 *   }
 * }
 * </pre>
 * 
 * @param <T> the type of interface that must be implemented by the derivative
 *          class.
 */
@AsyncProxy.DefaultValue()
public interface AsyncProxy<T> {
  /*
   * NB: This type is annotated with the DefaultValue annotation so that we can
   * always fall back on getting the default values from an instance of the
   * annotation, as opposed to maintaining the default in two places.
   */

  /**
   * If this annotation is applied to an AsyncProxy type, it will be legal for
   * the parameterized type <code>T</code> to declare non-void methods. These
   * methods will immediately return a default value of 0, false, or null if the
   * proxy has not yet instantiated the backing object. The use of this
   * annotation may cause surprising operation if the consuming code does not
   * expect this behavior; for example a call to a property setter followed by a
   * call to the getter could return null,
   * 
   * @see DefaultValue
   */
  @Documented
  @Target(ElementType.TYPE)
  public @interface AllowNonVoid {
  }

  /**
   * This interface should be applied to the AsyncProxy subtype in order to
   * specify the Class literal that will be passed to {@link GWT#create(Class)}.
   */
  @Documented
  @Target(ElementType.TYPE)
  public @interface ConcreteType {
    Class<?> value();
  }

  /**
   * This annotation specifies the return value for primitive methods when the
   * {@link AllowNonVoid} annotation has been applied to an AsyncProxy.
   * 
   * The annotation may be applied to the definition of the AsyncProxy type or
   * individual methods defined on the target interface. If the annotation is
   * applied to the AsyncProxy type, then it will apply to all methods
   * implemented by the proxy.
   * 
   * The correct default value will be chosen from the value methods defined in
   * this type based on the return type of the method.
   */
  @Documented
  @Target(value = {ElementType.METHOD, ElementType.TYPE})
  public @interface DefaultValue {
    /*
     * Consider adding an additional flag value that would make the generated
     * type fail an assertion on uninitialized access. Also consider whether or
     * not allowing a class literal to be specified for reference types would be
     * useful.
     */

    boolean booleanValue() default false;

    byte byteValue() default 0;

    char charValue() default 0;

    double doubleValue() default 0;

    float floatValue() default 0;

    int intValue() default 0;

    long longValue() default 0;

    short shortValue() default 0;
  }

  /**
   * The callback used by {@link AsyncProxy#setProxyCallback(ProxyCallback)}.
   * 
   * @param <T> the interface parameterization of AsyncProxy.
   */
  public abstract static class ProxyCallback<T> {
    /**
     * This method will be invoked by the AsyncProxy after method playback is
     * complete.
     * 
     * @param instance the instance
     */
    public void onComplete(T instance) {
    }

    /**
     * Invokes the global uncaught exception handler.
     */
    public void onFailure(Throwable t) {
      if (GWT.getUncaughtExceptionHandler() != null) {
        GWT.getUncaughtExceptionHandler().onUncaughtException(t);
      }
    }

    /**
     * This method will be called with the instance object before method replay
     * starts. This provides the developer with the opportunity to perform
     * secondary initialization of the backing object.
     * 
     * @param instance the instance
     */
    public void onInit(T instance) {
    }
  }

  /**
   * Returns the underlying proxied object if it has been instantiated or
   * <code>null</code>.
   */
  T getProxiedInstance();

  /**
   * Sets a callback that can be used to influence the initialization process.
   */
  void setProxyCallback(ProxyCallback<T> callback);
}
