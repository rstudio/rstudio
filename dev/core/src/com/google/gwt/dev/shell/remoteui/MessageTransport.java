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

import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Responsible for exchanging requests and responses between services.
 */
public class MessageTransport {
  class PendingRequest extends PendingSend {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition availableResponseCondition = lock.newCondition();
    private Response responseMessage;
    private Exception exception;
    private final Message message;

    public PendingRequest(Message message) {
      this.message = message;
    }

    @Override
    public void failed(Exception e) {
      pendingRequestMap.remove(message.getRequest().getRequestId());

      lock.lock();
      try {
        exception = e;
        availableResponseCondition.signal();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void send(OutputStream outputStream) throws IOException {
      Request request = message.getRequest();
      int requestId = request.getRequestId();
      pendingRequestMap.put(requestId, this);
      message.writeDelimitedTo(outputStream);
    }

    /**
     * Sets the response that was received from the server, and signals the
     * thread that is waiting on the response.
     * 
     * @param responseMessage the server's response
     * @throws InterruptedException
     */
    public void setResponse(Message.Response responseMessage)
        throws InterruptedException {
      assert (responseMessage != null);
      lock.lock();
      try {
        if (this.responseMessage != null) {
          throw new IllegalStateException("Response has already been set.");
        }
        this.responseMessage = responseMessage;
        availableResponseCondition.signal();
      } finally {
        lock.unlock();
      }
    }

    /**
     * Waits for a response to be returned for a given request.
     * 
     * @return the response from the server
     * @throws Exception if an exception occurred while processing the request
     */
    public Response waitForResponse() throws Exception {
      lock.lock();

      try {
        while (responseMessage == null && exception == null) {
          availableResponseCondition.await();
        }

        if (exception != null) {
          throw exception;
        }

        return responseMessage;
      } finally {
        lock.unlock();
      }
    }
  }

  static class PendingRequestMap {
    private final Lock mapLock = new ReentrantLock();
    private final Map<Integer, PendingRequest> requestIdToPendingServerRequest = new HashMap<Integer, PendingRequest>();
    private boolean noMoreAdds;

    public void blockAdds(Exception e) {
      mapLock.lock();
      try {
        noMoreAdds = true;
        for (PendingRequest pendingRequest : requestIdToPendingServerRequest.values()) {
          pendingRequest.failed(e);
        }
      } finally {
        mapLock.unlock();
      }
    }

    public PendingRequest remove(int requestId) {
      mapLock.lock();
      try {
        return requestIdToPendingServerRequest.remove(requestId);
      } finally {
        mapLock.unlock();
      }
    }

    void put(int requestId, PendingRequest pendingServerRequest) {
      mapLock.lock();
      try {
        if (noMoreAdds) {
          pendingServerRequest.failed(new IllegalStateException(
              "InputStream is closed"));
        } else {
          requestIdToPendingServerRequest.put(requestId, pendingServerRequest);
        }
      } finally {
        mapLock.unlock();
      }
    }
  }

  class PendingResponse extends PendingSend {
    Message message;

    public PendingResponse(Message message) {
      this.message = message;
    }

    @Override
    public void failed(Exception e) {
      // Do nothing
    }

    @Override
    public void send(OutputStream outputStream) throws IOException {
      message.writeDelimitedTo(outputStream);
    }
  }

  abstract class PendingSend {
    public abstract void failed(Exception e);

    public abstract void send(OutputStream outputStream) throws IOException;
  }

  private static final int DEFAULT_SERVICE_THREADS = 2;

  private final Thread messageProcessingThread;
  private final AtomicInteger nextMessageId = new AtomicInteger();
  private final RequestProcessor requestProcessor;
  private final LinkedBlockingQueue<PendingSend> sendQueue = new LinkedBlockingQueue<PendingSend>();
  private final Thread sendThread;
  private final ExecutorService serverRequestExecutor;
  private final PendingRequestMap pendingRequestMap = new PendingRequestMap();

  /**
   * Create a new instance using the given streams and request processor.
   * Closing either stream will cause the termination of the transport.
   * 
   * @param inputStream an input stream for reading messages
   * @param outputStream an output stream for writing messages
   * @param requestProcessor a callback interface for handling remote client
   *          requests
   */
  public MessageTransport(final InputStream inputStream,
      final OutputStream outputStream, RequestProcessor requestProcessor) {
    this.requestProcessor = requestProcessor;
    serverRequestExecutor = Executors.newFixedThreadPool(DEFAULT_SERVICE_THREADS);

    // This thread terminates on interruption or IO failure
    messageProcessingThread = new Thread(new Runnable() {
      public void run() {
        try {
          while (true) {
            Message message = Message.parseDelimitedFrom(inputStream);
            processMessage(message);
          }
        } catch (IOException e) {
          terminateDueToException(e);
        } catch (InterruptedException e) {
          terminateDueToException(e);
        }
      }
    });
    messageProcessingThread.start();

    // This thread only terminates if it is interrupted
    sendThread = new Thread(new Runnable() {
      public void run() {
        while (true) {
          try {
            PendingSend pendingSend = sendQueue.take();
            try {
              pendingSend.send(outputStream);
            } catch (IOException e) {
              pendingSend.failed(e);
            }
          } catch (InterruptedException e) {
            break;
          }
        }
      }
    });
    sendThread.setDaemon(true);
    sendThread.start();
  }

  /**
   * Asynchronously executes the request on a remote server.
   * 
   * @param requestMessage The request to execute
   * 
   * @return a {@link Future} that can be used to access the server's response
   */
  public Future<Response> executeRequestAsync(final Request requestMessage) {
    Future<Response> responseFuture = serverRequestExecutor.submit(new Callable<Response>() {
      public Response call() throws Exception {
        int requestId = nextMessageId.getAndIncrement();

        Message.Request.Builder requestBuilder = Message.Request.newBuilder(requestMessage);
        requestBuilder.setRequestId(requestId);

        Message.Builder messageBuilder = Message.newBuilder();
        messageBuilder.setMessageType(Message.MessageType.REQUEST);
        messageBuilder.setRequest(requestBuilder);

        Message message = messageBuilder.build();
        PendingRequest pendingRequest = new PendingRequest(message);
        sendQueue.put(pendingRequest);

        return pendingRequest.waitForResponse();
      }
    });

    return responseFuture;
  }

  private void processClientRequest(int requestId, Request request)
      throws InterruptedException {
    Message.Builder messageBuilder = Message.newBuilder();
    messageBuilder.setMessageType(Message.MessageType.RESPONSE);

    Response response = null;
    try {
      response = requestProcessor.execute(request);
    } catch (Exception e) {
      // TODO: Write error information into the builder and set the request Id
      return;
    }

    // This would not be necessary if the request id was not part of request
    // or response.
    Response.Builder responseBuilder = Response.newBuilder(response);
    responseBuilder.setRequestId(requestId);

    messageBuilder.setResponse(responseBuilder);
    PendingResponse pendingResponse = new PendingResponse(
        messageBuilder.build());
    sendQueue.put(pendingResponse);
  }

  private void processMessage(final Message message)
      throws InterruptedException {
    switch (message.getMessageType()) {
      case RESPONSE: {
        processServerResponse(message.getResponse().getRequestId(),
            message.getResponse());
        break;
      }

      case REQUEST: {
        processClientRequest(message.getRequest().getRequestId(),
            message.getRequest());
        break;
      }

      default: {
        // TODO: Return a response indicating that the message type
        // is unknown
        break;
      }
    }
  }

  private void processServerResponse(int requestId, Response response)
      throws InterruptedException {
    PendingRequest pendingServerRequest = pendingRequestMap.remove(requestId);
    if (pendingServerRequest != null) {
      pendingServerRequest.setResponse(response);
    }
  }

  private void terminateDueToException(Exception e) {
    pendingRequestMap.blockAdds(e);
  }
}
