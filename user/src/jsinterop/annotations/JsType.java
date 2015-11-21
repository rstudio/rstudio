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
 * <p>
 * Marking an object with JsType is similar to marking each public member of the class with
 * {@link JsProperty}/{@link JsMethod}/{@link JsConstructor} respectively. In order for this to work
 * correctly the JavaScript name needs to be unique for each member. Some unobvious ways to cause
 * name collisions are;
 * <p>
 * <li>Having method or constructor overloads.
 * <li>Using the same name for a method and a field.
 * <li>Shadowing a field from parent.
 * <p>
 * A name collision needs to be avoided by providing a custom name (e.g. {@link JsProperty#name}) or
 * by completely ignoring the member using {@link JsIgnore}.
 * <p>
 * If the JsType is marked as "native", then the type is considered as stub for an existing class
 * that is available in native JavaScript. If it is concrete type, the subclass will use the
 * designated type as super type opposed to the ordinary one (e.g. java.lang.Object).
 * <p>
 * Instanceof and Castability:
 * <p>
 * If the JsTypes is native then the generated code will try to mimic Javascript semantics.
 * <li>If it is concrete native JsType then cast checks and instanceof checks will be delegated to
 * the native JavaScript instanceof operator.
 * <li>If it is an interface and marked as native, no checks will be performed.
 * <p>
 * All non-native JsTypes will follow regular Java semantics in terms of castability.
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
