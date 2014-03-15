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
package com.google.gwt.dev.shell;

import com.google.gwt.dev.shell.BrowserChannel.FreeMessage;
import com.google.gwt.dev.shell.BrowserChannel.InvokeSpecialMessage;
import com.google.gwt.dev.shell.BrowserChannel.ReturnMessage;
import com.google.gwt.dev.shell.BrowserChannel.SessionHandler.SpecialDispatchId;
import com.google.gwt.dev.shell.BrowserChannel.Value;
import com.google.gwt.dev.shell.BrowserChannelClient.SessionHandlerClient;

import java.io.IOException;

/**
 * A class to encapsulate function invocations of objects on the server side.
 */
public class ServerMethods {
  /**
   * Tell the server that the client no longer has any references to the
   * specified Java object.
   *
   * @param ids IDs of objects to free
   * @return false if an error occurred
   */
  static boolean freeJava(BrowserChannelClient channel, int ids[]) {
    if (!channel.isConnected()) {
      // ignoring freeJava after disconnect.
      return true;
    }
    try {
      new FreeMessage(channel, ids).send();
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  /**
   * Get the value of a property on an object.
   *
   * @param objectRef ID of object to fetch field on
   * @param dispatchId dispatch ID of field
   * @return the value of the property, undef if none (or on error)
   */
  static Value getProperty(BrowserChannelClient channel,
      SessionHandlerClient handler, int objectRef, int dispatchId) {
    if (!channel.isConnected()) {
      // ignoring getProperty() after disconnect
      return new Value();
    }
    Value args[] = new Value[2];
    args[0] = new Value();
    args[0].setInt(objectRef);
    args[1] = new Value();
    args[1].setInt(dispatchId);

    synchronized (handler.getSynchronizationObject()) {
      try {
        new InvokeSpecialMessage(channel, SpecialDispatchId.GetProperty, args).send();
        ReturnMessage returnMessage = channel.reactToMessagesWhileWaitingForReturn(handler);
        if (!returnMessage.isException()) {
          return returnMessage.getReturnValue();
        }
      } catch (IOException e) {
      } catch (BrowserChannelException e) {
      }
    }
    return new Value();
  }

  /**
   * Set the value of a property on an object.
   *
   * @param objectRef ID of object to fetch field on
   * @param dispatchId dispatch ID of field
   * @param value value to store in the property
   * @return false if an error occurred
   */
  static boolean setProperty(BrowserChannelClient channel,
      SessionHandlerClient handler, int objectRef, int dispatchId, Value value) {
    Value args[] = new Value[3];
    for (int i = 0; i < args.length; i++) {
      args[i] = new Value();
    }
    args[0].setInt(objectRef);
    args[1].setInt(dispatchId);
    args[2] = value;
    synchronized (handler.getSynchronizationObject()) {
      try {
        new InvokeSpecialMessage(channel, SpecialDispatchId.SetProperty, args).send();
        ReturnMessage returnMessage = channel.reactToMessagesWhileWaitingForReturn(handler);
        if (!returnMessage.isException()) {
          return true;
        }
      } catch (IOException e) {
      } catch (BrowserChannelException e) {
      }
    }
    // TODO: use the returned exception?
    return false;
  }

}
