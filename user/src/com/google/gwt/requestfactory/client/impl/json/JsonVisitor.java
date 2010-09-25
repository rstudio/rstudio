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
package com.google.gwt.requestfactory.client.impl.json;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

/**
 * A visitor for JSON objects. For each unique JSON datatype, a callback is
 * invoked with a {@link com.google.gwt.requestfactory.client.impl.json.JsonContext}
 * that can be used to replace a value or remove it. For Object and Array
 * types, the {@link #visitKey} and {@link #visitIndex} methods are invoked
 * respectively for each contained value to determine if they should be
 * processed or not. Finally, the visit methods for Object and Array types
 * returns a boolean that determines whether or not to process its contained
 * values.
 */
class JsonVisitor {

  /**
   * Accept a JS array or JS object type and visit its members.
   * @param jso
   */
  public void accept(JavaScriptObject jso) {
    if (ClientJsonUtil.isArray(jso)) {
      acceptArray(jso);
    } else {
      acceptMap(jso);
    }
  }

  /**
   * Called after every element of array has been visited.
   */
  public void endVisit(JsArray array, JsonContext ctx) {
  }

  /**
   * Called after every field of an object has been visited.
   * @param object
   * @param ctx
   */
  public void endVisit(JsonMap object, JsonContext ctx) {
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
  public void visit(boolean bool, JsonContext ctx) {
  }

  /**
   * Called for JS arrays present in a JSON object. Return true if array
   * elements should be visited.
   * @param array a JS array
   * @param ctx a context to replace or delete the array
   * @return true if the array elements should be visited
   */
  public boolean visit(JsArray array, JsonContext ctx) {
    return true;
  }

  /**
   * Called for JS objects present in a JSON object. Return true if object
   * fields should be visited.
   * @param object a Json object
   * @param ctx a context to replace or delete the object
   * @return true if object fields should be visited
   */
  public boolean visit(JsonMap object, JsonContext ctx) {
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

  protected void acceptArray(JavaScriptObject jso) {
    acceptNative(new JsonArrayContext(jso));
  }

  protected void acceptMap(JavaScriptObject jso) {
    acceptNative(new JsonMapContext(jso));
  }

  /*
   * Implemented purely natively as DevMode would force wrapping / unwrapping
   * all return values otherwise.
   */
  protected native void acceptNative(JsonContext context) /*-{
    var thejso = context.@com.google.gwt.requestfactory.client.impl.json.JsonContext::getJso()();
    var isArray = @com.google.gwt.requestfactory.client.impl.json.ClientJsonUtil::isArray(Lcom/google/gwt/core/client/JavaScriptObject;)(thejso);

    function dispatch(v, ctx, value) {

        var type = typeof value;
        if (value == null || type == 'null') {
          v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::visitNull(Lcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(ctx)
        } else if (type == 'number') {
          v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::visit(DLcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(value, ctx);
        } else if (type == 'string') {
          v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::visit(Ljava/lang/String;Lcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(value, ctx);
        } else if (type == 'boolean') {
          v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::visit(ZLcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(value, ctx);
        } else if (type == 'object') {
          if (@com.google.gwt.requestfactory.client.impl.json.ClientJsonUtil::isArray(Lcom/google/gwt/core/client/JavaScriptObject;)(value)) {
            if (v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::visit(Lcom/google/gwt/core/client/JsArray;Lcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(value, ctx)) {
               v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::acceptArray(Lcom/google/gwt/core/client/JavaScriptObject;)(value);
            }
            v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::endVisit(Lcom/google/gwt/core/client/JsArray;Lcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(value, ctx)
          } else {
            if (v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::visit(Lcom/google/gwt/requestfactory/client/impl/json/JsonMap;Lcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(value, ctx)) {
               v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::acceptMap(Lcom/google/gwt/core/client/JavaScriptObject;)(value);
            }
            v.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::endVisit(Lcom/google/gwt/requestfactory/client/impl/json/JsonMap;Lcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(value, ctx)
          }
        }
        ctx.@com.google.gwt.requestfactory.client.impl.json.JsonContext::setFirst(Z)(false);
    }

    var value;
    if (isArray) {
     for (var i = 0; i < thejso.length; i++) {
       context.@com.google.gwt.requestfactory.client.impl.json.JsonArrayContext::setCurrentIndex(I)(i);
       value = thejso[i];
       if (this.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::visitIndex(ILcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(i, context)) {
         dispatch(this, context, value);
       }
     }
    } else {
     for (var prop in thejso) {
       context.@com.google.gwt.requestfactory.client.impl.json.JsonMapContext::setCurrentKey(Ljava/lang/String;)(prop);
       value = thejso[prop];
       if (thejso.hasOwnProperty(prop)
              && this.@com.google.gwt.requestfactory.client.impl.json.JsonVisitor::visitKey(Ljava/lang/String;Lcom/google/gwt/requestfactory/client/impl/json/JsonContext;)(prop, context)) {
         dispatch(this, context, value);
       }
     }
    }
  }-*/;
}
