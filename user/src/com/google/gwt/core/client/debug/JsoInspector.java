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

package com.google.gwt.core.client.debug;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class provides an API for IDEs to inspect JavaScript objects and is not
 * intended to be used in GWT applications. IDEs that allow custom value
 * renderers for debugging can use it to box JavaScript objects into suitable
 * Java types.
 * 
 * TODO: provide a way to test whether a node has children (to be used as an
 * optimization in IntelliJ).
 * 
 * TODO: implement and return concrete JsoProperty subtypes (Integer, Float,
 * etc.) to get more descriptive labels in IntelliJ.
 */
public class JsoInspector {

  /**
   * A simple Java object to hold a key and value pair.
   */
  public static class JsoProperty implements Comparable<JsoProperty> {
    public final String key;
    public final Object value;
    public final boolean isOwnProperty;

    private JsoProperty(String key, Object value, boolean isOwnProperty) {
      this.key = key;
      this.value = value;
      this.isOwnProperty = isOwnProperty;
    }

    @Override
    public int compareTo(JsoProperty o) {
      int keyComparison = key.compareTo(o.key);

      /*
       * The hash code comparison is so this class's natural ordering is
       * consistent (see Comparable interface javadoc). Ideally, we would have
       * done value.compareTo(o.value), but value may not implement Comparable.
       */
      return keyComparison != 0 ? keyComparison : value.hashCode()
          - o.value.hashCode();
    }

    @Override
    public String toString() {
      StringBuilder s = new StringBuilder();

      if (isOwnProperty) {
        s.append('*');
      }

      s.append(key).append(": ");

      if (value instanceof String) {
        s.append('"').append(value).append('"');
      } else {
        s.append(value);
      }

      return s.toString();
    }
  }

  private static class JsoBoxer extends JavaScriptObject {

    protected JsoBoxer() {
    }

    /**
     * Returns a Java object that represents this JavaScriptObject instance. The
     * returned Java object may still contain other JavaScriptObjects. Eclipse
     * will attempt to lazily fetch the logical structure for those children
     * JavaScriptObjects when the user clicks on one of them.
     */
    public final native Object box() /*-{

      // Returns a Java object for simpler JavaScript values
      // (primitives, functions, null, undefined), or the given value
      // if it is not a "simple" type
      var asJavaObjectForSimpleValue = function(value) {
        var valueType = typeof(value);

        // Earlier, I had function types printing their function contents,
        // but pure Java classes in GWT get converted to function prototypes
        // in Javascript, so there were issues
        if (valueType == "number") {
          // Differentiate int from float
          if (/[.]/.test(value + '')) {
            return @java.lang.Float::new(F)(value);
          } else {
            return @java.lang.Integer::new(I)(value);
          }
        } else if (valueType == "boolean") {
          return @java.lang.Boolean::new(Z)(value);
        } else if (valueType == "string") {
          return value;
        } else if (valueType == "undefined" || valueType == "null") {
          // Return the corresponding string
          return valueType;
        } else {
          return value;
        }
      }

      var asJavaObject = function(value) {
        var valueType = typeof(value);

        if (valueType == "object") {
          if (value instanceof Array) {
            var list = @java.util.ArrayList::new()();
            for (i in value) {
              list.@java.util.ArrayList::add(Ljava/lang/Object;)(asJavaObjectForSimpleValue(value[i]));
            }

            // If we return a List, Eclipse's Variables view will show one
            // extra level unnecessarily. It does not do this for Java arrays.
            return list.@java.util.ArrayList::toArray()();

          } else {
            var properties = @java.util.ArrayList::new()();

            for (var name in value) {
              var propertyValue;
              try {
                var origPropertyValue = value[name];
                if (typeof(origPropertyValue) == "function"
                    && !value.hasOwnProperty(name)) {
                  // Prevent every JavaScriptObject method from appearing
                  continue;
                }

                // Sometimes, we don't have permission to see the value
                // and an exception is thrown
                propertyValue = asJavaObjectForSimpleValue(origPropertyValue);
              } catch (e) {
                propertyValue = "ERROR: " + e.name + " - " + e.message;
              }

              // Wrap the (key, value) in our JsoProperty class
              var jsoProperty = @com.google.gwt.core.client.debug.JsoInspector.JsoProperty::new(Ljava/lang/String;Ljava/lang/Object;Z)(name, propertyValue, value.hasOwnProperty(name));
              properties.@java.util.ArrayList::add(Ljava/lang/Object;)(jsoProperty);
            }

            // Sort the properties
            @java.util.Collections::sort(Ljava/util/List;)(properties);

            // If we return a List, Eclipse's Variables view will show one
            // extra level unnecessarily. It does not do this for Java arrays.
            var propertiesAsArray = properties.@java.util.ArrayList::toArray()();

            return propertiesAsArray;
          }
        } else {
          return asJavaObjectForSimpleValue(value);
        }
      };

      return asJavaObject(this);
    }-*/;
  }

  /**
   * Wraps a JavaScript object into a suitable inspectable type.
   */
  public static Object convertToInspectableObject(Object jso) {
    try {
      JsoBoxer boxer = (JsoBoxer) jso;
      return boxer.box();
    } catch (Throwable t) {
      GWT.log("GWT could not inspect the JavaScriptObject.", t);
      return "ERROR: Could not inspect the JavaScriptObject, see GWT log for details.";
    }
  }
}
