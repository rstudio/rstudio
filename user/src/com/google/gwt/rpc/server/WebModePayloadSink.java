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
package com.google.gwt.rpc.server;

import static com.google.gwt.rpc.client.impl.CommandClientSerializationStreamReader.BACKREF_IDENT;

import com.google.gwt.rpc.client.ast.ArrayValueCommand;
import com.google.gwt.rpc.client.ast.BooleanValueCommand;
import com.google.gwt.rpc.client.ast.ByteValueCommand;
import com.google.gwt.rpc.client.ast.CharValueCommand;
import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.rpc.client.ast.DoubleValueCommand;
import com.google.gwt.rpc.client.ast.EnumValueCommand;
import com.google.gwt.rpc.client.ast.FloatValueCommand;
import com.google.gwt.rpc.client.ast.InstantiateCommand;
import com.google.gwt.rpc.client.ast.IntValueCommand;
import com.google.gwt.rpc.client.ast.InvokeCustomFieldSerializerCommand;
import com.google.gwt.rpc.client.ast.LongValueCommand;
import com.google.gwt.rpc.client.ast.NullValueCommand;
import com.google.gwt.rpc.client.ast.ReturnCommand;
import com.google.gwt.rpc.client.ast.RpcCommand;
import com.google.gwt.rpc.client.ast.RpcCommandVisitor;
import com.google.gwt.rpc.client.ast.SetCommand;
import com.google.gwt.rpc.client.ast.ShortValueCommand;
import com.google.gwt.rpc.client.ast.StringValueCommand;
import com.google.gwt.rpc.client.ast.ThrowCommand;
import com.google.gwt.rpc.client.ast.ValueCommand;
import com.google.gwt.rpc.client.impl.CommandClientSerializationStreamReader;
import com.google.gwt.rpc.client.impl.EscapeUtil;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A CommandSink that will generate a web-mode payload.
 * 
 * ONE-SHOT EVAL (no incremental evaluation, must call finish())
 */
public class WebModePayloadSink extends CommandSink {

  private class BackRefAssigner extends RpcCommandVisitor {
    private final Set<ValueCommand> seenOnce = new HashSet<ValueCommand>();

    @Override
    public void endVisit(InvokeCustomFieldSerializerCommand x, Context ctx) {
      // We always need a backref for custom serializers
      makeBackRef(x);
    }

    @Override
    public void endVisit(LongValueCommand x, Context ctx) {
      process(x);
    }

    @Override
    public void endVisit(StringValueCommand x, Context ctx) {
      process(x);
    }

    @Override
    public boolean visit(ArrayValueCommand x, Context ctx) {
      return process(x);
    }

    @Override
    public boolean visit(InstantiateCommand x, Context ctx) {
      return process(x);
    }

    private boolean process(ValueCommand x) {
      if (!seenOnce.add(x)) {
        makeBackRef(x);
        return false;
      }
      return true;
    }
  }
  private class PayloadVisitor extends RpcCommandVisitor {
    private final Map<Class<?>, byte[]> constructorFunctions = new IdentityHashMap<Class<?>, byte[]>();
    private final Map<RpcCommand, ByteBuffer> commandBuffers = new IdentityHashMap<RpcCommand, ByteBuffer>();
    private ByteBuffer currentBuffer;
    private final Stack<RpcCommand> stack = new Stack<RpcCommand>();
    private final Set<RpcCommand> started = new HashSet<RpcCommand>();

    @Override
    public void endVisit(BooleanValueCommand x, Context ctx) {
      if (x.getValue()) {
        one();
      } else {
        zero();
      }
    }

    @Override
    public void endVisit(ByteValueCommand x, Context ctx) {
      push(String.valueOf(x.getValue()));
    }

    @Override
    public void endVisit(CharValueCommand x, Context ctx) {
      push(String.valueOf((int) x.getValue()));
    }

    @Override
    public void endVisit(DoubleValueCommand x, Context ctx) {
      push(String.valueOf(x.getValue()));
    }

    @Override
    public void endVisit(EnumValueCommand x, Context ctx) {
      String fieldName = clientOracle.getFieldId(x.getValue());
      if (fieldName == null) {
        throw new IncompatibleRemoteServiceException(
            "The client cannot accept " + x.getValue().name());
      }
      String clinitName = clientOracle.getMethodId(
          x.getValue().getDeclaringClass(), "$clinit");
      assert clinitName != null;

      // (clinit(), A)
      lparen();
      push(clinitName);
      lparen();
      rparen();
      comma();
      push(fieldName);
      rparen();
    }

