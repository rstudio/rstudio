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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.protobuf.ByteString;
import com.google.gwt.dev.shell.remoteui.MessageTransport.RequestException;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.ViewerRequest;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.ViewerRequest.LogData;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.ViewerRequest.RequestType;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.ViewerResponse;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.ViewerResponse.CapabilityExchange.Capability;
import com.google.gwt.dev.util.Callback;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Used for making requests to a remote ViewerService server.
 *
 * TODO: If this becomes part of the public API, we'll need to provide a level
 * of indirection in front of the protobuf classes; We're going to be rebasing
 * the protobuf library, and we don't want to expose the rebased API as public.
 */
public class ViewerServiceClient {

  private final MessageTransport transport;
  private final TreeLogger unexpectedErrorLogger;

  /**
   * Create a new instance.
   *
   * @param processor A MessageProcessor that is used to communicate with the
   *          ViewerService server.
   */
  public ViewerServiceClient(MessageTransport processor,
      TreeLogger unexpectedErrorLogger) {
    this.transport = processor;
    this.unexpectedErrorLogger = unexpectedErrorLogger;
  }

  /**
   * Add an entry that also serves as a log branch.
   *
   * @param indexInParent The index of this entry/branch within the parent
   *          logger
   * @param type The severity of the log message.
   * @param msg The message.
   * @param caught An exception associated with the message
   * @param helpInfo A URL or message which directs the user to helpful
   *          information related to the log message
   * @param parentLogHandle The log handle of the parent of this log
   *          entry/branch
   * @param callback the callback to call when a branch handle is available
   */
  public void addLogBranch(int indexInParent, Type type, String msg,
      Throwable caught, HelpInfo helpInfo, int parentLogHandle,
      final Callback<Integer> callback) {

    LogData.Builder logDataBuilder = generateLogData(type, msg, caught,
        helpInfo);

    ViewerRequest.AddLogBranch.Builder addlogBranchBuilder = ViewerRequest.AddLogBranch.newBuilder();
    addlogBranchBuilder.setParentLogHandle(parentLogHandle);
    addlogBranchBuilder.setIndexInParent(indexInParent);
    addlogBranchBuilder.setLogData(logDataBuilder);

    ViewerRequest.Builder viewerRequestBuilder = ViewerRequest.newBuilder();
    viewerRequestBuilder.setRequestType(ViewerRequest.RequestType.ADD_LOG_BRANCH);
    viewerRequestBuilder.setAddLogBranch(addlogBranchBuilder);

    Request requestMessage = buildRequestMessageFromViewerRequest(
        viewerRequestBuilder).build();

    transport.executeRequestAsync(requestMessage, new Callback<Response>() {
      @Override
      public void onDone(Response result) {
        callback.onDone(result.getViewerResponse().getAddLogBranch().getLogHandle());
      }

      @Override
      public void onError(Throwable t) {
        callback.onError(t);
      }
    });
  }

  /**
   * Add a log entry.
   *
   * @param indexInParent The index of this entry within the parent logger
   * @param type The severity of the log message.
   * @param msg The message.
   * @param caught An exception associated with the message
   * @param helpInfo A URL or message which directs the user to helpful
   *          information related to the log message
   * @param logHandle The log handle of the parent of this log entry/branch
   */
  public void addLogEntry(int indexInParent, Type type, String msg,
      Throwable caught, HelpInfo helpInfo, int logHandle) {
    LogData.Builder logDataBuilder = generateLogData(type, msg, caught,
        helpInfo);

    ViewerRequest.AddLogEntry.Builder addLogEntryBuilder = ViewerRequest.AddLogEntry.newBuilder();
    addLogEntryBuilder.setLogHandle(logHandle);
    addLogEntryBuilder.setIndexInLog(indexInParent);
    addLogEntryBuilder.setLogData(logDataBuilder);

    ViewerRequest.Builder viewerRequestBuilder = ViewerRequest.newBuilder();
    viewerRequestBuilder.setRequestType(ViewerRequest.RequestType.ADD_LOG_ENTRY);
    viewerRequestBuilder.setAddLogEntry(addLogEntryBuilder);

    Request requestMessage = buildRequestMessageFromViewerRequest(
        viewerRequestBuilder).build();

    transport.executeRequestAsync(requestMessage, new Callback<Response>() {
      @Override
      public void onDone(Response result) {
      }

      @Override
      public void onError(Throwable t) {
        unexpectedErrorLogger.log(TreeLogger.WARN,
            "An error occurred while attempting to add a log entry.", t);
      }
    });
  }

