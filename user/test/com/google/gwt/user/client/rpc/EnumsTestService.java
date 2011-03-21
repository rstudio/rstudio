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
package com.google.gwt.user.client.rpc;

import java.io.Serializable;

/**
 * RemoteService used to test the use of enums over RPC.
 */
public interface EnumsTestService extends RemoteService {
  /**
   * Exception thrown when the enumeration state from the client makes it to the
   * server.
   */
  public class EnumStateModificationException extends SerializableException {
    public EnumStateModificationException() {
    }
    
    public EnumStateModificationException(String msg) {
      super(msg);
    }
  }
  
  /**
   * Simplest enum possible; no subtypes or enum constant specific state.
   */
  public enum Basic {
    A, B, C
  }

  /**
   * Enum that has no default constructor and includes state.
   */
  public enum Complex {
    A("X"), B("Y"), C("Z");

    public String value;

    Complex(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  /**
   * Enum that has local subtypes.
   */
  public enum Subclassing {
    A {
      @Override
      public String value() {
        return "X";
      }
    },
    B {
      @Override
      public String value() {
        return "Y";
      }
    },
    C {
      @Override
      public String value() {
        return "Z";
      }
    };

    public abstract String value();
  }
  
  /**
   * Enum to be used as a field in a wrapper class
   */
  public enum FieldEnum {
    X, Y, Z
  }
  
  /**
   * Wrapper class containing an enum field.
   */
  public class FieldEnumWrapper implements Serializable {
    private FieldEnum fieldEnum = FieldEnum.Z;
    
    public FieldEnum getFieldEnum() {
      return this.fieldEnum;
    }
    
    public void setFieldEnum(FieldEnum fieldEnum) {
      this.fieldEnum = fieldEnum;
    }
  }
  
  Basic echo(Basic value);
  Complex echo(Complex value) throws EnumStateModificationException;
  Subclassing echo(Subclassing value);
  FieldEnumWrapper echo(FieldEnumWrapper value);
}
