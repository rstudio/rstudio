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

/**
 * This class is defined outside of the TypeUncheckedObjectTestSetFactory
 * because of a bug where custom field serializers cannot be inner classes. Once
 * we fix this bug we can move this class into the test set factory.
 */
@SuppressWarnings("rawtypes")
public class TypeUncheckedGenericClass_CustomFieldSerializer {
  @SuppressWarnings("unchecked")
  public static void deserialize(SerializationStreamReader streamReader,
      TypeUncheckedGenericClass instance) throws SerializationException {
    instance.checkedField = (TypeCheckedGenericClass) streamReader.readObject();
  }
  
  @SuppressWarnings("unused")
  public static TypeUncheckedGenericClass instantiate(SerializationStreamReader streamReader) {
    return new TypeUncheckedGenericClass();
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      TypeUncheckedGenericClass instance) throws SerializationException {
    streamWriter.writeObject(instance.checkedField);
  }
}