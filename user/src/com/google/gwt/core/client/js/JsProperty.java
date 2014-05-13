/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.client.js;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JsProperty marks a method in a {@link JsType} as a property accessor and recognizes
 * JavaBean style naming convention. Instead of translating method calls to JsProperty methods
 * as method calls in JS, they will be replaced with dotted property lookups.
 *<p> Examples:
 * <ul>
 * <li> {@code @JsProperty getX()} translates as <tt>this.x</tt>
 * <li> {@code @JsProperty x()} translates as <tt>this.x</tt>
 * <li> {@code @JsProperty setX(int y)} translates as <tt>this.x=y</tt>
 * <li> {@code @JsProperty x(int x)} translates as <tt>this.x=y</tt>
 * <li> {@code @JsProperty hasX(int x)} translates as <tt>x in this</tt>
 *</ul>
 * <p>
 * In addition, fluent style <tt>return this</tt> syntax is supported for setters, so
 * {@code @JsProperty T setX(int x)} translates as <tt>this.x=x, return this</tt>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface JsProperty {
  String value() default "";
}
