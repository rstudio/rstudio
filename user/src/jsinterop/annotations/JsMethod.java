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
 * JsMethod marks a method that will be directly translated into a JavaScript method preserving
 * its name.
 * <p>
 * Note: In JavaScript, instance members are defined on the prototype and class members are defined
 * on the constructor function of the type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface JsMethod {

  /**
   * Customizes the name of the member in generated JavaScript. If not provided, the Java name will
   * be used.
   */
  String name() default "<auto>";

  /**
   * Customizes the namespace of the static member in generated JavaScript. If none is provided,
   * namespace is the enclosing class' fully qualified JavaScript name.
   */
  String namespace() default "<auto>";
}
