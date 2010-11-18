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
package com.google.gwt.requestfactory.server;

import com.google.gwt.autobean.server.AutoBeanFactoryMagic;
import com.google.gwt.autobean.server.Configuration;
import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanCodex;
import com.google.gwt.autobean.shared.AutoBeanUtils;
import com.google.gwt.autobean.shared.AutoBeanVisitor;
import com.google.gwt.autobean.shared.Splittable;
import com.google.gwt.autobean.shared.ValueCodex;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.requestfactory.shared.impl.BaseProxyCategory;
import com.google.gwt.requestfactory.shared.impl.Constants;
import com.google.gwt.requestfactory.shared.impl.EntityProxyCategory;
import com.google.gwt.requestfactory.shared.impl.SimpleProxyId;
import com.google.gwt.requestfactory.shared.impl.ValueProxyCategory;
import com.google.gwt.requestfactory.shared.messages.EntityCodex;
import com.google.gwt.requestfactory.shared.messages.EntityCodex.EntitySource;
import com.google.gwt.requestfactory.shared.messages.IdUtil;
import com.google.gwt.requestfactory.shared.messages.InvocationMessage;
import com.google.gwt.requestfactory.shared.messages.MessageFactory;
import com.google.gwt.requestfactory.shared.messages.OperationMessage;
import com.google.gwt.requestfactory.shared.messages.RequestMessage;
import com.google.gwt.requestfactory.shared.messages.ResponseMessage;
import com.google.gwt.requestfactory.shared.messages.ServerFailureMessage;
import com.google.gwt.requestfactory.shared.messages.ViolationMessage;
import com.google.gwt.user.server.Base64Utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * Processes request payloads from a RequestFactory client. This implementation
 * is stateless. A single instance may be reused and is thread-safe.
 */
public class SimpleRequestProcessor {
  /**
   * Abstracts all reflection operations from the request processor.
   */
  public interface ServiceLayer {
    Object createDomainObject(Class<?> clazz);

    /**
     * The way to introduce polymorphism.
     */
    Class<?> getClientType(Class<?> domainClass, Class<?> clientType);

    Class<?> getDomainClass(Class<?> clazz);

    /**
     * May return {@code null} to indicate that the domain object has not been
     * persisted.
     */
    String getFlatId(EntitySource source, Object domainObject);

    Object getProperty(Object domainObject, String property);

    /**
     * Compute the return type for a method declared in a RequestContext by
     * analyzing the generic method declaration.
     */
    Type getRequestReturnType(Method contextMethod);

    String getTypeToken(Class<?> domainClass);

    int getVersion(Object domainObject);

    Object invoke(Method domainMethod, Object[] args);

    Object loadDomainObject(EntitySource source, Class<?> clazz, String flatId);

    Class<? extends BaseProxy> resolveClass(String typeToken);

    Method resolveDomainMethod(Method requestContextMethod);

    Method resolveRequestContextMethod(String requestContextClass,
        String methodName);

    void setProperty(Object domainObject, String property,
        Class<?> expectedType, Object value);

    <T> Set<ConstraintViolation<T>> validate(T domainObject);
  }

  /**
   * This parameterization is so long, it improves readability to have a
   * specific type.
   */
  @SuppressWarnings("serial")
  static class IdToEntityMap extends
      HashMap<SimpleProxyId<?>, AutoBean<? extends BaseProxy>> {
  }

  /**
   * Allows the creation of properly-configured AutoBeans without having to
   * create an AutoBeanFactory with the desired annotations.
   */
  static final Configuration CONFIGURATION = new Configuration.Builder().setCategories(
      EntityProxyCategory.class, ValueProxyCategory.class,
      BaseProxyCategory.class).setNoWrap(EntityProxyId.class).build();

  /**
   * Vends message objects.
   */
  static final MessageFactory FACTORY = AutoBeanFactoryMagic.create(MessageFactory.class);

