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
package com.google.gwt.place.rebind;

import com.google.gwt.dev.javac.testing.impl.MockJavaResource;

/**
 * Constructs an empty interface representation of a type.
 * <p>
 * Copied from {@link com.google.gwt.editor.rebind.model.EditorModelTest}
 * pending a public API.
 */
class EmptyMockJavaResource extends MockJavaResource {
  private final StringBuilder code = new StringBuilder();

  public EmptyMockJavaResource(Class<?> clazz) {
    super(clazz.getName());

    code.append("package ").append(clazz.getPackage().getName()).append(";\n");
    code.append("public interface ").append(clazz.getSimpleName());

    int numParams = clazz.getTypeParameters().length;
    if (numParams != 0) {
      code.append("<");
      for (int i = 0; i < numParams; i++) {
        if (i != 0) {
          code.append(",");
        }
        code.append("T").append(i);
      }
      code.append(">");
    }

    code.append("{}\n");
  }

  @Override
  public CharSequence getContent() {
    return code;
  }
}
