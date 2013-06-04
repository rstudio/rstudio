/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.web.bindery.requestfactory.shared.impl;

import static com.google.web.bindery.requestfactory.shared.impl.BaseProxyCategory.stableId;
import static com.google.web.bindery.requestfactory.shared.impl.Constants.REQUEST_CONTEXT_STATE;
import static com.google.web.bindery.requestfactory.shared.impl.Constants.STABLE_ID;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.ValueCodex;
import com.google.web.bindery.autobean.shared.impl.AbstractAutoBean;
import com.google.web.bindery.autobean.shared.impl.EnumMap;
import com.google.web.bindery.autobean.shared.impl.StringQuoter;
import com.google.web.bindery.event.shared.UmbrellaException;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyChange;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.FanoutReceiver;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestTransport.TransportReceiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.google.web.bindery.requestfactory.shared.WriteOperation;
import com.google.web.bindery.requestfactory.shared.impl.posers.DatePoser;
import com.google.web.bindery.requestfactory.shared.messages.IdMessage;
import com.google.web.bindery.requestfactory.shared.messages.IdMessage.Strength;
import com.google.web.bindery.requestfactory.shared.messages.InvocationMessage;
import com.google.web.bindery.requestfactory.shared.messages.JsonRpcRequest;
import com.google.web.bindery.requestfactory.shared.messages.MessageFactory;
import com.google.web.bindery.requestfactory.shared.messages.OperationMessage;
import com.google.web.bindery.requestfactory.shared.messages.RequestMessage;
import com.google.web.bindery.requestfactory.shared.messages.ResponseMessage;
import com.google.web.bindery.requestfactory.shared.messages.ServerFailureMessage;
import com.google.web.bindery.requestfactory.shared.messages.ViolationMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * Base implementations for RequestContext services.
 */
public abstract class AbstractRequestContext implements RequestContext, EntityCodex.EntitySource {
  /**
   * Allows the payload dialect to be injected into the AbstractRequestContext without the caller
   * needing to be concerned with how the implementation object is instantiated.
   */
  public enum Dialect {
    STANDARD {
      @Override
      DialectImpl create(AbstractRequestContext context) {
        return context.new StandardPayloadDialect();
      }
    },
    JSON_RPC {
      @Override
      DialectImpl create(AbstractRequestContext context) {
        return context.new JsonRpcPayloadDialect();
      }
    };
    abstract DialectImpl create(AbstractRequestContext context);
  }

  /**
   * Encapsulates all state contained by the AbstractRequestContext.
   */
  protected static class State {
    /**
     * Supports the case where chained contexts are used and a response comes back from the server
     * with a proxy type not reachable from the canonical context.
     */
    public Set<AbstractRequestContext> appendedContexts;
    public final AbstractRequestContext canonical;
    public final DialectImpl dialect;
    public FanoutReceiver<Void> fanout;
    /**
     * When {@code true} the {@link AbstractRequestContext#fire()} method will be a no-op.
     */
    public boolean fireDisabled;
    public final List<AbstractRequest<?, ?>> invocations = new ArrayList<AbstractRequest<?, ?>>();

    public boolean locked;
    /**
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=5952.
     */
    public boolean diffing;
    /**
     * A map of all EntityProxies that the RequestContext has interacted with. Objects are placed
     * into this map by being returned from {@link #create}, passed into {@link #edit}, or used as
     * an invocation argument.
     */
    public final Map<SimpleProxyId<?>, AutoBean<? extends BaseProxy>> editedProxies =
        new LinkedHashMap<SimpleProxyId<?>, AutoBean<? extends BaseProxy>>();
    /**
     * A map that contains the canonical instance of an entity to return in the return graph, since
     * this is built from scratch.
     */
    public final Map<SimpleProxyId<?>, AutoBean<?>> returnedProxies =
        new HashMap<SimpleProxyId<?>, AutoBean<?>>();

    public final AbstractRequestFactory requestFactory;

    /**
     * A map that allows us to handle the case where the server has sent back an unpersisted entity.
     * Because we assume that the server is stateless, the client will need to swap out the
     * request-local ids with a regular client-allocated id.
     */
    public final Map<Integer, SimpleProxyId<?>> syntheticIds =
        new HashMap<Integer, SimpleProxyId<?>>();

    public State(AbstractRequestFactory requestFactory, DialectImpl dialect,
        AbstractRequestContext canonical) {
      this.requestFactory = requestFactory;
      this.canonical = canonical;
      this.dialect = dialect;
    }

    public void addContext(AbstractRequestContext ctx) {
      if (appendedContexts == null) {
        appendedContexts = Collections.singleton(ctx);
      } else {
        if (appendedContexts.size() == 1) {
          appendedContexts = new LinkedHashSet<AbstractRequestContext>(appendedContexts);
        }
        appendedContexts.add(ctx);
      }
    }

    public AbstractRequestContext getCanonicalContext() {
      return canonical;
    }

    public boolean isClean() {
      return editedProxies.isEmpty() && invocations.isEmpty() && !locked
          && returnedProxies.isEmpty() && syntheticIds.isEmpty();
    }

    public boolean isCompatible(State state) {
      // Object comparison intentional
      return requestFactory == state.requestFactory
          && dialect.getClass().equals(state.dialect.getClass());
    }
  }

  interface DialectImpl {

    void addInvocation(AbstractRequest<?, ?> request);

