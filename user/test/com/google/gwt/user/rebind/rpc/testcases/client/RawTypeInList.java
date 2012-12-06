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
import java.util.List;

/**
 * Sets up the situation where we need to find the covariant arrays of a List that
 * contains a raw type.
 */
public class RawTypeInList {

  /**
   * A root type to make sure we visit Item as a subtype first
   * (so that its subtypes aren't cached).
   */
  public interface Marker extends Serializable {
  }

  /**
   * A root type that uses Item as a raw type in a List.
   * (GWT-RPC automatically adds the corresponding array types for lists.)
   */
  public static class HasList implements Serializable {
    List<Item> field;
  }

  /**
   * A parameterized type.
   */
  public static class Item<T extends Comparable> implements Marker {
    T field;
  }

  /**
   * Arrays of this type should be added as well.
   */
  public static class Covariant extends Item<String> {
  }
}
