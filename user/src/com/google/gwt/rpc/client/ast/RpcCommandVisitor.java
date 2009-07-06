/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.rpc.client.ast;

import java.util.List;

/**
 * Describes a visitor that will traverse the RpcCommand structures.
 */
public class RpcCommandVisitor {
  /**
   * The context provides no services at this point and is provided as an
   * extension point for future development.
   */
  public interface Context {
  }

  public final void accept(List<? extends RpcCommand> x) {
    doAccept(x);
  }

  public final RpcCommand accept(RpcCommand x) {
    return doAccept(x);
  }

  public final void accept(RpcCommand[] x) {
    doAccept(x);
  }

  public void endVisit(ArrayValueCommand x, Context ctx) {
  }

  public void endVisit(BooleanValueCommand x, Context ctx) {
  }

  public void endVisit(ByteValueCommand x, Context ctx) {
  }

  public void endVisit(CharValueCommand x, Context ctx) {
  }

  public void endVisit(DoubleValueCommand x, Context ctx) {
  }

  public void endVisit(EnumValueCommand x, Context ctx) {
  }

  public void endVisit(FloatValueCommand x, Context ctx) {
  }

  public void endVisit(InstantiateCommand x, Context ctx) {
  }

  public void endVisit(IntValueCommand x, Context ctx) {
  }

  public void endVisit(InvokeCustomFieldSerializerCommand x, Context ctx) {
  }

  public void endVisit(LongValueCommand x, Context ctx) {
  }

  public void endVisit(NullValueCommand x, Context ctx) {
  }

  public void endVisit(ReturnCommand x, Context ctx) {
  }

  public void endVisit(SetCommand x, Context ctx) {
  }

  public void endVisit(ShortValueCommand x, Context ctx) {
  }

  public void endVisit(StringValueCommand x, Context ctx) {
  }

  public void endVisit(ThrowCommand x, Context ctx) {
  }

  public boolean visit(ArrayValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(BooleanValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(ByteValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(CharValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(DoubleValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(EnumValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(FloatValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(InstantiateCommand x, Context ctx) {
    return true;
  }

  public boolean visit(IntValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(InvokeCustomFieldSerializerCommand x, Context ctx) {
    return true;
  }

  public boolean visit(LongValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(NullValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(ReturnCommand x, Context ctx) {
    return true;
  }

  public boolean visit(SetCommand x, Context ctx) {
    return true;
  }

  public boolean visit(ShortValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(StringValueCommand x, Context ctx) {
    return true;
  }

  public boolean visit(ThrowCommand x, Context ctx) {
    return true;
  }

  protected void doAccept(List<? extends RpcCommand> x) {
    for (RpcCommand c : x) {
      accept(c);
    }
  }

  protected RpcCommand doAccept(RpcCommand x) {
    x.traverse(this, null);
    return x;
  }

  protected void doAccept(RpcCommand[] x) {
    for (RpcCommand c : x) {
      accept(c);
    }
  }

  /*
   * TODO: Make this fail more visibly
   */
  protected final void halt(Throwable t) {
    throw new RuntimeException("Unable to continue", t);
  }
}
