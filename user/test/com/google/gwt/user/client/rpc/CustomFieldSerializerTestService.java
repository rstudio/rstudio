// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

public interface CustomFieldSerializerTestService extends RemoteService {
  SerializableSubclass echo(SerializableSubclass serializableClass);

  UnserializableClass echo(UnserializableClass unserializableClass);
}
