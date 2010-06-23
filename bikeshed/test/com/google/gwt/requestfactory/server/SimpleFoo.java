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
package com.google.gwt.requestfactory.server;

import java.util.List;

/**
 * Domain object for SimpleFooRequest.
 */
public class SimpleFoo {
  public static Long countSimpleFoo() {
    return 0L;
  }

  public static List<SimpleFoo> findAll() {
    return null;
  }

  public static SimpleFoo findSimpleFooById(Long id) {
    return null;
  }

  @SuppressWarnings("unused")
  private static Integer privateMethod() {
    return 0;
  }
}