    String makePayload();

    void processPayload(Receiver<Void> receiver, String payload);
  }

  class JsonRpcPayloadDialect implements DialectImpl {
    /**
     * Called by generated subclasses to enqueue a method invocation.
     */
    public void addInvocation(AbstractRequest<?, ?> request) {
      /*
       * TODO(bobv): Support for multiple invocations per request needs to be ironed out. Once this
       * is done, addInvocation() can be removed from the DialectImpl interface and restored to to
       * AbstractRequestContext.
       */
      if (!state.invocations.isEmpty()) {
        throw new RuntimeException("Only one invocation per request, pending backend support");
      }
      state.invocations.add(request);
      for (Object arg : request.getRequestData().getOrderedParameters()) {
        retainArg(arg);
      }
    }

    public String makePayload() {
      RequestData data = state.invocations.get(0).getRequestData();

      AutoBean<JsonRpcRequest> bean = MessageFactoryHolder.FACTORY.jsonRpcRequest();
      JsonRpcRequest request = bean.as();

      request.setVersion("2.0");
      request.setApiVersion(data.getApiVersion());
      request.setId(payloadId++);

      Map<String, Splittable> params = new HashMap<String, Splittable>();
      for (Map.Entry<String, Object> entry : data.getNamedParameters().entrySet()) {
        Object obj = entry.getValue();
        Splittable value = encode(obj);
        params.put(entry.getKey(), value);
      }
      if (data.getRequestResource() != null) {
        params.put("resource", encode(data.getRequestResource()));
      }
      request.setParams(params);
      request.setMethod(data.getOperation());

      return AutoBeanCodex.encode(bean).getPayload();
    }

    public void processPayload(Receiver<Void> receiver, String payload) {
      Splittable raw = StringQuoter.split(payload);

      @SuppressWarnings("unchecked")
      Receiver<Object> callback = (Receiver<Object>) state.invocations.get(0).getReceiver();

      if (!raw.isNull("error")) {
        Splittable error = raw.get("error");
        ServerFailure failure =
            new ServerFailure(error.get("message").asString(), error.get("code").asString(),
                payload, true);
        fail(receiver, failure);
        return;
      }

      Splittable result = raw.get("result");
      @SuppressWarnings("unchecked")
      Class<BaseProxy> target =
          (Class<BaseProxy>) state.invocations.get(0).getRequestData().getReturnType();

      SimpleProxyId<BaseProxy> id = getRequestFactory().allocateId(target);
      AutoBean<BaseProxy> bean = createProxy(target, id, true);
      // XXX expose this as a proper API
      ((AbstractAutoBean<?>) bean).setData(result);
      // AutoBeanCodex.decodeInto(result, bean);

      if (callback != null) {
        callback.onSuccess(bean.as());
      }
      if (receiver != null) {
        receiver.onSuccess(null);
      }
    }

    Splittable encode(Object obj) {
      if (obj == null) {
        return Splittable.NULL;
      } else if (obj instanceof Collection) {
        return collectionEncode((Collection<?>) obj);
      }
      return nonCollectionEncode(obj);
    }

    private Splittable collectionEncode(Collection<?> collection) {
      StringBuffer sb = new StringBuffer();
      Iterator<?> it = collection.iterator();
      sb.append("[");
      if (it.hasNext()) {
        // TODO: Allow for the encoding of nested collections. See issue 5974.
        sb.append(nonCollectionEncode(it.next()).getPayload());
        while (it.hasNext()) {
          sb.append(",");
          // TODO: Allow for the encoding of nested collections. See issue 5974.
          sb.append(nonCollectionEncode(it.next()).getPayload());
        }
      }
      sb.append("]");

      return StringQuoter.split(sb.toString());
    }

    private Splittable nonCollectionEncode(Object obj) {
      if (obj instanceof Collection) {
        // TODO: Allow for the encoding of nested collections. See issue 5974.
        // Once we do this, this can turn into an assert.
        throw new RuntimeException(
            "Unable to encode request as JSON payload; Request methods must have parameters of the form List<T> or Set<T>, where T is a scalar (non-collection) type.");
      }
      Splittable value;
      if (obj instanceof Enum && getAutoBeanFactory() instanceof EnumMap) {
        value = ValueCodex.encode(((EnumMap) getAutoBeanFactory()).getToken((Enum<?>) obj));
      } else if (ValueCodex.canDecode(obj.getClass())) {
        value = ValueCodex.encode(obj);
      } else {
        // XXX user-provided implementation of interface?
        value = AutoBeanCodex.encode(AutoBeanUtils.getAutoBean(obj));
      }
      return value;
    }
  }

  class StandardPayloadDialect implements DialectImpl {

    /**
     * Called by generated subclasses to enqueue a method invocation.
     */
    public void addInvocation(AbstractRequest<?, ?> request) {
      state.invocations.add(request);
      for (Object arg : request.getRequestData().getOrderedParameters()) {
        retainArg(arg);
      }
    }

