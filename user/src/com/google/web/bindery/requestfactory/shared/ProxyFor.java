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
 * Annotation on EntityProxy and ValueProxy classes specifying the domain
 * (server-side) object type.
 * 
 * @see ProxyForName
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProxyFor {
  /**
   * The domain type that the proxy is mapped to.
   */
  Class<?> value();

  /**
   * An optional {@link Locator} that provides instances of the domain objects.
   */
  @SuppressWarnings("rawtypes")
  // http://bugs.sun.com/view_bug.do?bug_id=6512707
  Class<? extends Locator> locator() default com.google.web.bindery.requestfactory.shared.Locator.class;
}
