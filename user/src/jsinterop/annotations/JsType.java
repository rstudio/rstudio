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
 * JsType is used to describe the JavaScript API of an object, either one that already exists from
 * the external JavaScript environment, or one that will be accessible from the external JavaScript
 * environment.
 *
 * <p>Marking an object with JsType is similar to marking each public member of the class with
 * {@link JsProperty}/{@link JsMethod}/{@link JsConstructor} respectively. In order for this to work
 * correctly the JavaScript name needs to be unique for each member. Some unobvious ways to cause
 * name collisions are:
 *
 * <ul>
 * <li>having method or constructor overloads
 * <li>using the same name for a method and a field
 * <li>shadowing a field from parent
 * </ul>
 *
 * <p>Name collisions must be avoided by providing custom names (e.g. {@link JsProperty#name}) or by
 * ignoring members using {@link JsIgnore}.
 *
 * <p>If the JsType is marked as "native" via {@link #isNative}, then the type is considered a stub
 * for an existing class that is available in native JavaScript. Unlike non-native JsTypes, all
 * members are considered {@link JsProperty}/{@link JsMethod}/{@link JsConstructor} unless they are
 * explicitly marked with {@link JsOverlay}.
 *
 * <p><b>Instanceof and Castability:</b>
 *
 * <p>If the JsType is native, the generated code will try to mimic Javascript semantics.
 *
 * <p>All non-native JsTypes will follow regular Java semantics in terms of castability.
 *
 * <ul>
 * <li>For concrete native JsTypes, cast checks and instanceof checks will be delegated to the
 *     native JavaScript instanceof operator.
 * <li>For interface native JsTypes, instanceof is forbidden and casts to them always succeed.
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface JsType {

  /**
   * Customizes the name of the type in generated JavaScript. If not provided, the simple Java name
   * will be used.
   */
  String name() default "<auto>";

  /**
   * Customizes the namespace of the type in generated JavaScript.
   */
  String namespace() default "<auto>";

  /**
   * Set to {@code true}, this JsType is a native JavaScript type.
   */
  boolean isNative() default false;
}
