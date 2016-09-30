/*
 * Copyright 2015 Google Inc.
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
package jsinterop.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JsOverlay is used to enhance Java API of the native JsTypes and JsFunctions so richer and more
 * Java friendly abstractions could be provided.
 *
 * <pre>
 * {@literal @}JsType(isNative=true)
 * class Person {
 *   {@literal @}JsOverlay
 *   private static final Person NO_BODY = new Person();
 *
 *   private String name;
 *   private String lastName;
 *
 *   {@literal @}JsOverlay
 *   public String getFullName() {
 *     return (name + " " + lastName).trim();
 *   }
 * }</pre>
 *
 * <p>Note that:
 *
 * <ul>
 * <li> JsOverlay methods cannot override any existing methods.
 * <li> JsOverlay methods should be effectively final.
 * <li> JsOverlay methods cannot be called from JavaScript
 * </ul>
 *
 * These restrictions are in place to avoid polymorphism because underneath the original type is not
 * modified and the overlay fields/methods are simply turned into static dispatches.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface JsOverlay {
}
