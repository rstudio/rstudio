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
 * JsProperty marks a field or method that is translated directly into a JavaScript property
 * preserving its name.
 * <p>
 * If it is applied to a method, it will be treated as a property accessor. As a result, instead of
 * translating method calls to JsProperty methods as method calls in JS, they will be translated as
 * property lookups. When a JsProperty method implemented by a Java class, such methods will be
 * generated as custom property setter and getter in JavaScript, hence the property access will
 * trigger the execution of the matching getter or setter methods.
 * <p>
 * JsProperty follows JavaBean style naming convention to extract the default property name. If the
 * JavaBean convention is not followed, the name should be set explicitly. For example:
 * <ul>
 * <li> {@code @JsProperty getX()} or {@code @JsProperty isX()} translates as <tt>this.x</tt>
 * <li> {@code @JsProperty setX(int y)} translates as <tt>this.x=y</tt>
 * </ul>
 * <p>
 * Note: In JavaScript, instance members are defined on the prototype and class members are defined
 * on the constructor function of the type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface JsProperty {

  /**
   * Customizes the name of the member in generated JavaScript. If none is provided;
   * <p>
   * <li>if it is field, the simple Java name will be used.
   * <li>if it is a method, the name will be generated based on JavaBean conventions.
   */
  String name() default "<auto>";

  /**
   * Customizes the namespace of the static member in generated JavaScript. If none is provided,
   * namespace is the enclosing class' fully qualified JavaScript name.
   */
  String namespace() default "<auto>";
}
