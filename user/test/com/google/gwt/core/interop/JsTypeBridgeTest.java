/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.core.interop;

import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsType;

/**
 * Tests JsInterop bridge methods.
 */
public class JsTypeBridgeTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Interop";
  }

  @JsType
  private interface JsListInterface {
    void add(Object o);
  }

  private interface Collection {
    void add(Object o);
  }

  private static class CollectionBase implements Collection {
    Object x;

    public void add(Object o) {
      x = o + "CollectionBase";
    }
  }

  private interface List extends Collection, JsListInterface {
    void add(Object o);
  }

  private static class FooImpl extends CollectionBase implements Collection {
    @Override
    public void add(Object o) {
      super.add(o);
      x = x.toString() + "FooImpl";
    }
  }

  private static class ListImpl extends CollectionBase implements List {
    @Override
    public void add(Object o) {
      x = o + "ListImpl";
    }
  }

  public void testBridges() {
    ListImpl listWithExport = new ListImpl(); // Exports .add().
    FooImpl listNoExport = new FooImpl(); // Does not export .add().

    // Use a loose type reference to force polymorphic dispatch.
    Collection collectionWithExport = listWithExport;
    // Calls through a bridge method.
    collectionWithExport.add("Loose");
    assertEquals("LooseListImpl", listWithExport.x);

    // Use a loose type reference to force polymorphic dispatch.
    Collection collectionNoExport = listNoExport;
    collectionNoExport.add("Loose");
    assertEquals("LooseCollectionBaseFooImpl", listNoExport.x);

    // Calls directly.
    listNoExport.add("Tight");
    assertEquals("TightCollectionBaseFooImpl", listNoExport.x);

    listWithExport.add("Tight");
    assertEquals("TightListImpl", listWithExport.x);
  }
}
