package com.google.gwt.user.client.rpc;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * This class is the superclass for any test case that needs to work around 
 * a bug where the generated, client-side type serializer throws the wrong
 * type of exception.
 */
abstract class TypeSerializerWorkAround extends GWTTestCase {
  void workAroundTypeSerializerBug(RuntimeException ex) {
    if (ex.getCause() instanceof SerializationException) {
      finishTest();
    } else {
      fail("Unexpected exception");
    }
  }
}
