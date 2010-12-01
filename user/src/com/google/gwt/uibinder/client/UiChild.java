/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.uibinder.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method as the appropriate way to add a child widget to the parent
 * class.
 * 
 * <p>
 * The limit attribute specifies the number of times the function can be safely
 * called. If no limit is specified, it is assumed to be unlimited. Only one
 * child is permitted under each custom tag specified so the limit represents
 * the number of times the tag can be present in any object.
 * 
 * <p>
 * The tagname attribute indicates the name of the tag this method will handle
 * in the {@link UiBinder} template. If none is specified, the method name must
 * begin with "add", and the tag is assumed to be the remaining characters
 * (after the "add" prefix") entirely in lowercase.
 * 
 * <p>
 * For example, <code>
 * 
 * &#064;UiChild MyWidget#addCustomChild(Widget w) </code> and
 * 
 * <pre>
 *   &lt;p:MyWidget>
 *     &lt;p:customchild>
 *       &lt;g:SomeWidget />
 *     &lt;/p:customchild>
 *   &lt;/p:MyWidget>
 * </pre> 
 * would invoke the <code>addCustomChild</code> function to add an instance of
 * SomeWidget.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UiChild {

  int limit() default -1;

  String tagname() default "";
}
