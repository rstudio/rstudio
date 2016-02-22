/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.javac.JsInteropUtil;

import java.beans.Introspector;

/**
 * Abstracts JsInterop information for the AST nodes.
 */
public interface HasJsInfo extends HasJsName {
  /**
   * Indicates type of JsMember.
   */
  enum JsMemberType {
    /**
     * Not a js member.
     */
    NONE {
      @Override
      public String computeName(JMember member) {
          return null;
      }
    },
    /**
     * A JsConstructor.
     */
    CONSTRUCTOR {
      @Override
      public String computeName(JMember member) {
        return "";
      }
    },
    /**
     * A JsMethod.
     */
    METHOD,
    /**
     * A JsProperty.
     */
    PROPERTY,
    /**
     * A getter JsProperty accessor. Usually in the form of getX()/isX().
     */
    GETTER("get") {
      @Override
      public String computeName(JMember member) {
        String methodName = member.getName();
        if (startsWithCamelCase(methodName, "get")) {
          return Introspector.decapitalize(methodName.substring(3));
        }
        if (startsWithCamelCase(methodName, "is")) {
          return Introspector.decapitalize(methodName.substring(2));
        }
        return JsInteropUtil.INVALID_JSNAME;
      }
      @Override
      public boolean isPropertyAccessor() {
        return true;
      }
    },
    /**
     * A setter JsProperty accessor. Usually in the form of setX(x).
     */
    SETTER("set") {
      @Override
      public String computeName(JMember member) {
        String methodName = member.getName();
        if (startsWithCamelCase(methodName, "set")) {
          return Introspector.decapitalize(methodName.substring(3));
        }
        return JsInteropUtil.INVALID_JSNAME;
      }
      @Override
      public boolean isPropertyAccessor() {
        return true;
      }
    },
    /**
     * A property accessor but doesn't match setter/getter patterns.
     */
    UNDEFINED_ACCESSOR;

    private String accessorKey;

    private JsMemberType() { }

    private JsMemberType(String accessorKey) {
      this.accessorKey = accessorKey;
    }

    public String getPropertyAccessorKey() {
      return accessorKey;
    }

    public boolean isPropertyAccessor() {
      return getPropertyAccessorKey() != null;
    }

    public String computeName(JMember member) {
      return member.getName();
    }

    private static boolean startsWithCamelCase(String string, String prefix) {
      return string.length() > prefix.length() && string.startsWith(prefix)
          && Character.isUpperCase(string.charAt(prefix.length()));
    }
  }

  void setJsMemberInfo(JsMemberType type, String namespace, String name, boolean exported);

  void setJsOverlay();

  JsMemberType getJsMemberType();

  boolean isJsNative();

  boolean isJsMethodVarargs();

  boolean isJsOverlay();

  boolean canBeReferencedExternally();

  boolean canBeImplementedExternally();

  boolean isJsInteropEntryPoint();
}