    /**
     * Assemble all of the state that has been accumulated in this context. This includes:
     * <ul>
     * <li>Diffs accumulated on objects passed to {@link #edit}.
     * <li>Invocations accumulated as Request subtypes passed to {@link #addInvocation}.
     * </ul>
     */
    public String makePayload() {
      // Get the factory from the runtime-specific holder.
      MessageFactory f = MessageFactoryHolder.FACTORY;

      List<OperationMessage> operations = makePayloadOperations();
      List<InvocationMessage> invocationMessages = makePayloadInvocations();

      // Create the outer envelope message
      AutoBean<RequestMessage> bean = f.request();
      RequestMessage requestMessage = bean.as();
      requestMessage.setRequestFactory(getRequestFactory().getFactoryTypeToken());
      if (!invocationMessages.isEmpty()) {
        requestMessage.setInvocations(invocationMessages);
      }
      if (!operations.isEmpty()) {
        requestMessage.setOperations(operations);
      }
      return AutoBeanCodex.encode(bean).getPayload();
    }

    public void processPayload(final Receiver<Void> receiver, String payload) {
      ResponseMessage response =
          AutoBeanCodex.decode(MessageFactoryHolder.FACTORY, ResponseMessage.class, payload).as();
      if (response.getGeneralFailure() != null) {
        ServerFailureMessage failure = response.getGeneralFailure();
        ServerFailure fail =
            new ServerFailure(failure.getMessage(), failure.getExceptionType(), failure
                .getStackTrace(), failure.isFatal());

        fail(receiver, fail);
        return;
      }

      // Process violations and then stop
      if (response.getViolations() != null) {
        Set<ConstraintViolation<?>> errors = new HashSet<ConstraintViolation<?>>();
        for (ViolationMessage message : response.getViolations()) {
          errors.add(new MyConstraintViolation(message));
        }

        violation(receiver, errors);
        return;
      }

      // Process operations
      processReturnOperations(response);

      // Send return values
      Set<Throwable> causes = null;
      for (int i = 0, j = state.invocations.size(); i < j; i++) {
        try {
          if (response.getStatusCodes().get(i)) {
            state.invocations.get(i).onSuccess(response.getInvocationResults().get(i));
          } else {
            ServerFailureMessage failure =
                AutoBeanCodex.decode(MessageFactoryHolder.FACTORY, ServerFailureMessage.class,
                    response.getInvocationResults().get(i)).as();
            state.invocations.get(i).onFail(
                new ServerFailure(failure.getMessage(), failure.getExceptionType(), failure
                    .getStackTrace(), failure.isFatal()));
          }
        } catch (Throwable t) {
          if (causes == null) {
            causes = new HashSet<Throwable>();
          }
          causes.add(t);
        }
      }

      if (receiver != null) {
        try {
          receiver.onSuccess(null);
        } catch (Throwable t) {
          if (causes == null) {
            causes = new HashSet<Throwable>();
          }
          causes.add(t);
        }
      }
      // After success, shut down the context
      state.editedProxies.clear();
      state.invocations.clear();
      state.returnedProxies.clear();

      if (causes != null) {
        throw new UmbrellaException(causes);
      }
    }
  }

  private class MyConstraintViolation implements ConstraintViolation<BaseProxy> {
    private final BaseProxy leafBean;
    private final String messageTemplate;
    private final String message;
    private final String path;
    private final BaseProxy rootBean;
    private final Class<? extends BaseProxy> rootBeanClass;

    public MyConstraintViolation(ViolationMessage msg) {
      AutoBean<? extends BaseProxy> leafProxy = findEditedProxy(msg.getLeafBeanId());
      leafBean = leafProxy == null ? null : leafProxy.as();
      message = msg.getMessage();
      messageTemplate = msg.getMessageTemplate();
      path = msg.getPath();
      AutoBean<? extends BaseProxy> rootProxy = findEditedProxy(msg.getRootBeanId());
      rootBeanClass = rootProxy.getType();
      rootBean = rootProxy.as();
    }

    public ConstraintDescriptor<?> getConstraintDescriptor() {
      return null;
    }

    public Object getInvalidValue() {
      return null;
    }

    public Object getLeafBean() {
      return leafBean;
    }

    public String getMessage() {
      return message;
    }

    public String getMessageTemplate() {
      return messageTemplate;
    }

    public Path getPropertyPath() {
      return new Path() {
        public Iterator<Node> iterator() {
          return Collections.<Node> emptyList().iterator();
        }

        @Override
        public String toString() {
          return path;
        }
      };
    }

    public BaseProxy getRootBean() {
      return rootBean;
    }

    @SuppressWarnings("unchecked")
    public Class<BaseProxy> getRootBeanClass() {
      return (Class<BaseProxy>) rootBeanClass;
    }

    private AutoBean<? extends BaseProxy> findEditedProxy(IdMessage idMessage) {
      // Support violations for value objects.
      SimpleProxyId<BaseProxy> rootId = getId(idMessage);

      // The stub is empty, since we don't process any OperationMessages
      AutoBean<BaseProxy> stub = getProxyForReturnPayloadGraph(rootId);

      // So pick up the instance that we just sent to the server
      return state.editedProxies.get(BaseProxyCategory.stableId(stub));
    }
  }

  private static final WriteOperation[] DELETE_ONLY = {WriteOperation.DELETE};
  private static final WriteOperation[] PERSIST_AND_UPDATE = {
      WriteOperation.PERSIST, WriteOperation.UPDATE};
  private static final WriteOperation[] UPDATE_ONLY = {WriteOperation.UPDATE};
  private static int payloadId = 100;

  private State state;

  protected AbstractRequestContext(AbstractRequestFactory factory, Dialect dialect) {
    setState(new State(factory, dialect.create(this), this));
  }

