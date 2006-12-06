// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

/*
 * This class is defined outside of the CustomFieldSerializerTestSetFactory
 * because of a bug where custom field serializers cannot be inner classes. Once
 * we fix this bug we can move this class into the test set factory.
 */
public class UnserializableClass_CustomFieldSerializer {
  public static void deserialize(SerializationStreamReader streamReader,
      UnserializableClass instance) throws SerializationException {
    instance.setA(streamReader.readInt());
    instance.setB(streamReader.readInt());
    instance.setC(streamReader.readInt());
    instance.setObj(streamReader.readString());
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      UnserializableClass instance) throws SerializationException {
    streamWriter.writeInt(4);
    streamWriter.writeInt(5);
    streamWriter.writeInt(6);
    streamWriter.writeString("bye");
  }
}