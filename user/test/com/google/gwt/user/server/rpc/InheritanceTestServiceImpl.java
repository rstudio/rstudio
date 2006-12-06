package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory;
import com.google.gwt.user.client.rpc.InheritanceTestSetValidator;
import com.google.gwt.user.client.rpc.InheritanceTestService;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;

public class InheritanceTestServiceImpl extends RemoteServiceServlet implements
    InheritanceTestService {

  public AnonymousClassInterface echo(AnonymousClassInterface serializable) {
    return serializable;
  }

  public IsSerializable echo(IsSerializable serializable) {
    return serializable;
  }

  public SerializableClass echo(SerializableClass serializableClass) {
    if (!InheritanceTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    return serializableClass;
  }

  public SerializableClassWithTransientField echo(
      SerializableClassWithTransientField serializableClass) {
    if (!InheritanceTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    // this should not be sent back across the wire, the client will verify
    // that this is true
    serializableClass.setObj("hello");

    return serializableClass;
  }

  public SerializableSubclass echo(SerializableSubclass serializableSubclass) {
    if (!InheritanceTestSetValidator.isValid(serializableSubclass)) {
      throw new RuntimeException();
    }

    return serializableSubclass;
  }

  public SerializableClass getUnserializableClass() {
    return new InheritanceTestSetFactory.SerializableClassWithUnserializableClassField();
  }
}
