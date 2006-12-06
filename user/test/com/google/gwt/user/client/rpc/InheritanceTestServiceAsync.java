// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;

public interface InheritanceTestServiceAsync {
  void echo(AnonymousClassInterface serializable, AsyncCallback callback);

  void echo(IsSerializable serializable, AsyncCallback callback);

  void echo(SerializableClass serializableClass, AsyncCallback callback);

  void echo(SerializableClassWithTransientField serializableClass,
      AsyncCallback callback);

  void echo(SerializableSubclass serializableSubclass, AsyncCallback callback);

  void getUnserializableClass(AsyncCallback callback);
}