    @Override
    public void endVisit(FloatValueCommand x, Context ctx) {
      push(String.valueOf((double) x.getValue()));
    }

    @Override
    public void endVisit(IntValueCommand x, Context ctx) {
      push(String.valueOf(x.getValue()));
    }

    @Override
    public void endVisit(LongValueCommand x, Context ctx) {
      // TODO (rice): use backwards-compatible wire format?
      long fieldValue = x.getValue();
      
      /*
       * Client code represents longs internally as an Object with numeric
       * properties l, m, and h. In order to make serialization of longs faster,
       * we'll send the component parts so that the value can be directly
       * reconstituted on the client.
       */
      int l = (int) (fieldValue & 0x3fffff);
      int m = (int) ((fieldValue >> 22) & 0x3fffff);
      int h = (int) ((fieldValue >> 44) & 0xfffff);
      // CHECKSTYLE_OFF
      push("{l:" + l + ",m:" + m + ",h:" + h + "}");
      // CHECKSTYLE_ON
    }

    @Override
    public void endVisit(NullValueCommand x, Context ctx) {
      _null();
    }

    @Override
    public void endVisit(ShortValueCommand x, Context ctx) {
      push(String.valueOf(x.getValue()));
    }

    @Override
    public void endVisit(StringValueCommand x, Context ctx) {
      if (hasBackRef(x)) {
        if (!isStarted(x)) {
          String escaped = EscapeUtil.escape(x.getValue());
          push(begin(x));
          eq();
          quote();
          push(escaped);
          quote();
          commit(x, false);
        } else {
          push(makeBackRef(x));
        }
      } else {
        String escaped = EscapeUtil.escape(x.getValue());
        quote();
        push(escaped);
        quote();
      }
    }

    @Override
    public boolean visit(ArrayValueCommand x, Context ctx) {
      boolean hasBackRef = hasBackRef(x);
      if (hasBackRef && isStarted(x)) {
        push(makeBackRef(x));
        return false;
      }

      // constructorFunction(x = [value,value,value])
      byte[] currentBackRef = begin(x);
      push(constructorFunction(x));
      lparen();
      if (hasBackRef) {
        push(currentBackRef);
        eq();
      }
      lbracket();
      for (Iterator<ValueCommand> it = x.getComponentValues().iterator(); it.hasNext();) {
        accept(it.next());
        if (it.hasNext()) {
          comma();
        }
      }
      rbracket();
      rparen();
      commit(x, false);
      if (!hasBackRef) {
        forget(x);
      }
      return false;
    }

    @Override
    public boolean visit(InstantiateCommand x, Context ctx) {
      boolean hasBackRef = hasBackRef(x);
      if (hasBackRef && isStarted(x)) {
        push(makeBackRef(x));
        return false;
      }

      byte[] currentBackRef = begin(x);
      byte[] constructorFunction = constructorFunction(x);

      String getSeedFunc = clientOracle.getMethodId("java.lang.Class",
          "getSeedFunction", "Ljava/lang/Class;");
      String classLitId = clientOracle.getFieldId(
               "com.google.gwt.lang.ClassLiteralHolder",
               getJavahSignatureName(x.getTargetClass()) + "_classLit");
           assert classLitId != null : "No class literal for "
               + x.getTargetClass().getName();

      /*
       * If we need to maintain a backreference to the object, it's established
       * in the first argument instead of using the return value of the
       * constructorFunction. This is done in case one of the fields should
       * require a reference to the object that is currently being constructed.
       */
      // constructorFunctionFoo(x = new (classLit.getSeedFunction()), field1, field2)
      push(constructorFunction);
      lparen();
      if (hasBackRef) {
        push(currentBackRef);
        eq();
      }
      _new();
      lparen();
      push(getSeedFunc);
      lparen();
      push(classLitId);
      rparen();
      rparen();
      for (SetCommand setter : x.getSetters()) {
        comma();
        accept(setter.getValue());
      }
      rparen();

      commit(x, false);
      if (!hasBackRef) {
        forget(x);
      }
      return false;
    }

