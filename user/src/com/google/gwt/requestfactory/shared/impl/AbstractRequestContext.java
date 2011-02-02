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
package com.google.gwt.requestfactory.shared.impl;

import static com.google.gwt.requestfactory.shared.impl.BaseProxyCategory.stableId;
import static com.google.gwt.requestfactory.shared.impl.Constants.REQUEST_CONTEXT;

import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanCodex;
import com.google.gwt.autobean.shared.AutoBeanUtils;
import com.google.gwt.autobean.shared.AutoBeanVisitor;
import com.google.gwt.autobean.shared.Splittable;
import com.google.gwt.autobean.shared.ValueCodex;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.RequestTransport.TransportReceiver;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.ValueProxy;
import com.google.gwt.requestfactory.shared.Violation;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.requestfactory.shared.impl.posers.DatePoser;
import com.google.gwt.requestfactory.shared.messages.IdMessage;
import com.google.gwt.requestfactory.shared.messages.IdMessage.Strength;
import com.google.gwt.requestfactory.shared.messages.InvocationMessage;
import com.google.gwt.requestfactory.shared.messages.MessageFactory;
import com.google.gwt.requestfactory.shared.messages.OperationMessage;
import com.google.gwt.requestfactory.shared.messages.RequestMessage;
import com.google.gwt.requestfactory.shared.messages.ResponseMessage;
import com.google.gwt.requestfactory.shared.messages.ServerFailureMessage;
import com.google.gwt.requestfactory.shared.messages.ViolationMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base implementations for RequestContext services.
 */
