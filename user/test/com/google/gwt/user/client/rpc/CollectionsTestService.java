// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public interface CollectionsTestService extends RemoteService {

  public static class CollectionsTestServiceException extends
      SerializableException {
    public CollectionsTestServiceException() {
    }

    public CollectionsTestServiceException(String msg) {
      super(msg);
    }
  }

  /**
   * @gwt.typeArgs value <com.google.gwt.user.client.rpc.IsSerializable>
   * @gwt.typeArgs <com.google.gwt.user.client.rpc.IsSerializable>
   */
  ArrayList echo(ArrayList value) throws CollectionsTestServiceException;

  boolean[] echo(boolean[] value) throws CollectionsTestServiceException;

  Boolean[] echo(Boolean[] value) throws CollectionsTestServiceException;

  byte[] echo(byte[] value) throws CollectionsTestServiceException;

  Byte[] echo(Byte[] value) throws CollectionsTestServiceException;

  char[] echo(char[] value) throws CollectionsTestServiceException;

  Character[] echo(Character[] value) throws CollectionsTestServiceException;

  Date[] echo(Date[] date) throws CollectionsTestServiceException;

  double[] echo(double[] value) throws CollectionsTestServiceException;

  Double[] echo(Double[] value) throws CollectionsTestServiceException;

  float[] echo(float[] value) throws CollectionsTestServiceException;

  Float[] echo(Float[] value) throws CollectionsTestServiceException;

  /**
   * @gwt.typeArgs value <java.lang.String,
   *               com.google.gwt.user.client.rpc.IsSerializable>
   * @gwt.typeArgs <java.lang.String,
   *               com.google.gwt.user.client.rpc.IsSerializable>
   */
  HashMap echo(HashMap value) throws CollectionsTestServiceException;

  /**
   * @gwt.typeArgs value <com.google.gwt.user.client.rpc.IsSerializable>
   * @gwt.typeArgs <com.google.gwt.user.client.rpc.IsSerializable>
   */
  HashSet echo(HashSet value) throws CollectionsTestServiceException;

  int[] echo(int[] value) throws CollectionsTestServiceException;

  Integer[] echo(Integer[] value) throws CollectionsTestServiceException;

  long[] echo(long[] value) throws CollectionsTestServiceException;

  Long[] echo(Long[] value) throws CollectionsTestServiceException;

  short[] echo(short[] value) throws CollectionsTestServiceException;

  Short[] echo(Short[] value) throws CollectionsTestServiceException;

  String[] echo(String[] value) throws CollectionsTestServiceException;

  /**
   * @gwt.typeArgs value <com.google.gwt.user.client.rpc.IsSerializable>
   * @gwt.typeArgs <com.google.gwt.user.client.rpc.IsSerializable>
   */
  Vector echo(Vector value) throws CollectionsTestServiceException;
}