  public <T extends RequestContext> T append(T other) {
    AbstractRequestContext child = (AbstractRequestContext) other;
    if (!state.isCompatible(child.state)) {
      throw new IllegalStateException(getClass().getName() + " and " + child.getClass().getName()
          + " are not compatible");
    }
    if (!child.state.isClean()) {
      throw new IllegalStateException("The provided RequestContext has been changed");
    }
    child.setState(state);
    return other;
  }

  /**
   * Create a new object, with an ephemeral id.
   */
  public <T extends BaseProxy> T create(Class<T> clazz) {
    checkLocked();

    SimpleProxyId<T> id = state.requestFactory.allocateId(clazz);
    AutoBean<T> created = createProxy(clazz, id, false);
    return takeOwnership(created);
  }

  public <T extends BaseProxy> T edit(T object) {
    return editProxy(object);
  }

  /**
   * Take ownership of a proxy instance and make it editable.
   */
  public <T extends BaseProxy> T editProxy(T object) {
    AutoBean<T> bean = checkStreamsNotCrossed(object);
    checkLocked();

    @SuppressWarnings("unchecked")
    AutoBean<T> previouslySeen =
        (AutoBean<T>) state.editedProxies.get(BaseProxyCategory.stableId(bean));
    if (previouslySeen != null && !previouslySeen.isFrozen()) {
      /*
       * If we've seen the object before, it might be because it was passed in as a method argument.
       * This does not guarantee its mutability, so check that here before returning the cached
       * object.
       */
      return previouslySeen.as();
    }

    // Create editable copies
    AutoBean<T> parent = bean;
    bean = cloneBeanAndCollections(bean);
    bean.setTag(Constants.PARENT_OBJECT, parent);
    return bean.as();
  }

  @Override
  public <P extends EntityProxy> Request<P> find(final EntityProxyId<P> proxyId) {
    return new AbstractRequest<BaseProxy, P>(this) {
      {
        requestContext.addInvocation(this);
      }

      @Override
      protected RequestData makeRequestData() {
        // This method is normally generated, hence the ugly constructor
        return new RequestData(Constants.FIND_METHOD_OPERATION, new Object[] {proxyId},
            propertyRefs, proxyId.getProxyClass(), null);
      }
    };
  }

  /**
   * Make sure there's a default receiver so errors don't get dropped. This behavior should be
   * revisited when chaining is supported, depending on whether or not chained invocations can fail
   * independently.
   */
  public void fire() {
    boolean needsReceiver = true;
    for (AbstractRequest<?, ?> request : state.invocations) {
      if (request.hasReceiver()) {
        needsReceiver = false;
        break;
      }
    }

    if (needsReceiver) {
      doFire(new Receiver<Void>() {
        @Override
        public void onSuccess(Void response) {
          // Don't care
        }
      });
    } else {
      doFire(null);
    }
  }

  public void fire(final Receiver<Void> receiver) {
    if (receiver == null) {
      throw new IllegalArgumentException();
    }
    doFire(receiver);
  }

  /**
   * EntityCodex support.
   */
  public <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(Splittable serializedProxyId) {
    IdMessage ref =
        AutoBeanCodex.decode(MessageFactoryHolder.FACTORY, IdMessage.class, serializedProxyId).as();
    @SuppressWarnings("unchecked")
    SimpleProxyId<Q> id = (SimpleProxyId<Q>) getId(ref);
    return getProxyForReturnPayloadGraph(id);
  }

  public AbstractRequestFactory getRequestFactory() {
    return state.requestFactory;
  }

  /**
   * EntityCodex support.
   */
  public Splittable getSerializedProxyId(SimpleProxyId<?> stableId) {
    AutoBean<IdMessage> bean = MessageFactoryHolder.FACTORY.id();
    IdMessage ref = bean.as();
    ref.setServerId(stableId.getServerId());
    ref.setTypeToken(getRequestFactory().getTypeToken(stableId.getProxyClass()));
    if (stableId.isSynthetic()) {
      ref.setStrength(Strength.SYNTHETIC);
      ref.setSyntheticId(stableId.getSyntheticId());
    } else if (stableId.isEphemeral()) {
      ref.setStrength(Strength.EPHEMERAL);
      ref.setClientId(stableId.getClientId());
    }
    return AutoBeanCodex.encode(bean);
  }

  public boolean isChanged() {
    /*
     * NB: Don't use the presence of ephemeral objects for this test.
     *
     * Diff the objects until one is found to be different. It's not just a
     * simple flag-check because of the possibility of "unmaking" a change, per
     * the JavaDoc.
     *
     * TODO: try to get rid of the 'diffing' flag and optimize the diffing of
     * objects: http://code.google.com/p/google-web-toolkit/issues/detail?id=7379
     */
    assert !state.diffing;
    state.diffing = true;
    try {
      for (AutoBean<? extends BaseProxy> bean : state.editedProxies.values()) {
        AutoBean<?> previous = bean.getTag(Constants.PARENT_OBJECT);
        if (previous == null) {
          // Compare to empty object
          Class<?> proxyClass = stableId(bean).getProxyClass();
          previous = getAutoBeanFactory().create(proxyClass);
        }
        if (!AutoBeanUtils.diff(previous, bean).isEmpty()) {
          return true;
        }
      }
      return false;
    } finally {
      state.diffing = false;
    }
  }

