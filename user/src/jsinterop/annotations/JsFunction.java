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
 * JsFunction marks a functional interface as being the definition of a JavaScript function.
 *
 * <p>There are some limitations exists on JsFunction to make them practical and efficient:
 *
 * <ul>
 * <li>A JsFunction interface cannot extend any other interfaces.
 * <li>A class may not implement more than one JsFunction interface.
 * <li>A class that implements a JsFunction type cannot be a {@link JsType} (directly or
 *     indirectly).
 * <li>Fields and defender methods of the interfaces should be marked with {@link JsOverlay} and
 *     cannot be overridden by the implementations.
 * </ul>
 *
 * <p>As a best practice, we also recommend marking JsFunction interfaces with FunctionalInterface
 * to get improved checking in IDEs.
 *
 * <p><b>Instanceof and Castability:</b>
 *
 * <p>Instanceof and casting for JsFunction is effectively a JavaScript <tt>'typeof'</tt> check to
 * determine if the instance is a function.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface JsFunction {
}
