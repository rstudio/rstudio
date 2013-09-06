/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.user.server.rpc.testcases;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

/**
 * An example of how to create a cycle using type variable inference. To determine
 * the expected type of the HELLO.ptr field, the type inference in
 * {@link com.google.gwt.user.server.rpc.impl.SerializabilityUtil}
 * starts with PtrPtr.ptr and deduces the following:
 *
 * <ul>
 *   <li>C = B (by inheritance)
 *   <li>B = A (by inheritance)
 *   <li>A = C (because the raw type of ptr is BasePtr&lt;A&gt; and we are substituting C into it)
 * </pre>
 *
 * <p>If we put these deductions into a map then there will be a cycle in the map.
 * The type inferencer needs to detect the cycle and conclude that there is no constraint
 * on any of these type variables, so HELLO.ptr.target could be any serializable
 * type.</p>
 *
 * <p>TODO(skybrian): it's unclear whether this should really be a cycle. The 'A' in the second
 * deduction annotates the PtrPtr object (HELLO) and the 'A' in the last deduction is for the
 * SimplePtr object (HELLO.ptr) so perhaps they are two type variables of the same name in different
 * scopes? Currently the algorithm in SerializabilityUtil doesn't have scopes so this might
 * just be a false alias. It doesn't affect the conclusion for this example, though.</p>
 */
public class TypeVariableCycle implements RemoteService {

  /**
   * A value we that we want to send via GWT-RPC.
   */
  public static final PtrPtr<String> HELLO =
      new PtrPtr<String>(new SimplePtr<String>("hello"));

  /**
   * The RPC method that we will call.
   */
  @SuppressWarnings("unused")
  public static <X> X dereference(BasePtr<X> any) {
    return any.get();
  }

  /**
   * The base of a convoluted class hierarchy of pointer types.
   */
  public static abstract class BasePtr<A> implements IsSerializable {
    abstract A get();
  }

  /**
   * An unneeded intermediate class to make a size-3 cycle. (Intermediate
   * classes could be added to make a cycle of any size.)
   */
  public abstract static class NonSimplePtr<B> extends BasePtr<B> {
  }

  /**
   * A pointer to a pointer.
   */
  public static class PtrPtr<C> extends NonSimplePtr<C> {
    public BasePtr<C> ptr;

    @SuppressWarnings("unused")
    PtrPtr() {
    }

    PtrPtr(BasePtr<C> ptr) {
      this.ptr = ptr;
    }

    @Override
    public C get() {
      return ptr.get();
    }
  }

  /**
   * A trivial implementation of BasePtr.
   */
  public static class SimplePtr<D> extends BasePtr<D> {
    public D target;

    @SuppressWarnings("unused")
    SimplePtr() {
    }

    SimplePtr(D target) {
      this.target = target;
    }

    @Override
    D get() {
      return target;
    }
  }
}