  /**
   * EntityCodex support.
   */
  public boolean isEntityType(Class<?> clazz) {
    return state.requestFactory.isEntityType(clazz);
  }

  public boolean isLocked() {
    return state.locked;
  }

  /**
   * EntityCodex support.
   */
  public boolean isValueType(Class<?> clazz) {
    return state.requestFactory.isValueType(clazz);
  }

  public void setFireDisabled(boolean disabled) {
    state.fireDisabled = disabled;
  }

  /**
   * Called by generated subclasses to enqueue a method invocation.
   */
  protected void addInvocation(AbstractRequest<?, ?> request) {
    state.dialect.addInvocation(request);
  };

  /**
   * Creates a new proxy with an assigned ID.
   *
   * @param clazz The proxy type
   * @param id The id to be assigned to the new proxy
   * @param useAppendedContexts if {@code true} use the AutoBeanFactory types associated with any
   *          contexts that have been passed into {@link #append(RequestContext)}. If {@code false},
   *          this method will only create proxy types reachable from the implemented RequestContext
   *          interface.
   * @throws IllegalArgumentException if the requested proxy type cannot be created
   */
  protected <T extends BaseProxy> AutoBean<T> createProxy(Class<T> clazz, SimpleProxyId<T> id,
      boolean useAppendedContexts) {
    AutoBean<T> created = null;
    if (useAppendedContexts) {
      for (AbstractRequestContext ctx : state.appendedContexts) {
        created = ctx.getAutoBeanFactory().create(clazz);
        if (created != null) {
          break;
        }
      }
    } else {
      created = getAutoBeanFactory().create(clazz);
    }
    if (created != null) {
      created.setTag(STABLE_ID, id);
      return created;
    }
    throw new IllegalArgumentException("Unknown proxy type " + clazz.getName());
  }

  /**
   * Invoke the appropriate {@code onFailure} callbacks, possibly throwing an
   * {@link UmbrellaException} if one or more callbacks fails.
   */
  protected void fail(Receiver<Void> receiver, ServerFailure failure) {
    reuse();
    failure.setRequestContext(this);
    Set<Throwable> causes = null;
    for (AbstractRequest<?, ?> request : new ArrayList<AbstractRequest<?, ?>>(state.invocations)) {
      try {
        request.onFail(failure);
      } catch (Throwable t) {
        if (causes == null) {
          causes = new HashSet<Throwable>();
        }
        causes.add(t);
      }
    }
    if (receiver != null) {
      try {
        receiver.onFailure(failure);
      } catch (Throwable t) {
        if (causes == null) {
          causes = new HashSet<Throwable>();
        }
        causes.add(t);
      }
    }

    if (causes != null) {
      throw new UmbrellaException(causes);
    }
  }

  /**
   * Returns an AutoBeanFactory that can produce the types reachable only from this RequestContext.
   */
  protected abstract AutoBeanFactory getAutoBeanFactory();

  /**
   * Invoke the appropriate {@code onViolation} callbacks, possibly throwing an
   * {@link UmbrellaException} if one or more callbacks fails.
   */
  protected void violation(final Receiver<Void> receiver, Set<ConstraintViolation<?>> errors) {
    reuse();
    Set<Throwable> causes = null;
    for (AbstractRequest<?, ?> request : new ArrayList<AbstractRequest<?, ?>>(state.invocations)) {
      try {
        request.onViolation(errors);
      } catch (Throwable t) {
        if (causes == null) {
          causes = new HashSet<Throwable>();
        }
        causes.add(t);
      }
    }
    if (receiver != null) {
      try {
        receiver.onConstraintViolation(errors);
      } catch (Throwable t) {
        if (causes == null) {
          causes = new HashSet<Throwable>();
        }
        causes.add(t);
      }
    }

    if (causes != null) {
      throw new UmbrellaException(causes);
    }
  }

  /**
   * Resolves an IdMessage into an SimpleProxyId.
   */
  SimpleProxyId<BaseProxy> getId(IdMessage op) {
    if (Strength.SYNTHETIC.equals(op.getStrength())) {
      return allocateSyntheticId(op.getTypeToken(), op.getSyntheticId());
    }
    return state.requestFactory.getId(op.getTypeToken(), op.getServerId(), op.getClientId());
  }

  /**
   * Creates or retrieves a new canonical AutoBean to represent the given id in the returned
   * payload.
   */
  <Q extends BaseProxy> AutoBean<Q> getProxyForReturnPayloadGraph(SimpleProxyId<Q> id) {
    @SuppressWarnings("unchecked")
    AutoBean<Q> bean = (AutoBean<Q>) state.returnedProxies.get(id);
    if (bean == null) {
      Class<Q> proxyClass = id.getProxyClass();
      bean = createProxy(proxyClass, id, true);
      state.returnedProxies.put(id, bean);
    }

    return bean;
  }

  /**
   * Whether the RequestContext is currently diffing proxies.
   * <p>
   * This flag is used in {@link BaseProxyCategory} and
   * {@link EntityProxyCategory} to influence the way proxies are being
   * compared for equality, and to prevent auto-editing proxies when
   * walking reference properties.
   * <p>
   * See http://code.google.com/p/google-web-toolkit/issues/detail?id=5952
   * <p>
   * TODO: try to get rid of this flag.
   * See http://code.google.com/p/google-web-toolkit/issues/detail?id=7379
   */
  boolean isDiffing() {
    return state.diffing;
  }