  static String fromBase64(String encoded) {
    try {
      return new String(Base64Utils.fromBase64(encoded), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new UnexpectedException(e);
    }
  }

  static String toBase64(String data) {
    try {
      return Base64Utils.toBase64(data.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new UnexpectedException(e);
    }
  }

  private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();

  private final ServiceLayer service;

  public SimpleRequestProcessor(ServiceLayer serviceLayer) {
    this.service = serviceLayer;
  }

  public String process(String payload) {
    RequestMessage req = AutoBeanCodex.decode(FACTORY, RequestMessage.class,
        payload).as();
    AutoBean<ResponseMessage> responseBean = FACTORY.response();
    try {
      process(req, responseBean.as());
    } catch (ReportableException e) {
      e.printStackTrace();
      // Create a new response envelope, since the state is unknown
      responseBean = FACTORY.response();
      responseBean.as().setGeneralFailure(createFailureMessage(e).as());
    }
    // Return a JSON-formatted payload
    return AutoBeanCodex.encode(responseBean).getPayload();
  }

  public void setExceptionHandler(ExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * Main processing method.
   */
  void process(RequestMessage req, ResponseMessage resp) {
    final RequestState source = new RequestState(service);
    // Apply operations
    processOperationMessages(source, req);

    // Validate entities
    List<ViolationMessage> errorMessages = validateEntities(source);

    if (!errorMessages.isEmpty()) {
      resp.setViolations(errorMessages);
      return;
    }

    RequestState returnState = new RequestState(source);

    // Invoke methods
    List<Splittable> invocationResults = new ArrayList<Splittable>();
    List<Boolean> invocationSuccess = new ArrayList<Boolean>();
    List<InvocationMessage> invlist = req.getInvocations();
    processInvocationMessages(source, invlist, invocationResults,
        invocationSuccess, returnState);

    // Store return objects
    List<OperationMessage> operations = new ArrayList<OperationMessage>();
    IdToEntityMap toProcess = new IdToEntityMap();
    toProcess.putAll(source.beans);
    toProcess.putAll(returnState.beans);
    createReturnOperations(operations, returnState, toProcess);

    resp.setInvocationResults(invocationResults);
    resp.setStatusCodes(invocationSuccess);
    if (!operations.isEmpty()) {
      resp.setOperations(operations);
    }
  }

  private AutoBean<ServerFailureMessage> createFailureMessage(
      ReportableException e) {
    ServerFailure failure = exceptionHandler.createServerFailure(e.getCause());
    AutoBean<ServerFailureMessage> bean = FACTORY.failure();
    ServerFailureMessage msg = bean.as();
    msg.setExceptionType(failure.getExceptionType());
    msg.setMessage(failure.getMessage());
    msg.setStackTrace(failure.getStackTraceString());
    return bean;
  }

  private void createReturnOperations(List<OperationMessage> operations,
      RequestState returnState, IdToEntityMap toProcess) {
    for (Map.Entry<SimpleProxyId<?>, AutoBean<? extends BaseProxy>> entry : toProcess.entrySet()) {
      SimpleProxyId<?> id = entry.getKey();

      AutoBean<? extends BaseProxy> bean = entry.getValue();
      Object domainObject = bean.getTag(Constants.DOMAIN_OBJECT);
      WriteOperation writeOperation;

      if (id.isEphemeral()) {
        // See if the entity has been persisted in the meantime
        returnState.getResolver().resolveClientValue(domainObject,
            id.getProxyClass(), Collections.<String> emptySet());
      }

      int version;
      if (id.isEphemeral() || id.isSynthetic()) {
        // If the object isn't persistent, there's no reason to send an update
        writeOperation = null;
        version = 0;
      } else if (service.loadDomainObject(returnState,
          service.getDomainClass(id.getProxyClass()), id.getServerId()) == null) {
        writeOperation = WriteOperation.DELETE;
        version = 0;
      } else if (id.wasEphemeral()) {
        writeOperation = WriteOperation.PERSIST;
        version = service.getVersion(domainObject);
      } else {
        writeOperation = WriteOperation.UPDATE;
        version = service.getVersion(domainObject);
      }

      boolean inResponse = bean.getTag(Constants.IN_RESPONSE) != null;

      /*
       * Don't send any data back to the client for an update on an object that
       * isn't part of the response payload when the client's version matches
       * the domain version.
       */
      if (WriteOperation.UPDATE.equals(writeOperation) && !inResponse) {
        if (Integer.valueOf(version).equals(
            bean.getTag(Constants.ENCODED_VERSION_PROPERTY))) {
          continue;
        }
      }

      OperationMessage op = FACTORY.operation().as();

      /*
       * Send a client id if the id is ephemeral or was previously associated
       * with a client id.
       */
      if (id.wasEphemeral()) {
        op.setClientId(id.getClientId());
      }

      op.setOperation(writeOperation);

      // Only send properties for entities that are part of the return graph
      if (inResponse) {
        Map<String, Splittable> propertyMap = new LinkedHashMap<String, Splittable>();
        // Add all non-null properties to the serialized form
        Map<String, Object> diff = AutoBeanUtils.getAllProperties(bean);
        for (Map.Entry<String, Object> d : diff.entrySet()) {
          Object value = d.getValue();
          if (value != null) {
            propertyMap.put(d.getKey(), EntityCodex.encode(returnState, value));
          }
        }
        op.setPropertyMap(propertyMap);
      }

      if (!id.isEphemeral() && !id.isSynthetic()) {
        // Send the server address only for persistent objects
        op.setServerId(toBase64(id.getServerId()));
      }

      op.setSyntheticId(id.getSyntheticId());
      op.setTypeToken(service.getTypeToken(id.getProxyClass()));
      op.setVersion(version);

      operations.add(op);
    }
  }

  /**
   * Handles instance invocations as the instance at the 0th parameter.
   */
  private List<Object> decodeInvocationArguments(RequestState source,
      InvocationMessage invocation, Class<?>[] contextArgs, Type[] genericArgs) {
    if (invocation.getParameters() == null) {
      return Collections.emptyList();
    }

    assert invocation.getParameters().size() == contextArgs.length;
    List<Object> args = new ArrayList<Object>(contextArgs.length);
    for (int i = 0, j = contextArgs.length; i < j; i++) {
      Class<?> type = contextArgs[i];
      Class<?> elementType = null;
      Splittable split;
      if (Collection.class.isAssignableFrom(type)) {
        elementType = TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(
            Collection.class, genericArgs[i]));
        split = invocation.getParameters().get(i);
      } else {
        split = invocation.getParameters().get(i);
      }
      Object arg = EntityCodex.decode(source, type, elementType, split);
      arg = source.getResolver().resolveDomainValue(arg,
          !EntityProxyId.class.equals(contextArgs[i]));
      args.add(arg);
    }

    return args;
  }

  /**
   * Decode the arguments to pass into the domain method. If the domain method
   * is not static, the instance object will be in the 0th position.
   */
  private List<Object> decodeInvocationArguments(RequestState source,
      InvocationMessage invocation, Method contextMethod, Method domainMethod) {
    boolean isStatic = Modifier.isStatic(domainMethod.getModifiers());
    int baseLength = contextMethod.getParameterTypes().length;
    int length = baseLength + (isStatic ? 0 : 1);
    int offset = isStatic ? 0 : 1;
    Class<?>[] contextArgs = new Class<?>[length];
    Type[] genericArgs = new Type[length];

    if (!isStatic) {
      genericArgs[0] = TypeUtils.getSingleParameterization(
          InstanceRequest.class, contextMethod.getGenericReturnType());
      contextArgs[0] = TypeUtils.ensureBaseType(genericArgs[0]);
    }
    System.arraycopy(contextMethod.getParameterTypes(), 0, contextArgs, offset,
        baseLength);
    System.arraycopy(contextMethod.getGenericParameterTypes(), 0, genericArgs,
        offset, baseLength);

    List<Object> args = decodeInvocationArguments(source, invocation,
        contextArgs, genericArgs);
    return args;
  }

  private void processInvocationMessages(RequestState state,
      List<InvocationMessage> invlist, List<Splittable> results,
      List<Boolean> success, RequestState returnState) {
    for (InvocationMessage invocation : invlist) {
      try {
        // Find the Method
        String[] operation = invocation.getOperation().split("::");
        Method contextMethod = service.resolveRequestContextMethod(
            operation[0], operation[1]);
        Method domainMethod = service.resolveDomainMethod(contextMethod);

        // Invoke it
        List<Object> args = decodeInvocationArguments(state, invocation,
            contextMethod, domainMethod);
        Object returnValue = service.invoke(domainMethod, args.toArray());

        // Convert domain object to client object
        Type requestReturnType = service.getRequestReturnType(contextMethod);
        returnValue = state.getResolver().resolveClientValue(returnValue,
            requestReturnType, invocation.getPropertyRefs());

        // Convert the client object to a string
        results.add(EntityCodex.encode(returnState, returnValue));
        success.add(true);
      } catch (ReportableException e) {
        results.add(AutoBeanCodex.encode(createFailureMessage(e)));
        success.add(false);
      }
    }
  }

  private void processOperationMessages(final RequestState state,
      RequestMessage req) {
    List<OperationMessage> operations = req.getOperations();
    if (operations == null) {
      return;
    }

    for (final OperationMessage operation : operations) {
      // Unflatten properties
      String payloadId = operation.getOperation().equals(WriteOperation.PERSIST)
          ? IdUtil.ephemeralId(operation.getClientId(),
              operation.getTypeToken()) : IdUtil.persistedId(
              operation.getServerId(), operation.getTypeToken());
      AutoBean<? extends EntityProxy> bean = state.getBeanForPayload(payloadId);
      // Use the version later to know which objects need to be sent back
      bean.setTag(Constants.ENCODED_VERSION_PROPERTY, operation.getVersion());

      // Load the domain object with properties, if it exists
      final Object domain = bean.getTag(Constants.DOMAIN_OBJECT);
      if (domain != null) {
        // Apply any property updates
        final Map<String, Splittable> flatValueMap = operation.getPropertyMap();
        if (flatValueMap != null) {
          bean.accept(new AutoBeanVisitor() {
            @Override
            public boolean visitReferenceProperty(String propertyName,
                AutoBean<?> value, PropertyContext ctx) {
              // containsKey to distinguish null from unknown
              if (flatValueMap.containsKey(propertyName)) {
                Class<?> elementType = ctx instanceof CollectionPropertyContext
                    ? ((CollectionPropertyContext) ctx).getElementType() : null;
                Object newValue = EntityCodex.decode(state, ctx.getType(),
                    elementType, flatValueMap.get(propertyName));
                Object resolved = state.getResolver().resolveDomainValue(
                    newValue, false);
                service.setProperty(domain, propertyName,
                    service.getDomainClass(ctx.getType()), resolved);
              }
              return false;
            }

            @Override
            public boolean visitValueProperty(String propertyName,
                Object value, PropertyContext ctx) {
              if (flatValueMap.containsKey(propertyName)) {
                Splittable split = flatValueMap.get(propertyName);
                Object newValue = ValueCodex.decode(ctx.getType(), split);
                Object resolved = state.getResolver().resolveDomainValue(
                    newValue, false);
                service.setProperty(domain, propertyName, ctx.getType(),
                    resolved);
              }
              return false;
            }
          });
        }
      }
    }
  }

  /**
   * Validate all of the entities referenced in a RequestState.
   */
  private List<ViolationMessage> validateEntities(RequestState source) {
    List<ViolationMessage> errorMessages = new ArrayList<ViolationMessage>();
    for (Map.Entry<SimpleProxyId<?>, AutoBean<? extends BaseProxy>> entry : source.beans.entrySet()) {
      AutoBean<? extends BaseProxy> bean = entry.getValue();
      Object domainObject = bean.getTag(Constants.DOMAIN_OBJECT);

      // The object could have been deleted
      if (domainObject != null) {
        Set<ConstraintViolation<Object>> errors = service.validate(domainObject);
        if (errors != null && !errors.isEmpty()) {
          SimpleProxyId<?> id = entry.getKey();
          for (ConstraintViolation<Object> error : errors) {
            ViolationMessage message = FACTORY.violation().as();
            message.setClientId(id.getClientId());
            message.setMessage(error.getMessage());
            message.setPath(error.getPropertyPath().toString());
            if (id.isEphemeral()) {
              message.setClientId(id.getClientId());
            }
            if (id.getServerId() != null) {
              message.setServerId(toBase64(id.getServerId()));
            }
            message.setTypeToken(service.getTypeToken(id.getProxyClass()));
            errorMessages.add(message);
          }
        }
      }
    }
    return errorMessages;
  }
}
