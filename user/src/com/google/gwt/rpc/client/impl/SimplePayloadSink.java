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
package com.google.gwt.rpc.client.impl;

import com.google.gwt.core.client.GWT;
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
import com.google.gwt.user.client.rpc.SerializationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This implementation of CommandSink encodes RpcCommands in a simple transport
 * format that can be interpreted by both the client and the server.
 */
public class SimplePayloadSink extends CommandSink {

  private class Visitor extends RpcCommandVisitor {

    @Override
    public void endVisit(BooleanValueCommand x, Context ctx) {
      appendTypedData(BOOLEAN_TYPE, x.getValue() ? "1" : "0");
    }

    @Override
    public void endVisit(ByteValueCommand x, Context ctx) {
      appendTypedData(BYTE_TYPE, x.getValue().toString());
    }

    @Override
    public void endVisit(CharValueCommand x, Context ctx) {
      appendTypedData(CHAR_TYPE, String.valueOf((int) x.getValue()));
    }

    @Override
    public void endVisit(DoubleValueCommand x, Context ctx) {
      appendTypedData(DOUBLE_TYPE, x.getValue().toString());
    }

    @Override
    public void endVisit(EnumValueCommand x, Context ctx) {
      // ETypeSeedName~IOrdinal~
      if (appendIdentity(x)) {
        appendTypedData(ENUM_TYPE, x.getValue().getDeclaringClass().getName());
        // use ordinal (and not name), since name might have been obfuscated
        appendTypedData(INT_TYPE, String.valueOf(x.getValue().ordinal()));
      }
    }

    @Override
    public void endVisit(FloatValueCommand x, Context ctx) {
      appendTypedData(FLOAT_TYPE, x.getValue().toString());
    }

    @Override
    public void endVisit(IntValueCommand x, Context ctx) {
      appendTypedData(INT_TYPE, x.getValue().toString());
    }

    @Override
    public void endVisit(LongValueCommand x, Context ctx) {
      appendTypedData(LONG_TYPE, x.getValue().toString());
    }

    @Override
    public void endVisit(NullValueCommand x, Context ctx) {
      appendTypedData(VOID_TYPE, "");
    }

    @Override
    public void endVisit(ShortValueCommand x, Context ctx) {
      appendTypedData(SHORT_TYPE, x.getValue().toString());
    }

    @Override
    public void endVisit(StringValueCommand x, Context ctx) {
      // "4~abcd
      if (appendIdentity(x)) {
        String value = x.getValue();
        /*
         * Emit this a a Pascal-style string, using an explicit length. This
         * avoids the need to escape the value.
         */
        appendTypedData(STRING_TYPE, String.valueOf(value.length()));
        append(value);
      }
    }

    @Override
    public boolean visit(ArrayValueCommand x, Context ctx) {
      /*
       * Encoded as (leafType, dimensions, length, .... )
       * 
       * Object[] foo = new Object[3];
       * 
       * becomes
       * 
       * [ObjectSeedname~1~3~@....~@....~@...~
       * 
       * Object[][] foo = new Object[3][];
       * 
       * becomes
       * 
       * [ObjectSeedName~2~3~...three one-dim arrays...
       */
      if (appendIdentity(x)) {
        int dims = 1;
        Class<?> leaf = x.getComponentType();
        while (leaf.getComponentType() != null) {
          dims++;
          leaf = leaf.getComponentType();
        }

        appendTypedData(ARRAY_TYPE, leaf.getName());
        accept(new IntValueCommand(dims));
        accept(new IntValueCommand(x.getComponentValues().size()));
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean visit(InstantiateCommand x, Context ctx) {
      // @TypeSeedName~3~... N-many setters ...
      if (appendIdentity(x)) {
        appendTypedData(OBJECT_TYPE, x.getTargetClass().getName());
        accept(new IntValueCommand(x.getSetters().size()));
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean visit(InvokeCustomFieldSerializerCommand x, Context ctx) {
      // !TypeSeedName~Number of objects written by CFS~...CFS objects...~
      // Number of extra fields~...N-many setters...
      if (appendIdentity(x)) {
        appendTypedData(INVOKE_TYPE, x.getTargetClass().getName());
        accept(new IntValueCommand(x.getValues().size()));
        accept(x.getValues());
        accept(new IntValueCommand(x.getSetters().size()));
        accept(x.getSetters());
        return false;
      } else {
        return false;
      }
    }

    @Override
    public boolean visit(ReturnCommand x, Context ctx) {
      // R4~...values...
      appendTypedData(RETURN_TYPE, String.valueOf(x.getValues().size()));
      return true;
    }

    @Override
    public boolean visit(SetCommand x, Context ctx) {
      /*
       * In Development Mode, the field's declaring class is written to the
       * stream to handle field shadowing. In Production Mode, this isn't
       * necessary because all field names are allocated in the same "object"
       * scope.
       *
       * DeclaringClassName~FieldName~...value...
       */
      if (!GWT.isScript()) {
        accept(new StringValueCommand(x.getFieldDeclClass().getName()));
      }
      accept(new StringValueCommand(x.getField()));
      return true;
    }

    @Override
    public boolean visit(ThrowCommand x, Context ctx) {
      // T...value...
      appendTypedData(THROW_TYPE, "");
      return true;
    }

    private void append(String x) {
      try {
        buffer.append(EscapeUtil.escape(x)).append(RPC_SEPARATOR_CHAR);
      } catch (IOException e) {
        halt(e);
      }
    }

    private boolean appendIdentity(ValueCommand x) {
      Integer backRef = backRefs.get(x);
      if (backRef != null) {
        if (PRETTY) {
          try {
            buffer.append(NL_CHAR);
          } catch (IOException e) {
            halt(e);
          }
        }
        append(BACKREF_TYPE + String.valueOf(backRef));
        return false;
      } else {
        backRefs.put(x, backRefs.size());
        return true;
      }
    }

    private void appendTypedData(char type, String value) {
      try {
        if (PRETTY) {
          buffer.append(NL_CHAR);
        }
        buffer.append(type).append(value).append(RPC_SEPARATOR_CHAR);
      } catch (IOException e) {
        halt(e);
      }
    }
  }

  /**
   * Used for diagnostics.
   */
  static final boolean PRETTY = false;

  public static final char ARRAY_TYPE = '[';
  public static final char BACKREF_TYPE = '@';
  public static final char BOOLEAN_TYPE = 'Z';
  public static final char BYTE_TYPE = 'B';
  public static final char CHAR_TYPE = 'C';
  public static final char DOUBLE_TYPE = 'D';
  public static final char ENUM_TYPE = 'E';
  public static final char FLOAT_TYPE = 'F';
  public static final char INT_TYPE = 'I';
  public static final char INVOKE_TYPE = '!';
  public static final char LONG_TYPE = 'J';
  public static final char NL_CHAR = '\n';
  public static final char OBJECT_TYPE = 'L';
  public static final char RETURN_TYPE = 'R';
  public static final char RPC_SEPARATOR_CHAR = '~';
  public static final char SHORT_TYPE = 'S';
  public static final char STRING_TYPE = '"';
  public static final char THROW_TYPE = 'T';
  public static final char VOID_TYPE = 'V';

  private final Map<ValueCommand, Integer> backRefs = new HashMap<ValueCommand, Integer>();

  private final Appendable buffer;

  public SimplePayloadSink(Appendable buffer) {
    this.buffer = buffer;
  }

  @Override
  public void accept(RpcCommand command) throws SerializationException {
    (new Visitor()).accept(command);
  }

  @Override
  public void finish() throws SerializationException {
  }
}
