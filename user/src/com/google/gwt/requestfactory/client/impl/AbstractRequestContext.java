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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.editor.client.AutoBean;
import com.google.gwt.editor.client.AutoBeanUtils;
import com.google.gwt.editor.client.AutoBeanVisitor;
import com.google.gwt.requestfactory.client.impl.messages.RequestContentData;
import com.google.gwt.requestfactory.client.impl.messages.ReturnRecord;
import com.google.gwt.requestfactory.client.impl.messages.SideEffects;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.ValueCodex;
import com.google.gwt.requestfactory.shared.Violation;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.requestfactory.shared.RequestTransport.TransportReceiver;
import com.google.gwt.requestfactory.shared.impl.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base implementations for RequestContext services.
 */
public class AbstractRequestContext implements RequestContext {
  private static final String PARENT_OBJECT = "parentObject";

  private List<AbstractRequest<?>> invocations = new ArrayList<AbstractRequest<?>>();
  private boolean locked;
  private final AbstractRequestFactory requestFactory;
  /**
   * A map of all EntityProxies that the RequestContext has interacted with.
   * Objects are placed into this map by being passed into {@link #edit} or as
   * an invocation argument.
   */
  private final Map<SimpleEntityProxyId<?>, EntityProxy> seenProxies = new LinkedHashMap<SimpleEntityProxyId<?>, EntityProxy>();
  private Set<Violation> errors = new LinkedHashSet<Violation>();

  protected AbstractRequestContext(AbstractRequestFactory factory) {
    this.requestFactory = factory;
  }

  /**
   * Create a new object, with an ephemeral id.
   */
  public <T extends EntityProxy> T create(Class<T> clazz) {
    checkLocked();

    AutoBean<T> created = requestFactory.createEntityProxy(clazz,
        requestFactory.allocateId(clazz));
    return makeEdited(created);
  }