  /**
   * Add a new Module logger. This method should not be called multiple times
   * with the exact same arguments (as there should only be one logger
   * associated with that set of arguments).
   *
   * @param remoteSocket name of remote socket endpoint in host:port format
   * @param url URL of top-level window
   * @param tabKey stable browser tab identifier, or the empty string if no such
   *          identifier is available
   * @param moduleName the name of the module loaded
   * @param sessionKey a unique session key
   * @param agentTag short-form user agent identifier, suitable for use in a
   *          label for this connection
   * @param agentIcon icon to use for the user agent (fits inside 24x24) or null
   *          if unavailable
   * @return the log handle for the newly-created Module logger
   */
  public int addModuleLog(String remoteSocket, String url, String tabKey,
      String moduleName, String sessionKey, String agentTag, byte[] agentIcon) {
    ViewerRequest.AddLog.ModuleLog.Builder moduleLogBuilder = ViewerRequest.AddLog.ModuleLog.newBuilder();
    moduleLogBuilder.setName(moduleName);
    moduleLogBuilder.setUserAgent(agentTag);

    if (url != null) {
      moduleLogBuilder.setUrl(url);
    }

    moduleLogBuilder.setRemoteHost(remoteSocket);
    moduleLogBuilder.setSessionKey(sessionKey);

    if (tabKey != null) {
      moduleLogBuilder.setTabKey(tabKey);
    }

    if (agentIcon != null) {
      moduleLogBuilder = moduleLogBuilder.setIcon(ByteString.copyFrom(agentIcon));
    }

    ViewerRequest.AddLog.Builder addLogBuilder = ViewerRequest.AddLog.newBuilder();
    addLogBuilder.setType(ViewerRequest.AddLog.LogType.MODULE);
    addLogBuilder.setModuleLog(moduleLogBuilder);

    return createLogger(addLogBuilder);
  }

  /**
   * Check the capabilities of the ViewerService. Ensures that the ViewerService
   * supports: adding a log, adding a log branch, adding a log entry, and
   * disconnecting a log.
   *
   * TODO: Should we be checking the specific capability of the
   * ViewerService to support logs of type MAIN, SERVER, and MODULE? Right now,
   * we assume that if they can support the addition of logs, they can handle
   * the addition of any types of logs that we throw at them.
   */
  public void checkCapabilities() {
    ViewerRequest.CapabilityExchange.Builder capabilityExchangeBuilder = ViewerRequest.CapabilityExchange.newBuilder();
    ViewerRequest.Builder viewerRequestBuilder = ViewerRequest.newBuilder();
    viewerRequestBuilder.setRequestType(ViewerRequest.RequestType.CAPABILITY_EXCHANGE);
    viewerRequestBuilder.setCapabilityExchange(capabilityExchangeBuilder);

    Request.Builder request = buildRequestMessageFromViewerRequest(viewerRequestBuilder);

    Future<Response> responseFuture = transport.executeRequestAsync(request.build());
    Response response = waitForResponseOrThrowUncheckedException(responseFuture);

    ViewerResponse.CapabilityExchange capabilityExchangeResponse = response.getViewerResponse().getCapabilityExchange();
    List<Capability> capabilityList = capabilityExchangeResponse.getCapabilitiesList();

    // Check for the add log ability
    checkCapability(capabilityList, RequestType.ADD_LOG);

    // Check for the add log branch ability
    checkCapability(capabilityList, RequestType.ADD_LOG_BRANCH);

    // Check for the add log branch ability
    checkCapability(capabilityList, RequestType.ADD_LOG_BRANCH);

    // Check for the disconnect log capability
    checkCapability(capabilityList, RequestType.DISCONNECT_LOG);
  }

