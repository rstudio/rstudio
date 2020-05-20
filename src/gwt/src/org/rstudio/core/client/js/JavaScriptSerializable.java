/*
 * JavaScriptSerializable.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.core.client.js;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/*
 * JavaScriptSerializable is an annotation that may be placed on any class to
 * make it possible to both serialize the class to a JavaScriptObject and to
 * deserialize the JavaScriptObject into a new instance of the class.
 * 
 * There are a few prerequisites that must be met for classes that wish to be
 * serialized:
 * 
 * 1. The class must contain only fields which are "plain old data" (e.g.
 *    String, int, boolean, JavaScriptObject), or classes which are themselves 
 *    JavaScriptSerializable The behavior for other fields is not currently
 *    defined; these fields may be null on hydration.
 *    
 * 2. The class must have an empty constructor; the deserializer works by 
 *    constructing an empty instance of the class and then using JSNI to 
 *    populate its fields.
 * 
 * See remarks in JavaScriptSerializer for notes on how to serialize and 
 * deserialize annotated classes, and example usage.
 * 
 */
@Target(ElementType.TYPE)
public @interface JavaScriptSerializable
{
   String value() default "";
}
