/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.user.rebind.rpc.testcases.client;

import java.io.Serializable;

/**
 * Sets up a situation where an array type has a covariant type but its leaf's subtypes aren't
 * cached.
 */
public class SubclassUsedInArray {

  /** Root type. */
  public interface Base extends Serializable {
  }

  /** Array's leaf type. Not a root so its subtypes aren't cached. */
  public static class Subtype implements Base {
    // Has a field so it's not trivially serializable and full analysis will be done.
    private FieldType fieldType;
  }

  /** A subtype to trigger a covariant array type. */
  public static class LeafType extends Subtype {
  }

  /** Root type to trigger the array. */
  public static class HasArray implements Serializable {
    private Subtype[] array;
  }

  /** Just a field. */
  public static class FieldType implements Serializable {
  }
}
