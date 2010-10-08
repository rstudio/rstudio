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
import com.google.gwt.requestfactory.client.impl.messages.JsonResults;
import com.google.gwt.requestfactory.client.impl.messages.JsonServerException;
import com.google.gwt.requestfactory.client.impl.messages.RelatedObjects;
import com.google.gwt.requestfactory.client.impl.messages.RequestData;
import com.google.gwt.requestfactory.client.impl.messages.ReturnRecord;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.Violation;
import com.google.gwt.requestfactory.shared.WriteOperation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract implementation of {@link Request}. Each request stores a
 * {@link DeltaValueStoreJsonImpl}.
 *
 * @param <T> return type
 */
public abstract class AbstractRequest<T> implements Request<T>,
    InstanceRequest<EntityProxy, T> {

  /**
   * Used by generated subtypes.
   */
  protected final Set<String> propertyRefs = new HashSet<String>();
  protected final AbstractRequestContext requestContext;
  private Receiver<? super T> receiver;
  private RequestData requestData;

  protected AbstractRequest(AbstractRequestContext requestContext) {
    this.requestContext = requestContext;
  }

  public void fire() {
    requestContext.fire();
  }

  public void fire(Receiver<? super T> receiver) {
    to(receiver);
    fire();
  }

  /**
   * Returns the properties.
   */
  public Set<String> getPropertyRefs() {
    return Collections.unmodifiableSet(propertyRefs);
  }

  public RequestData getRequestData() {
    if (requestData == null) {
      requestData = makeRequestData();
    }
    return requestData;
  }

  public void handleResponseText(String responseText) {
    JsonResults results = JsonResults.fromResults(responseText);
    JsonServerException cause = results.getException();
    if (cause != null) {
      fail(new ServerFailure(cause.getMessage(), cause.getType(),
          cause.getTrace()));
      return;
    }
    // handle violations
    JsArray<ReturnRecord> violationsArray = results.getViolations();
    if (violationsArray != null) {
      processViolations(violationsArray);
    } else {
      requestContext.processSideEffects(results.getSideEffects());
      processRelated(results.getRelated());
      if (results.isNullResult()) {
        // Indicates the server explicitly meant to send a null value
        succeed(null);
      } else {
        handleResult(results.getResult());
      }
    }
  }

  public RequestContext to(Receiver<? super T> receiver) {
    this.receiver = receiver;
    return requestContext;
  }

  /**
   * This method comes from the {@link InstanceRequest} interface. Instance
   * methods place the instance in the first parameter slot.
   */
  public Request<T> using(EntityProxy instanceObject) {
    getRequestData().getParameters()[0] = instanceObject;
    /*
     * Instance methods enqueue themselves when their using() method is called.
     * This ensures that the instance parameter will have been set when
     * AbstractRequestContext.retainArg() is called.
     */
    requestContext.addInvocation(this);
    return this;
  }

  public Request<T> with(String... propertyRefs) {
    this.propertyRefs.addAll(Arrays.asList(propertyRefs));
    return this;
  }

  /**
   * This method is called by generated subclasses to process the main return
   * property of the JSON payload. The return record isn't just a reference to a
   * persist or update side-effect, so it has to be processed separately.
   */
  protected <Q extends EntityProxy> Q decodeReturnObject(Class<Q> clazz,
      Object obj) {
    ReturnRecord jso = (ReturnRecord) obj;
    SimpleEntityProxyId<Q> id = requestContext.getRequestFactory().getId(clazz,
        jso.getSimpleId());
    Q proxy = requestContext.processReturnRecord(id, (ReturnRecord) obj,
        WriteOperation.UPDATE);
    return proxy;
  }

  protected <Q extends EntityProxy> void decodeReturnObjectList(
      Class<Q> elementType, Object obj, Collection<Q> accumulator) {
    @SuppressWarnings("unchecked")
    JsArray<ReturnRecord> array = (JsArray<ReturnRecord>) obj;
    for (int i = 0, j = array.length(); i < j; i++) {
      ReturnRecord record = array.get(i);
      if (record == null) {
        accumulator.add(null);
        continue;
      }

      // Decode the individual object
      Q decoded = decodeReturnObject(elementType, record);

      // Really want Class.isInstance()
      assert elementType.equals(decoded.stableId().getProxyClass());
      accumulator.add(decoded);
    }
  }

  protected <Q> void decodeReturnValueList(Class<Q> elementType, Object obj,
      Collection<Q> accumulator) {
    @SuppressWarnings("unchecked")
    List<Q> temp = (List<Q>) EntityCodex.decode(List.class, elementType,
        requestContext, obj);
    accumulator.addAll(temp);
  }

  protected void fail(ServerFailure failure) {
    requestContext.reuse();
    if (receiver != null) {
      receiver.onFailure(failure);
    }
  }

  /**
   * Process the response and call {@link #succeed(Object) or
   * #fail(com.google.gwt.requestfactory.shared.ServerFailure).
   */
  protected abstract void handleResult(Object result);

  protected boolean hasReceiver() {
    return receiver != null;
  }

  protected abstract RequestData makeRequestData();

  protected void processRelated(RelatedObjects related) {
    for (String token : related.getHistoryTokens()) {
      SimpleEntityProxyId<EntityProxy> id = requestContext.getRequestFactory().getProxyId(
          token);
      requestContext.processReturnRecord(id, related.getReturnRecord(token));
    }
  }

  protected void succeed(T t) {
    // The user may not have called to()
    if (receiver != null) {
      receiver.onSuccess(t);
    }
  }

  private void processViolations(JsArray<ReturnRecord> violationsArray) {
    int length = violationsArray.length();
    Set<Violation> errors = new HashSet<Violation>(length);

    for (int i = 0; i < length; i++) {
      ReturnRecord violationRecord = violationsArray.get(i);
      final EntityProxyId<?> key = requestContext.getRequestFactory().getId(
          violationRecord.getSchema(), violationRecord.getEncodedId(),
          violationRecord.getFutureId());

      HashMap<String, String> violations = new HashMap<String, String>();
      assert violationRecord.hasViolations();
      violationRecord.fillViolations(violations);

      for (Map.Entry<String, String> entry : violations.entrySet()) {
        final String path = entry.getKey();
        final String message = entry.getValue();
        errors.add(new Violation() {
          public String getMessage() {
            return message;
          }

          public String getPath() {
            return path;
          }

          public EntityProxyId<?> getProxyId() {
            return key;
          }
        });
      }
    }

    requestContext.reuse();
    requestContext.addErrors(errors);
    if (receiver != null) {
      receiver.onViolation(errors);
    }
  }
}
