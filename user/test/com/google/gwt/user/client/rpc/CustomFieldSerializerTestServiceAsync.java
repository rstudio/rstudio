// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

public interface CustomFieldSerializerTestServiceAsync {
  void echo(SerializableSubclass serializableClass, AsyncCallback callback);

  void echo(UnserializableClass unserializableClass, AsyncCallback callback);
}
