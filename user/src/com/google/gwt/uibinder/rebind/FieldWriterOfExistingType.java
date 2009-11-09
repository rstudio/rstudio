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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * Implementation of FieldWriter for fields whose type already exists (that is,
 * for whom we have a {@link JClassType}.
 */
class FieldWriterOfExistingType extends AbstractFieldWriter {
  final JClassType type;
  final MortalLogger logger;

  FieldWriterOfExistingType(JClassType type, String name, MortalLogger logger) {
    super(name, logger);
    this.logger = logger;
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    this.type = type;
  }

  public String getQualifiedSourceName() {
    return type.getQualifiedSourceName();
  }

  public JClassType getType() {
    return type;
  }
}
