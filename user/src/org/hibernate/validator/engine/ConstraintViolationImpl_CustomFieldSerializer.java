/*
 * Copyright 2010 Google Inc.
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
package org.hibernate.validator.engine;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.lang.annotation.ElementType;

import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * Custom Serializer for {@link ConstraintViolationImpl}.
 */
@SuppressWarnings("rawtypes")
public class ConstraintViolationImpl_CustomFieldSerializer extends
    CustomFieldSerializer<ConstraintViolationImpl> {

  @SuppressWarnings("unused")
  public static void deserialize(SerializationStreamReader streamReader,
      ConstraintViolationImpl instance) throws SerializationException {
    // no fields
  }

  public static ConstraintViolationImpl<Object> instantiate(
      SerializationStreamReader streamReader) throws SerializationException {

    String messageTemplate = null;
    String interpolatedMessage = streamReader.readString();
    Class<Object> rootBeanClass = null;
    Object rootBean = null;
    Object leafBeanInstance = null;
    Object value = null;
    Path propertyPath = (Path) streamReader.readObject();
    ConstraintDescriptor<?> constraintDescriptor = null;
    ElementType elementType = null;
    return new ConstraintViolationImpl<Object>(messageTemplate,
        interpolatedMessage, rootBeanClass, rootBean, leafBeanInstance, value,
        propertyPath, constraintDescriptor, elementType);
  }

  /**
   * Only a subset of fields are actually serialized.
   * <p/>
   * There is no guarantee that the root bean is GWT-serializable or that it's
   * appropriate for it to be exposed on the client. Even if the root bean could
   * be sent back, the lack of reflection on the client makes it troublesome to
   * interpret the path as a sequence of property accesses.
   * <p/>
   * The current implementation is the simplest-to-implement properties.
   * <ol>
   * <li>Message</li>
   * <li>Property Path</li>
   * </ol>
   */
  public static void serialize(SerializationStreamWriter streamWriter,
      ConstraintViolationImpl instance) throws SerializationException {

    // streamWriter.writeString(instance.getMessageTemplate());
    streamWriter.writeString(instance.getMessage());
    // streamWriter.writeObject(instance.getRootBeanClass());
    // streamWriter.writeObject(instance.getRootBean());
    // streamWriter.writeObject(instance.getLeafBean());
    // streamWriter.writeObject(instance.getInvalidValue());
    streamWriter.writeObject(instance.getPropertyPath());
    // streamWriter.writeObject(instance.getConstraintDescriptor());
    // ElementType
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader,
      ConstraintViolationImpl instance) throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public ConstraintViolationImpl instantiateInstance(
      SerializationStreamReader streamReader) throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter,
      ConstraintViolationImpl instance) throws SerializationException {
    serialize(streamWriter, instance);
  }
}