    @Override
    public boolean visit(InvokeCustomFieldSerializerCommand x, Context ctx) {
      if (isStarted(x)) {
        push(makeBackRef(x));
        return false;
      }

      // ( backref = instantiate(), deserialize(), setter, ..., backref )
      byte[] currentBackRef = begin(x);

      lparen();

      InstantiateCommand makeReader = new InstantiateCommand(
          CommandClientSerializationStreamReader.class);
      /*
       * Ensure that the reader will stick around for both instantiate and
       * deserialize calls.
       */
      makeBackRef(makeReader);

      ArrayValueCommand payload = new ArrayValueCommand(Object.class);
      for (ValueCommand value : x.getValues()) {
        payload.add(value);
      }
      makeReader.set(CommandClientSerializationStreamReader.class, "payload",
          payload);

      String instantiateIdent = clientOracle.getMethodId(
          x.getSerializerClass(), "instantiate",
          SerializationStreamReader.class);

      // x = new Foo,
      // x = instantiate(reader),
      push(currentBackRef);
      eq();
      if (instantiateIdent == null) {
        // No instantiate method, we'll have to invoke the constructor

        // new Foo()
        String constructorMethodName;
        if (x.getTargetClass().getEnclosingClass() == null) {
          constructorMethodName = x.getTargetClass().getSimpleName();
        } else {
          String name = x.getTargetClass().getName();
          constructorMethodName = name.substring(name.lastIndexOf('.') + 1);
        }

        String constructorIdent = clientOracle.getMethodId(x.getTargetClass(),
            constructorMethodName);
        assert constructorIdent != null : "constructorIdent "
            + constructorMethodName;

        // new constructor,
        _new();
        push(constructorIdent);
        comma();
      } else {
        // instantiate(reader),
        push(instantiateIdent);
        lparen();
        accept(makeReader);
        rparen();
        comma();
      }

      // Call the deserialize method if it exists
      String deserializeIdent = clientOracle.getMethodId(
          x.getSerializerClass(), "deserialize",
          SerializationStreamReader.class, x.getManuallySerializedType());
      if (deserializeIdent != null) {
        // deserialize(reader, obj),
        push(deserializeIdent);
        lparen();
        accept(makeReader);
        comma();
        push(currentBackRef);
        rparen();
        comma();
      }

      // If there are extra fields, set them
      for (SetCommand setter : x.getSetters()) {
        accept(setter);
        comma();
      }

      push(currentBackRef);
      rparen();
      commit(x, false);
      forget(makeReader);

      return false;
    }

    @Override
    public boolean visit(ReturnCommand x, Context ctx) {
      int size = x.getValues().size();

      begin(x);
      _return();

      // return [a,b,c];
      lbracket();
      for (int i = 0; i < size; i++) {
        accept(x.getValues().get(i));
        if (i < size - 1) {
          comma();
        }
      }
      rbracket();

      semi();
      commit(x);

      return false;
    }

    @Override
    public boolean visit(SetCommand x, Context ctx) {
      String fieldName = clientOracle.getFieldId(x.getFieldDeclClass(),
          x.getField());

      if (fieldName == null) {
        // TODO: What does it mean if the client doesn't have a field?
        throw new IncompatibleRemoteServiceException(
            "The client does not have field " + x.getField() + " in type "
                + x.getFieldDeclClass().getName());
      }

      // i[3].foo = bar
      push(makeBackRef((ValueCommand) stack.peek()));
      dot();
      push(fieldName);
      eq();
      accept(x.getValue());

      return false;
    }

    /**
     * In order to improve robustness of the payload, we perform the throw from
     * within a function.
     */
    @Override
    public boolean visit(ThrowCommand x, Context ctx) {
      // throw foo;
      begin(x);
      _throw();

      assert x.getValues().size() == 1;
      accept(x.getValues());

      semi();
      commit(x);

      return false;
    }

    // CHECKSTYLE_OFF

    private void _new() {
      push(NEW_BYTES);
    }

    private void _null() {
      push(NULL_BYTES);
    }

    private void _return() {
      push(RETURN_BYTES);
    }

    private void _throw() {
      push(THROW_BYTES);
    }

    // CHECKSTYLE_ON

