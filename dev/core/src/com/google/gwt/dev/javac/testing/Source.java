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
package com.google.gwt.dev.javac.testing;

/**
 * Sources used to provide content to a {@link GeneratorContext} when testing
 * generators.
 */
public interface Source {

  /**
   * Returns the source path of this source fragment within the compilation,
   * for example {@code com/example/client/Foo.java} or
   * {@code com/example/public/bar.css}.
   */
  String getPath();

  /**
   * Returns the contents of this source fragment.
   */
  String getSource();
}
