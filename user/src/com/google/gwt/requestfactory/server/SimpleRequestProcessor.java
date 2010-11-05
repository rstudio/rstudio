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
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.requestfactory.shared.impl.Constants;
import com.google.gwt.requestfactory.shared.impl.EntityProxyCategory;
import com.google.gwt.requestfactory.shared.impl.IdFactory;
import com.google.gwt.requestfactory.shared.impl.SimpleEntityProxyId;
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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
    Class<?> getClientType(Class<?> domainClass);

    Class<?> getDomainClass(Class<?> clazz);

    String getFlatId(EntitySource source, Object domainObject);

    Object getProperty(Object domainObject, String property);

    String getTypeToken(Class<?> domainClass);

    int getVersion(Object domainObject);

    Object invoke(Method domainMethod, Object[] args);

    Object loadDomainObject(EntitySource source, Class<?> clazz, String flatId);

    Class<? extends EntityProxy> resolveClass(String typeToken);

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
  private static class IdToEntityMap extends
      HashMap<SimpleEntityProxyId<?>, AutoBean<? extends EntityProxy>> {
  }

  private class RequestState implements EntityCodex.EntitySource {
    private final IdToEntityMap beans = new IdToEntityMap();
    private final Map<Object, Integer> domainObjectsToClientId;
    private final IdFactory idFactory;

    public RequestState() {
      idFactory = new IdFactory() {
        @Override
        @SuppressWarnings("unchecked")
        protected <P extends EntityProxy> Class<P> getTypeFromToken(
            String typeToken) {
          return (Class<P>) service.resolveClass(typeToken);
        }

        @Override
        protected String getTypeToken(Class<?> clazz) {
          return service.getTypeToken(clazz);
        }
      };
      domainObjectsToClientId = new IdentityHashMap<Object, Integer>();
    }

    public RequestState(RequestState parent) {
      idFactory = parent.idFactory;
      domainObjectsToClientId = parent.domainObjectsToClientId;
    }

    public <Q extends EntityProxy> AutoBean<Q> getBeanForPayload(
        SimpleEntityProxyId<Q> id) {
      @SuppressWarnings("unchecked")
      AutoBean<Q> toReturn = (AutoBean<Q>) beans.get(id);
      if (toReturn == null) {
        toReturn = AutoBeanFactoryMagic.createBean(id.getProxyClass(),
            CONFIGURATION);
        toReturn.setTag(STABLE_ID, id);

        // Resolve the domain object
        Class<?> domainClass = service.getDomainClass(id.getProxyClass());

        Object domain;
        if (id.isEphemeral()) {
          domain = service.createDomainObject(domainClass);
          // Don't call getFlatId here, resolve the ids after invocations
          domainObjectsToClientId.put(domain, id.getClientId());
        } else {
          domain = service.loadDomainObject(this, domainClass,
              fromBase64(id.getServerId()));
        }
        toReturn.setTag(DOMAIN_OBJECT, domain);

        beans.put(id, toReturn);
      }
      return toReturn;
    }

    /**
     * EntityCodex support.
     */
    public <Q extends EntityProxy> AutoBean<Q> getBeanForPayload(
        String serializedProxyId) {
      String typeToken = IdUtil.getTypeToken(serializedProxyId);
      SimpleEntityProxyId<Q> id;
      if (IdUtil.isEphemeral(serializedProxyId)) {
        id = idFactory.getId(typeToken, null,
            IdUtil.getClientId(serializedProxyId));
      } else {
        id = idFactory.getId(typeToken, IdUtil.getServerId(serializedProxyId));
      }
      return getBeanForPayload(id);
    }

    /**
     * If the domain object was sent with an ephemeral id, return the client id.
     */
    public Integer getClientId(Object domainEntity) {
      return domainObjectsToClientId.get(domainEntity);
    }

    /**
     * EntityCodex support.
     */
    public String getSerializedProxyId(EntityProxyId<?> stableId) {
      SimpleEntityProxyId<?> impl = (SimpleEntityProxyId<?>) stableId;
      if (impl.isEphemeral()) {
        return IdUtil.ephemeralId(impl.getClientId(),
            service.getTypeToken(stableId.getProxyClass()));
      } else {
        return IdUtil.persistedId(impl.getServerId(),
            service.getTypeToken(stableId.getProxyClass()));
      }
    }

    /**
     * EntityCodex support.
     */
    public boolean isEntityType(Class<?> clazz) {
      return EntityProxy.class.isAssignableFrom(clazz);
    }
  }

  private static final Configuration CONFIGURATION = new Configuration.Builder().setCategories(
      EntityProxyCategory.class).setNoWrap(EntityProxyId.class).build();
  private static final MessageFactory FACTORY = AutoBeanFactoryMagic.create(MessageFactory.class);

  // Tag constants
  private static final String DOMAIN_OBJECT = "domainObject";
  private static final String IN_RESPONSE = "inResponse";
  private static final String STABLE_ID = "stableId";

  private static String fromBase64(String encoded) {
    try {
      return new String(Base64Utils.fromBase64(encoded), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new UnexpectedException(e);
    }
  }

  private static String toBase64(String data) {
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
    final RequestState source = new RequestState();
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
    for (Map.Entry<SimpleEntityProxyId<?>, AutoBean<? extends EntityProxy>> entry : toProcess.entrySet()) {
      SimpleEntityProxyId<?> id = entry.getKey();

      AutoBean<? extends EntityProxy> bean = entry.getValue();
      Object domainObject = bean.getTag(DOMAIN_OBJECT);
      WriteOperation writeOperation;

      if (id.isEphemeral()) {
        resolveClientEntityProxy(returnState, domainObject,
            Collections.<String> emptySet(), "");
        if (id.isEphemeral()) {
          throw new ReportableException("Could not persist entity "
              + service.getFlatId(returnState, domainObject.toString()));
        }
      }

      if (service.loadDomainObject(returnState,
          service.getDomainClass(id.getProxyClass()),
          fromBase64(id.getServerId())) == null) {
        writeOperation = WriteOperation.DELETE;
      } else if (id.wasEphemeral()) {
        writeOperation = WriteOperation.PERSIST;
      } else {
        writeOperation = WriteOperation.UPDATE;
      }

      boolean inResponse = bean.getTag(IN_RESPONSE) != null;
      int version = domainObject == null ? 0 : service.getVersion(domainObject);

      /*
       * Don't send any data back to the client for an update on an object that
       * isn't part of the response payload when the client's version matches
       * the domain version.
       */
      if (writeOperation.equals(WriteOperation.UPDATE) && !inResponse) {
        if (Integer.valueOf(version).equals(
            bean.getTag(Constants.ENCODED_VERSION_PROPERTY))) {
          continue;
        }
      }

      OperationMessage op = FACTORY.operation().as();
      if (writeOperation == WriteOperation.PERSIST) {
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

      op.setServerId(id.getServerId());
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
      arg = resolveDomainValue(arg, !EntityProxyId.class.equals(contextArgs[i]));
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

  /**
   * Expand the property references in an InvocationMessage into a
   * fully-expanded list of properties. For example, <code>[foo.bar.baz]</code>
   * will be converted into <code>[foo, foo.bar, foo.bar.baz]</code>.
   */
  private Set<String> getPropertyRefs(InvocationMessage invocation) {
    Set<String> refs = invocation.getPropertyRefs();
    if (refs == null) {
      return Collections.emptySet();
    }

    Set<String> toReturn = new TreeSet<String>();
    for (String raw : refs) {
      for (int idx = raw.length(); idx >= 0; idx = raw.lastIndexOf('.', idx - 1)) {
        toReturn.add(raw.substring(0, idx));
      }
    }
    return toReturn;
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
        returnValue = resolveClientValue(returnState, returnValue,
            getPropertyRefs(invocation), "");

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
      bean.setTag(Constants.ENCODED_ID_PROPERTY, operation.getVersion());

      // Load the domain object with properties, if it exists
      final Object domain = bean.getTag(DOMAIN_OBJECT);
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
                Object resolved = resolveDomainValue(newValue, false);
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
                Object resolved = resolveDomainValue(newValue, false);
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
   * Converts a domain entity into an EntityProxy that will be sent to the
   * client.
   */
  private EntityProxy resolveClientEntityProxy(final RequestState state,
      final Object domainEntity, final Set<String> propertyRefs,
      final String prefix) {
    if (domainEntity == null) {
      return null;
    }

    // Compute data needed to return id to the client
    String flatId = toBase64(service.getFlatId(state, domainEntity));
    Class<? extends EntityProxy> proxyType = service.getClientType(
        domainEntity.getClass()).asSubclass(EntityProxy.class);

    // Retrieve the id, possibly setting its persisted value
    SimpleEntityProxyId<? extends EntityProxy> id = state.idFactory.getId(
        proxyType, flatId, state.getClientId(domainEntity));

    AutoBean<? extends EntityProxy> bean = state.getBeanForPayload(id);
    bean.setTag(IN_RESPONSE, true);
    bean.accept(new AutoBeanVisitor() {
      @Override
      public boolean visitReferenceProperty(String propertyName,
          AutoBean<?> value, PropertyContext ctx) {
        // Does the user care about the property?
        String newPrefix = (prefix.length() > 0 ? (prefix + ".") : "")
            + propertyName;

        /*
         * Send if the user cares about the property or if it's a collection of
         * values.
         */
        Class<?> elementType = ctx instanceof CollectionPropertyContext
            ? ((CollectionPropertyContext) ctx).getElementType() : null;
        boolean shouldSend = propertyRefs.contains(newPrefix)
            || elementType != null && !state.isEntityType(elementType);

        if (!shouldSend) {
          return false;
        }

        // Call the getter
        Object domainValue = service.getProperty(domainEntity, propertyName);
        if (domainValue == null) {
          return false;
        }

        // Turn the domain object into something usable on the client side
        Object resolved = resolveClientValue(state, domainValue, propertyRefs,
            newPrefix);

        ctx.set(ctx.getType().cast(resolved));
        return false;
      }

      @Override
      public boolean visitValueProperty(String propertyName, Object value,
          PropertyContext ctx) {
        // Limit unrequested value properties?
        value = service.getProperty(domainEntity, propertyName);
        ctx.set(ctx.getType().cast(value));
        return false;
      }
    });
    bean.setTag(Constants.ENCODED_VERSION_PROPERTY,
        service.getVersion(domainEntity));
    return proxyType.cast(bean.as());
  }

  /**
   * Given a method a domain object, return a value that can be encoded by the
   * client.
   */
  private Object resolveClientValue(RequestState source, Object domainValue,
      Set<String> propertyRefs, String prefix) {
    if (domainValue == null) {
      return null;
    }

    Class<?> returnClass = service.getClientType(domainValue.getClass());

    // Pass simple values through
    if (ValueCodex.canDecode(returnClass)) {
      return domainValue;
    }

    // Convert entities to EntityProxies
    if (EntityProxy.class.isAssignableFrom(returnClass)) {
      return resolveClientEntityProxy(source, domainValue, propertyRefs, prefix);
    }

    // Convert collections
    if (Collection.class.isAssignableFrom(returnClass)) {
      Collection<Object> accumulator;
      if (List.class.isAssignableFrom(returnClass)) {
        accumulator = new ArrayList<Object>();
      } else if (Set.class.isAssignableFrom(returnClass)) {
        accumulator = new HashSet<Object>();
      } else {
        throw new ReportableException("Unsupported collection type"
            + returnClass.getName());
      }

      for (Object o : (Collection<?>) domainValue) {
        accumulator.add(resolveClientValue(source, o, propertyRefs, prefix));
      }
      return accumulator;
    }

    throw new ReportableException("Unsupported domain type "
        + returnClass.getCanonicalName());
  }

  /**
   * Convert a client-side value into a domain value.
   * 
   * @param the client object to resolve
   * @param detectDeadEntities if <code>true</code> this method will throw a
   *          ReportableException containing a {@link DeadEntityException} if an
   *          EntityProxy cannot be resolved
   */
  private Object resolveDomainValue(Object maybeEntityProxy,
      boolean detectDeadEntities) {
    if (maybeEntityProxy instanceof EntityProxy) {
      AutoBean<EntityProxy> bean = AutoBeanUtils.getAutoBean((EntityProxy) maybeEntityProxy);
      Object domain = bean.getTag(DOMAIN_OBJECT);
      if (domain == null && detectDeadEntities) {
        throw new ReportableException(new DeadEntityException(
            "The requested entity is not available on the server"));
      }
      return domain;
    } else if (maybeEntityProxy instanceof Collection<?>) {
      Collection<Object> accumulator;
      if (maybeEntityProxy instanceof List) {
        accumulator = new ArrayList<Object>();
      } else if (maybeEntityProxy instanceof Set) {
        accumulator = new HashSet<Object>();
      } else {
        throw new ReportableException("Unsupported collection type "
            + maybeEntityProxy.getClass().getName());
      }
      for (Object o : (Collection<?>) maybeEntityProxy) {
        accumulator.add(resolveDomainValue(o, detectDeadEntities));
      }
      return accumulator;
    }
    return maybeEntityProxy;
  }

  /**
   * Validate all of the entities referenced in a RequestState.
   */
  private List<ViolationMessage> validateEntities(RequestState source) {
    List<ViolationMessage> errorMessages = new ArrayList<ViolationMessage>();
    for (Map.Entry<SimpleEntityProxyId<?>, AutoBean<? extends EntityProxy>> entry : source.beans.entrySet()) {
      Object domainObject = entry.getValue().getTag(DOMAIN_OBJECT);

      // The object could have been deleted
      if (domainObject != null) {
        Set<ConstraintViolation<Object>> errors = service.validate(domainObject);
        if (errors != null && !errors.isEmpty()) {
          SimpleEntityProxyId<?> id = entry.getKey();
          for (ConstraintViolation<Object> error : errors) {
            ViolationMessage message = FACTORY.violation().as();
            message.setClientId(id.getClientId());
            message.setMessage(error.getMessage());
            message.setPath(error.getPropertyPath().toString());
            message.setServerId(id.getServerId());
            message.setTypeToken(service.getTypeToken(id.getProxyClass()));
            errorMessages.add(message);
          }
        }
      }
    }
    return errorMessages;
  }
}