    private void begin(RpcCommand x) {
      assert !commandBuffers.containsKey(x) : "ValueCommand already active";

      started.add(x);
      stack.push(x);
      currentBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
      commandBuffers.put(x, currentBuffer);
    }

    private byte[] begin(ValueCommand x) {
      begin((RpcCommand) x);
      return makeBackRef(x);
    }

    private void comma() {
      push(COMMA_BYTES);
      spaceOpt();
    }

    private void commit(RpcCommand x) {
      commit(x, true);
    }

    private void commit(RpcCommand x, boolean send) {
      if (stack.pop() != x) {
        throw new IllegalStateException("Did not pop expected command");
      }

      // Don't need to retain any internal data
      x.clear();

      ByteBuffer sb = commandBuffers.remove(x);
      assert sb != null : "No ByteBuffer for " + x;

      if (!stack.isEmpty()) {
        currentBuffer = commandBuffers.get(stack.peek());
        assert currentBuffer != null : "Could not restore currentBuilder";
      } else {
        currentBuffer = null;
      }

      sb.limit(sb.position()).rewind();

      if (send) {
        try {
          send(sb);
        } catch (SerializationException e) {
          halt(e);
        }
      } else {
        push(sb);
      }
    }

    private byte[] constructorFunction(ArrayValueCommand x) {
      Class<?> targetClass = Array.newInstance(x.getComponentType(), 0).getClass();
      byte[] functionName = constructorFunctions.get(targetClass);
      if (functionName != null) {
        return functionName;
      }

      String initValuesId = clientOracle.getMethodId(
          "com.google.gwt.lang.Array", "initValues", "Ljava/lang/Class;",
          "Lcom/google/gwt/core/client/JavaScriptObject;", "I", 
          "Lcom/google/gwt/lang/Array;");
      assert initValuesId != null : "Could not find initValues";

      String classLitId = clientOracle.getFieldId(
          "com.google.gwt.lang.ClassLiteralHolder",
          getJavahSignatureName(x.getComponentType()) + "_classLit");
      assert classLitId != null : "No class literal for "
          + x.getComponentType().getName();

      functionName = getBytes(clientOracle.createUnusedIdent(classLitId));
      constructorFunctions.put(targetClass, functionName);

      /*
       * Set the castableTypeData and queryIds to exact values, 
       * or fall back to acting like a plain Object[] array.
       */
      CastableTypeData castableTypeData = clientOracle.getCastableTypeData(targetClass);
      if (castableTypeData == null) {
        castableTypeData = clientOracle.getCastableTypeData(Object[].class);
      }

      int queryId = clientOracle.getQueryId(x.getComponentType());
      if (queryId == 0) {
        queryId = clientOracle.getQueryId(Object.class);
      }

      byte[] ident = getBytes("_0");

      // function foo(_0) {return initValues(classLit, castableTypeData, queryId, _0)}
      function();
      push(functionName);
      lparen();
      push(ident);
      rparen();
      lbrace();
      _return();
      push(initValuesId);
      lparen();
      push(classLitId);
      comma();
      push(castableTypeData.toJs());
      comma();
      push(String.valueOf(queryId));
      comma();
      push(ident);
      rparen();
      rbrace();

      flush(x);

      return functionName;
    }

    private byte[] constructorFunction(InstantiateCommand x) {
      Class<?> targetClass = x.getTargetClass();
      byte[] functionName = constructorFunctions.get(targetClass);
      if (functionName != null) {
        return functionName;
      }

      String seedName = clientOracle.getSeedName(targetClass);
      assert seedName != null : "TypeOverride failed to rescue "
          + targetClass.getName();
      functionName = getBytes(clientOracle.createUnusedIdent(seedName));
      constructorFunctions.put(targetClass, functionName);
      byte[][] idents = new byte[x.getSetters().size() + 1][];
      for (int i = 0, j = idents.length; i < j; i++) {
        idents[i] = getBytes("_" + i);
      }

      // function foo(_0, _1, _2) {_0.a = _1; _0.b=_2; return _0}
      function();
      push(functionName);
      lparen();
      for (int i = 0, j = idents.length; i < j; i++) {
        push(idents[i]);
        if (i < j - 1) {
          comma();
        }
      }
      rparen();
      lbrace();
      newlineOpt();
      for (int i = 1, j = idents.length; i < j; i++) {
        SetCommand setter = x.getSetters().get(i - 1);
        String fieldIdent = clientOracle.getFieldId(setter.getFieldDeclClass(),
            setter.getField());

        // _0.foo = bar;
        spaceOpt();
        push(idents[0]);
        dot();
        push(fieldIdent);
        eq();
        push(idents[i]);
        semi();
      }
      spaceOpt();
      _return();
      push(idents[0]);
      rbrace();
      newlineOpt();

      flush(x);

      return functionName;
    }

