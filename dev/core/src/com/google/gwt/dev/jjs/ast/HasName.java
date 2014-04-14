/*
 * Copyright 2007 Google Inc.
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Interface implemented by named entities.
 */
public interface HasName {

  /**
   * Collection of utilities to deal with HasName objects.
   */
  public static final class Util {
    public static <T extends HasName> void sortByName(List<T> list) {
      Collections.sort(list, new Comparator<T>() {
        @Override
        public int compare(T o1, T o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
    }
  }

  String getName();
}
