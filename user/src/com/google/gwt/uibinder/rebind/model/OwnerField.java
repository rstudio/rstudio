/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderContext;

/**
 * Descriptor for a field of the owner class.
 * <p>
 * Please notice that some fields defined in the XML and in the generated binder
 * class may not be present in the owner class - for instance, they may not be
 * relevant to the code of the owner class.
 * The fields in the binder class are instead represented by an instance of
 * {@link com.google.gwt.uibinder.rebind.FieldWriter}.
 */
public class OwnerField {
  private final String name;
  private final OwnerFieldClass fieldType;
  private final boolean isProvided;

  /**
   * Constructor.
   *
   * @param field the field of the owner class
   * @param logger
   * @param context 
   */
  public OwnerField(JField field, MortalLogger logger, UiBinderContext context)
      throws UnableToCompleteException {
    this.name = field.getName();

    // Get the field type and ensure it's a class or interface
    JClassType fieldClassType = field.getType().isClassOrInterface();

    if (fieldClassType == null) {
      logger.die("Type for field " + name + " is not a class: "
          + field.getType().getSimpleSourceName());
    }

    this.fieldType = OwnerFieldClass.getFieldClass(fieldClassType, logger,
        context);

    // Get the UiField annotation and process it
    UiField annotation = field.getAnnotation(UiField.class);

    if (annotation == null) {
      logger.die("Field " + name + " is not annotated with @UiField");
    }

    isProvided = annotation.provided();
  }

  /**
   * Returns the name of the field in the owner class.
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the type associated with this field.
   */
  public JClassType getRawType() {
    // This shorten getType().getRawType() and make tests easier.
    return getType().getRawType();
  }

  /**
   * Returns a descriptor for the type of the field.
   */
  public OwnerFieldClass getType() {
    return fieldType;
  }

  /**
   * Returns whether this field's value is provided by owner class.
   * If it's not provided, then it's the binder's responsibility to assign it.
   */
  public boolean isProvided() {
    return isProvided;
  }

  @Override
  public String toString() {
    return String.format("%s.%s#%s", fieldType.getRawType().getPackage(),
        fieldType.getRawType().getName(), name);
  }
}
