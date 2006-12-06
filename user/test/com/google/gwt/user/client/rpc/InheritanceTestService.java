// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;

public interface InheritanceTestService extends RemoteService {
  AnonymousClassInterface echo(AnonymousClassInterface serializable);

  IsSerializable echo(IsSerializable serializable);

  SerializableClass echo(SerializableClass serializableClass);

  SerializableClassWithTransientField echo(
      SerializableClassWithTransientField serializableClass);

  SerializableSubclass echo(SerializableSubclass serializableSubclass);

  SerializableClass getUnserializableClass();
}
