/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;

/**
 * Adapter from a {@link RequestCallback} interface to an {@link AsyncCallback}
 * interface.
 * 
 * For internal use only.
 * 
 * @param <T> the type parameter for the {@link AsyncCallback}
 */
public class RequestCallbackAdapter<T> implements RequestCallback {
  /**
   * Enumeration used to read specific return types out of a
   * {@link SerializationStreamReader}.
   */
  public enum ResponseReader {
    BOOLEAN {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readBoolean();
      }
    },

    BYTE {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readByte();
      }
    },

    CHAR {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readChar();
      }
    },

    DOUBLE {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readDouble();
      }
    },

    FLOAT {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readFloat();
      }
    },

    INT {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readInt();
      }
    },

    LONG {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readLong();
      }
    },

    OBJECT {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readObject();
      }
    },

    SHORT {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readShort();
      }
    },

    STRING {
      @Override
      public Object read(SerializationStreamReader streamReader)
          throws SerializationException {
        return streamReader.readString();
      }
    },

    VOID {
      @Override
      public Object read(SerializationStreamReader streamReader) {
        return null;
      }
    };

    public abstract Object read(SerializationStreamReader streamReader)
        throws SerializationException;
  }

  /**
   * Returns a string that encodes the result of a method invocation.
   * Effectively, this just removes any headers from the encoded response.
   * 
   * @param encodedResponse
   * @return string that encodes the result of a method invocation
   */
  static String getEncodedInstance(String encodedResponse) {
    assert (!isInvocationException(encodedResponse));
    return encodedResponse.substring(4);
  }

  private static boolean isInvocationException(String encodedResponse) {
    return !isThrownException(encodedResponse)
        && !isReturnValue(encodedResponse);
  }

  /**
   * Return <code>true</code> if the encoded response contains a value
   * returned by the method invocation.
   * 
   * @param encodedResponse
   * @return <code>true</code> if the encoded response contains a value
   *         returned by the method invocation
   */
  private static boolean isReturnValue(String encodedResponse) {
    return encodedResponse.startsWith("//OK");
  }

  /**
   * Return <code>true</code> if the encoded response contains a checked
   * exception that was thrown by the method invocation.
   * 
   * @param encodedResponse
   * @return <code>true</code> if the encoded response contains a checked
   *         exception that was thrown by the method invocation
   */
  private static boolean isThrownException(String encodedResponse) {
    return encodedResponse.startsWith("//EX");
  }

  /**
   * {@link AsyncCallback} to notify or success or failure.
   */
  private final AsyncCallback<T> callback;

  /**
   * Instance which will read the expected return type out of the
   * {@link SerializationStreamReader}.
   */
  private final ResponseReader responseReader;

  /**
   * {@link Serializer} instance used by the
   * {@link ClientSerializationStreamReader} for serialization.
   */
  private final Serializer serializer;

  public RequestCallbackAdapter(Serializer serializer,
      AsyncCallback<T> callback, ResponseReader responseReader) {
    assert (serializer != null);
    assert (callback != null);
    assert (responseReader != null);

    this.serializer = serializer;
    this.callback = callback;
    this.responseReader = responseReader;
  }

  public void onError(Request request, Throwable exception) {
    callback.onFailure(exception);
  }

  @SuppressWarnings("unchecked")
  public void onResponseReceived(Request request, Response response) {
    final ClientSerializationStreamReader streamReader = new ClientSerializationStreamReader(
        serializer);
    T result = null;
    Throwable caught = null;
    String encodedResponse = response.getText();
    try {
      if (isReturnValue(encodedResponse)) {
        streamReader.prepareToRead(getEncodedInstance(encodedResponse));
        result = (T) responseReader.read(streamReader);
      } else if (isThrownException(encodedResponse)) {
        streamReader.prepareToRead(getEncodedInstance(encodedResponse));
        caught = (Throwable) streamReader.readObject();
      } else {
        assert (isInvocationException(encodedResponse));
        caught = new com.google.gwt.user.client.rpc.InvocationException(
            encodedResponse);
      }
    } catch (com.google.gwt.user.client.rpc.SerializationException e) {
      caught = new com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException();
    } catch (Throwable e) {
      caught = e;
    }

    if (caught == null) {
      callback.onSuccess(result);
    } else {
      callback.onFailure(caught);
    }
  }
}