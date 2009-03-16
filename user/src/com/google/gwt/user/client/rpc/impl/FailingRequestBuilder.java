/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A {@link RequestBuilder} that always immediately fails.
 */
public class FailingRequestBuilder extends RequestBuilder {
  private final Throwable cause;
  private final AsyncCallback<?> rpcCallback;

  public FailingRequestBuilder(Throwable cause, AsyncCallback<?> rpcCallback) {
    super(GET, "(bogus)");
    this.cause = cause;
    this.rpcCallback = rpcCallback;
  }

  @Override
  public Request send() throws RequestException {
    rpcCallback.onFailure(cause);
    return new FailedRequest();
  }

  @Override
  public Request sendRequest(String requestData, RequestCallback callback)
      throws RequestException {
    // This method would not normally be called
    throw new RequestException(cause);
  }
}
