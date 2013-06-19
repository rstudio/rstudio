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
package com.google.gwt.core.shared;

import com.google.gwt.core.shared.impl.ThrowableTypeResolver;


/**
 * A serializable copy of a {@link Throwable}, including its causes and stack trace. It overrides
 * {@code #toString} to mimic original {@link Throwable#toString()} so that {@link #printStackTrace}
 * will work as if it is coming from the original exception.
 *
 * <p>
 * This class is especially useful for logging and testing as the emulated Throwable class does not
 * serialize recursively and does not serialize the stack trace. This class, as an alternative, can
 * be used to transfer the Throwable without losing any of these data, even if the underlying
 * Throwable is not serializable.
 * <p>
 * Please note that, to get more useful stack traces from client side, this class needs to be used
 * in conjunction with {@link com.google.gwt.core.server.StackTraceDeobfuscator}.
 * <p>
 * NOTE: Does not serialize suppressed exceptions to remain compatible with Java 6 and below.
 */
public final class SerializableThrowable extends Throwable {

  /**
   * Create a new {@link SerializableThrowable} from a provided throwable and its causes
   * recursively.
   *
   * @return a new SerializableThrowable or the passed object itself if it is {@code null} or
   *         already a SerializableThrowable.
   */
  public static SerializableThrowable fromThrowable(Throwable throwable) {
    if (throwable instanceof SerializableThrowable) {
      return (SerializableThrowable) throwable;
    } else if (throwable != null) {
      return createSerializable(throwable);
    } else {
      return null;
    }
  }

  private String typeName;
  private boolean exactTypeKnown;
  private transient Throwable originalThrowable;
  private StackTraceElement[] dummyFieldToIncludeTheTypeInSerialization;

  /**
   * Constructs a new SerializableThrowable with the specified detail message.
   */
  public SerializableThrowable(String designatedType, String message) {
    super(message);
    this.typeName = designatedType;
  }

  @Override
  public Throwable fillInStackTrace() {
    // This is a no-op for optimization as we don't need stack traces to be auto-filled.
    // TODO(goktug): Java 7 let's you disable this by constructor flag
    return this;
  }

  /**
   * Sets the designated Throwable's type name.
   *
   * @param typeName the class name of the underlying designated throwable.
   * @param isExactType {@code false} if provided type name is not the exact type.
   *
   * @see #isExactDesignatedTypeKnown()
   */
  public void setDesignatedType(String typeName, boolean isExactType) {
    this.typeName = typeName;
    this.exactTypeKnown = isExactType;
  }

  /**
   * Returns the designated throwable's type name.
   *
   * @see #isExactDesignatedTypeKnown()
   */
  public String getDesignatedType() {
    return typeName;
  }

  /**
   * Return {@code true} if provided type name is the exact type of the throwable that is designated
   * by this instance. This can return {@code false} if the class metadata is not available in the
   * runtime. In that case {@link #getDesignatedType()} will return the type resolved by best-effort
   * and may not be the exact type; instead it can be one of the ancestors of the real type that
   * this instance designates.
   */
  public boolean isExactDesignatedTypeKnown() {
    return exactTypeKnown;
  }

  /**
   * Initializes the cause of this throwable.
   * <p>
   * This method will convert the cause to {@link SerializableThrowable} if it is not already.
   */
  @Override
  public Throwable initCause(Throwable cause) {
    return super.initCause(fromThrowable(cause));
  }

  /**
   * Returns the original throwable that this serializable throwable is derived from. Note that the
   * original throwable is kept in a transient field; that is; it will not be transferred to server
   * side. In that case this method will return {@code null}.
   */
  public Throwable getOriginalThrowable() {
    return originalThrowable;
  }

  @Override
  public String toString() {
    String type = exactTypeKnown ? typeName : (typeName + "(EXACT TYPE UNKNOWN)");
    String msg = getMessage();
    return msg == null ? type : (type + ": " + msg);
  }

  private static SerializableThrowable createSerializable(Throwable t) {
    SerializableThrowable throwable = new SerializableThrowable(null, t.getMessage());
    throwable.setStackTrace(t.getStackTrace());
    throwable.initCause(t.getCause());
    throwable.originalThrowable = t;
    ThrowableTypeResolver.resolveDesignatedType(throwable, t);
    return throwable;
  }
}