  /**
   * Disconnect the log. Indicate to the log that the process which was logging
   * messages to it is now dead, and no more messages will be logged to it.
   *
   * Note that the log handle should refer to a top-level log, not a branch log.
   *
   * @param logHandle the handle of the top-level log to disconnect
   */
  public void disconnectLog(int logHandle) {
    ViewerRequest.DisconnectLog.Builder disconnectLogbuilder = ViewerRequest.DisconnectLog.newBuilder();
    disconnectLogbuilder.setLogHandle(logHandle);

    ViewerRequest.Builder viewerRequestBuilder = ViewerRequest.newBuilder();
    viewerRequestBuilder.setRequestType(RequestType.DISCONNECT_LOG);
    viewerRequestBuilder.setDisconnectLog(disconnectLogbuilder);

    Request.Builder request = buildRequestMessageFromViewerRequest(viewerRequestBuilder);
    Future<Response> responseFuture = transport.executeRequestAsync(request.build());
    waitForResponseOrThrowUncheckedException(responseFuture);
  }

  public void initialize(String clientId, List<String> startupURLs) {
    ViewerRequest.Initialize.Builder initializationBuilder = ViewerRequest.Initialize.newBuilder();
    initializationBuilder.setClientId(clientId);
    initializationBuilder.addAllStartupURLs(startupURLs);

    ViewerRequest.Builder viewerRequestBuilder = ViewerRequest.newBuilder();
    viewerRequestBuilder.setRequestType(ViewerRequest.RequestType.INITIALIZE);
    viewerRequestBuilder.setInitialize(initializationBuilder);

    Request.Builder request = buildRequestMessageFromViewerRequest(viewerRequestBuilder);

    Future<Response> responseFuture = transport.executeRequestAsync(request.build());
    waitForResponseOrThrowUncheckedException(responseFuture);
  }

  private Request.Builder buildRequestMessageFromViewerRequest(
      ViewerRequest.Builder viewerRequestBuilder) {
    return Request.newBuilder().setServiceType(Request.ServiceType.VIEWER).setViewerRequest(
        viewerRequestBuilder);
  }

  private void checkCapability(List<Capability> viewerCapabilityList,
      RequestType capabilityWeNeed) {
    for (Capability c : viewerCapabilityList) {
      if (c.getCapability() == capabilityWeNeed) {
        return;
      }
    }
    throw new RuntimeException("ViewerService does not support "
        + capabilityWeNeed.toString());
  }

  private int createLogger(ViewerRequest.AddLog.Builder addLogBuilder) {
    ViewerRequest.Builder viewerRequestBuilder = ViewerRequest.newBuilder();
    viewerRequestBuilder.setRequestType(ViewerRequest.RequestType.ADD_LOG);
    viewerRequestBuilder.setAddLog(addLogBuilder);

    Request.Builder request = buildRequestMessageFromViewerRequest(viewerRequestBuilder);

    Future<Response> responseFuture = transport.executeRequestAsync(request.build());
    return waitForResponseOrThrowUncheckedException(responseFuture).getViewerResponse().getAddLog().getLogHandle();
  }

  private LogData.Builder generateLogData(Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {
    LogData.Builder logBuilder = LogData.newBuilder().setSummary(msg);
    logBuilder.setLevel(type.getLabel());

    if (caught != null) {
      String stackTraceAsString = AbstractTreeLogger.getStackTraceAsString(caught);
      if (stackTraceAsString != null) {
        logBuilder = logBuilder.setDetails(stackTraceAsString);
      }
    }

    if (helpInfo != null) {
      LogData.HelpInfo.Builder helpInfoBuilder = LogData.HelpInfo.newBuilder();

      if (helpInfo.getURL() != null) {
        helpInfoBuilder.setUrl(helpInfo.getURL().toExternalForm());
      }

      if (helpInfo.getAnchorText() != null) {
        helpInfoBuilder.setText(helpInfo.getAnchorText());
      }

      logBuilder.setHelpInfo(helpInfoBuilder);
    }

    if (type.needsAttention()) {
      // Set this field if attention is actually needed
      logBuilder.setNeedsAttention(true);
    }

    return logBuilder;
  }

  /**
   * Waits for response and throws a checked exception if the request failed.
   *
   * Requests can fail if the other side does not understand the message -- for
   * example, if it is running an older version.
   *
   * @throws RequestException if the request failed
   */
  private Response waitForResponse(Future<Response> future)
      throws RequestException {
    try {
      return future.get();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RequestException) {
        throw (RequestException) cause;
      } else {
        throw new RuntimeException(e);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Waits for response and throws an unchecked exception if the request failed.
   */
  private Response waitForResponseOrThrowUncheckedException(
      Future<Response> future) {
    try {
      return waitForResponse(future);
    } catch (RequestException e) {
      throw new RuntimeException(e);
    }
  }
}
