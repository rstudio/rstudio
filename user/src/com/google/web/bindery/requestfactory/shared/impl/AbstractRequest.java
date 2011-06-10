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
package com.google.web.bindery.requestfactory.shared.impl;

import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * Abstract implementation of {@link Request}. Each request stores a
 * {@link DeltaValueStoreJsonImpl}.
 * 
 * @param <T> return type
 */
public abstract class AbstractRequest<T> implements Request<T>, InstanceRequest<BaseProxy, T> {

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

  @Override
  public RequestContext getRequestContext() {
    return requestContext;
  }

  public RequestData getRequestData() {
    if (requestData == null) {
      requestData = makeRequestData();
    }
    return requestData;
  }

  public RequestContext to(Receiver<? super T> receiver) {
    this.receiver = receiver;
    return requestContext;
  }

  /**
   * This method comes from the {@link InstanceRequest} interface. Instance
   * methods place the instance in the first parameter slot.
   */
  public Request<T> using(BaseProxy instanceObject) {
    getRequestData().getOrderedParameters()[0] = instanceObject;
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

  protected abstract RequestData makeRequestData();

  Receiver<? super T> getReceiver() {
    return receiver;
  }

  boolean hasReceiver() {
    return receiver != null;
  }

  void onFail(ServerFailure failure) {
    if (receiver != null) {
      receiver.onFailure(failure);
    }
  }

  void onSuccess(Splittable split) {
    // The user may not have called to()
    if (receiver != null) {
      @SuppressWarnings("unchecked")
      T result =
          (T) EntityCodex.decode(requestContext, requestData.getReturnType(), requestData
              .getElementType(), split);
      receiver.onSuccess(result);
    }
  }

  void onViolation(Set<ConstraintViolation<?>> errors) {
    // The user may not have called to()
    if (receiver != null) {
      receiver.onConstraintViolation(errors);
    }
  }
}
