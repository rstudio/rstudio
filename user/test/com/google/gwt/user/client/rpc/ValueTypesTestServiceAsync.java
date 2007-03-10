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
package com.google.gwt.user.client.rpc;

/**
 * TODO: document me.
 */
public interface ValueTypesTestServiceAsync {
  void echo(byte value, AsyncCallback callback);

  void echo(char value, AsyncCallback callback);

  void echo(double value, AsyncCallback callback);

  void echo(float value, AsyncCallback callback);

  void echo(int value, AsyncCallback callback);

  void echo(long value, AsyncCallback callback);

  void echo(short value, AsyncCallback callback);

  void echo_FALSE(boolean value, AsyncCallback callback);

  void echo_MAX_VALUE(byte value, AsyncCallback callback);

  void echo_MAX_VALUE(char value, AsyncCallback callback);

  void echo_MAX_VALUE(double value, AsyncCallback callback);

  void echo_MAX_VALUE(float value, AsyncCallback callback);

  void echo_MAX_VALUE(int value, AsyncCallback callback);

  void echo_MAX_VALUE(long value, AsyncCallback callback);

  void echo_MAX_VALUE(short value, AsyncCallback callback);

  void echo_MIN_VALUE(byte value, AsyncCallback callback);

  void echo_MIN_VALUE(char value, AsyncCallback callback);

  void echo_MIN_VALUE(double value, AsyncCallback callback);

  void echo_MIN_VALUE(float value, AsyncCallback callback);

  void echo_MIN_VALUE(int value, AsyncCallback callback);

  void echo_MIN_VALUE(long value, AsyncCallback callback);

  void echo_MIN_VALUE(short value, AsyncCallback callback);

  void echo_TRUE(boolean value, AsyncCallback callback);
}
