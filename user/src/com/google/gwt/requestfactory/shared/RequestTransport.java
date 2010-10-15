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
package com.google.gwt.requestfactory.shared;

/**
 * Abstracts the mechanism by which a RequestFactory instance transmits its
 * payload to the backend.
 * 
 * @see com.google.gwt.requestfactory.client.DefaultRequestTransport
 */
public interface RequestTransport {
  /**
   * A callback interface.
   */
  public interface TransportReceiver {
    /**
     * Called when the transmission succeeds.
     *
     * @param payload the String payload
     */
    void onTransportSuccess(String payload);

    /**
     * Called when the transmission fails.
     *
     * @param message the String error message
     */
    void onTransportFailure(String message);
  }

  /**
   * Called by the RequestFactory implementation.
   *
   * @param payload the String payload
   * @param receiver a {@link TransportReceiver} instance
   */
  void send(String payload, TransportReceiver receiver);
}