public class AbstractRequestContext implements RequestContext,
    EntityCodex.EntitySource {
  private class MyViolation implements Violation {

    private final BaseProxy currentProxy;
    private final EntityProxyId<?> id;
    private final String message;
    private final String path;
    private final BaseProxy parentProxy;

    public MyViolation(ViolationMessage message) {
      // Support violations for value objects.
      SimpleProxyId<BaseProxy> baseId = getId(message);
      if (baseId instanceof EntityProxyId<?>) {
        id = (EntityProxyId<?>) baseId;
      } else {
        id = null;
      }
      // The stub is empty, since we don't process any OperationMessages
      AutoBean<BaseProxy> stub = getProxyForReturnPayloadGraph(baseId);

      // So pick up the instance that we just sent to the server
      AutoBean<?> edited = editedProxies.get(BaseProxyCategory.stableId(stub));
      currentProxy = (BaseProxy) edited.as();

      // Try to find the original, immutable version.
      AutoBean<BaseProxy> parentBean = edited.getTag(Constants.PARENT_OBJECT);
      parentProxy = parentBean == null ? null : parentBean.as();
      path = message.getPath();
      this.message = message.getMessage();
    }

    public BaseProxy getInvalidProxy() {
      return currentProxy;
    }

    public String getMessage() {
      return message;
    }

    public BaseProxy getOriginalProxy() {
      return parentProxy;
    }

    public String getPath() {
      return path;
    }

    public EntityProxyId<?> getProxyId() {
      return id;
    }
  }

  private static final WriteOperation[] DELETE_ONLY = {WriteOperation.DELETE};
  private static final WriteOperation[] PERSIST_AND_UPDATE = {
    WriteOperation.PERSIST, WriteOperation.UPDATE};
  private static final WriteOperation[] UPDATE_ONLY = {WriteOperation.UPDATE};

  private final List<AbstractRequest<?>> invocations = new ArrayList<AbstractRequest<?>>();
  private boolean locked;
  private final AbstractRequestFactory requestFactory;
  /**
   * A map of all EntityProxies that the RequestContext has interacted with.
   * Objects are placed into this map by being passed into {@link #edit} or as
   * an invocation argument.
   */
  private final Map<SimpleProxyId<?>, AutoBean<? extends BaseProxy>> editedProxies = new LinkedHashMap<SimpleProxyId<?>, AutoBean<? extends BaseProxy>>();
  /**
   * A map that contains the canonical instance of an entity to return in the
   * return graph, since this is built from scratch.
   */
  private final Map<SimpleProxyId<?>, AutoBean<?>> returnedProxies = new HashMap<SimpleProxyId<?>, AutoBean<?>>();

  /**
   * A map that allows us to handle the case where the server has sent back an
   * unpersisted entity. Because we assume that the server is stateless, the
   * client will need to swap out the request-local ids with a regular
   * client-allocated id.
   */
  private final Map<Integer, SimpleProxyId<?>> syntheticIds = new HashMap<Integer, SimpleProxyId<?>>();

  protected AbstractRequestContext(AbstractRequestFactory factory) {
    this.requestFactory = factory;
  }

  /**
   * Create a new object, with an ephemeral id.
   */
  public <T extends BaseProxy> T create(Class<T> clazz) {
    checkLocked();

    SimpleProxyId<T> id = requestFactory.allocateId(clazz);
    AutoBean<T> created = requestFactory.createProxy(clazz, id);
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
    AutoBean<T> previouslySeen = (AutoBean<T>) editedProxies.get(BaseProxyCategory.stableId(bean));
    if (previouslySeen != null && !previouslySeen.isFrozen()) {
      /*
       * If we've seen the object before, it might be because it was passed in
       * as a method argument. This does not guarantee its mutability, so check
       * that here before returning the cached object.
       */
      return previouslySeen.as();
    }

    // Create editable copies
    AutoBean<T> parent = bean;
    bean = cloneBeanAndCollections(bean);
    bean.setTag(Constants.PARENT_OBJECT, parent);
    return bean.as();
  }

  /**
   * Make sure there's a default receiver so errors don't get dropped. This
   * behavior should be revisited when chaining is supported, depending on
   * whether or not chained invocations can fail independently.
   */
  public void fire() {
    boolean needsReceiver = true;
    for (AbstractRequest<?> request : invocations) {
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
  public <Q extends BaseProxy> AutoBean<Q> getBeanForPayload(
      Splittable serializedProxyId) {
    IdMessage ref = AutoBeanCodex.decode(MessageFactoryHolder.FACTORY,
        IdMessage.class, serializedProxyId).as();
    @SuppressWarnings("unchecked")
    SimpleProxyId<Q> id = (SimpleProxyId<Q>) getId(ref);
    return getProxyForReturnPayloadGraph(id);
  }

  public AbstractRequestFactory getRequestFactory() {
    return requestFactory;
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
     */
    for (AutoBean<?> bean : editedProxies.values()) {
      AutoBean<?> previous = bean.getTag(Constants.PARENT_OBJECT);
      if (previous == null) {
        // Compare to empty object
        Class<?> proxyClass = ((EntityProxy) bean.as()).stableId().getProxyClass();
        previous = getRequestFactory().getAutoBeanFactory().create(proxyClass);
      }
      if (!AutoBeanUtils.diff(previous, bean).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * EntityCodex support.
   */
  public boolean isEntityType(Class<?> clazz) {
    return requestFactory.isEntityType(clazz);
  }

  public boolean isLocked() {
    return locked;
  }

  /**
   * EntityCodex support.
   */
  public boolean isValueType(Class<?> clazz) {
    return requestFactory.isValueType(clazz);
  }

  /**
   * Called by generated subclasses to enqueue a method invocation.
   */
  protected void addInvocation(AbstractRequest<?> request) {
    invocations.add(request);
    for (Object arg : request.getRequestData().getParameters()) {
      retainArg(arg);
    }
  }

  /**
   * Resolves an IdMessage into an SimpleProxyId.
   */
  SimpleProxyId<BaseProxy> getId(IdMessage op) {
    if (Strength.SYNTHETIC.equals(op.getStrength())) {
      return allocateSyntheticId(op.getTypeToken(), op.getSyntheticId());
    }
    return requestFactory.getId(op.getTypeToken(), op.getServerId(),
        op.getClientId());
  }

  /**
   * Creates or retrieves a new canonical AutoBean to represent the given id in
   * the returned payload.
   */
  <Q extends BaseProxy> AutoBean<Q> getProxyForReturnPayloadGraph(
      SimpleProxyId<Q> id) {
    @SuppressWarnings("unchecked")
    AutoBean<Q> bean = (AutoBean<Q>) returnedProxies.get(id);
    if (bean == null) {
      Class<Q> proxyClass = id.getProxyClass();
      bean = requestFactory.createProxy(proxyClass, id);
      returnedProxies.put(id, bean);
    }

    return bean;
  }

  /**
   * Create a single OperationMessage that encapsulates the state of a proxy
   * AutoBean.
   */
  AutoBean<OperationMessage> makeOperationMessage(
      SimpleProxyId<BaseProxy> stableId, AutoBean<?> proxyBean, boolean useDelta) {

    // The OperationMessages describes operations on exactly one entity
    AutoBean<OperationMessage> toReturn = MessageFactoryHolder.FACTORY.operation();
    OperationMessage operation = toReturn.as();
    operation.setTypeToken(requestFactory.getTypeToken(stableId.getProxyClass()));

    // Find the object to compare against
    AutoBean<?> parent;
    if (stableId.isEphemeral()) {
      // Newly-created object, use a blank object to compare against
      parent = requestFactory.createProxy(stableId.getProxyClass(), stableId);

      // Newly-created objects go into the persist operation bucket
      operation.setOperation(WriteOperation.PERSIST);
      // The ephemeral id is passed to the server
      operation.setClientId(stableId.getClientId());
      operation.setStrength(Strength.EPHEMERAL);
    } else if (stableId.isSynthetic()) {
      // Newly-created object, use a blank object to compare against
      parent = requestFactory.createProxy(stableId.getProxyClass(), stableId);

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
      diff = useDelta ? AutoBeanUtils.diff(parent, proxyBean)
          : AutoBeanUtils.getAllProperties(proxyBean);
    } else if (isValueType(stableId.getProxyClass())) {
      // Send everything
      diff = AutoBeanUtils.getAllProperties(proxyBean);
    }

    if (!diff.isEmpty()) {
      Map<String, Splittable> propertyMap = new HashMap<String, Splittable>();
      for (Map.Entry<String, Object> entry : diff.entrySet()) {
        propertyMap.put(entry.getKey(),
            EntityCodex.encode(this, entry.getValue()));
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
  <Q extends BaseProxy> Q processReturnOperation(SimpleProxyId<Q> id,
      OperationMessage op, WriteOperation... operations) {

    AutoBean<Q> toMutate = getProxyForReturnPayloadGraph(id);
    toMutate.setTag(Constants.VERSION_PROPERTY_B64, op.getVersion());

    final Map<String, Splittable> properties = op.getPropertyMap();
    if (properties != null) {
      // Apply updates
      toMutate.accept(new AutoBeanVisitor() {
        @Override
        public boolean visitReferenceProperty(String propertyName,
            AutoBean<?> value, PropertyContext ctx) {
          if (ctx.canSet()) {
            if (properties.containsKey(propertyName)) {
              Splittable raw = properties.get(propertyName);
              Class<?> elementType = ctx instanceof CollectionPropertyContext
                  ? ((CollectionPropertyContext) ctx).getElementType() : null;
              Object decoded = EntityCodex.decode(AbstractRequestContext.this,
                  ctx.getType(), elementType, raw);
              ctx.set(decoded);
            }
          }
          return false;
        }

        @Override
        public boolean visitValueProperty(String propertyName, Object value,
            PropertyContext ctx) {
          if (ctx.canSet()) {
            if (properties.containsKey(propertyName)) {
              Splittable raw = properties.get(propertyName);
              Object decoded = ValueCodex.decode(ctx.getType(), raw);
              /*
               * Hack for Date subtypes, consider generalizing for
               * "custom serializers"
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
     * Notify subscribers if the object differs from when it first came into the
     * RequestContext.
     */
    if (operations != null && requestFactory.isEntityType(id.getProxyClass())) {
      for (WriteOperation writeOperation : operations) {
        if (writeOperation.equals(WriteOperation.UPDATE)
            && !requestFactory.hasVersionChanged(id, op.getVersion())) {
          // No updates if the server reports no change
          continue;
        }
        requestFactory.getEventBus().fireEventFromSource(
            new EntityProxyChange<EntityProxy>((EntityProxy) proxy,
                writeOperation), id.getProxyClass());
      }
    }
    return proxy;
  }

  /**
   * Get-or-create method for synthetic ids.
   * 
   * @see #syntheticIds
   */
  private <Q extends BaseProxy> SimpleProxyId<Q> allocateSyntheticId(
      String typeToken, int syntheticId) {
    @SuppressWarnings("unchecked")
    SimpleProxyId<Q> toReturn = (SimpleProxyId<Q>) syntheticIds.get(syntheticId);
    if (toReturn == null) {
      toReturn = requestFactory.allocateId(requestFactory.<Q> getTypeFromToken(typeToken));
      syntheticIds.put(syntheticId, toReturn);
    }
    return toReturn;
  }

  private void checkLocked() {
    if (locked) {
      throw new IllegalStateException("A request is already in progress");
    }
  }

  /**
   * This method checks that a proxy object is either immutable, or already
   * edited by this context.
   */
  private <T> AutoBean<T> checkStreamsNotCrossed(T object) {
    AutoBean<T> bean = AutoBeanUtils.getAutoBean(object);
    if (bean == null) {
      // Unexpected; some kind of foreign implementation?
      throw new IllegalArgumentException(object.getClass().getName());
    }

    RequestContext context = bean.getTag(REQUEST_CONTEXT);
    if (!bean.isFrozen() && context != this) {
      /*
       * This means something is way off in the weeds. If a bean is editable,
       * it's supposed to be associated with a RequestContext.
       */
      assert context != null : "Unfrozen bean with null RequestContext";

      /*
       * Already editing the object in another context or it would have been in
       * the editing map.
       */
      throw new IllegalArgumentException("Attempting to edit an EntityProxy"
          + " previously edited by another RequestContext");
    }
    return bean;
  }

  /**
   * Shallow-clones an autobean and makes duplicates of the collection types. A
   * regular {@link AutoBean#clone} won't duplicate reference properties.
   */
  private <T extends BaseProxy> AutoBean<T> cloneBeanAndCollections(
      AutoBean<T> toClone) {
    AutoBean<T> clone = toClone.clone(false);
    /*
     * Take ownership here to prevent cycles in value objects from overflowing
     * the stack.
     */
    takeOwnership(clone);
    clone.accept(new AutoBeanVisitor() {

      @Override
      public boolean visitCollectionProperty(String propertyName,
          AutoBean<Collection<?>> value, CollectionPropertyContext ctx) {
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

          if (isValueType(ctx.getElementType())) {
            /*
             * Value proxies must be cloned upfront, since the value is replaced
             * outright.
             */
            for (Object o : value.as()) {
              if (o == null) {
                collection.add(null);
              } else {
                collection.add(editProxy((ValueProxy) o));
              }
            }
          } else {
            // For entities and simple values, just alias the values
            collection.addAll(value.as());
          }

          ctx.set(collection);
        }
        return false;
      }

      @Override
      public boolean visitReferenceProperty(String propertyName,
          AutoBean<?> value, PropertyContext ctx) {
        if (value != null) {
          if (isValueType(ctx.getType())) {
            /*
             * Value proxies must be cloned upfront, since the value is replaced
             * outright.
             */
            @SuppressWarnings("unchecked")
            AutoBean<ValueProxy> valueBean = (AutoBean<ValueProxy>) value;
            ctx.set(editProxy(valueBean.as()));
          }
        }
        return false;
      }
    });
    return clone;
  }

  private void doFire(final Receiver<Void> receiver) {
    checkLocked();
    locked = true;

    freezeEntities(true);

    String payload = makePayload();
    requestFactory.getRequestTransport().send(payload, new TransportReceiver() {
      public void onTransportFailure(ServerFailure failure) {
        fail(receiver, failure);
      }

      public void onTransportSuccess(String payload) {
        ResponseMessage response = AutoBeanCodex.decode(
            MessageFactoryHolder.FACTORY, ResponseMessage.class, payload).as();
        if (response.getGeneralFailure() != null) {
          ServerFailureMessage failure = response.getGeneralFailure();
          ServerFailure fail = new ServerFailure(failure.getMessage(),
              failure.getExceptionType(), failure.getStackTrace(),
              failure.isFatal());

          fail(receiver, fail);
          return;
        }

        // Process violations and then stop
        if (response.getViolations() != null) {
          Set<Violation> errors = new HashSet<Violation>();
          for (ViolationMessage message : response.getViolations()) {
            errors.add(new MyViolation(message));
          }

          violation(receiver, errors);
          return;
        }

        // Process operations
        processReturnOperations(response);

        // Send return values
        Set<Throwable> causes = null;
        for (int i = 0, j = invocations.size(); i < j; i++) {
          try {
            if (response.getStatusCodes().get(i)) {
              invocations.get(i).onSuccess(
                  response.getInvocationResults().get(i));
            } else {
              ServerFailureMessage failure = AutoBeanCodex.decode(
                  MessageFactoryHolder.FACTORY, ServerFailureMessage.class,
                  response.getInvocationResults().get(i)).as();
              invocations.get(i).onFail(
                  new ServerFailure(failure.getMessage(),
                      failure.getExceptionType(), failure.getStackTrace(),
                      failure.isFatal()));
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
        editedProxies.clear();
        invocations.clear();
        returnedProxies.clear();

        if (causes != null) {
          throw new UmbrellaException(causes);
        }
      }

      /**
       * Invoke the appropriate {@code onFailure} callbacks, possibly throwing
       * an {@link UmbrellaException} if one or more callbacks fails.
       */
      private void fail(Receiver<Void> receiver, ServerFailure failure) {
        reuse();
        Set<Throwable> causes = null;
        for (AbstractRequest<?> request : new ArrayList<AbstractRequest<?>>(
            invocations)) {
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
       * Invoke the appropriate {@code onViolation} callbacks, possibly throwing
       * an {@link UmbrellaException} if one or more callbacks fails.
       */
      private void violation(final Receiver<Void> receiver,
          Set<Violation> errors) {
        reuse();
        Set<Throwable> causes = null;
        for (AbstractRequest<?> request : new ArrayList<AbstractRequest<?>>(
            invocations)) {
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
            receiver.onViolation(errors);
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
    });
  }

  /**
   * Set the frozen status of all EntityProxies owned by this context.
   */
  private void freezeEntities(boolean frozen) {
    for (AutoBean<?> bean : editedProxies.values()) {
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
    toMutate.setTag(REQUEST_CONTEXT, null);
    toMutate.setFrozen(true);
  }

  /**
   * Assemble all of the state that has been accumulated in this context. This
   * includes:
   * <ul>
   * <li>Diffs accumulated on objects passed to {@link #edit}.
   * <li>Invocations accumulated as Request subtypes passed to
   * {@link #addInvocation}.
   * </ul>
   */
  private String makePayload() {
    // Get the factory from the runtime-specific holder.
    MessageFactory f = MessageFactoryHolder.FACTORY;

    List<OperationMessage> operations = makePayloadOperations();
    List<InvocationMessage> invocationMessages = makePayloadInvocations();

    // Create the outer envelope message
    AutoBean<RequestMessage> bean = f.request();
    RequestMessage requestMessage = bean.as();
    if (!invocationMessages.isEmpty()) {
      requestMessage.setInvocations(invocationMessages);
    }
    if (!operations.isEmpty()) {
      requestMessage.setOperations(operations);
    }
    return AutoBeanCodex.encode(bean).getPayload();
  }

  /**
   * Create an InvocationMessage for each remote method call being made by the
   * context.
   */
  private List<InvocationMessage> makePayloadInvocations() {
    MessageFactory f = MessageFactoryHolder.FACTORY;

    List<InvocationMessage> invocationMessages = new ArrayList<InvocationMessage>();
    for (AbstractRequest<?> invocation : invocations) {
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
      List<Splittable> parameters = new ArrayList<Splittable>(
          data.getParameters().length);
      for (Object param : data.getParameters()) {
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
   */
  private List<OperationMessage> makePayloadOperations() {
    List<OperationMessage> operations = new ArrayList<OperationMessage>();
    for (AutoBean<? extends BaseProxy> currentView : editedProxies.values()) {
      OperationMessage operation = makeOperationMessage(
          BaseProxyCategory.stableId(currentView), currentView, true).as();
      operations.add(operation);
    }
    return operations;
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
  }

  /**
   * Ensures that any method arguments are retained in the context's sphere of
   * influence.
   */
  private void retainArg(Object arg) {
    if (arg instanceof Iterable<?>) {
      for (Object o : (Iterable<?>) arg) {
        retainArg(o);
      }
    } else if (arg instanceof EntityProxy) {
      // Calling edit will validate and set up the tracking we need
      edit((EntityProxy) arg);
    }
  }

  /**
   * Returns the requests that were dequeued as part of reusing the context.
   */
  private void reuse() {
    freezeEntities(false);
    locked = false;
  }

  /**
   * Make the EnityProxy bean edited and owned by this RequestContext.
   */
  private <T extends BaseProxy> T takeOwnership(AutoBean<T> bean) {
    editedProxies.put(stableId(bean), bean);
    bean.setTag(REQUEST_CONTEXT, AbstractRequestContext.this);
    return bean.as();
  }
}
