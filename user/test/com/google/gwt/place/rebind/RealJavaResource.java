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
import com.google.gwt.dev.util.Util;

import java.io.InputStream;

/**
 * Loads the actual source of a type. This should be used only for types
 * directly tested by this package's tests. Note that use of this class
 * requires your source files to be on your classpath.
 * <p>
 * Copied from {@link com.google.gwt.editor.rebind.model.EditorModelTest}
 * pending a public API.
 */
class RealJavaResource extends MockJavaResource {
  public RealJavaResource(Class<?> clazz) {
    super(clazz.getName());
  }

  @Override
  public CharSequence getContent() {
    String resourceName = getTypeName().replace('.', '/') + ".java";
    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
        resourceName);
    return Util.readStreamAsString(stream);
  }
}
