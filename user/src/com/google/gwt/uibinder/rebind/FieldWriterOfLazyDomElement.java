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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.uibinder.rebind.model.OwnerField;

/**
 * Implementation of FieldWriter for a {@link com.google.gwt.uibinder.client.LazyDomElement}.
 */
public class FieldWriterOfLazyDomElement extends AbstractFieldWriter {

  /**
   * The field type for @UiField LazyDomElement&lt;T&gt;.
   */
  private final JParameterizedType ownerFieldType;

  /**
   * The T parameter in LazyDomElement&lt;T&gt;.
   */
  private final JClassType parameterType;

  public FieldWriterOfLazyDomElement(FieldManager manager, JClassType templateFieldType,
      OwnerField ownerField, MortalLogger logger) throws UnableToCompleteException {
    super(manager, FieldWriterType.DEFAULT, ownerField.getName(), logger);

    // ownerFieldType null means LazyDomElement is not parameterized.
    this.ownerFieldType = ownerField.getRawType().isParameterized();
    if (ownerFieldType == null) {
      logger.die("LazyDomElement must be of type LazyDomElement<? extends Element>.");
    }

    // Parameterized LazyDomElement<T> must match its respective html element.
    // Example:
    //  DivElement -> div
    //  SpanElement -> span
    parameterType = ownerFieldType.getTypeArgs()[0];
    if (!templateFieldType.isAssignableTo(parameterType)) {
      logger.die("Field %s is %s<%s>, must be %s<%s>.", ownerField.getName(),
          ownerFieldType.getQualifiedSourceName(), parameterType,
          ownerFieldType.getQualifiedSourceName(), templateFieldType);
    }
  }

  public JClassType getAssignableType() {
    return ownerFieldType;
  }

  public JClassType getInstantiableType() {
    return ownerFieldType;
  }

  public String getQualifiedSourceName() {
    return ownerFieldType.getQualifiedSourceName()
        + "<" + parameterType.getQualifiedSourceName() + ">";
  }
}
