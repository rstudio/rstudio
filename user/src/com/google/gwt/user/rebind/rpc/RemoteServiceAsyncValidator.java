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
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.Map;
import java.util.TreeMap;

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

  RemoteServiceAsyncValidator(TreeLogger logger, TypeOracle typeOracle)
      throws UnableToCompleteException {
    this.typeOracle = typeOracle;
    try {
      asyncCallbackClass = typeOracle.getType(AsyncCallback.class.getName());
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, null, e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Checks that for there is an asynchronous
   * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}
   * interface and that it has an asynchronous version of every synchronous
   * method.
   * 
   * @throws UnableToCompleteException if the asynchronous
   *           {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}
   *           was not found, or if it does not have an asynchronous method
   *           version of every synchronous one
   */
  public void validateRemoteServiceAsync(TreeLogger logger,
      JClassType remoteService) throws UnableToCompleteException {
    TreeLogger branch = logger.branch(TreeLogger.DEBUG,
        "Checking the synchronous interface '"
            + remoteService.getQualifiedSourceName()
            + "' against its asynchronous version '"
            + remoteService.getQualifiedSourceName() + "Async'", null);
    boolean failed = false;
    JClassType serviceAsync = typeOracle.findType(remoteService.getQualifiedSourceName()
        + "Async");
    if (serviceAsync == null) {
      failed = true;
      branch.branch(TreeLogger.ERROR,
          "Could not find an asynchronous version for the service interface "
              + remoteService.getQualifiedSourceName(), null);
    } else {
      JMethod[] asyncMethods = serviceAsync.getOverridableMethods();
      JMethod[] syncMethods = remoteService.getOverridableMethods();

      if (asyncMethods.length != syncMethods.length) {
        branch.branch(TreeLogger.ERROR, "The asynchronous version of "
            + remoteService.getQualifiedSourceName() + " has "
            + (asyncMethods.length > syncMethods.length ? "more" : "less")
            + " methods than the synchronous version", null);
        failed = true;
      } else {
        Map<String, JMethod> asyncMethodMap = initializeAsyncMethodMap(asyncMethods);
        for (JMethod syncMethod : syncMethods) {
          String asyncSig = computeAsyncMethodSignature(syncMethod);
          JMethod asyncMethod = asyncMethodMap.get(asyncSig);
          if (asyncMethod == null) {
            branch.branch(TreeLogger.ERROR,
                "Missing asynchronous version of the synchronous method '"
                    + syncMethod.getReadableDeclaration() + "'", null);
            failed = true;
          } else if (asyncMethod.getReturnType() != JPrimitiveType.VOID) {
            branch.branch(TreeLogger.ERROR,
                "The asynchronous version of the synchronous method '"
                    + syncMethod.getReadableDeclaration()
                    + "' must have a 'void' return type", null);
            failed = true;
          }
        }
      }
    }

    if (failed) {
      logValidAsyncInterfaceDeclaration(branch, remoteService);
      throw new UnableToCompleteException();
    }
  }

  private String computeAsyncMethodSignature(JMethod syncMethod) {
    return computeInternalSignature(syncMethod) + "/"
        + asyncCallbackClass.getQualifiedSourceName();
  }

  private String computeInternalSignature(JMethod method) {
    StringBuffer sb = new StringBuffer();
    sb.setLength(0);
    sb.append(method.getName());
    JParameter[] params = method.getParameters();
    for (JParameter param : params) {
      sb.append("/");
      sb.append(param.getType().getQualifiedSourceName());
    }
    return sb.toString();
  }

  /**
   * Builds a map of asynchronous method internal signatures to the
   * corresponding asynchronous {@link JMethod}.
   */
  private Map<String, JMethod> initializeAsyncMethodMap(JMethod[] asyncMethods) {
    Map<String, JMethod> sigs = new TreeMap<String, JMethod>();
    for (JMethod asyncMethod : asyncMethods) {
      sigs.put(computeInternalSignature(asyncMethod), asyncMethod);
    }
    return sigs;
  }

  private void logValidAsyncInterfaceDeclaration(TreeLogger logger,
      JClassType remoteService) {
    TreeLogger branch = logger.branch(TreeLogger.INFO,
        "A valid definition for the asynchronous version of interface '"
            + remoteService.getQualifiedSourceName() + "' would be:\n", null);
    branch.log(TreeLogger.ERROR,
        synthesizeAsynchronousInterfaceDefinition(remoteService), null);
  }
}