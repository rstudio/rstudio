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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Validates the asynchronous version of
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface.
 */
class RemoteServiceAsyncValidator {
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

        sb.append(param.toString());
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
  private final JClassType asyncCallbackClass;

  private final TypeOracle typeOracle;

  RemoteServiceAsyncValidator(TypeOracle typeOracle) throws NotFoundException {
    this.typeOracle = typeOracle;
    asyncCallbackClass = typeOracle.getType(AsyncCallback.class.getName());
  }

  public void validateRemoteServiceAsync(TreeLogger logger,
      JClassType remoteService) throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG,
        "Checking the synchronous interface '"
            + remoteService.getQualifiedSourceName()
            + "' against its asynchronous version '"
            + remoteService.getQualifiedSourceName() + "Async'", null);

    JClassType remoteServiceAsync = typeOracle.findType(remoteService.getQualifiedSourceName()
        + "Async");
    boolean failed = false;
    if (remoteServiceAsync == null) {
      logger.branch(TreeLogger.ERROR,
          "Could not find an asynchronous version for the service interface "
              + remoteService.getQualifiedSourceName(), null);
      failed = true;
    } else {
      JMethod[] syncMethods = remoteService.getOverridableMethods();
      JMethod[] asyncMethods = remoteServiceAsync.getOverridableMethods();

      if (syncMethods.length != asyncMethods.length) {
        logger.branch(TreeLogger.ERROR, "The asynchronous version of "
            + remoteService.getQualifiedSourceName() + " has "
            + (asyncMethods.length > syncMethods.length ? "more" : "less")
            + " methods than the synchronous version", null);
        failed = true;
      } else {
        for (int i = 0; i < syncMethods.length; ++i) {
          JMethod method = syncMethods[i];

          JMethod asyncMethod = remoteServiceAsync.findMethod(method.getName(),
              getAsyncParamTypes(method));

          if (asyncMethod == null) {
            logger.branch(TreeLogger.ERROR,
                "Missing asynchronous version of the synchronous method '"
                    + method.getReadableDeclaration() + "'", null);
            failed = true;
          } else if (asyncMethod.getReturnType() != JPrimitiveType.VOID) {
            logger.branch(TreeLogger.ERROR,
                "The asynchronous version of the synchronous method '"
                    + method.getReadableDeclaration()
                    + "' must have a void return type", null);
            failed = true;
          }
        }
      }
    }

    if (failed) {
      logValidAsyncInterfaceDeclaration(logger, remoteService);
      throw new UnableToCompleteException();
    }
  }

  private JType[] getAsyncParamTypes(JMethod method) {
    JParameter[] params = method.getParameters();
    JType[] asyncParamTypes = new JType[params.length + 1];

    for (int index = 0; index < params.length; ++index) {
      asyncParamTypes[index] = getRawType(params[index].getType());
    }

    asyncParamTypes[params.length] = asyncCallbackClass;

    return asyncParamTypes;
  }

  private JType getRawType(JType type) {
    JParameterizedType parameterized = type.isParameterized();
    if (parameterized != null) {
      return getRawType(parameterized.getRawType());
    }

    JArrayType arrayType = type.isArray();
    if (arrayType != null) {
      return typeOracle.getArrayType(getRawType(arrayType.getComponentType()));
    }

    return type;
  }

  private void logValidAsyncInterfaceDeclaration(TreeLogger logger,
      JClassType remoteService) {
    logger = logger.branch(TreeLogger.INFO,
        "A valid definition for an asynchronous version of interface '"
            + remoteService.getQualifiedSourceName() + "' would be:\n", null);
    logger.log(TreeLogger.ERROR,
        synthesizeAsynchronousInterfaceDefinition(remoteService), null);
  }
}