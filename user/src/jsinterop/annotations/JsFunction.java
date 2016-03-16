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
 * Marks a type containing a Single Abstract Method (SAM) as eligible for automatic conversion into
 * a JavaScript function.
 * <p>
 * This enables lambda expressions to be passed directly to JavaScript as callbacks.
 * <p>
 * However there are some additional limitations that are imposed to make this practical and
 * efficient:
 * <li>A class may not implement more than one @JsFunction type. This restriction allows the
 * compiler to construct a one-to-one mapping between the Java class and the generated JavaScript
 * function and preserve referential equality.
 * <li>A JsFunction interface cannot extend any other interfaces.
 * <li>A JsFunction interface cannot have defender methods.
 * <li>A class that implements a @JsFunction type (directly or indirectly) cannot be a @JsType.
 * <p>
 * As a best practice, we also recommend marking @JsFunction interfaces with @FunctionalInterface
 * to get improved checking in IDEs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface JsFunction {
}
