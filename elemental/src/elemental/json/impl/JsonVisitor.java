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
package elemental.json.impl;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonContext;

/**
 * A visitor for JSON objects. For each unique JSON datatype, a callback is
 * invoked with a {@link elemental.json.impl.JsonContext}
 * that can be used to replace a value or remove it. For Object and Array
 * types, the {@link #visitKey} and {@link #visitIndex} methods are invoked
 * respectively for each contained value to determine if they should be
 * processed or not. Finally, the visit methods for Object and Array types
 * returns a boolean that determines whether or not to process its contained
 * values.
 */
class JsonVisitor {

  private class ImmutableJsonContext extends JsonContext {

    public ImmutableJsonContext(JsonValue node) {
      super(node);
    }
    
    @Override
    public void removeMe() {
      immutableError();
    }

    @Override
    public void replaceMe(double d) {
      immutableError();
    }

    @Override
    public void replaceMe(String d) {
      immutableError();
    }

    @Override
    public void replaceMe(boolean d) {
      immutableError();
    }

    @Override
    public void replaceMe(JsonValue value) {
      immutableError();
    }

    private void immutableError() {
      throw new UnsupportedOperationException("Immutable context");
    }
  }

  public void accept(JsonValue node) {
    accept(node, new ImmutableJsonContext(node));
  }
  
  /**
   * Accept array or object type and visit its members.
   */
  public void accept(JsonValue node, JsonContext ctx) {
    if (node == null) {
      return;
    }
    ((JreJsonValue) node).traverse(this, ctx);
  }

  /**
   * Called after every element of array has been visited.
   */
  public void endVisit(JsonArray array, JsonContext ctx) {
  }

  /**
   * Called after every field of an object has been visited.
   * @param object
   * @param ctx
   */
  public void endVisit(JsonObject object, JsonContext ctx) {
  }

  /**
   * Called for JS numbers present in a JSON object.
   */
  public void visit(double number, JsonContext ctx) {
  }

  /**
   * Called for JS strings present in a JSON object.
   */
  public void visit(String string, JsonContext ctx) {
  }

  /**
   * Called for JS boolean present in a JSON object.
   */
  public void visit(boolean bool, elemental.json.impl.JsonContext ctx) {
  }

  /**
   * Called for JS arrays present in a JSON object. Return true if array
   * elements should be visited.
   * @param array a JS array
   * @param ctx a context to replace or delete the array
   * @return true if the array elements should be visited
   */
  public boolean visit(JsonArray array, JsonContext ctx) {
    return true;
  }

  /**
   * Called for JS objects present in a JSON object. Return true if object
   * fields should be visited.
   * @param object a Json object
   * @param ctx a context to replace or delete the object
   * @return true if object fields should be visited
   */
  public boolean visit(JsonObject object, JsonContext ctx) {
    return true;
  }

  /**
   * Return true if the value for a given array index should be visited.
   * @param index an index in a JSON array
   * @param ctx a context object used to delete or replace values
   * @return true if the value associated with the index should be visited
   */
  public boolean visitIndex(int index, JsonContext ctx) {
    return true;
  }

  /**
   * Return true if the value for a given object key should be visited.
   * @param key a key in a JSON object
   * @param ctx a context object used to delete or replace values
   * @return true if the value associated with the key should be visited
   */
  public boolean visitKey(String key, JsonContext ctx) {
    return true;
  }

  /**
   * Called for nulls present in a JSON object.
   */
  public void visitNull(JsonContext ctx) {
  }
}