  public <T extends EntityProxy> T edit(T object) {
    AutoBean<T> bean = checkStreamsNotCrossed(object);

    checkLocked();

    @SuppressWarnings("unchecked")
    T toReturn = (T) seenProxies.get(object.stableId());
    if (toReturn != null) {
      /*
       * If we've seen the object before, it might be because it was passed in
       * as a method argument. This does not guarantee its mutability, so check
       * that here before returning the cached object.
       */
      AutoBean<T> previouslySeen = AutoBeanUtils.getAutoBean(toReturn);
      if (!previouslySeen.isFrozen()) {
        return toReturn;
      }
    }

    // Create editable copies
    AutoBean<T> parent = bean;
    bean = cloneBeanAndCollections(bean);
    toReturn = makeEdited(bean);
    bean.setTag(PARENT_OBJECT, parent);
    return toReturn;
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

  public AbstractRequestFactory getRequestFactory() {
    return requestFactory;
  }

  public boolean isChanged() {
    /*
     * NB: Don't use the presence of ephemeral objects for this test.
     * 
     * Diffing the objects until one is found to be different. It's not just a
     * simple flag-check because of the possibility of "unmaking" a change, per
     * the JavaDoc.
     */
    for (EntityProxy edited : seenProxies.values()) {
      AutoBean<EntityProxy> bean = AutoBeanUtils.getAutoBean(edited);
      AutoBean<?> previous = AutoBeanUtils.getAutoBean((EntityProxy) bean.getTag(PARENT_OBJECT));
      if (previous == null) {
        // Compare to empty object
        previous = getRequestFactory().getAutoBeanFactory().create(
            edited.stableId().getProxyClass());
      }
      if (!AutoBeanUtils.diff(previous, bean).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  public boolean isLocked() {
    return locked;
  }

  public void reuse() {
    freezeEntities(false);
    locked = false;
  }

  /**
   * Called by individual invocations to aggregate all errors.
   */
  protected void addErrors(Collection<Violation> errors) {
    this.errors.addAll(errors);
  }

  /**
   * Called by generated subclasses to enqueue a method invocation.
   */
  protected void addInvocation(AbstractRequest<?> request) {
    if (invocations.size() > 0) {
      // TODO(bobv): Upgrade wire protocal and server to handle chains
      throw new IllegalStateException("Method chaining not implemented");
    }
    invocations.add(request);
    for (Object arg : request.getRequestData().getParameters()) {
      retainArg(arg);
    }
  }

  /**
   * Called by generated subclasses when decoding a request.
   */
  EntityProxy getSeenEntityProxy(SimpleEntityProxyId<?> id) {
    return seenProxies.get(id);
  }

  /**
   * Apply the deltas in a ReturnRecord to an EntityProxy.
   * 
   * @param id the EntityProxyId of the object being mutated
   * @param returnRecord the JSON map containing property/value pairs
   * @param operations the WriteOperation eventns to broadcast over the EventBus
   */
  <Q extends EntityProxy> Q processReturnRecord(SimpleEntityProxyId<Q> id,
      final ReturnRecord returnRecord, WriteOperation... operations) {
    @SuppressWarnings("unchecked")
    Q proxy = (Q) seenProxies.get(id);
    AutoBean<Q> toMutate;

    if (proxy == null) {
      // The server is sending us an object that hasn't been seen before
      assert !id.isEphemeral();
      Class<Q> proxyClass = id.getProxyClass();
      toMutate = requestFactory.createEntityProxy(proxyClass, id);
      makeEdited(toMutate);
    } else {
      // Create a new copy of the object
      AutoBean<Q> original = AutoBeanUtils.getAutoBean(proxy);
      toMutate = cloneBeanAndCollections(original);
    }

    proxy = toMutate.as();

    // Apply updates
    toMutate.accept(new AutoBeanVisitor() {
      @Override
      public boolean visitReferenceProperty(String propertyName,
          AutoBean<?> value, PropertyContext ctx) {
        if (ctx.canSet()) {
          if (returnRecord.hasProperty(propertyName)) {
            Object raw = returnRecord.get(propertyName);
            if (returnRecord.isNull(propertyName)) {
              ctx.set(null);
            } else {
              Object decoded = EntityCodex.decode(ctx.getType(),
                  ctx.getElementType(), AbstractRequestContext.this, raw);
              ctx.set(decoded);
            }
          }
        }
        return false;
      }

      @Override
      public boolean visitValueProperty(String propertyName, Object value,
          PropertyContext ctx) {
        if (ctx.canSet()) {
          if (returnRecord.hasProperty(propertyName)) {
            Object raw = returnRecord.get(propertyName);
            if (returnRecord.isNull(propertyName)) {
              ctx.set(null);
            } else {
              Object decoded = ValueCodex.convertFromString(ctx.getType(),
                  String.valueOf(raw));
              ctx.set(decoded);
            }
          }
        }
        return false;
      }
    });

    // Finished applying updates, freeze the bean
    makeImmutable(toMutate);

    /*
     * Notify subscribers if the object differs from when it first came into the
     * RequestContext.
     */
    if (operations != null) {
      for (WriteOperation op : operations) {
        if (op.equals(WriteOperation.UPDATE)
            && !requestFactory.hasVersionChanged(id, returnRecord.getVersion())) {
          // No updates if the server reports no change
          continue;
        }
        requestFactory.getEventBus().fireEventFromSource(
            new EntityProxyChange<EntityProxy>(proxy, op), id.getProxyClass());
      }
    }
    return proxy;
  }

  /**
   * Process a SideEffects message.
   */
  void processSideEffects(SideEffects sideEffects) {
    JsArray<ReturnRecord> persisted = sideEffects.getPersist();
    if (persisted != null) {
      processReturnRecords(persisted, WriteOperation.PERSIST);
    }
    JsArray<ReturnRecord> updated = sideEffects.getUpdate();
    if (updated != null) {
      processReturnRecords(updated, WriteOperation.UPDATE);
    }
    JsArray<ReturnRecord> deleted = sideEffects.getDelete();
    if (deleted != null) {
      processReturnRecords(deleted, WriteOperation.DELETE);
    }
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

    RequestContext context = bean.getTag(EntityProxyCategory.REQUEST_CONTEXT);
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
  private <T> AutoBean<T> cloneBeanAndCollections(AutoBean<T> toClone) {
    AutoBean<T> clone = toClone.clone(false);
    clone.accept(new AutoBeanVisitor() {
      @Override
      public boolean visitReferenceProperty(String propertyName,
          AutoBean<?> value, PropertyContext ctx) {
        if (value != null) {
          if (List.class == ctx.getType()) {
            ctx.set(new ArrayList<Object>((List<?>) value.as()));
          } else if (Set.class == ctx.getType()) {
            ctx.set(new HashSet<Object>((Set<?>) value.as()));
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
      public void onTransportFailure(String message) {
        ServerFailure failure = new ServerFailure(message, null, null);
        try {
          // TODO: chained methods
          assert invocations.size() == 1;
          invocations.get(0).fail(failure);
          if (receiver != null) {
            receiver.onFailure(failure);
          }
        } finally {
          postRequestCleanup();
        }
      }

      public void onTransportSuccess(String payload) {
        try {
          // TODO: chained methods
          assert invocations.size() == 1;
          invocations.get(0).handleResponseText(payload);

          if (receiver != null) {
            if (errors.isEmpty()) {
              receiver.onSuccess(null);
              // After success, shut down the context
              seenProxies.clear();
              invocations.clear();
            } else {
              receiver.onViolation(errors);
            }
          }
        } finally {
          postRequestCleanup();
        }
      }
    });
  }

  /**
   * Set the frozen status of all EntityProxies owned by this context.
   */
  private void freezeEntities(boolean frozen) {
    for (EntityProxy proxy : seenProxies.values()) {
      AutoBean<?> bean = AutoBeanUtils.getAutoBean(proxy);
      bean.setFrozen(frozen);
    }
  }

  /**
   * Make the EnityProxy bean edited and owned by this RequestContext.
   */
  private <T extends EntityProxy> T makeEdited(AutoBean<T> bean) {
    T toReturn = bean.as();
    seenProxies.put(EntityProxyCategory.stableId(bean), toReturn);
    bean.setTag(EntityProxyCategory.REQUEST_CONTEXT,
        AbstractRequestContext.this);
    return toReturn;
  }

  /**
   * Make an EntityProxy immutable.
   */
  private void makeImmutable(final AutoBean<? extends EntityProxy> toMutate) {
    // Always diff'ed against itself, producing a no-op
    toMutate.setTag(PARENT_OBJECT, toMutate);
    // Act with entity-identity semantics
    toMutate.setTag(EntityProxyCategory.REQUEST_CONTEXT, null);
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
    // TODO: Chained invocations
    assert invocations.size() == 1 : "addInvocation() should have failed";

    // Produces the contentData payload fragment
    RequestContentData data = new RequestContentData();

    // Compute deltas for each entity seen by the context
    for (EntityProxy proxy : seenProxies.values()) {
      boolean isPersist = false;
      @SuppressWarnings("unchecked")
      SimpleEntityProxyId<EntityProxy> stableId = (SimpleEntityProxyId<EntityProxy>) proxy.stableId();

      // Encoded string representations of the properties
      Map<String, String> encoded = new LinkedHashMap<String, String>();

      {
        // Find the object to compare against
        AutoBean<?> currentView = AutoBeanUtils.getAutoBean(proxy);
        AutoBean<?> parent = currentView.getTag(PARENT_OBJECT);
        if (parent == null) {
          // Newly-created object, use a blank object to compare against
          parent = requestFactory.createEntityProxy(stableId.getProxyClass(),
              stableId);

          // Newly-created objects go into the persist operation bucket
          isPersist = true;
          // The ephemeral id is passed to the server
          String clientId = String.valueOf(stableId.getClientId());
          encoded.put(Constants.ENCODED_ID_PROPERTY,
              ValueCodex.encodeForJsonPayload(clientId));
        } else {
          // Requests involving existing objects use the persisted id
          encoded.put(Constants.ENCODED_ID_PROPERTY,
              ValueCodex.encodeForJsonPayload(stableId.getServerId()));
        }

        // Compute the diff
        Map<String, Object> diff = AutoBeanUtils.diff(parent, currentView);
        for (Map.Entry<String, Object> entry : diff.entrySet()) {
          // Make JSON representations of the new values
          encoded.put(entry.getKey(),
              EntityCodex.encodeForJsonPayload(entry.getValue()));
        }
      }

      // Append the payload fragment to the correct bucket
      String typeToken = requestFactory.getTypeToken(stableId.getProxyClass());
      if (isPersist) {
        data.addPersist(typeToken, encoded);
      } else {
        data.addUpdate(typeToken, encoded);
      }
    }

    AbstractRequest<?> request = invocations.get(0);
    // Known issue that the data is double-quoted
    Map<String, String> requestMap = request.getRequestData().getRequestMap(
        data.toJson());
    String string = RequestContentData.flattenKeysToExpressions(requestMap);
    return string;
  }

  /**
   * Delete state that's no longer required.
   */
  private void postRequestCleanup() {
    errors.clear();
  }

  /**
   * Process an array of ReturnRecords, delegating to
   * {@link #processReturnRecord}.
   */
  private void processReturnRecords(JsArray<ReturnRecord> records,
      WriteOperation operations) {
    for (int i = 0, j = records.length(); i < j; i++) {
      ReturnRecord record = records.get(i);
      SimpleEntityProxyId<EntityProxy> id = requestFactory.getId(
          record.getSchema(), record.getEncodedId(), record.getFutureId());
      processReturnRecord(id, record, operations);
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
}
