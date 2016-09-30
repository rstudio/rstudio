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
 * JsConstructor marks a constructor that will be translated into a JavaScript constructor function.
 *
 * <p>Due to ES6 class semantics, for non-native JsTypes only one JsConstructor is allowed to exist
 * in the type which becomes the 'primary constructor'. All other constructors in the type must
 * delegate to it. Subclasses of a type with JsConstructor should follow the same restriction with
 * the exception that the primary constructor is not required to be marked as JsConstructor but
 * still need to delegate to super primary constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
@Documented
public @interface JsConstructor {
}
