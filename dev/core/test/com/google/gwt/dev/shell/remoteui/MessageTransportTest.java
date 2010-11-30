package com.google.gwt.dev.shell.remoteui;

import com.google.gwt.dev.shell.remoteui.MessageTransport.RequestException;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Failure;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.DevModeRequest;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.DevModeRequest.RequestType;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.DevModeResponse;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MessageTransportTest extends TestCase {

  private static class MockNetwork {
    private final Socket clientSocket;
    private final Socket serverSocket;

    private final ServerSocket listenSocket;

    public MockNetwork(Socket clientSocket, Socket serverSocket,
        ServerSocket listenSocket) {
      this.clientSocket = clientSocket;
      this.serverSocket = serverSocket;
      this.listenSocket = listenSocket;
    }

    public Socket getClientSocket() {
      return clientSocket;
    }

    public Socket getServerSocket() {
      return serverSocket;
    }

    public void shutdown() {
      try {
        clientSocket.close();
      } catch (IOException e) {
        // Ignore
      }

      try {
        serverSocket.close();
      } catch (IOException e) {
        // Ignore
      }

      try {
        listenSocket.close();
      } catch (IOException e) {
        // Ignore
      }
    }
  }

  private static MockNetwork createMockNetwork() throws IOException {
    InetAddress localHost = InetAddress.getLocalHost();
    ServerSocket listenSocket = new ServerSocket(0, 1, localHost);
    Socket clientSocket = new Socket(localHost, listenSocket.getLocalPort());
    Socket serverSocket = listenSocket.accept();
    return new MockNetwork(clientSocket, serverSocket, listenSocket);
  }

  /**
   * Tests that sending an async request to a server when the server's socket is
   * closed with result in an ExecutionException on a call to future.get().
   * 
   * @throws ExecutionException
   * @throws InterruptedException
   * @throws IOException
   */
  public void testExecuteAsyncRequestWithClosedServerSocket()
      throws IOException, InterruptedException {
    MockNetwork network = createMockNetwork();

    /*
     * Define a dummy request processor. The message transport is being set up
     * on the client side, which means that it should not be receiving any
     * requests (any responses).
     */
    RequestProcessor requestProcessor = new RequestProcessor() {
      public Response execute(Request request) throws Exception {
        fail("Should not reach here.");
        return null;
      }
    };

    // Set up a transport on the client side
    MessageTransport messageTransport = new MessageTransport(
        network.getClientSocket().getInputStream(),
        network.getClientSocket().getOutputStream(), requestProcessor,
        new MessageTransport.ErrorCallback() {
          public void onResponseException(Exception e) {
          }
          public void onTermination(Exception e) {
          }
        });
    messageTransport.start();

    Message.Request.Builder requestMessageBuilder = Message.Request.newBuilder();
    requestMessageBuilder.setServiceType(Message.Request.ServiceType.DEV_MODE);
    Message.Request request = requestMessageBuilder.build();

    // Close the server's socket; that will close the client's output
    // stream
    network.getServerSocket().close();

    int sleepCycles = 0;
    while (!network.getServerSocket().isClosed() && sleepCycles < 8) {
      // Wait until the stream is closed before attempting to execute the
      // request.
      Thread.sleep(250);
      sleepCycles++;
    }

    assertTrue("Unable to close socket; cannot proceed with the test.",
        network.getServerSocket().isClosed());

    Future<Response> responseFuture = null;
    responseFuture = messageTransport.executeRequestAsync(request);
    assertNotNull(responseFuture);

    try {
      responseFuture.get(2, TimeUnit.SECONDS);
      fail("Should have thrown an exception");
    } catch (TimeoutException te) {
      fail("Should not have timed out");
    } catch (ExecutionException e) {
      /*
       * An IOException can happen if the request gets in the queue before the
       * message processing thread terminates. If the request gets in the queue
       * after the message processing thread terminates, then the result will be
       * an IllegalStateException.
       */
      assertTrue("Expected: IllegalStateException or IOException, actual:"
          + e.getCause(), e.getCause() instanceof IllegalStateException
          || e.getCause() instanceof IOException);
    } catch (Exception e) {
      fail("Should not have thrown any other exception");
    }

    network.shutdown();
  }

  /**
   * Tests that an async request to a remote server is successfully sent, and
   * the server's response is successfully received.
   */
  public void testExecuteRequestAsync() throws InterruptedException,
      ExecutionException, IOException, TimeoutException {

    MockNetwork network = createMockNetwork();

    /*
     * Define a dummy request processor. The message transport is being set up
     * on the client side, which means that it should not be receiving any
     * requests (any responses).
     */
    RequestProcessor requestProcessor = new RequestProcessor() {
      public Response execute(Request request) throws Exception {
        fail("Should not reach here.");
        return null;
      }
    };

    // Set up a transport on the client side
    MessageTransport messageTransport = new MessageTransport(
        network.getClientSocket().getInputStream(),
        network.getClientSocket().getOutputStream(), requestProcessor, null);
    messageTransport.start();

    // Generate a new request
    DevModeRequest.Builder devModeRequestBuilder = DevModeRequest.newBuilder();
    devModeRequestBuilder.setRequestType(RequestType.CAPABILITY_EXCHANGE);
    Message.Request.Builder requestMessageBuilder = Message.Request.newBuilder();
    requestMessageBuilder.setServiceType(Message.Request.ServiceType.DEV_MODE);
    requestMessageBuilder.setDevModeRequest(devModeRequestBuilder);
    Message.Request request = requestMessageBuilder.build();

    // Execute the request on the remote server
    Future<Response> responseFuture = messageTransport.executeRequestAsync(request);
    assertNotNull(responseFuture);

    // Get the request on the server side
    Message receivedRequest = Message.parseDelimitedFrom(network.getServerSocket().getInputStream());
    assertEquals(receivedRequest.getRequest(), request);

    // Generate a response on the server
    DevModeResponse.CapabilityExchange.Capability.Builder capabilityBuilder = DevModeResponse.CapabilityExchange.Capability.newBuilder();
    capabilityBuilder.setCapability(DevModeRequest.RequestType.RESTART_WEB_SERVER);
    DevModeResponse.CapabilityExchange.Builder capabilityExchangeResponseBuilder = DevModeResponse.CapabilityExchange.newBuilder();
    capabilityExchangeResponseBuilder.addCapabilities(capabilityBuilder);
    DevModeResponse.Builder devModeResponseBuilder = DevModeResponse.newBuilder();
    devModeResponseBuilder.setResponseType(DevModeResponse.ResponseType.CAPABILITY_EXCHANGE);
    devModeResponseBuilder.setCapabilityExchange(capabilityExchangeResponseBuilder);
    Response.Builder responseBuilder = Response.newBuilder();
    responseBuilder.setDevModeResponse(devModeResponseBuilder);
    Response response = responseBuilder.build();
    Message.Builder responseMsgBuilder = Message.newBuilder();
    responseMsgBuilder.setMessageType(Message.MessageType.RESPONSE);
    // Make sure we set the right message id
    responseMsgBuilder.setMessageId(receivedRequest.getMessageId());
    responseMsgBuilder.setResponse(response);
    Message responseMsg = responseMsgBuilder.build();

    // Send the response back to the client
    responseMsg.writeDelimitedTo(network.getServerSocket().getOutputStream());

    // Make sure that the response received on the client is identical to
    // the response sent by the server
    assertEquals(responseFuture.get(2, TimeUnit.SECONDS), response);

    network.shutdown();
  }

  /**
   * Tests that an async request to a remote server which ends up throwing an
   * exception on the server side ends up throwing the proper exception via the
   * future that the client is waiting on.
   */
  public void testExecuteRequestAsyncServerThrowsException() throws IOException {
    MockNetwork network = createMockNetwork();

    /*
     * Define a dummy request processor. The message transport is being set up
     * on the client side, which means that it should not be receiving any
     * requests (any responses).
     */
    RequestProcessor requestProcessor = new RequestProcessor() {
      public Response execute(Request request) throws Exception {
        fail("Should not reach here.");
        return null;
      }
    };

    // Set up a message transport on the client side
    MessageTransport messageTransport = new MessageTransport(
        network.getClientSocket().getInputStream(),
        network.getClientSocket().getOutputStream(), requestProcessor,
        new MessageTransport.ErrorCallback() {
          public void onResponseException(Exception e) {
          }
          public void onTermination(Exception e) {
          }
        });
    messageTransport.start();

    // Generate a new request
    Message.Request.Builder requestMessageBuilder = Message.Request.newBuilder();
    requestMessageBuilder.setServiceType(Message.Request.ServiceType.DEV_MODE);
    Message.Request request = requestMessageBuilder.build();

    // Execute the request on the remote server
    Future<Response> responseFuture = messageTransport.executeRequestAsync(request);
    assertNotNull(responseFuture);

    // Get the request on the server side
    Message receivedRequest = Message.parseDelimitedFrom(network.getServerSocket().getInputStream());
    assertEquals(receivedRequest.getRequest(), request);

    // Generate a failure response on the server
    Failure.Builder failureBuilder = Failure.newBuilder();
    failureBuilder.setMessage("Unable to process the request.");
    Message.Builder messageBuilder = Message.newBuilder();
    // Make sure that we set the matching message id
    messageBuilder.setMessageId(receivedRequest.getMessageId());
    messageBuilder.setMessageType(Message.MessageType.FAILURE);
    messageBuilder.setFailure(failureBuilder);
    Message failureMsg = messageBuilder.build();

    // Send the failure message back to the client
    failureMsg.writeDelimitedTo(network.getServerSocket().getOutputStream());

    // Wait for the response on the client. This should result in a
    // RequestException being thrown.
    try {
      responseFuture.get(2, TimeUnit.SECONDS);
      fail("Should have thrown an exception");
    } catch (TimeoutException te) {
      fail("Should not have timed out");
    } catch (ExecutionException e) {
      // This is where we should hit
      assertTrue("Expected: MessageTransport.RequestException, actual:"
          + e.getCause(), e.getCause() instanceof RequestException);
      RequestException re = (RequestException) e.getCause();
      assertEquals(re.getMessage(), "Unable to process the request.");
    } catch (Exception e) {
      fail("Should not have thrown any other exception");
    }

    network.shutdown();
  }

  /**
   * Tests that a client request is successfully received by the
   * RequestProcessor, and the response generated by the RequestProcessor is
   * successfully received by the client.
   * 
   * @throws IOException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void testRequestProcessor() throws IOException {
    MockNetwork network = createMockNetwork();

    // Create the request that will be sent to the server
    DevModeRequest.Builder devModeRequestBuilder = DevModeRequest.newBuilder();
    devModeRequestBuilder.setRequestType(DevModeRequest.RequestType.CAPABILITY_EXCHANGE);
    Message.Request.Builder clientRequestBuilder = Message.Request.newBuilder();
    clientRequestBuilder.setDevModeRequest(devModeRequestBuilder);
    clientRequestBuilder.setServiceType(Message.Request.ServiceType.DEV_MODE);
    final Message.Request clientRequest = clientRequestBuilder.build();

    // Create the response that will be sent back from the server
    DevModeResponse.Builder devModeResponseBuilder = DevModeResponse.newBuilder();
    devModeResponseBuilder.setResponseType(DevModeResponse.ResponseType.CAPABILITY_EXCHANGE);
    Message.Response.Builder clientResponseBuilder = Message.Response.newBuilder();
    clientResponseBuilder.setDevModeResponse(devModeResponseBuilder);
    final Message.Response clientResponse = clientResponseBuilder.build();

    /*
     * Define a request processor, which will expect to receive the request that
     * we've defined, and then return the response that we've defined.
     */
    RequestProcessor requestProcessor = new RequestProcessor() {
      public Response execute(Request request) throws Exception {
        assertEquals(clientRequest, request);
        return clientResponse;
      }
    };

    // Start up the message transport on the server side
    MessageTransport messageTransport = new MessageTransport(
        network.getClientSocket().getInputStream(),
        network.getClientSocket().getOutputStream(), requestProcessor,
        new MessageTransport.ErrorCallback() {
          public void onResponseException(Exception e) {
          }
          public void onTermination(Exception e) {
          }
        });
    messageTransport.start();

    // Send the request from the client to the server
    Message.Builder clientRequestMsgBuilder = Message.newBuilder();
    clientRequestMsgBuilder.setMessageType(Message.MessageType.REQUEST);
    clientRequestMsgBuilder.setMessageId(25);
    clientRequestMsgBuilder.setRequest(clientRequest);
    Message clientRequestMsg = clientRequestMsgBuilder.build();
    clientRequestMsg.writeDelimitedTo(network.getServerSocket().getOutputStream());

    // Receive the response on the client (which was returned by the
    // RequestProcessor)
    Message receivedResponseMsg = Message.parseDelimitedFrom(network.getServerSocket().getInputStream());

    // Make sure the message ids match
    assertEquals(receivedResponseMsg.getMessageId(), 25);

    // Make sure that the response matches the one that was returned by the
    // RequestProcessor
    assertEquals(receivedResponseMsg.getResponse(), clientResponse);

    network.shutdown();
  }

  /**
   * Tests that a client request is successfully received by the
   * RequestProcessor, and the exception thrown by the RequestProcessor is
   * passed back in the form of an error response to the client.
   * 
   * @throws IOException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void testRequestProcessorThrowsException() throws IOException {
    MockNetwork network = createMockNetwork();

    /*
     * Define a request processor that throws an exception when it receives the
     * request. We'll expect to receive this exception as a failure message on
     * the client side.
     */
    RequestProcessor requestProcessor = new RequestProcessor() {
      public Response execute(Request request) throws Exception {
        throw new Exception("There was an exception processing this request.");
      }
    };

    // Start up the message transport on the server side
    MessageTransport messageTransport = new MessageTransport(
        network.getClientSocket().getInputStream(),
        network.getClientSocket().getOutputStream(), requestProcessor,
        new MessageTransport.ErrorCallback() {
          public void onResponseException(Exception e) {
          }
          public void onTermination(Exception e) {
          }
        });
    messageTransport.start();

    // Send a request to the server
    Message.Request.Builder clientRequestBuilder = Message.Request.newBuilder();
    clientRequestBuilder.setServiceType(Message.Request.ServiceType.DEV_MODE);
    final Message.Request clientRequest = clientRequestBuilder.build();
    Message.Builder clientRequestMsgBuilder = Message.newBuilder();
    clientRequestMsgBuilder.setMessageType(Message.MessageType.REQUEST);
    clientRequestMsgBuilder.setMessageId(25);
    clientRequestMsgBuilder.setRequest(clientRequest);
    Message clientRequestMsg = clientRequestMsgBuilder.build();
    clientRequestMsg.writeDelimitedTo(network.getServerSocket().getOutputStream());

    // Receive the response on the client (which was returned by the
    // RequestProcessor)
    Message receivedResponseMsg = Message.parseDelimitedFrom(network.getServerSocket().getInputStream());

    // Make sure the message ids match
    assertEquals(receivedResponseMsg.getMessageId(), 25);

    // Verify that the message is of type FAILURE
    assertEquals(receivedResponseMsg.getMessageType(),
        Message.MessageType.FAILURE);

    // Verify that the failure message field is set
    assertNotNull(receivedResponseMsg.getFailure());

    // Verify that the actual failure message is equal to the message
    // set for the Exception in the RequestProcessor
    assertEquals(receivedResponseMsg.getFailure().getMessage(),
        "There was an exception processing this request.");

    network.shutdown();
  }
}
