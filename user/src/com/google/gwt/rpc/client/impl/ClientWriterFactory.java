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
package com.google.gwt.rpc.client.impl;

import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.rpc.server.CommandServerSerializationStreamReader;
import com.google.gwt.rpc.server.CommandServerSerializationStreamWriter;
import com.google.gwt.rpc.server.HostedModeClientOracle;
import com.google.gwt.rpc.server.SimplePayloadDecoder;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.Collections;

/**
 * Isolates client code from swapping out the command factory in hosted versus
 * Production Mode. This type has a super-source peer which is used in
 * Production Mode.
 */
public class ClientWriterFactory {

  public static SerializationStreamReader createReader(String payload)
      throws IncompatibleRemoteServiceException, RemoteException {
    SimplePayloadDecoder decoder;
    try {
      decoder = new SimplePayloadDecoder(new HostedModeClientOracle(), payload);
    } catch (ClassNotFoundException e) {
      throw new IncompatibleRemoteServiceException(
          "Client does not have a type sent by the server", e);
    }
    CommandServerSerializationStreamReader reader = new CommandServerSerializationStreamReader();
    if (decoder.getThrownValue() != null) {
      reader.prepareToRead(Collections.singletonList(decoder.getThrownValue()));
      try {
        throw new RemoteException((Throwable) reader.readObject());
      } catch (ClassCastException e) {
        throw new RemoteException(
            "The remote end threw something other than a Throwable", e);
      } catch (SerializationException e) {
        throw new RemoteException(
            "The remote end threw an exception which could not be deserialized",
            e);
      }
    } else {
      reader.prepareToRead(decoder.getValues());
    }
    return reader;
  }

  @SuppressWarnings("unused") // used by super-source peer
  public static SerializationStreamWriter createWriter(
      TypeOverrides typeOverrides, CommandSink commandSink) {
    return new CommandServerSerializationStreamWriter(commandSink);
  }
}
