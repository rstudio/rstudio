/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.javac;

/**
 * A generated Java compilation unit.
 */
public interface GeneratedUnit {
  long creationTime();

  String getSource();

  /**
   * Returns the path to this resource as it should appear in a source map. This should be a
   * path that's relative to either an entry in the classpath, or start with some fixed prefix such
   * as "gen/" for generated files. (It can't be an absolute path because that would make the GWT
   * compiler output unstable and defeat caching.)
   */
  String getSourceMapPath();

  /**
   * Returns the source code as a token for {@link DiskCache#INSTANCE}, or -1 if
   * the source is not cached.
   */
  long getSourceToken();

  String getStrongHash();

  String getTypeName();

  /**
   * If the generated file was saved to a directory using the -gen option, returns the file's
   * location on disk. Otherwise null.
   */
  String optionalFileLocation();
}
