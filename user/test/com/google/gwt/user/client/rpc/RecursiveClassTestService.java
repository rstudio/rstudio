/*
 * Copyright 2010 Google Inc.
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
 * Service used to test generics with wild cards and recursive references.
 */
public interface RecursiveClassTestService extends RemoteService {

  /**
   * This class has a self-reference and wild cards in its parameterization.
   */
  public class ResultNode<T extends ResultNode<?>> implements Serializable {
    private static final long serialVersionUID = -3560238969723137110L;

    public int dataType;
    public int id1;
    public int id2;
    public int id3;
    public int id4;
    public int numChildren;
    public String data;
    public T next;

    public ResultNode() {
    }
  }
  
  /**
   * Subclass of the self-referencing class; necessary to test a previous bug.
   */
  public class SubResultNode<S extends ResultNode<?>> extends ResultNode<S> {
    int i;
  }
  
  <U extends ResultNode<U>> ResultNode<U> greetServer(String name) throws IllegalArgumentException;
}
