/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.user.client.rpc;

import java.util.HashMap;

/**
 * This class is defined outside of the TypeCheckedObjectTestSetFactory
 * because of a bug where custom field serializers cannot be inner classes. Once
 * we fix this bug we can move this class into the test set factory.
 */
@SuppressWarnings("rawtypes")
public class TypeCheckedGenericClass_CustomFieldSerializer {
  @SuppressWarnings("unchecked")
  public static void deserialize(SerializationStreamReader streamReader,
      TypeCheckedGenericClass instance) throws SerializationException {
    Object markerKey = streamReader.readObject();
    Object markerValue = streamReader.readObject();
    instance.setMarker(markerKey, markerValue);

    instance.hashField = (HashMap) streamReader.readObject();
  }
  
  @SuppressWarnings("unused")
  public static TypeCheckedGenericClass instantiate(SerializationStreamReader streamReader) {
    return new TypeCheckedGenericClass();
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      TypeCheckedGenericClass instance) throws SerializationException {
    streamWriter.writeObject(instance.getMarkerKey());
    streamWriter.writeObject(instance.getMarkerValue());
    streamWriter.writeObject(instance.hashField);
  }
}