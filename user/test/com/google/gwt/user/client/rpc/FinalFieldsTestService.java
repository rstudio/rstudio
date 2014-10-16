/*
 * Copyright 2014 Google Inc.
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

import java.io.Serializable;

/**
 * TestService to test final fields serialization in RPC.
 */
public interface FinalFieldsTestService extends RemoteService {

  /**
   * The class used in RPC serialization.
   */
  public class FinalFieldsNode implements Serializable {

    public final int i;
    public final String str;
    public final float[] f;

    public FinalFieldsNode() {
      this.i = 5;
      this.str = "A";
      this.f = new float[3];
    }

    public FinalFieldsNode(int i, String str, int f_len) {
      this.i = i;
      this.str = str;
      this.f = new float[f_len];
    }
  }

  /**
   * Exception specific to testing of GWT final fields in RPC.
   */
  final class FinalFieldsException extends Exception {
    public FinalFieldsException() {
    }
  }

  FinalFieldsNode transferObject(FinalFieldsNode node) throws FinalFieldsException;

  int returnI(FinalFieldsNode node);
}
