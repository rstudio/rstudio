// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

public interface ValueTypesTestService extends RemoteService {
  byte echo(byte value);

  char echo(char value);

  double echo(double value);

  float echo(float value);

  int echo(int value);

  long echo(long value);

  short echo(short value);

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
