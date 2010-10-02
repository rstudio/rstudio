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
package com.google.gwt.requestfactory.rebind.model;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.rebind.model.ModelUtils;
import com.google.gwt.requestfactory.rebind.model.RequestMethod.CollectionType;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a RequestFactory interface declaration.
 */
public class RequestFactoryModel {
  static String badContextReturnType(JMethod method,
      JClassType requestInterface, JClassType instanceRequestInterface) {
    return String.format(
        "Return type %s in method %s must be an interface assignable"
            + " to %s or %s", method.getReturnType(), method.getName(),
        requestInterface.getSimpleSourceName(),
        instanceRequestInterface.getSimpleSourceName());
  }

  private final TreeLogger logger;
  private final JClassType collectionInterface;
  private final List<ContextMethod> contextMethods = new ArrayList<ContextMethod>();
  private final JClassType entityProxyInterface;
  private final JClassType factoryType;
  private final JClassType instanceRequestInterface;
  private final JClassType listInterface;
  private final TypeOracle oracle;
  /**
   * This map prevents cyclic type dependencies from overflowing the stack.
   */
  private final Map<JClassType, EntityProxyModel.Builder> peerBuilders = new HashMap<JClassType, EntityProxyModel.Builder>();
  /**
   * Iterated by {@link #getAllProxyModels()}.
   */
  private final Map<JClassType, EntityProxyModel> peers = new LinkedHashMap<JClassType, EntityProxyModel>();
  private boolean poisoned;
  private final JClassType setInterface;
  private final JClassType requestContextInterface;
  private final JClassType requestFactoryInterface;
  private final JClassType requestInterface;

  public RequestFactoryModel(TreeLogger logger, JClassType factoryType)
      throws UnableToCompleteException {
    this.logger = logger;
    this.factoryType = factoryType;
    this.oracle = factoryType.getOracle();
    collectionInterface = oracle.findType(Collection.class.getCanonicalName());
    entityProxyInterface = oracle.findType(EntityProxy.class.getCanonicalName());
    instanceRequestInterface = oracle.findType(InstanceRequest.class.getCanonicalName());
    listInterface = oracle.findType(List.class.getCanonicalName());
    setInterface = oracle.findType(Set.class.getCanonicalName());
    requestContextInterface = oracle.findType(RequestContext.class.getCanonicalName());
    requestFactoryInterface = oracle.findType(RequestFactory.class.getCanonicalName());
    requestInterface = oracle.findType(Request.class.getCanonicalName());

    for (JMethod method : factoryType.getOverridableMethods()) {
      if (method.getEnclosingType().equals(requestFactoryInterface)) {
        // Ignore methods defined an RequestFactory itself
        continue;
      }

      if (method.getParameters().length > 0) {
        poison("Unexpected parameter on method %s", method.getName());
        continue;
      }

      JClassType contextType = method.getReturnType().isInterface();
      if (contextType == null
          || !requestContextInterface.isAssignableFrom(contextType)) {
        poison("Unexpected return type %s on method %s is not"
            + " an interface assignable to %s",
            method.getReturnType().getQualifiedSourceName(), method.getName(),
            requestContextInterface.getSimpleSourceName());
        continue;
      }

      ContextMethod.Builder builder = new ContextMethod.Builder();
      builder.setDeclaredMethod(method);
      buildContextMethod(builder, contextType);
      contextMethods.add(builder.build());
    }

    if (poisoned) {
      die("Unable to complete due to previous errors");
    }
  }

  public Collection<EntityProxyModel> getAllProxyModels() {
    return Collections.unmodifiableCollection(peers.values());
  }

  public JClassType getFactoryType() {
    return factoryType;
  }

  public List<ContextMethod> getMethods() {
    return Collections.unmodifiableList(contextMethods);
  }

  public EntityProxyModel getPeer(JClassType entityProxyType) {
    return peers.get(entityProxyType);
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return getFactoryType().getQualifiedSourceName();
  }

  /**
   * Examine a RequestContext subtype to populate a ContextMethod.
   */
  private void buildContextMethod(ContextMethod.Builder contextBuilder,
      JClassType contextType) {
    Service serviceAnnotation = contextType.getAnnotation(Service.class);
    if (serviceAnnotation == null) {
      poison("RequestContext subtype %s is missing a @%s annotation",
          contextType.getQualifiedSourceName(), Service.class.getSimpleName());
      return;
    }
    contextBuilder.setServiceClass(serviceAnnotation.value());

    List<RequestMethod> requestMethods = new ArrayList<RequestMethod>();
    for (JMethod method : contextType.getInheritableMethods()) {
      if (method.getEnclosingType().equals(requestContextInterface)) {
        // Ignore methods declared in RequestContext
        continue;
      }

      RequestMethod.Builder methodBuilder = new RequestMethod.Builder();
      methodBuilder.setDeclarationMethod(method);

      if (!validateContextMethodAndSetDataType(methodBuilder, method)) {
        continue;
      }

      requestMethods.add(methodBuilder.build());
    }

    contextBuilder.setRequestMethods(requestMethods);
  }

  private void die(String message) throws UnableToCompleteException {
    poison(message);
    throw new UnableToCompleteException();
  }

