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

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Isolates client code from swapping out the command factory in hosted versus
 * web mode.
 */
@GwtScriptOnly
public class ClientWriterFactory {

  public static SerializationStreamReader createReader(String payload)
      throws IncompatibleRemoteServiceException, RemoteException {
    CommandClientSerializationStreamReader toReturn = new CommandClientSerializationStreamReader();
    toReturn.prepareToRead(payload);
    return toReturn;
  }

  public static SerializationStreamWriter createWriter(
      TypeOverrides typeOverrides, CommandSink commandSink) {
    return new CommandClientSerializationStreamWriter(typeOverrides,
        commandSink);
  }
}
