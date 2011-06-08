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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Validates the asynchronous version of
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface.
 */
class RemoteServiceAsyncValidator {
  static void logValidAsyncInterfaceDeclaration(TreeLogger logger, JClassType remoteService) {
    TreeLogger branch =
        logger.branch(TreeLogger.INFO,
            "A valid definition for the asynchronous version of interface '"
                + remoteService.getQualifiedSourceName() + "' would be:\n", null);
    branch.log(TreeLogger.ERROR, synthesizeAsynchronousInterfaceDefinition(remoteService), null);
  }

  private static String computeAsyncMethodSignature(JMethod syncMethod,
      JClassType asyncCallbackClass) {
    return computeInternalSignature(syncMethod) + "/" + asyncCallbackClass.getQualifiedSourceName();
  }

  private static String computeInternalSignature(JMethod method) {
    StringBuffer sb = new StringBuffer();
    sb.setLength(0);
    sb.append(method.getName());
    JParameter[] params = method.getParameters();
    for (JParameter param : params) {
      sb.append("/");
      JType paramType = param.getType();
      sb.append(paramType.getErasedType().getQualifiedSourceName());
    }
    return sb.toString();
  }

  /**
   * Builds a map of asynchronous method internal signatures to the
   * corresponding asynchronous {@link JMethod}.
   */
  private static Map<String, JMethod> initializeAsyncMethodMap(JMethod[] asyncMethods) {
    Map<String, JMethod> sigs = new TreeMap<String, JMethod>();
    for (JMethod asyncMethod : asyncMethods) {
      sigs.put(computeInternalSignature(asyncMethod), asyncMethod);
    }
    return sigs;
  }

  private static String synthesizeAsynchronousInterfaceDefinition(JClassType serviceIntf) {
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

    JMethod[] methods = serviceIntf.getOverridableMethods();
    for (JMethod method : methods) {
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

      JType returnType = method.getReturnType();
      sb.append(AsyncCallback.class.getName());
      sb.append("<");
      if (returnType instanceof JPrimitiveType) {
        sb.append(((JPrimitiveType) returnType).getQualifiedBoxedSourceName());
      } else {
        sb.append(returnType.getParameterizedQualifiedSourceName());
      }
      sb.append("> arg");
      sb.append(Integer.toString(params.length + 1));
      sb.append(");\n");
    }

    sb.append("}");

    return sb.toString();
  }

  private static void validationFailed(TreeLogger branch, JClassType remoteService)
      throws UnableToCompleteException {
    logValidAsyncInterfaceDeclaration(branch, remoteService);
    throw new UnableToCompleteException();
  }

  /**
   * {@link JClassType} for the {@link AsyncCallback} interface.
   */
  private final JClassType asyncCallbackClass;

  /**
   * {@link JClassType} for the {@link RequestBuilder} class.
   */
  private final JClassType requestBuilderType;

  /**
   * {@link JClassType} for the {@link Request} class.
   */
  private final JClassType requestType;

  RemoteServiceAsyncValidator(TreeLogger logger, TypeOracle typeOracle)
      throws UnableToCompleteException {
    try {
      asyncCallbackClass = typeOracle.getType(AsyncCallback.class.getName());
      requestType = typeOracle.getType(Request.class.getCanonicalName());
      requestBuilderType = typeOracle.getType(RequestBuilder.class.getCanonicalName());
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, null, e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Checks that for every method on the synchronous remote service interface
   * there is a corresponding asynchronous version in the asynchronous version
   * of the remote service. If the validation succeeds, a map of synchronous to
   * asynchronous methods is returned.
   * 
   * @param logger
   * @param remoteService
   * @return map of synchronous method to asynchronous method
   * 
   * @throws UnableToCompleteException if the asynchronous
   *           {@link com.google.gwt.user.client.rpc.RemoteService
   *           RemoteService} was not found, or if it does not have an
   *           asynchronous method version of every synchronous one
   */
  public Map<JMethod, JMethod> validate(TreeLogger logger, JClassType remoteService,
      JClassType remoteServiceAsync) throws UnableToCompleteException {
    TreeLogger branch =
        logger.branch(TreeLogger.DEBUG, "Checking the synchronous interface '"
            + remoteService.getQualifiedSourceName() + "' against its asynchronous version '"
            + remoteServiceAsync.getQualifiedSourceName() + "'", null);

    // Sync and async versions must have the same number of methods
    JMethod[] asyncMethods = remoteServiceAsync.getOverridableMethods();
    JMethod[] syncMethods = remoteService.getOverridableMethods();
    if (asyncMethods.length != syncMethods.length) {
      branch.branch(TreeLogger.ERROR, "The asynchronous version of "
          + remoteService.getQualifiedSourceName() + " has "
          + (asyncMethods.length > syncMethods.length ? "more" : "less")
          + " methods than the synchronous version", null);
      validationFailed(branch, remoteService);
    }

    // Check that for every sync method there is a corresponding async method
    boolean failed = false;
    Map<String, JMethod> asyncMethodMap = initializeAsyncMethodMap(asyncMethods);
    Map<JMethod, JMethod> syncMethodToAsyncMethodMap = new HashMap<JMethod, JMethod>();
    for (JMethod syncMethod : syncMethods) {
      String asyncSig = computeAsyncMethodSignature(syncMethod, asyncCallbackClass);
      JMethod asyncMethod = asyncMethodMap.get(asyncSig);
      if (asyncMethod == null) {
        branch.branch(TreeLogger.ERROR, "Missing asynchronous version of the synchronous method '"
            + syncMethod.getReadableDeclaration() + "'", null);
        failed = true;
      } else {
        // TODO if async param is parameterized make sure that the sync return
        // type is assignable to the first type argument
        JType returnType = asyncMethod.getReturnType();
        if (returnType != JPrimitiveType.VOID && returnType != requestType
            && returnType != requestBuilderType) {
          branch.branch(TreeLogger.ERROR, "The asynchronous version of the synchronous method '"
              + syncMethod.getReadableDeclaration() + "' must have a return type of 'void' or '"
              + Request.class.getCanonicalName() + "' or '"
              + RequestBuilder.class.getCanonicalName() + "'", null);
          failed = true;
        } else {
          syncMethodToAsyncMethodMap.put(syncMethod, asyncMethod);
        }
      }
    }

    if (failed) {
      validationFailed(branch, remoteService);
    }

    branch.log(TreeLogger.DEBUG, "Interfaces are in sync");

    return syncMethodToAsyncMethodMap;
  }
}
