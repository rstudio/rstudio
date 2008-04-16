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

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * TODO: document me.
 */
public interface CollectionsTestServiceAsync {
  void echo(ArrayList<IsSerializable> value,
      AsyncCallback<ArrayList<IsSerializable>> callback);

  void echo(boolean[] value, AsyncCallback<boolean[]> callback);

  void echo(Boolean[] value, AsyncCallback<Boolean[]> callback);

  void echo(byte[] value, AsyncCallback<byte[]> callback);

  void echo(Byte[] value, AsyncCallback<Byte[]> callback);

  void echo(char[] value, AsyncCallback<char[]> callback);

  void echo(Character[] value, AsyncCallback<Character[]> callback);

  void echo(Date[] date, AsyncCallback<Date[]> callback);

  void echo(java.sql.Date[] value, AsyncCallback<java.sql.Date[]> callback);

  void echo(double[] value, AsyncCallback<double[]> callback);

  void echo(Double[] value, AsyncCallback<Double[]> callback);

  void echo(float[] value, AsyncCallback<float[]> callback);

  void echo(Float[] value, AsyncCallback<Float[]> callback);

  void echo(HashMap<String, IsSerializable> value,
      AsyncCallback<HashMap<String, IsSerializable>> callback);

  void echo(HashSet<IsSerializable> value,
      AsyncCallback<HashSet<IsSerializable>> callback);

  void echo(int[] value, AsyncCallback<int[]> callback);

  void echo(Integer[] value, AsyncCallback<Integer[]> callback);

  void echo(long[] value, AsyncCallback<long[]> callback);

  void echo(Long[] value, AsyncCallback<Long[]> callback);

  void echo(short[] value, AsyncCallback<short[]> callback);

  void echo(Short[] value, AsyncCallback<Short[]> callback);

  void echo(String[] value, AsyncCallback<String[]> callback);

  void echo(String[][] value, AsyncCallback<String[][]> callback);

  void echo(Time[] value, AsyncCallback<Time[]> callback);

  void echo(Timestamp[] value, AsyncCallback<Timestamp[]> callback);

  void echo(Vector<IsSerializable> value,
      AsyncCallback<Vector<IsSerializable>> callback);

  void echoArraysAsList(List<IsSerializable> value,
      AsyncCallback<List<IsSerializable>> callback);
}
