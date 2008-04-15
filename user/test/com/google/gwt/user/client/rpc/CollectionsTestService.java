/*
 * Copyright 2008 Google Inc.
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
public interface CollectionsTestService extends RemoteService {

  /**
   * TODO: document me.
   */
  public static class CollectionsTestServiceException extends Exception {
    public CollectionsTestServiceException() {
    }

    public CollectionsTestServiceException(String msg) {
      super(msg);
    }
  }

  ArrayList<IsSerializable> echo(ArrayList<IsSerializable> value)
      throws CollectionsTestServiceException;

  boolean[] echo(boolean[] value) throws CollectionsTestServiceException;

  Boolean[] echo(Boolean[] value) throws CollectionsTestServiceException;

  byte[] echo(byte[] value) throws CollectionsTestServiceException;

  Byte[] echo(Byte[] value) throws CollectionsTestServiceException;

  char[] echo(char[] value) throws CollectionsTestServiceException;

  Character[] echo(Character[] value) throws CollectionsTestServiceException;

  Date[] echo(Date[] date) throws CollectionsTestServiceException;

  java.sql.Date[] echo(java.sql.Date[] value)
      throws CollectionsTestServiceException;

  double[] echo(double[] value) throws CollectionsTestServiceException;

  Double[] echo(Double[] value) throws CollectionsTestServiceException;

  float[] echo(float[] value) throws CollectionsTestServiceException;

  Float[] echo(Float[] value) throws CollectionsTestServiceException;

  HashMap<String, IsSerializable> echo(HashMap<String, IsSerializable> value)
      throws CollectionsTestServiceException;

  HashSet<IsSerializable> echo(HashSet<IsSerializable> value)
      throws CollectionsTestServiceException;

  int[] echo(int[] value) throws CollectionsTestServiceException;

  Integer[] echo(Integer[] value) throws CollectionsTestServiceException;

  long[] echo(long[] value) throws CollectionsTestServiceException;

  Long[] echo(Long[] value) throws CollectionsTestServiceException;

  short[] echo(short[] value) throws CollectionsTestServiceException;

  Short[] echo(Short[] value) throws CollectionsTestServiceException;

  String[] echo(String[] value) throws CollectionsTestServiceException;

  String[][] echo(String[][] value) throws CollectionsTestServiceException;

  Time[] echo(Time[] value) throws CollectionsTestServiceException;

  Timestamp[] echo(Timestamp[] value) throws CollectionsTestServiceException;

  Vector<IsSerializable> echo(Vector<IsSerializable> value)
      throws CollectionsTestServiceException;

  List<IsSerializable> getArraysAsList(List<IsSerializable> value);
}
