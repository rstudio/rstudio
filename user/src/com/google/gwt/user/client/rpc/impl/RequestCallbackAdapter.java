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
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
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
   * {@link AsyncCallback} to notify or success or failure.
   */
  private final AsyncCallback<T> callback;

  /**
   * Instance which will read the expected return type out of the
   * {@link SerializationStreamReader}.
   */
  private final ResponseReader responseReader;

  /**
   * {@link SerializationStreamFactory} for creating
   * {@link SerializationStreamReader}s.
   */
  private final SerializationStreamFactory streamFactory;

  public RequestCallbackAdapter(SerializationStreamFactory streamFactory,
      AsyncCallback<T> callback, ResponseReader responseReader) {
    assert (streamFactory != null);
    assert (callback != null);
    assert (responseReader != null);

    this.streamFactory = streamFactory;
    this.callback = callback;
    this.responseReader = responseReader;
  }

  public void onError(Request request, Throwable exception) {
    callback.onFailure(exception);
  }

  @SuppressWarnings("unchecked")
  public void onResponseReceived(Request request, Response response) {
    T result = null;
    Throwable caught = null;
    try {
      String encodedResponse = response.getText();

      if (RemoteServiceProxy.isReturnValue(encodedResponse)) {
        result = (T) responseReader.read(streamFactory.createStreamReader(encodedResponse));
      } else if (RemoteServiceProxy.isThrownException(encodedResponse)) {
        caught = (Throwable) streamFactory.createStreamReader(encodedResponse).readObject();
      } else {
        caught = new InvocationException(encodedResponse);
      }
    } catch (com.google.gwt.user.client.rpc.SerializationException e) {
      caught = new IncompatibleRemoteServiceException();
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