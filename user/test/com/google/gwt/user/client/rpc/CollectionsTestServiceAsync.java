// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public interface CollectionsTestServiceAsync {
  void echo(ArrayList value, AsyncCallback callback);

  void echo(boolean[] value, AsyncCallback callback);

  void echo(Boolean[] value, AsyncCallback callback);

  void echo(byte[] value, AsyncCallback callback);

  void echo(Byte[] value, AsyncCallback callback);

  void echo(char[] value, AsyncCallback callback);

  void echo(Character[] value, AsyncCallback callback);

  void echo(Date[] date, AsyncCallback callback);

  void echo(double[] value, AsyncCallback callback);

  void echo(Double[] value, AsyncCallback callback);

  void echo(float[] value, AsyncCallback callback);

  void echo(Float[] value, AsyncCallback callback);

  void echo(HashMap value, AsyncCallback callback);

  void echo(HashSet value, AsyncCallback callback);

  void echo(int[] value, AsyncCallback callback);

  void echo(Integer[] value, AsyncCallback callback);

  void echo(long[] value, AsyncCallback callback);

  void echo(Long[] value, AsyncCallback callback);

  void echo(short[] value, AsyncCallback callback);

  void echo(Short[] value, AsyncCallback callback);

  void echo(String[] value, AsyncCallback callback);

  void echo(Vector value, AsyncCallback callback);
}