    private void dot() {
      push(DOT_BYTES);
    }

    private void eq() {
      spaceOpt();
      push(EQ_BYTES);
      spaceOpt();
    }

    /**
     * Cause an immediate write of accumulated output for a command. This is
     * used primarily for writing object allocations
     */
    private void flush(RpcCommand x) {
      ByteBuffer sb = commandBuffers.get(x);
      if (sb == null || sb.position() == 0) {
        return;
      }

      sb.limit(sb.position()).rewind();
      try {
        send(sb);
      } catch (SerializationException e) {
        halt(e);
      }
      sb.clear();
    }

    private void function() {
      newlineOpt();
      push(FUNCTION_BYTES);
    }

    /**
     * Keep in sync with JReferenceType implementations.
     */
    private String getJavahSignatureName(Class<?> clazz) {
      if (clazz.isArray()) {
        Class<?> leafType = clazz;
        int dims = 0;
        do {
          dims++;
          leafType = leafType.getComponentType();
        } while (leafType.getComponentType() != null);
        assert dims > 0;
        // leafType cannot be null here

        String s = getJavahSignatureName(leafType);
        for (int i = 0; i < dims; ++i) {
          s = "_3" + s;
        }
        return s;
      } else if (clazz.isPrimitive()) {
        return WebModeClientOracle.jsniName(clazz);
      } else {
        String name = clazz.getName();
        return "L" + name.replaceAll("_", "_1").replace('.', '_') + "_2";
      }
    }

    private boolean isStarted(RpcCommand x) {
      return started.contains(x);
    }

    private void lbrace() {
      push(LBRACE_BYTES);
    }

    private void lbracket() {
      push(LBRACKET_BYTES);
    }

    private void lparen() {
      push(LPAREN_BYTES);
    }

    private void newlineOpt() {
      pushOpt(NEWLINE_BYTES);
    }

    private void one() {
      push(ONE_BYTES);
    }

    /**
     * Add data to the current command's serialization output.
     */
    private void push(byte[] bytes) {
      assert currentBuffer != null : "Must call begin(RpcCommand) first";
      try {
        currentBuffer.put(bytes);
      } catch (BufferOverflowException e) {
        reallocateCurrentBuffer(bytes.length);
        currentBuffer.put(bytes);
      }
    }

    /**
     * Add data to the current command's serialization output.
     */
    private void push(ByteBuffer buffer) {
      assert currentBuffer != null : "Must call begin(RpcCommand) first";
      try {
        currentBuffer.put(buffer);
      } catch (BufferOverflowException e) {
        reallocateCurrentBuffer(buffer.remaining());
        currentBuffer.put(buffer);
      }
    }

    /**
     * Add data to the current command's serialization output.
     */
    private void push(String s) {
      push(getBytes(s));
    }

    /**
     * Optionally add data to the current command's serialization output.
     */
    private void pushOpt(byte[] x) {
      if (PRETTY) {
        push(x);
      }
    }

    private void quote() {
      push(QUOTE_BYTES);
    }

    private void rbrace() {
      push(RBRACE_BYTES);
    }

    private void rbracket() {
      push(RBRACKET_BYTES);
    }

    private void reallocateCurrentBuffer(int bytesNeeded) {
      // Allocate a new buffer of sufficient size
      int newSize = currentBuffer.capacity()
          + Math.max(2 * bytesNeeded, currentBuffer.capacity());
      ByteBuffer newBuffer = ByteBuffer.allocate(newSize);

      // Copy the old buffer over
      currentBuffer.limit(currentBuffer.position()).rewind();
      newBuffer.put(currentBuffer);

      // Reassign the current buffer
      assert commandBuffers.get(stack.peek()) == currentBuffer;
      commandBuffers.put(stack.peek(), newBuffer);
      currentBuffer = newBuffer;
    }

