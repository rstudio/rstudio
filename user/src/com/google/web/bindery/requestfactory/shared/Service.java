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
package com.google.web.bindery.requestfactory.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation on Request classes specifying the server side implementations that
 * back them.
 * 
 * @see ServiceName
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
  /**
   * The domain type that provides the implementations for the methods defined
   * in the RequestContext.
   */
  Class<?> value();

  /**
   * An optional {@link ServiceLocator} that provides instances of service
   * objects used when invoking instance methods on the type returned by
   * {@link #value()}.
   */
  // http://bugs.sun.com/view_bug.do?bug_id=6512707
  Class<? extends ServiceLocator> locator() default com.google.web.bindery.requestfactory.shared.ServiceLocator.class;
}
