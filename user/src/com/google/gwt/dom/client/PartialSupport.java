/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dom.client;

/**
 * Annotation for classes that are only supported on a limited set of browsers.
 * 
 * <p>
 * By convention, classes annotated with PartialSupport will provide two static 
 * methods:
 * <ol>
 *   <li> <code>static boolean isSupported()</code> that returns whether the 
 *   feature is supported. </li>
 *   <li> <code>static YourType createIfSupported()</code> factory method that 
 *   returns a new feature if supported, or null otherwise. </li>
 * </ol>
 * </p>
 */
public @interface PartialSupport {

}
