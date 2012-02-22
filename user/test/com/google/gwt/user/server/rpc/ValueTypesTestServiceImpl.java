/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.SerializableGenericWrapperType;
import com.google.gwt.user.client.rpc.ValueTypesTestService;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Server-side code for ValueTypesTest. Mostly just echoes back the values
 * received from the client, but some methods verify the value received to make
 * sure it isn't mangled on the way to the server and then unmangled on the way
 * back to the client.
 */
public class ValueTypesTestServiceImpl extends HybridServiceServlet implements
    ValueTypesTestService {

  public byte echo(byte value) {
    return value;
  }

  public char echo(char value) {
    return value;
  }

  public double echo(double value) {
    return value;
  }

  public float echo(float value) {
    return value;
  }

  public int echo(int value) {
    return value;
  }

  public long echo(long value) {
    return value;
  }

  public short echo(short value) {
    return value;
  }

  public BigDecimal echo(BigDecimal value) {
    return value;
  }

  public BigInteger echo(BigInteger value) {
    return value;
  }

  public SerializableGenericWrapperType<Void> echo(
      SerializableGenericWrapperType<Void> value) {
    return value;
  }

  public String echo(String value) {
    return value;
  }

  public boolean echo_FALSE(boolean value) {
    if (value != false) {
      throw new RuntimeException();
    }

    return value;
  }

  public byte echo_MAX_VALUE(byte value) {
    if (value != Byte.MAX_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public char echo_MAX_VALUE(char value) {
    if (value != Character.MAX_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public double echo_MAX_VALUE(double value) {
    if (value != Double.MAX_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public float echo_MAX_VALUE(float value) {
    if (value != Float.MAX_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public int echo_MAX_VALUE(int value) {
    if (value != Integer.MAX_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public long echo_MAX_VALUE(long value) {
    if (value != Long.MAX_VALUE) {
      throw new RuntimeException("expected: " + Long.toString(Long.MAX_VALUE)
          + " actual: " + Long.toString(value));
    }

    return value;
  }

  public short echo_MAX_VALUE(short value) {
    if (value != Short.MAX_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public byte echo_MIN_VALUE(byte value) {
    if (value != Byte.MIN_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public char echo_MIN_VALUE(char value) {
    if (value != Character.MIN_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public double echo_MIN_VALUE(double value) {
    if (value != Double.MIN_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public float echo_MIN_VALUE(float value) {
    if (value != Float.MIN_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public int echo_MIN_VALUE(int value) {
    if (value != Integer.MIN_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public long echo_MIN_VALUE(long value) {
    if (value != Long.MIN_VALUE) {
      throw new RuntimeException("expected: " + Long.toString(Long.MIN_VALUE)
          + " actual: " + Long.toString(value));
    }

    return value;
  }

  public short echo_MIN_VALUE(short value) {
    if (value != Short.MIN_VALUE) {
      throw new RuntimeException();
    }

    return value;
  }

  public boolean echo_TRUE(boolean value) {
    if (value != true) {
      throw new RuntimeException();
    }

    return value;
  }
}