    private void rparen() {
      push(RPAREN_BYTES);
    }

    private void semi() {
      push(SEMI_BYTES);
      newlineOpt();
    }

    private void spaceOpt() {
      pushOpt(SPACE_BYTES);
    }

    private void zero() {
      push(ZERO_BYTES);
    }
  }

  /*
   * Instead of converting these commonly-used strings to bytes every time we
   * want to write them to the output, we'll simply create a fixed pool.
   */
  static final byte[] COMMA_BYTES = getBytes(",");
  static final byte[] DOT_BYTES = getBytes(".");
  static final byte[] EQ_BYTES = getBytes("=");
  static final byte[] FUNCTION_BYTES = getBytes("function ");
  static final byte[] LBRACE_BYTES = getBytes("{");
  static final byte[] LBRACKET_BYTES = getBytes("[");
  static final byte[] LPAREN_BYTES = getBytes("(");
  static final byte[] NEW_BYTES = getBytes("new ");
  static final byte[] NEWLINE_BYTES = getBytes("\n");
  static final byte[] NULL_BYTES = getBytes("null");
  static final byte[] ONE_BYTES = getBytes("1");
  static final byte[] QUOTE_BYTES = getBytes("\"");
  static final byte[] RBRACE_BYTES = getBytes("}");
  static final byte[] RBRACKET_BYTES = getBytes("]");
  static final byte[] RETURN_BYTES = getBytes("return ");
  static final byte[] RPAREN_BYTES = getBytes(")");
  static final byte[] SPACE_BYTES = getBytes(" ");
  static final byte[] SEMI_BYTES = getBytes(";");
  static final byte[] THROW_BYTES = getBytes("throw ");
  static final byte[] ZERO_BYTES = getBytes("0");

  /**
   * A runtime flag to indicate that the generated output should be made to be
   * human-readable.
   */
  static final boolean PRETTY = Boolean.getBoolean("gwt.rpc.pretty");

  private static final int DEFAULT_BUFFER_SIZE = 256;

  static byte[] getBytes(String x) {
    try {
      return x.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 is unsupported", e);
    }
  }

  private final ClientOracle clientOracle;
  private boolean finished = false;
  private final OutputStream out;
  private final Map<ValueCommand, byte[]> valueBackRefs = new HashMap<ValueCommand, byte[]>();
  private final PayloadVisitor visitor = new PayloadVisitor();

  private Stack<byte[]> freeBackRefs = new Stack<byte[]>();

  public WebModePayloadSink(ClientOracle clientOracle, OutputStream out) {
    this.clientOracle = clientOracle;
    this.out = out;
  }

  @Override
  public void accept(RpcCommand command) throws SerializationException {
    if (finished) {
      throw new IllegalStateException("finish() has already been called");
    }

    new BackRefAssigner().accept(command);

    if (command instanceof ValueCommand) {
      makeBackRef((ValueCommand) command);
    }
    visitor.accept(command);
  }

  /**
   * The caller must close the stream.
   */
  @Override
  public void finish() throws SerializationException {
    finished = true;
  }

  void forget(ValueCommand x) {
    assert valueBackRefs.containsKey(x);
    freeBackRefs.push(valueBackRefs.remove(x));
  }

  boolean hasBackRef(ValueCommand x) {
    return valueBackRefs.containsKey(x);
  }

  byte[] makeBackRef(ValueCommand x) {
    byte[] toReturn = valueBackRefs.get(x);
    if (toReturn == null) {
      if (freeBackRefs.isEmpty()) {
        int idx = valueBackRefs.size();
        toReturn = getBytes(BACKREF_IDENT + "._"
            + Integer.toString(idx, Character.MAX_RADIX));
      } else {
        toReturn = freeBackRefs.pop();
      }
      valueBackRefs.put(x, toReturn);
    }
    return toReturn;
  }

  void send(ByteBuffer x) throws SerializationException {
    try {
      assert x.hasArray();
      out.write(x.array(), x.position(), x.limit());
    } catch (IOException e) {
      throw new SerializationException("Could not send data", e);
    }
  }

  void send(String x) throws SerializationException {
    try {
      out.write(getBytes(x));
    } catch (IOException e) {
      throw new SerializationException("Could not send data", e);
    }
  }
}