  /**
   * Create a single OperationMessage that encapsulates the state of a proxy AutoBean.
   */
  AutoBean<OperationMessage> makeOperationMessage(SimpleProxyId<BaseProxy> stableId,
      AutoBean<?> proxyBean, boolean useDelta) {

    // The OperationMessages describes operations on exactly one entity
    AutoBean<OperationMessage> toReturn = MessageFactoryHolder.FACTORY.operation();
    OperationMessage operation = toReturn.as();
    operation.setTypeToken(state.requestFactory.getTypeToken(stableId.getProxyClass()));

    // Find the object to compare against
    AutoBean<?> parent;
    if (stableId.isEphemeral()) {
      // Newly-created object, use a blank object to compare against
      parent = createProxy(stableId.getProxyClass(), stableId, true);

      // Newly-created objects go into the persist operation bucket
      operation.setOperation(WriteOperation.PERSIST);
      // The ephemeral id is passed to the server
      operation.setClientId(stableId.getClientId());
      operation.setStrength(Strength.EPHEMERAL);
    } else if (stableId.isSynthetic()) {
      // Newly-created object, use a blank object to compare against
      parent = createProxy(stableId.getProxyClass(), stableId, true);

      // Newly-created objects go into the persist operation bucket
      operation.setOperation(WriteOperation.PERSIST);
      // The ephemeral id is passed to the server
      operation.setSyntheticId(stableId.getSyntheticId());
      operation.setStrength(Strength.SYNTHETIC);
    } else {
      parent = proxyBean.getTag(Constants.PARENT_OBJECT);
      // Requests involving existing objects use the persisted id
      operation.setServerId(stableId.getServerId());
      operation.setOperation(WriteOperation.UPDATE);
    }
    assert !useDelta || parent != null;

    // Send our version number to the server to cut down on future payloads
    String version = proxyBean.getTag(Constants.VERSION_PROPERTY_B64);
    if (version != null) {
      operation.setVersion(version);
    }

    Map<String, Object> diff = Collections.emptyMap();
    if (isEntityType(stableId.getProxyClass())) {
      // Compute what's changed on the client
      diff =
          useDelta ? AutoBeanUtils.diff(parent, proxyBean) : AutoBeanUtils
              .getAllProperties(proxyBean);
    } else if (isValueType(stableId.getProxyClass())) {
      // Send everything
      diff = AutoBeanUtils.getAllProperties(proxyBean);
    }

    if (!diff.isEmpty()) {
      Map<String, Splittable> propertyMap = new HashMap<String, Splittable>();
      for (Map.Entry<String, Object> entry : diff.entrySet()) {
        propertyMap.put(entry.getKey(), EntityCodex.encode(this, entry.getValue()));
      }
      operation.setPropertyMap(propertyMap);
    }
    return toReturn;
  }

  /**
   * Create a new EntityProxy from a snapshot in the return payload.
   *
   * @param id the EntityProxyId of the object
   * @param returnRecord the JSON map containing property/value pairs
   * @param operations the WriteOperation eventns to broadcast over the EventBus
   */
  <Q extends BaseProxy> Q processReturnOperation(SimpleProxyId<Q> id, OperationMessage op,
      WriteOperation... operations) {

    AutoBean<Q> toMutate = getProxyForReturnPayloadGraph(id);
    toMutate.setTag(Constants.VERSION_PROPERTY_B64, op.getVersion());

    final Map<String, Splittable> properties = op.getPropertyMap();
    if (properties != null) {
      // Apply updates
      toMutate.accept(new AutoBeanVisitor() {
        @Override
        public boolean visitReferenceProperty(String propertyName, AutoBean<?> value,
            PropertyContext ctx) {
          if (ctx.canSet()) {
            if (properties.containsKey(propertyName)) {
              Splittable raw = properties.get(propertyName);
              Object decoded = null;
              if (ctx.getType() == Map.class) {
                MapPropertyContext mapCtx = (MapPropertyContext) ctx;
                Class<?> keyType = mapCtx.getKeyType();
                Class<?> valueType = mapCtx.getValueType();
                decoded =
                    EntityCodex.decode(AbstractRequestContext.this, mapCtx.getType(), keyType,
                        valueType, raw);
              } else {
                Class<?> elementType =
                    ctx instanceof CollectionPropertyContext ? ((CollectionPropertyContext) ctx)
                        .getElementType() : null;
                decoded =
                    EntityCodex.decode(AbstractRequestContext.this, ctx.getType(), elementType, raw);
              }
              ctx.set(decoded);
            }
          }
          return false;
        }

        @Override
        public boolean visitValueProperty(String propertyName, Object value, PropertyContext ctx) {
          if (ctx.canSet()) {
            if (properties.containsKey(propertyName)) {
              Splittable raw = properties.get(propertyName);
              Object decoded = ValueCodex.decode(ctx.getType(), raw);
              /*
               * Hack for Date subtypes, consider generalizing for "custom serializers"
               */
              if (decoded != null && Date.class.equals(ctx.getType())) {
                decoded = new DatePoser((Date) decoded);
              }
              ctx.set(decoded);
            }
          }
          return false;
        }
      });
    }

    // Finished applying updates, freeze the bean
    makeImmutable(toMutate);
    Q proxy = toMutate.as();

    /*
     * Notify subscribers if the object differs from when it first came into the RequestContext.
     */
    if (operations != null && state.requestFactory.isEntityType(id.getProxyClass())) {
      for (WriteOperation writeOperation : operations) {
        if (writeOperation.equals(WriteOperation.UPDATE)
            && !state.requestFactory.hasVersionChanged(id, op.getVersion())) {
          // No updates if the server reports no change
          continue;
        }
        state.requestFactory.getEventBus().fireEventFromSource(
            new EntityProxyChange<EntityProxy>((EntityProxy) proxy, writeOperation),
            id.getProxyClass());
      }
    }
    return proxy;
  }

