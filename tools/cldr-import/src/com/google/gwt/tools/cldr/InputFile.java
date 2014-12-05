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
package com.google.gwt.tools.cldr;

import org.unicode.cldr.util.CLDRFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A wrapper around a CLDR file.
 * Note that CLDR "files" are usually a merged views of multiple XML files.
 */
class InputFile {
  private final CLDRFile delegate;

  InputFile(CLDRFile delegate) {
    this.delegate = delegate;
  }

  /**
   * Returns the "distinguished paths" with the given prefix, in sorted order.
   */
  List<String> listPaths(String prefix) {
    List<String> out = new ArrayList<String>();
    Iterator<String> it = delegate.iterator(prefix);
    while (it.hasNext()) {
      out.add(it.next());
    }
    Collections.sort(out);
    return out;
  }

  String getFullXPath(String path) {
    return delegate.getFullXPath(path);
  }

  String getStringValue(String fullPath) {
    return delegate.getStringValue(fullPath);
  }
}
