/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The presence of this annotation on an interface type allows one
 * JavaScriptObject subclass to implement the interface. Any number of types
 * that are not derived from JavaScriptObject may also implement the interface.
 * <p>
 * The use of the SingleJsoImpl annotation is subject to the following
 * restrictions:
 * <ul>
 * <li>Must be applied only to interface types</li>
 * <li>Any super-interfaces of the annotated interface must also be annotated
 * with the SingleJsoImpl</li>
 * </ul>
 */
@Documented
@Target(ElementType.TYPE)
public @interface SingleJsoImpl {
}