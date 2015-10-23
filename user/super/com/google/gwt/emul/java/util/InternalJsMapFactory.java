/*
 * Copyright 2014 Google Inc.
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
package java.util;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A factory to create JavaScript Map instances.
 */
class InternalJsMapFactory {

  private static final JavaScriptObject jsMapCtor = getJsMapConstructor();

  private static native JavaScriptObject getJsMapConstructor() /*-{
    // Firefox 24 & 25 throws StopIteration to signal the end of iteration.
    function isCorrectIterationProtocol() {
      try {
        return new Map().entries().next().done;
      } catch(e) {
        return false;
      }
    }

    if (typeof Map === 'function' && Map.prototype.entries && isCorrectIterationProtocol()) {
      return Map;
    } else {
      return @InternalJsMapFactory::getJsMapPolyFill()();
    }
  }-*/;

  public static native <V> InternalJsMap<V> newJsMap() /*-{
    return new @InternalJsMapFactory::jsMapCtor;
  }-*/;

  /**
   * Returns a partial polyfill for Map that can handle String keys.
   * <p>Implementation notes:
   * <p>String keys are mapped to their values via a JS associative map. String keys could collide
   * with intrinsic properties (like watch, constructor). To avoid that; the polyfill uses
   * {@code Object.create(null)} so it doesn't inherit any properties.
   * <p>For legacy browsers where {@code Object.create} is not available or handling of
   * {@code __proto__} is broken, the polyfill is patched to prepend each key with a ':' while
   * storing.
   */
  private static native JavaScriptObject getJsMapPolyFill() /*-{
    function Stringmap() {
      this.obj = this.createObject();
    };

    Stringmap.prototype.createObject = function(key) {
      return Object.create(null);
    }

    Stringmap.prototype.get = function(key) {
      return this.obj[key];
    };

    Stringmap.prototype.set = function(key, value) {
      this.obj[key] = value;
    };

    Stringmap.prototype['delete'] = function(key) {
      delete this.obj[key];
    };

    Stringmap.prototype.keys = function() {
      return Object.getOwnPropertyNames(this.obj);
    };

    Stringmap.prototype.entries = function() {
      var keys = this.keys();
      var map = this;
      var nextIndex = 0;
      return {
        next: function() {
          if (nextIndex >= keys.length) return {done: true};
          var key = keys[nextIndex++];
          return {value: [key, map.get(key)], done: false};
        }
      };
    };

    if (!@InternalJsMapFactory::canHandleObjectCreateAndProto()()) {
      // Patches the polyfill to drop Object.create(null) and prefix each key with ':" so it will
      // not interfere with intrinsic fields.

      Stringmap.prototype.createObject = function() { return {}; };

      Stringmap.prototype.get = function(key) {
        return this.obj[':' + key];
      };

      Stringmap.prototype.set = function(key, value) {
        this.obj[':' + key] = value;
      };

      Stringmap.prototype['delete'] = function(key) {
        delete this.obj[':' + key];
      };

      Stringmap.prototype.keys = function() {
        var result = [];
        for (var key in this.obj) {
          // char code for ':' is 58
          if (key.charCodeAt(0) == 58) {
            result.push(key.substring(1));
          }
        }
        return result;
      };
    }

    return Stringmap;
  }-*/;

  /**
   * Return {@code true} if the browser is modern enough to handle Object.create and also properly
   * handles '__proto__' field with Object.create(null). (Safari 5, Android, old Firefox)
   */
  private static native boolean canHandleObjectCreateAndProto() /*-{
    if (!Object.create || !Object.getOwnPropertyNames) {
      return false;
    }

    var protoField = "__proto__";

    var map = Object.create(null);
    if (map[protoField] !== undefined) {
      return false;
    }

    var keys = Object.getOwnPropertyNames(map);
    if (keys.length != 0) {
      return false;
    }

    map[protoField] = 42;
    if (map[protoField] !== 42) {
      return false;
    }

    // For old Firefox version who doesn't have native Map. See the Firefox bug:
    // https://bugzilla.mozilla.org/show_bug.cgi?id=837630
    if (Object.getOwnPropertyNames(map).length == 0) {
      return false;
    }

    // Looks like the browser has a workable handling of proto field.
    return true;
  }-*/;

  private InternalJsMapFactory() {
    // Hides the constructor.
  }
}