  private EntityProxyModel getEntityProxyType(JClassType entityProxyType) {
    EntityProxyModel toReturn = peers.get(entityProxyType);
    if (toReturn == null) {
      EntityProxyModel.Builder inProgress = peerBuilders.get(entityProxyType);
      if (inProgress != null) {
        toReturn = inProgress.peek();
      }
    }
    if (toReturn == null) {
      EntityProxyModel.Builder builder = new EntityProxyModel.Builder();
      peerBuilders.put(entityProxyType, builder);

      builder.setQualifiedSourceName(entityProxyType.getQualifiedSourceName());

      // Get the server domain object type
      ProxyFor proxyFor = entityProxyType.getAnnotation(ProxyFor.class);
      if (proxyFor == null) {
        poison("The %s type does not have a @%s annotation",
            entityProxyType.getQualifiedSourceName(),
            ProxyFor.class.getSimpleName());
      } else {
        builder.setProxyFor(proxyFor.value());
      }

      // Look at the methods declared on the EntityProxy
      List<RequestMethod> requestMethods = new ArrayList<RequestMethod>();
      for (JMethod method : entityProxyType.getInheritableMethods()) {
        if (method.getEnclosingType().equals(entityProxyInterface)) {
          // Ignore methods on EntityProxy
          continue;
        }
        RequestMethod.Builder methodBuilder = new RequestMethod.Builder();
        methodBuilder.setDeclarationMethod(method);

        JType transportedType;
        String name = method.getName();
        if (name.startsWith("get") && method.getParameters().length == 0) {
          // Getter
          transportedType = method.getReturnType();

        } else if (name.startsWith("set") && method.getParameters().length == 1) {
          transportedType = method.getParameters()[0].getType();

        } else if (name.equals("stableId")
            && method.getParameters().length == 0) {
          // Ignore any overload of stableId
          continue;
        } else {
          poison("The method %s is neither a getter nor a setter",
              method.getReadableDeclaration());
          continue;
        }
        validateTransportableType(methodBuilder, transportedType, false);
        requestMethods.add(methodBuilder.build());
      }
      builder.setRequestMethods(requestMethods);

      toReturn = builder.build();
      peers.put(entityProxyType, toReturn);
      peerBuilders.remove(entityProxyType);
    }
    return toReturn;
  }

  private void poison(String message, Object... args) {
    logger.log(TreeLogger.ERROR, String.format(message, args));
    poisoned = true;
  }

  /**
   * Examine a RequestContext method to see if it returns a transportable type.
   */
  private boolean validateContextMethodAndSetDataType(
      RequestMethod.Builder methodBuilder, JMethod method) {
    JClassType requestReturnType = method.getReturnType().isInterface();
    JClassType invocationReturnType;
    if (requestReturnType == null) {
      // Primitive return type
      poison(badContextReturnType(method, requestInterface,
          instanceRequestInterface));
      return false;
    }

    if (instanceRequestInterface.isAssignableFrom(requestReturnType)) {
      // Instance method invocation
      JClassType[] params = ModelUtils.findParameterizationOf(
          instanceRequestInterface, requestReturnType);
      methodBuilder.setInstanceType(getEntityProxyType(params[0]));
      invocationReturnType = params[1];

    } else if (requestInterface.isAssignableFrom(requestReturnType)) {
      // Static method invocation
      JClassType[] params = ModelUtils.findParameterizationOf(requestInterface,
          requestReturnType);
      invocationReturnType = params[0];

    } else {
      // Unhandled return type, must be something random
      poison(badContextReturnType(method, requestInterface,
          instanceRequestInterface));
      return false;
    }

    // Validate the parameters
    boolean paramsOk = true;
    for (JParameter param : method.getParameters()) {
      paramsOk = validateTransportableType(new RequestMethod.Builder(),
          param.getType(), false)
          && paramsOk;
    }

    return validateTransportableType(methodBuilder, invocationReturnType, true)
        && paramsOk;
  }

  /**
   * Examines a type to see if it can be transported.
   */
  private boolean validateTransportableType(
      RequestMethod.Builder methodBuilder, JType type, boolean requireObject) {
    JClassType transportedClass = type.isClassOrInterface();
    if (transportedClass == null) {
      if (requireObject) {
        poison("The type %s cannot be transported by RequestFactory as"
            + " a return type", type.getQualifiedSourceName());
        return false;
      } else {
        // Primitives always ok
        return true;
      }
    }

    if (ModelUtils.isValueType(oracle, transportedClass)) {
      // Simple values, like Integer and String
      methodBuilder.setValueType(true);
    } else if (entityProxyInterface.isAssignableFrom(transportedClass)) {
      // EntityProxy return types
      methodBuilder.setEntityType(getEntityProxyType(transportedClass));
    } else if (collectionInterface.isAssignableFrom(transportedClass)) {
      // Only allow certain collections for now
      if (listInterface.equals(transportedClass.isParameterized().getBaseType())) {
        methodBuilder.setCollectionType(CollectionType.LIST);
      } else if (setInterface.equals(transportedClass.isParameterized().getBaseType())) {
        methodBuilder.setCollectionType(CollectionType.SET);
      } else {
        poison("Requests that return collections may be declared with"
            + " %s or %s only", listInterface.getQualifiedSourceName(),
            setInterface.getQualifiedSourceName());
        return false;
      }
      // Also record the element type in the method builder
      JClassType elementType = ModelUtils.findParameterizationOf(
          collectionInterface, transportedClass)[0];
      methodBuilder.setCollectionElementType(elementType);
      validateTransportableType(methodBuilder, elementType, requireObject);
    } else {
      // Unknown type, fail
      poison("Invalid Request parameterization %s",
          transportedClass.getQualifiedSourceName());
      return false;
    }
    methodBuilder.setDataType(transportedClass);
    return true;
  }

}
