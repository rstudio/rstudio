/*
 * Copyright 2010 Google Inc.
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
package com.google.web.bindery.autobean.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A tag interface for the AutoBean generator. Instances of AutoBeans are
 * created by declaring factory methods on a subtype of this interface.
 * <p>
 * Simple interfaces, consisting of only getters and setters, can be constructed
 * with a no-arg method. Non-simple interfaces must provide a delegate object to
 * implement a non-simple interface or use a {@link Category}.
 * 
 * <pre>
 * interface MyFactory extends AutoBeanFactory {
 *   // A factory method for a simple bean
 *   AutoBean&lt;BeanInterface> beanInterface();
 *   // A factory method for a wrapper bean
 *   AutoBean&lt;ArbitraryInterface> wrapper(ArbitraryInterface delegate);
 * }
 * </pre>
 * 
 * @see <a
 *      href="http://code.google.com/p/google-web-toolkit/wiki/AutoBean">AutoBean
 *      wiki page</a>
 */
public interface AutoBeanFactory {
  /**
   * Allows non-property methods on simple bean implementations when applied.
   * For any given method, the specified classes will be searched for a public,
   * static method whose method signature is exactly equal to the declared
   * method's signature, save for the addition of a new initial paramater that
   * must accept <code>AutoBean&lt;T></code>.
   * 
   * <pre>
   * interface HasMethod {
   *   void doSomething(int a, double b);
   * }
   * </pre>
   * 
   * would be paired with a category implemenation such as
   * 
   * <pre>
   * class HasMethodCategory {
   *   public static void doSomething(AutoBean&lt;HasMethod> bean, int a, double b) {
   *   }
   * }
   * </pre>
   * 
   * and registered with
   * 
   * <pre>
   * {@literal @}Category(HasMethodCategory.class)
   * interface MyBeanFactory extends AutoBeanFactory {
   *   AutoBean&lt;HasMethod> hasMethod();
   * }
   * </pre>
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface Category {
    Class<?>[] value();
  }

  /**
   * The types specified by this annotation will not be wrapped by an AutoBean
   * when returned from an AutoBean-controlled method.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface NoWrap {
    /**
     * The interface types that should not be wrapped.
     */
    Class<?>[] value();
  }

  /**
   * Allows dynamic creation of AutoBean instances based on declared
   * parameterizations.
   * 
   * @param <T> the parameterization of the created {@link AutoBean}
   * @param clazz the Class of type T of the new instance
   * @return an {@link AutoBean} of type T or {@code null} if the interface type
   *         is unknown to the factory
   */
  <T> AutoBean<T> create(Class<T> clazz);

  /**
   * Allows dynamic creation of wrapped AutoBean instances based on declared
   * parameterizations.
   * 
   * @param <T> the parameterization of the created {@link AutoBean}
   * @param <U> the delegate's type, a subtype of T
   * @param clazz the Class of type T of the new instance
   * @param delegate a delegate that extends type T
   * @return an {@link AutoBean} of type T or {@code null} if the interface type
   *         is unknown to the factory
   */
  <T, U extends T> AutoBean<T> create(Class<T> clazz, U delegate);
}
