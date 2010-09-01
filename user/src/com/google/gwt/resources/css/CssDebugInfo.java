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
package com.google.gwt.resources.css;

/**
 * This interface allows diagnostic information about a CssResource to be
 * accumulated during processing.
 */
public interface CssDebugInfo {
  /**
   * A no-op implementation.
   */
  CssDebugInfo NULL = new CssDebugInfo() {
    public void addToClassMap(String rawClassName, String obfuscatedClassName) {
    }

    public boolean isEnabled() {
      return false;
    }

    public void setMethodName(String methodName) {
    }

    public void setOwnerType(String ownerType) {
    }

    public void setSource(String[] source) {
    }
  };

  void addToClassMap(String rawClassName, String obfuscatedClassName);

  boolean isEnabled();

  void setMethodName(String methodName);

  void setOwnerType(String ownerType);

  void setSource(String[] source);
}
