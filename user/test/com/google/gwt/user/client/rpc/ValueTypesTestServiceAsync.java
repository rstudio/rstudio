// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

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
