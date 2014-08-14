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

import java.util.InternalJsHashCodeMap.InternalJsHashCodeMapLegacy;
import java.util.InternalJsStringMap.InternalJsStringMapLegacy;
import java.util.InternalJsStringMap.InternalJsStringMapWithKeysWorkaround;

/**
 * A factory to create internal JS map instances for modern browsers.
 */
class InternalJsMapFactory {

  public <K, V> InternalJsHashCodeMap<K, V> createJsHashCodeMap() {
    return new InternalJsHashCodeMap<K, V>();
  }

  public <K, V> InternalJsStringMap<K, V> createJsStringMap() {
    return new InternalJsStringMap<K, V>();
  }

  /**
   * A {@code InternalJsMapFactory} that returns JS map instances compatible with legacy browsers.
   */
  static class LegacyInternalJsMapFactory extends InternalJsMapFactory {
    @Override
    public <K, V> InternalJsHashCodeMap<K, V> createJsHashCodeMap() {
      return new InternalJsHashCodeMapLegacy<K, V>();
    }

    @Override
    public <K, V> InternalJsStringMap<K, V> createJsStringMap() {
      return new InternalJsStringMapLegacy<K, V>();
    }
  }

  /**
   * A {@code InternalJsMapFactory} that returns JS map instances that works around keys bug.
   */
  static class KeysWorkaroundJsMapFactory extends InternalJsMapFactory {
    @Override
    public <K, V> InternalJsStringMap<K, V> createJsStringMap() {
      return new InternalJsStringMapWithKeysWorkaround<K, V>();
    }
  }

  /**
   * A replacement factory that chooses best JS map implementation based on capability check.
   */
  static class BackwardCompatibleJsMapFactory extends InternalJsMapFactory {

    private static InternalJsMapFactory delegate = createFactory();

    private static InternalJsMapFactory createFactory() {
      if (isModern() && canHandleProto()) {
        return needsKeysWorkaround() ? new KeysWorkaroundJsMapFactory()
            : new InternalJsMapFactory();
      }
      return new LegacyInternalJsMapFactory();
    }

    private static native boolean isModern() /*-{
      return Object.create && Object.getOwnPropertyNames;
    }-*/;

    // See the Firefox bug: https://bugzilla.mozilla.org/show_bug.cgi?id=837630
    private static native boolean needsKeysWorkaround() /*-{
      var map = Object.create(null);
      map["__proto__"] = 42;
      return Object.getOwnPropertyNames(map).length == 0;
    }-*/;

    // Some older browsers doesn't properly handle __proto__ at all (Safari 5, Android).
    private static native boolean canHandleProto() /*-{
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

      // Looks like the browser has a workable handling of proto field.
      return true;
    }-*/;

    @Override
    public <K, V> InternalJsHashCodeMap<K, V> createJsHashCodeMap() {
      return delegate.createJsHashCodeMap();
    }

    @Override
    public <K, V> InternalJsStringMap<K, V> createJsStringMap() {
      return delegate.createJsStringMap();
    }
  }
}
