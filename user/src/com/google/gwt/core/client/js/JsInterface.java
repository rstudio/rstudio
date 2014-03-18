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
 * JsInterface is used to describe the interface of a Javascript object, either one that already
 * exists from the external Javascript environment, or one that will be accessible to the
 * external Javascript environment. Calls to methods on interfaces marked with this annotation
 * are treated specially by the GWT compiler for interoperability purposes. Such methods need
 * not be backed by an Overlay type implementation, the GWT compiler will assume that a JS method on
 * the prototype of the underlying reference will match the name of the method on this interface.
 * <p>
 * Furthermore, if the JsInterface is marked with a prototype reference, then concrete
 * implementations of the class emitted by the GWT compiler will use the specified prototype as
 * opposed the the ordinary one (e.g. java.lang.Object).
 * <p>
 * JsInterfaces act like JavaScriptObject in terms of castability, except when a prototype is
 * specified, in which case, cast checks and instanceof checks will be delegated to the native
 * JS instanceof operator.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface JsInterface {
  String prototype() default "";
  boolean isNative() default false;
}
