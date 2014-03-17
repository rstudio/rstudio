/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.cfg;

import java.util.List;

/**
 * An immutable library group.<br>
 *
 * Makes a good starter value for libraryGroup variables in that it allows read calling code without
 * having to check for a null but will force write calling code to be guarded a condition that
 * verifies that values even should be inserted.
 */
public class ImmutableLibraryGroup extends LibraryGroup {

  @Override
  public LibraryGroup createSubgroup(List<String> libraryNames) {
    throw new UnsupportedOperationException("ImmutableLibraryGroup does not support modification.");
  }
}
