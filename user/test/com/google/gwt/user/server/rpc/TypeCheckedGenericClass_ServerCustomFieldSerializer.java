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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.TypeCheckedGenericClass;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetValidator;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;

/**
 * This class is defined outside of the TypeCheckedObjectTestSetFactory because
 * of a bug where custom field serializers cannot be inner classes. Once we fix
 * this bug we can move this class into the test set factory.
 */
@SuppressWarnings({"rawtypes", "unused"})
public class TypeCheckedGenericClass_ServerCustomFieldSerializer {
  @SuppressWarnings("unchecked")
  public static void deserializeChecked(ServerSerializationStreamReader streamReader,
      TypeCheckedGenericClass instance, Type[] expectedParameterTypes,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) throws SerializationException {
    Object junkKey = streamReader.readObject();
    Object junkValue = streamReader.readObject();

    /*
     * If deserializing a superclass we will not have been instantiated using
     * the custom instantiator, so skip the checks for correct markers.
     */
    if (instance.getClass() != TypeCheckedGenericClass.class
        || ((instance.getMarkerKey() instanceof Integer)
            && ((Integer) instance.getMarkerKey()).intValue() == 54321
            && (instance.getMarkerValue() instanceof String) && ((String) instance.getMarkerValue())
            .equals("LocalMarker"))) {
      instance.setMarker(TypeCheckedObjectsTestSetValidator.markerKey,
          TypeCheckedObjectsTestSetValidator.markerValue);
    } else {
      throw new SerializationException(
          "Incorrect markers in TypeCheckedGenericClass server deserialization. "
              + "Custom instantiate probably not called.");
    }

    try {
      Field declField = TypeCheckedGenericClass.class.getField("hashField");
      Type declGenericType = declField.getGenericType();
      SerializabilityUtil.resolveTypes(declGenericType, resolvedTypes);
      instance.hashField = (HashMap) streamReader.readObject(declGenericType, resolvedTypes);
      SerializabilityUtil.releaseTypes(declGenericType, resolvedTypes);
    } catch (Exception e) {
      throw new SerializationException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static void deserialize(ServerSerializationStreamReader streamReader,
      TypeCheckedGenericClass instance) throws SerializationException {
    Object junkKey = streamReader.readObject();
    Object junkValue = streamReader.readObject();

    /*
     * If deserializing a superclass we will not have been instantiated using
     * the custom instantiator, so skip the checks for correct markers.
     */
    if (instance.getClass() != TypeCheckedGenericClass.class
        || ((instance.getMarkerKey() instanceof Integer)
            && ((Integer) instance.getMarkerKey()).intValue() == 54321
            && (instance.getMarkerValue() instanceof String) && ((String) instance.getMarkerValue())
            .equals("LocalMarker"))) {
      instance.setMarker(TypeCheckedObjectsTestSetValidator.markerKey,
          TypeCheckedObjectsTestSetValidator.markerValue);
    } else {
      throw new SerializationException(
          "Incorrect markers in TypeCheckedGenericClass server deserialization. "
              + "Custom instantiate probably not called.");
    }

    instance.hashField = (HashMap) streamReader.readObject();
  }

  public static TypeCheckedGenericClass instantiateChecked(
      ServerSerializationStreamReader streamReader, Type[] expectedParameterTypes,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) {
    TypeCheckedGenericClass<Integer, String> result =
        new TypeCheckedGenericClass<Integer, String>();
    result.setMarker(54321, "LocalMarker");
    return result;
  }

  public static TypeCheckedGenericClass instantiate(
      ServerSerializationStreamReader streamReader) {
    TypeCheckedGenericClass<Integer, String> result =
        new TypeCheckedGenericClass<Integer, String>();
    result.setMarker(54321, "LocalMarker");
    return result;
  }
}