package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestService;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetValidator;
import com.google.gwt.user.client.rpc.UnserializableClass;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

public class CustomFieldSerializerTestServiceImpl extends RemoteServiceServlet
    implements CustomFieldSerializerTestService {

  public SerializableSubclass echo(SerializableSubclass serializableClass) {
    if (!CustomFieldSerializerTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    return serializableClass;
  }

  public UnserializableClass echo(UnserializableClass unserializableClass) {
    if (!CustomFieldSerializerTestSetValidator.isValid(unserializableClass)) {
      throw new RuntimeException();
    }

    return unserializableClass;
  }
}
