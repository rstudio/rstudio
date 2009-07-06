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
import com.google.gwt.rpc.client.ast.RpcCommand;
import com.google.gwt.user.client.rpc.SerializationException;

import java.util.List;

/**
 * A simple CommandSink that adds observed commands to a list.
 */
public class ListCommandSink extends CommandSink {
  private List<RpcCommand> commands;

  public ListCommandSink(List<RpcCommand> commands) {
    this.commands = commands;
  }

  @Override
  public void accept(RpcCommand command) throws SerializationException {
    commands.add(command);
  }

  /**
   * Does nothing.
   */
  @Override
  public void finish() throws SerializationException {
  }
}
