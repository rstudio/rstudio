/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This class implements the validation policy for a service interface.
 */
final class ServiceInterfaceValidator {
  private static final String ASYNC_INTERFACE_SUFFIX = "Async";

  private static JType[] getAsyncParamTypes(JMethod method,
      TypeOracle typeOracle) throws TypeOracleException {
    JParameter[] params = method.getParameters();
    JType[] asyncParamTypes = new JType[params.length + 1];

    for (int index = 0; index < params.length; ++index) {
      asyncParamTypes[index] = getUnparameterizedType(typeOracle,
          params[index].getType());
    }

    asyncParamTypes[params.length] = typeOracle.findType(AsyncCallback.class.getName());
    if (asyncParamTypes[params.length] == null) {
      throw new IllegalStateException("Unable to locate definition of "
          + AsyncCallback.class.getName());
    }

    return asyncParamTypes;
  }

  private static JType getUnparameterizedType(TypeOracle typeOracle, JType type)
      throws TypeOracleException {
    if (type.isParameterized() != null) {
      return type.getLeafType();
    }

    JArrayType arrayType = type.isArray();
    if (arrayType != null) {
      int rank = arrayType.getRank();

      JType componentType = arrayType.getComponentType();
      if (componentType.isParameterized() != null) {
        type = getUnparameterizedType(typeOracle, componentType);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < rank; ++i) {
          sb.append("[]");
        }

        return typeOracle.parse(type.getQualifiedSourceName() + sb.toString());
      }
    }

