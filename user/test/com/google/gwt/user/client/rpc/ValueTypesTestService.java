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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Test sending value types via RPC.
 */
public interface ValueTypesTestService extends RemoteService {
  byte echo(byte value);

  char echo(char value);

  double echo(double value);

  float echo(float value);

  int echo(int value);

  long echo(long value);

  short echo(short value);

  BigDecimal echo(BigDecimal value);

  BigInteger echo(BigInteger value);

  SerializableGenericWrapperType<Void> echo(
      SerializableGenericWrapperType<Void> value);

  String echo(String value);

  boolean echo_FALSE(boolean value);

  byte echo_MAX_VALUE(byte value);

  char echo_MAX_VALUE(char value);

  double echo_MAX_VALUE(double value);

  float echo_MAX_VALUE(float value);

  int echo_MAX_VALUE(int value);

  long echo_MAX_VALUE(long value);

  short echo_MAX_VALUE(short value);

  byte echo_MIN_VALUE(byte value);

  char echo_MIN_VALUE(char value);

  double echo_MIN_VALUE(double value);

  float echo_MIN_VALUE(float value);

  int echo_MIN_VALUE(int value);

  long echo_MIN_VALUE(long value);

  short echo_MIN_VALUE(short value);

  boolean echo_TRUE(boolean value);
}
