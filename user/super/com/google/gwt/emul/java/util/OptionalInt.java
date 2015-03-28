/*
 * Copyright 2015 Google Inc.
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
package java.util;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static javaemul.internal.InternalPreconditions.checkCriticalElement;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/OptionalInt.html">
 * the official Java API doc</a> for details.
 */
public final class OptionalInt {

  public static OptionalInt empty() {
    return EMPTY;
  }

  public static OptionalInt of(int value) {
    return new OptionalInt(value);
  }

  private static final OptionalInt EMPTY = new OptionalInt();

  private final int ref;
  private final boolean present;

  private OptionalInt() {
    ref = 0;
    present = false;
  }

  private OptionalInt(int value) {
    ref = value;
    present = true;
  }

  public boolean isPresent() {
    return present;
  }

  public int getAsInt() {
    checkCriticalElement(present);
    return ref;
  }

  public void ifPresent(IntConsumer consumer) {
    if (present) {
      consumer.accept(ref);
    }
  }

  public int orElse(int other) {
    return present ? ref : other;
  }

  public int orElseGet(IntSupplier other) {
    return present ? ref : other.getAsInt();
  }

  public <X extends Throwable> int orElseThrow(Supplier<X> exceptionSupplier) throws X {
    if (present) {
      return ref;
    }
    throw exceptionSupplier.get();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof OptionalInt)) {
      return false;
    }
    OptionalInt other = (OptionalInt) obj;
    return present == other.present && Integer.compare(ref, other.ref) == 0;
  }

  @Override
  public int hashCode() {
    return present ? Integer.hashCode(ref) : 0;
  }

  @Override
  public String toString() {
    return present ? "OptionalInt.of(" + Integer.toString(ref) + ")" : "OptionalInt.empty()";
  }
}
