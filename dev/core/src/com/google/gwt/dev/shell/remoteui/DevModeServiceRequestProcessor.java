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
package com.google.gwt.dev.shell.remoteui;

import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.DevModeRequest;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.DevModeRequest.RequestType;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.DevModeResponse;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.DevModeResponse.CapabilityExchange;

/**
 * A request processor that handles DevModeService requests. There should only
 * be one instance of this class for a given {@link RemoteUI} instance.
 * 
 * TODO: We currently reference protobuf classes directly. We're going to be
 * re-basing the protobuf API, and we don't want to expose these rebased classes
 * directly to users of this API. We need to provide a level of indirection
 * between protobuf messages.
 */
public class DevModeServiceRequestProcessor implements RequestProcessor {

  private final RemoteUI remoteUI;

  /**
   * Create a new instance for the given remoteUI.
   */
  public DevModeServiceRequestProcessor(RemoteUI remoteUI) {
    this.remoteUI = remoteUI;
  }

  public Response execute(Request request) throws Exception {
    if (request.getServiceType() != Request.ServiceType.DEV_MODE) {
      throw new IllegalArgumentException(
          "Unknown Service Type: This request processor cannot handle requests of type "
              + request.getServiceType().name());
    }

    RequestType requestType = request.getDevModeRequest().getRequestType();
    if (requestType != null) {
      switch (requestType) {
        case CAPABILITY_EXCHANGE:
          return processCapabilityExchange();

        case RESTART_WEB_SERVER:
          return processRestartServer();

        default: {
          break;
        }
      }
    }

    throw new IllegalArgumentException(
        "Unknown DevModeService Request: The DevModeService cannot handle requests of type "
            + requestType == null ? "(unknown)" : requestType.name());
  }

  private Response processCapabilityExchange() {
    CapabilityExchange.Builder capabilityExchangeBuilder = CapabilityExchange.newBuilder();

    CapabilityExchange.Capability.Builder c1Builder = CapabilityExchange.Capability.newBuilder();
    c1Builder.setCapability(DevModeRequest.RequestType.CAPABILITY_EXCHANGE);
    capabilityExchangeBuilder.addCapabilities(c1Builder);

    CapabilityExchange.Capability.Builder c2Builder = CapabilityExchange.Capability.newBuilder();
    c2Builder.setCapability(DevModeRequest.RequestType.RESTART_WEB_SERVER);
    capabilityExchangeBuilder.addCapabilities(c2Builder);

    DevModeResponse.Builder devModeResponseBuilder = DevModeResponse.newBuilder();
    devModeResponseBuilder.setResponseType(DevModeResponse.ResponseType.CAPABILITY_EXCHANGE);
    devModeResponseBuilder.setCapabilityExchange(capabilityExchangeBuilder);

    Response.Builder responseBuilder = Response.newBuilder();
    responseBuilder.setDevModeResponse(devModeResponseBuilder);

    return responseBuilder.build();
  }

  private Response processRestartServer() {
    if (!remoteUI.restartWebServer()) {
      throw new IllegalStateException(
          "Unable to restart the web server. It is still in the process of starting up. Wait a few seconds and try again.");
    }

    DevModeResponse.Builder devModeResponseBuilder = DevModeResponse.newBuilder();
    devModeResponseBuilder.setResponseType(DevModeResponse.ResponseType.RESTART_WEB_SERVER);

    Response.Builder responseBuilder = Response.newBuilder();
    responseBuilder.setDevModeResponse(devModeResponseBuilder);

    return responseBuilder.build();
  }
}
