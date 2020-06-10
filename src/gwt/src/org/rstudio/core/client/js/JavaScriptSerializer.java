/*
 * JavaScriptSerializer.java
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

import com.google.gwt.core.client.JavaScriptObject;

/* 
 * JavaScriptSerializer is a class that knows how to serialize and deserialize
 * Java objects to and from plain JavaScriptObject objects. Its implementation
 * is generated at compile time by JavaScriptSerializerGenerator. 
 * 
 * In order to be serialized or deserialized, a class must be annotated with 
 * @JavaScriptSerializable (see notes there for other requirements), which 
 * triggers the generation of the appropriate serialization methods for the
 * class.
 * 
 * Example usage:
 * 
 * // Create the class
 * @JavaScriptSerializable
 * public class Foo 
 * {
 *    public Foo()
 *    {
 *    }
 *    
 *    int bar;
 * }
 * 
 * // Create the instance to serialize
 * Foo foo = new Foo();
 * foo.bar = 153;
 * 
 * // Serialize the instance of the class 
 * 
 * JavaScriptSerializer serializer = GWT.create(JavaScriptSerializer.class);
 * JavaScriptObject jso = serializer.serialize(foo);
 * 
 * // Deserialize
 * Foo baz = serializer.deserialize(jso);
 * 
 * if (baz.bar == 153) {
 *    // true
 * }
 * 
 * Currently, the primary usage of the JavaScriptSerializer is marshaling GWT
 * "Java" objects across window boundaries (see CrossWindowEvent).
 */
public interface JavaScriptSerializer
{
   <T> JavaScriptObject serialize(T source);
   <T> T deserialize(JavaScriptObject jso);
}