  /**
   * Get-or-create method for synthetic ids.
   *
   * @see #syntheticIds
   */
  private <Q extends BaseProxy> SimpleProxyId<Q> allocateSyntheticId(String typeToken,
      int syntheticId) {
    @SuppressWarnings("unchecked")
    SimpleProxyId<Q> toReturn = (SimpleProxyId<Q>) state.syntheticIds.get(syntheticId);
    if (toReturn == null) {
      toReturn =
          state.requestFactory.allocateId(state.requestFactory.<Q> getTypeFromToken(typeToken));
      state.syntheticIds.put(syntheticId, toReturn);
    }
    return toReturn;
  }

  private void checkLocked() {
    if (state.locked) {
      throw new IllegalStateException("A request is already in progress");
    }
  }

  /**
   * This method checks that a proxy object is either immutable, or already edited by this context.
   */
  private <T> AutoBean<T> checkStreamsNotCrossed(T object) {
    AutoBean<T> bean = AutoBeanUtils.getAutoBean(object);
    if (bean == null) {
      // Unexpected; some kind of foreign implementation?
      throw new IllegalArgumentException(object.getClass().getName());
    }

    State otherState = bean.getTag(REQUEST_CONTEXT_STATE);
    if (!bean.isFrozen() && otherState != this.state) {
      /*
       * This means something is way off in the weeds. If a bean is editable, it's supposed to be
       * associated with a RequestContext.
       */
      assert otherState != null : "Unfrozen bean with null RequestContext";

      /*
       * Already editing the object in another context or it would have been in the editing map.
       */
      throw new IllegalArgumentException("Attempting to edit an EntityProxy"
          + " previously edited by another RequestContext");
    }
    return bean;
  }

  /**
   * Shallow-clones an autobean and makes duplicates of the collection types. A regular
   * {@link AutoBean#clone} won't duplicate reference properties.
   */
  private <T extends BaseProxy> AutoBean<T> cloneBeanAndCollections(final AutoBean<T> toClone) {
    AutoBean<T> clone = toClone.getFactory().create(toClone.getType());
    clone.setTag(STABLE_ID, toClone.getTag(STABLE_ID));
    clone.setTag(Constants.VERSION_PROPERTY_B64, toClone.getTag(Constants.VERSION_PROPERTY_B64));
    /*
     * Take ownership here to prevent cycles in value objects from overflowing the stack.
     */
    takeOwnership(clone);
    clone.accept(new AutoBeanVisitor() {
      final Map<String, Object> values = AutoBeanUtils.getAllProperties(toClone);

      @Override
      public boolean visitCollectionProperty(String propertyName, AutoBean<Collection<?>> value,
          CollectionPropertyContext ctx) {
        // javac generics bug
        value =
            AutoBeanUtils.<Collection<?>, Collection<?>> getAutoBean((Collection<?>) values
                .get(propertyName));
        if (value != null) {
          Collection<Object> collection;
          if (List.class == ctx.getType()) {
            collection = new ArrayList<Object>();
          } else if (Set.class == ctx.getType()) {
            collection = new HashSet<Object>();
          } else {
            // Should not get here if the validator works correctly
            throw new IllegalArgumentException(ctx.getType().getName());
          }

          if (isValueType(ctx.getElementType()) || isEntityType(ctx.getElementType())) {
            /*
             * Proxies must be edited up-front so that the elements in the collection have stable
             * identity.
             */
            for (Object o : value.as()) {
              if (o == null) {
                collection.add(null);
              } else {
                collection.add(editProxy((BaseProxy) o));
              }
            }
          } else {
            // For simple values, just copy the values
            collection.addAll(value.as());
          }

          ctx.set(collection);
        }
        return false;
      }

      @Override
      public boolean visitReferenceProperty(String propertyName, AutoBean<?> value,
          PropertyContext ctx) {
        value = AutoBeanUtils.getAutoBean(values.get(propertyName));
        if (value != null) {
          if (isValueType(ctx.getType()) || isEntityType(ctx.getType())) {
            /*
             * Value proxies must be cloned upfront, since the value is replaced outright.
             */
            @SuppressWarnings("unchecked")
            AutoBean<BaseProxy> valueBean = (AutoBean<BaseProxy>) value;
            ctx.set(editProxy(valueBean.as()));
          } else {
            ctx.set(value.as());
          }
        }
        return false;
      }

      @Override
      public boolean visitValueProperty(String propertyName, Object value, PropertyContext ctx) {
        ctx.set(values.get(propertyName));
        return false;
      }
    });
    return clone;
  }

