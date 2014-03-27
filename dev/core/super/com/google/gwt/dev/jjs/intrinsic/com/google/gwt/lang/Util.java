/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class is used to access the private, GWT-specific
 * castableTypeMap and typeMarker fields.
 */
final class Util {

  static native JavaScriptObject getCastableTypeMap(Object o) /*-{
    return o.@java.lang.Object::castableTypeMap;
  }-*/;

  static native void setTypeMarker(Object o) /*-{
      o.@java.lang.Object::typeMarker =
          @com.google.gwt.lang.JavaClassHierarchySetupUtil::typeMarkerFn(*);
  }-*/;

  static native boolean hasTypeMarker(Object o) /*-{
    return o.@java.lang.Object::typeMarker ===
        @com.google.gwt.lang.JavaClassHierarchySetupUtil::typeMarkerFn(*);
  }-*/;

  static native void setCastableTypeMap(Object o, JavaScriptObject castableTypeMap) /*-{
    o.@java.lang.Object::castableTypeMap = castableTypeMap;
  }-*/;
}
