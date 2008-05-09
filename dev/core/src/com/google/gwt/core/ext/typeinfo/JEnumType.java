/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Type representing a Java enumerated type.
 */
public class JEnumType extends JRealClassType {
  private JEnumConstant[] lazyEnumConstants;

  /**
   * @param oracle
   * @param cup
   * @param declaringPackage
   * @param enclosingType
   * @param isLocalType
   * @param name
   * @param declStart
   * @param declEnd
   * @param bodyStart
   * @param bodyEnd
   * @param isInterface
   */
  public JEnumType(TypeOracle oracle, JPackage declaringPackage,
      JClassType enclosingType, boolean isLocalType, String name,
      boolean isInterface) {
    super(oracle, declaringPackage, enclosingType, isLocalType, name,
        isInterface);
  }

  /**
   * Returns the enumeration constants declared by this enumeration.
   * 
   * @return enumeration constants declared by this enumeration
   */
  public JEnumConstant[] getEnumConstants() {
    if (lazyEnumConstants == null) {
      List<JEnumConstant> enumConstants = new ArrayList<JEnumConstant>();
      for (JField field : getFields()) {
        if (field.isEnumConstant() != null) {
          enumConstants.add(field.isEnumConstant());
        }
      }

      lazyEnumConstants = enumConstants.toArray(new JEnumConstant[0]);
    }

    return lazyEnumConstants;
  }

  @Override
  public JEnumType isEnum() {
    return this;
  }
}