    return type;
  }

  private static void raiseInvalidAsyncIntf(TreeLogger logger,
      JClassType serviceIntf) {
    logger = logger.branch(TreeLogger.ERROR,
        "No valid asynchronous version for the service interface "
            + serviceIntf.getQualifiedSourceName(), null);
    String correctAsyncIntf = synthesizeAsynchronousInterfaceDefinition(serviceIntf);
    logger.log(TreeLogger.INFO,
        "A valid definition for an asychronous version of interface "
            + serviceIntf.getQualifiedSourceName() + " would be:\n"
            + correctAsyncIntf, null);
  }

  private static String synthesizeAsynchronousInterfaceDefinition(
      JClassType serviceIntf) {
    StringBuffer sb = new StringBuffer();
    JPackage pkg = serviceIntf.getPackage();
    if (pkg != null) {
      sb.append("\npackage ");
      sb.append(pkg.getName());
      sb.append(";\n");
    }

    sb.append("\npublic interface ");
    sb.append(serviceIntf.getSimpleSourceName());
    sb.append("Async {\n");

    JMethod[] methods = serviceIntf.getMethods();
    for (int index = 0; index < methods.length; ++index) {
      JMethod method = methods[index];
      assert (method != null);

      sb.append("\tvoid ");
      sb.append(method.getName());
      sb.append("(");

      JParameter[] params = method.getParameters();
      for (int paramIndex = 0; paramIndex < params.length; ++paramIndex) {
        JParameter param = params[paramIndex];

        if (paramIndex > 0) {
          sb.append(", ");
        }

        sb.append(param.getType().getSimpleSourceName());
        sb.append(" ");
        sb.append(param.getName());
      }

      if (params.length > 0) {
        sb.append(", ");
      }

      sb.append(AsyncCallback.class.getName());
      sb.append(" arg");
      sb.append(Integer.toString(params.length + 1));
      sb.append(");\n");
    }

    sb.append("}");

    return sb.toString();
  }

  private JClassType objectType;

  private TreeLogger rootLogger;

  private SerializableTypeOracle serializationOracle;

  private JClassType serviceIntf;

  private TypeOracle typeOracle;

  private Map typeToFinalInstanceFieldCache = new IdentityHashMap();

  public ServiceInterfaceValidator(TreeLogger logger, GeneratorContext genCtx,
      SerializableTypeOracle serializationOracle, JClassType serviceIntf) {
    this.rootLogger = logger;
    this.serializationOracle = serializationOracle;
    this.serviceIntf = serviceIntf;
    typeOracle = genCtx.getTypeOracle();

    this.objectType = typeOracle.getJavaLangObject();
    if (this.objectType == null) {
      logger.log(TreeLogger.ERROR,
          "Could not find a definition of java.lang.Object", null);
      throw new RuntimeException();
    }
  }

  /**
   * Perform a set of consistency checks on the service interface.
   * 
   * @return true if the service interface is valid, false otherwise
   */
  public boolean isValid() throws TypeOracleException {
    TreeLogger logger = rootLogger.branch(TreeLogger.SPAM,
        "Validating service interface '" + serviceIntf.getQualifiedSourceName()
            + "'", null);

    typeToFinalInstanceFieldCache.clear();

    if (!validServiceInterface(logger)) {
      return false;
    }

    if (!validAsyncServiceInterface(logger)) {
      return false;
    }

    return true;
  }

  /*
   * Returns true if the type has non-static, final fields.
   */
  private JField findFirstInvalidFinalField(JClassType classType) {
    if (typeToFinalInstanceFieldCache.containsKey(classType)) {
      return (JField) typeToFinalInstanceFieldCache.get(classType);
    }

    JField firstFinalInstField = null;
    JField[] declFields = classType.getFields();
    for (int i = 0; i < declFields.length; ++i) {
      JField declField = declFields[i];

      if (declField.isFinal() && !declField.isStatic()
          && !declField.isTransient()) {
        // we found an invalid final instance field
        firstFinalInstField = declField;
        break;
      }
    }

    if (firstFinalInstField == null) {
      // Check the superclass if no final instance fields have been found at
      // this point
      JClassType superClass = classType.getSuperclass();
      if (superClass != null) {
        // check out superclass
        firstFinalInstField = findFirstInvalidFinalField(superClass);
      }
    }

    typeToFinalInstanceFieldCache.put(classType, firstFinalInstField);

    return firstFinalInstField;
  }

  private String getAsyncInterfaceQualifiedName() {
    return serviceIntf.getQualifiedSourceName() + ASYNC_INTERFACE_SUFFIX;
  }

  private boolean hasCustomInstantiation(JClassType classType) {
    JMethod instantiateMethod = serializationOracle.getCustomFieldSerializerInstantiateMethodForType(classType);
    if (instantiateMethod != null) {
      return true;
    }

    return false;
  }

  private void logSerializableArgumentTypes(TreeLogger logger,
      JParameter param, JType[] serializableTypes) {
    logger = logger.branch(TreeLogger.SPAM, "Argument '" + param.getName()
        + "' of type '" + param.getType().getQualifiedSourceName()
        + "' can only be serialized as: ", null);
    logSerializableTypes(logger, serializableTypes);
  }

  private void logSerializableExceptionTypes(TreeLogger logger, JType ex,
      JType[] serializableTypes) {
    logger = logger.branch(TreeLogger.SPAM, "Exception type "
        + ex.getQualifiedSourceName()
        + " can only be one of the following serializable types: ", null);
    logSerializableTypes(logger, serializableTypes);
  }

  private void logSerializableReturnTypes(TreeLogger logger, JType returnType,
      JType[] serializableTypes) {
    logger = logger.branch(TreeLogger.SPAM, "Return type "
        + returnType.getQualifiedSourceName()
        + " can only be one of the following serializable types: ", null);
    logSerializableTypes(logger, serializableTypes);
  }

  private void logSerializableTypes(TreeLogger logger, JType[] serializableTypes) {
    for (int index = 0; index < serializableTypes.length; ++index) {
      JType type = serializableTypes[index];
      assert (type != null);

      JClassType classType = type.isClass();
      if (classType != null) {
        if (classType.isAbstract()) {
          // Abstract types are not logged since they are not concrete types
          continue;
        }

        TreeLogger branchedLog = logger.branch(TreeLogger.SPAM,
            classType.getParameterizedQualifiedSourceName(), null);

        JField problematicField = findFirstInvalidFinalField(classType);
        if (problematicField != null && !hasCustomInstantiation(classType)) {
          JType enclosingType = problematicField.getEnclosingType();

          String warningMessage = "The field '" + problematicField.toString()
              + "'";
          if (enclosingType != classType) {
            warningMessage += ", inherited from '"
                + enclosingType.getParameterizedQualifiedSourceName() + "',";
          }

          warningMessage += " will not be included because it is a 'final' instance field that is not also 'transient'";

          // We issue a warning here because final instance fields are not
          // handled by the serialization subsystem.
          //
          branchedLog.branch(TreeLogger.WARN, warningMessage, null);
        }
      }
    }
  }

  private void raiseInvalidArgumentType(TreeLogger logger, JParameter param) {
    if (param.getType() == objectType) {
      logger.branch(
          TreeLogger.ERROR,
          "In order to produce smaller client-side code, methods cannot specify 'Object' for parameter types; please choose a more specific parameter type",
          null);
    } else {
      logger.branch(TreeLogger.ERROR, "Parameter '" + param.getName()
          + "' of type '"
          + param.getType().getParameterizedQualifiedSourceName()
          + "' is not serializable and/or has no serializable subtypes", null);
    }
  }

  private void raiseInvalidExceptionType(TreeLogger logger, JType ex) {
    logger.branch(TreeLogger.ERROR, "Exception type "
        + ex.getQualifiedSourceName()
        + " is not serializable and/or has no serializable subtypes", null);
  }

  private void raiseInvalidReturnType(TreeLogger logger, JType returnType) {
    if (returnType == objectType) {
      logger.branch(
          TreeLogger.ERROR,
          "In order to produce smaller client-side code, methods cannot specify 'Object' for return types; please choose a more specific return type",
          null);
    } else {
      logger.branch(TreeLogger.ERROR, "Return type "
          + returnType.getParameterizedQualifiedSourceName()
          + " is not serializable and/or has no serializable subtypes", null);
    }
  }

  /**
   * Check that thee is an asynchronous serivce interface associated with the
   * synchronous version. Also check that for every method on the synchronous
   * service interface there exists an asynchrous version of that method on the
   * asynchrounous interface.
   * 
   * @return true if the asynchronous interface definition is valid, false
   *         otherwise. If the asynchronous service interface is invalid then a
   *         correct definition is written to the log file.
   */
  private boolean validAsyncServiceInterface(TreeLogger logger)
      throws TypeOracleException {
    String asyncIntfQualifiedName = getAsyncInterfaceQualifiedName();
    JClassType asyncServiceIntf = typeOracle.findType(asyncIntfQualifiedName);
    if (asyncServiceIntf == null) {
      raiseInvalidAsyncIntf(logger, serviceIntf);
      return false;
    }

    boolean failed = false;
    JMethod[] methods = serviceIntf.getMethods();
    for (int index = 0; index < methods.length; ++index) {
      JMethod method = methods[index];
      assert (method != null);

      JType[] asyncParamTypes = getAsyncParamTypes(method, typeOracle);
      JMethod asyncMethod = asyncServiceIntf.findMethod(method.getName(),
          asyncParamTypes);
      if (asyncMethod == null) {
        logger.branch(TreeLogger.ERROR,
            "No asynchronous version of the synchronous method "
                + method.getReadableDeclaration(), null);
        failed = true;
      }
    }

    if (failed) {
      raiseInvalidAsyncIntf(logger, serviceIntf);
    }
    return !failed;
  }

  /**
   * Check that the types used in the service interface are serializable or have
   * a serializable subtype.
   * 
   * @param logger
   * @param classOrInterface
   */
  private boolean validInterface(TreeLogger logger, JClassType classOrInterface) {
    boolean failed = false;

    logger = logger.branch(TreeLogger.SPAM, "Service interface: "
        + classOrInterface.getQualifiedSourceName(), null);

    JClassType intfs[] = classOrInterface.getImplementedInterfaces();
    for (int index = 0; index < intfs.length; ++index) {
      JClassType intf = intfs[index];

      if (!validInterface(logger, intf)) {
        failed = true;
      }
    }

    JMethod[] methods = classOrInterface.getMethods();
    for (int index = 0; index < methods.length; ++index) {
      JMethod method = methods[index];

      if (!validMethod(logger, method)) {
        failed = true;
      }
    }

    return !failed;
  }

  /*
   * Check that all of the types reachable from a method are actually
   * serializable.
   */
  private boolean validMethod(TreeLogger logger, JMethod method) {
    logger = logger.branch(TreeLogger.SPAM, "Service method: "
        + method.getReadableDeclaration(), null);
    boolean failed = false;
    JType returnType = method.getReturnType();
    if (returnType != null && returnType != JPrimitiveType.VOID) {
      JType[] serializableTypes = serializationOracle.getSerializableTypesAssignableTo(returnType);
      if (!validSerializableTypes(serializableTypes)) {
        failed = true;
        raiseInvalidReturnType(logger, returnType);
      } else {
        logSerializableReturnTypes(logger, returnType, serializableTypes);
      }
    }

    JParameter[] params = method.getParameters();
    for (int paramIndex = 0; paramIndex < params.length; ++paramIndex) {
      JParameter param = params[paramIndex];

      JType paramType = param.getType();
      JType[] serializableTypes = serializationOracle.getSerializableTypesAssignableTo(paramType);
      if (!validSerializableTypes(serializableTypes)) {
        failed = true;
        raiseInvalidArgumentType(logger, param);
      } else {
        logSerializableArgumentTypes(logger, param, serializableTypes);
      }
    }

    JType[] exs = method.getThrows();
    for (int throwsIndex = 0; throwsIndex < exs.length; ++throwsIndex) {
      JType ex = exs[throwsIndex];
      JType[] serializableTypes = serializationOracle.getSerializableTypesAssignableTo(ex);
      if (!validSerializableTypes(serializableTypes)) {
        failed = true;
        raiseInvalidExceptionType(logger, ex);
      } else {
        logSerializableExceptionTypes(logger, ex, serializableTypes);
      }
    }

    return !failed;
  }

  /**
   * Return true if at least one of the items in the list is serializable.
   * 
   * @param serializableTypes
   */
  private boolean validSerializableTypes(JType[] serializableTypes) {
    if (serializableTypes.length == 0) {
      return false;
    }

    for (int index = 0; index < serializableTypes.length; ++index) {
      JType type = serializableTypes[index];
      assert (type != null);

      if (type.isPrimitive() != null) {
        return true;
      }

      JClassType classType = type.isClass();
      if (classType != null && !classType.isAbstract()) {
        return true;
      }

      JArrayType arrayType = type.isArray();
      if (arrayType != null) {
        if (serializationOracle.isSerializable(arrayType.getComponentType())) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean validServiceInterface(TreeLogger logger) {
    boolean failed = false;

    logger = logger.branch(TreeLogger.SPAM, "Service interface: "
        + serviceIntf.getQualifiedSourceName(), null);

    JClassType intfs[] = serviceIntf.getImplementedInterfaces();
    for (int index = 0; index < intfs.length; ++index) {
      JClassType intf = intfs[index];

      if (!validInterface(logger, intf)) {
        failed = true;
      }
    }

    JMethod[] methods = serviceIntf.getMethods();
    for (int index = 0; index < methods.length; ++index) {
      JMethod method = methods[index];

      if (!validMethod(logger, method)) {
        failed = true;
      }
    }

    return !failed;
  }
}