  private void doFire(Receiver<Void> receiver) {
    final Receiver<Void> finalReceiver;
    if (state.fireDisabled) {
      if (receiver != null) {
        if (state.fanout == null) {
          state.fanout = new FanoutReceiver<Void>();
        }
        state.fanout.add(receiver);
      }
      return;
    } else if (state.fanout != null) {
      if (receiver != null) {
        state.fanout.add(receiver);
      }
      finalReceiver = state.fanout;
    } else {
      finalReceiver = receiver;
    }
    checkLocked();
    state.locked = true;

    freezeEntities(true);

    String payload = state.dialect.makePayload();
    state.requestFactory.getRequestTransport().send(payload, new TransportReceiver() {
      public void onTransportFailure(ServerFailure failure) {
        fail(finalReceiver, failure);
      }

      public void onTransportSuccess(String payload) {
        state.dialect.processPayload(finalReceiver, payload);
      }
    });
  }

  /**
   * Set the frozen status of all EntityProxies owned by this context.
   */
  private void freezeEntities(boolean frozen) {
    for (AutoBean<?> bean : state.editedProxies.values()) {
      bean.setFrozen(frozen);
    }
  }

  /**
   * Make an EntityProxy immutable.
   */
  private void makeImmutable(final AutoBean<? extends BaseProxy> toMutate) {
    // Always diff'ed against itself, producing a no-op
    toMutate.setTag(Constants.PARENT_OBJECT, toMutate);
    // Act with entity-identity semantics
    toMutate.setTag(REQUEST_CONTEXT_STATE, null);
    toMutate.setFrozen(true);
  }

  /**
   * Create an InvocationMessage for each remote method call being made by the context.
   */
  private List<InvocationMessage> makePayloadInvocations() {
    MessageFactory f = MessageFactoryHolder.FACTORY;

    List<InvocationMessage> invocationMessages = new ArrayList<InvocationMessage>();
    for (AbstractRequest<?, ?> invocation : state.invocations) {
      // RequestData is produced by the generated subclass
      RequestData data = invocation.getRequestData();
      InvocationMessage message = f.invocation().as();

      // Operation; essentially a method descriptor
      message.setOperation(data.getOperation());

      // The arguments to the with() calls
      Set<String> refsToSend = data.getPropertyRefs();
      if (!refsToSend.isEmpty()) {
        message.setPropertyRefs(refsToSend);
      }

      // Parameter values or references
      List<Splittable> parameters = new ArrayList<Splittable>(data.getOrderedParameters().length);
      for (Object param : data.getOrderedParameters()) {
        parameters.add(EntityCodex.encode(this, param));
      }
      if (!parameters.isEmpty()) {
        message.setParameters(parameters);
      }

      invocationMessages.add(message);
    }
    return invocationMessages;
  }

  /**
   * Compute deltas for each entity seen by the context.
   * <p>
   * TODO(t.broyer): reduce payload size by only sending proxies that are
   * directly referenced by invocation arguments or by other proxies. For
   * backwards-compatibility with no-op requests and operation-only requests,
   * only do so when there's at least one invocation (or we can choose to
   * break backwards compatibility for those edge-cases).
   */
  private List<OperationMessage> makePayloadOperations() {
    assert isLocked();
    assert !state.diffing;
    state.diffing = true;
    try {
      List<OperationMessage> operations = new ArrayList<OperationMessage>();
      for (AutoBean<? extends BaseProxy> currentView : state.editedProxies.values()) {
        OperationMessage operation =
            makeOperationMessage(BaseProxyCategory.stableId(currentView), currentView, true).as();
        operations.add(operation);
      }
      return operations;
    } finally {
      state.diffing = false;
    }
  }

  /**
   * Process an array of OperationMessages.
   */
  private void processReturnOperations(ResponseMessage response) {
    List<OperationMessage> ops = response.getOperations();

    // If there are no observable effects, this will be null
    if (ops == null) {
      return;
    }

    for (OperationMessage op : ops) {
      SimpleProxyId<?> id = getId(op);
      WriteOperation[] toPropagate = null;

      // May be null if the server is returning an unpersisted object
      WriteOperation effect = op.getOperation();
      if (effect != null) {
        switch (effect) {
          case DELETE:
            toPropagate = DELETE_ONLY;
            break;
          case PERSIST:
            toPropagate = PERSIST_AND_UPDATE;
            break;
          case UPDATE:
            toPropagate = UPDATE_ONLY;
            break;
          default:
            // Should never reach here
            throw new RuntimeException(effect.toString());
        }
      }
      processReturnOperation(id, op, toPropagate);
    }

    assert state.returnedProxies.size() == ops.size();
  }

  /**
   * Ensures that any method arguments are retained in the context's sphere of influence.
   */
  private void retainArg(Object arg) {
    if (arg instanceof Iterable<?>) {
      for (Object o : (Iterable<?>) arg) {
        retainArg(o);
      }
    } else if (arg instanceof BaseProxy) {
      // Calling edit will validate and set up the tracking we need
      edit((BaseProxy) arg);
    }
  }

  /**
   * Returns the requests that were dequeued as part of reusing the context.
   */
  private void reuse() {
    freezeEntities(false);
    state.locked = false;
  }

  private void setState(State state) {
    this.state = state;
    state.addContext(this);
  }

  /**
   * Make the EnityProxy bean edited and owned by this RequestContext.
   */
  private <T extends BaseProxy> T takeOwnership(AutoBean<T> bean) {
    state.editedProxies.put(stableId(bean), bean);
    bean.setTag(REQUEST_CONTEXT_STATE, this.state);
    return bean.as();
  }
}
