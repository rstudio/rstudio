/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.user.server.rpc.testcases;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

/**
 * Tests that we can use the type parameter on a subtype two different ways.
 */
public class SubtypeUsedTwice implements RemoteService {

  /**
   * Creates the argument we will use to calling {@link #send}.
   */
  public static Arg makeArg(int value) {
    TypedHandle<Integer> intHolder = new MyTypedHandle<Integer>();
    intHolder.thing = value;

    Any any = new Any();
    any.any = intHolder;

    Arg arg = new Arg();
    arg.handle.thing = any;

    return arg;
  }

  /**
   * A dummy RPC call that sends one argument to the server.
   */
  public static void send(Arg a) {
  }

  /**
   * The RPC call's argument.
   */
  public static class Arg implements IsSerializable {
    public TypedHandle<Any> handle = new MyTypedHandle<Any>();
  }

  /**
   * A class that will hold the second instance of MyTypedHandle,
   * without constraining it.
   */
  public static class Any implements IsSerializable {
    public IsSerializable any;
  }

  /**
   * Superclass with a type variable.
   */
  public static abstract class TypedHandle<T> implements IsSerializable {
    public T thing;
  }

  /**
   * Subclass with a different type variable.
   */
  public static class MyTypedHandle<T2> extends TypedHandle<T2> {
  }
}
