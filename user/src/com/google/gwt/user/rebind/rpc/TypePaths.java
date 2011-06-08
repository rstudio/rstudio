/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;

/**
 * Paths of types and utility methods for creating them. These are used by
 * {@link SerializableTypeOracleBuilder} to record why it visits the types it
 * does.
 */
class TypePaths {
  /**
   * A path of types. This interface does not currently expose the type itself,
   * because these are currently only used for logging.
   */
  interface TypePath {
    /**
     * Get the previous element on this type path, or <code>null</code> if this
     * is a one-element path.
     */
    TypePath getParent();

    String toString();
  }

  static TypePaths.TypePath createArrayComponentPath(final JArrayType arrayType,
      final TypePaths.TypePath parent) {
    assert (arrayType != null);

    return new TypePaths.TypePath() {
      public TypePaths.TypePath getParent() {
        return parent;
      }

      @Override
      public String toString() {
        return "Type '" + arrayType.getComponentType().getParameterizedQualifiedSourceName()
            + "' is reachable from array type '" + arrayType.getParameterizedQualifiedSourceName()
            + "'";
      }
    };
  }

  static TypePaths.TypePath createFieldPath(final TypePaths.TypePath parent, final JField field) {
    return new TypePaths.TypePath() {
      public TypePaths.TypePath getParent() {
        return parent;
      }

      @Override
      public String toString() {
        JType type = field.getType();
        JClassType enclosingType = field.getEnclosingType();
        return "'" + type.getParameterizedQualifiedSourceName() + "' is reachable from field '"
            + field.getName() + "' of type '" + enclosingType.getParameterizedQualifiedSourceName()
            + "'";
      }
    };
  }

  static TypePaths.TypePath createRootPath(final JType type) {
    assert (type != null);

    return new TypePaths.TypePath() {
      public TypePaths.TypePath getParent() {
        return null;
      }

      @Override
      public String toString() {
        return "Started from '" + type.getParameterizedQualifiedSourceName() + "'";
      }
    };
  }

  static TypePaths.TypePath createSubtypePath(final TypePaths.TypePath parent, final JType type,
      final JClassType supertype) {
    assert (type != null);
    assert (supertype != null);

    return new TypePaths.TypePath() {
      public TypePaths.TypePath getParent() {
        return parent;
      }

      @Override
      public String toString() {
        return "'" + type.getParameterizedQualifiedSourceName()
            + "' is reachable as a subtype of type '" + supertype + "'";
      }
    };
  }

  static TypePaths.TypePath createSupertypePath(final TypePaths.TypePath parent, final JType type,
      final JClassType subtype) {
    assert (type != null);
    assert (subtype != null);

    return new TypePaths.TypePath() {
      public TypePaths.TypePath getParent() {
        return parent;
      }

      @Override
      public String toString() {
        return "'" + type.getParameterizedQualifiedSourceName()
            + "' is reachable as a supertype of type '" + subtype + "'";
      }
    };
  }

  static TypePaths.TypePath createTypeArgumentPath(final TypePaths.TypePath parent,
      final JGenericType baseType, final int typeArgIndex, final JClassType typeArg) {
    assert (baseType != null);
    assert (typeArg != null);

    return new TypePaths.TypePath() {
      public TypePaths.TypePath getParent() {
        return parent;
      }

      @Override
      public String toString() {
        return "'" + typeArg.getParameterizedQualifiedSourceName()
            + "' is reachable from type argument " + typeArgIndex + " of type '"
            + baseType.getParameterizedQualifiedSourceName() + "'";
      }
    };
  }

  static TypePaths.TypePath createTypeParameterInRootPath(final TypePaths.TypePath parent,
      final JTypeParameter typeParameter) {
    assert (typeParameter != null);

    return new TypePaths.TypePath() {
      public TypePaths.TypePath getParent() {
        return parent;
      }

      @Override
      public String toString() {
        String parameterString = typeParameter.getName();
        if (typeParameter.getDeclaringClass() != null) {
          parameterString +=
              " of class " + typeParameter.getDeclaringClass().getQualifiedSourceName();
        }
        return "'" + typeParameter.getFirstBound().getParameterizedQualifiedSourceName()
            + "' is reachable as an upper bound of type parameter " + parameterString
            + ", which appears in a root type";
      }
    };
  }
}
