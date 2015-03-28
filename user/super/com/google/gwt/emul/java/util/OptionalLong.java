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

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static javaemul.internal.InternalPreconditions.checkCriticalElement;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/OptionalLong.html">
 * the official Java API doc</a> for details.
 */
public final class OptionalLong {

  public static OptionalLong empty() {
    return EMPTY;
  }

  public static OptionalLong of(long value) {
    return new OptionalLong(value);
  }

  private static final OptionalLong EMPTY = new OptionalLong();

  private final long ref;
  private final boolean present;

  private OptionalLong() {
    ref = 0;
    present = false;
  }

  private OptionalLong(long value) {
    ref = value;
    present = true;
  }

  public boolean isPresent() {
    return present;
  }

  public long getAsLong() {
    checkCriticalElement(present);
    return ref;
  }

  public void ifPresent(LongConsumer consumer) {
    if (present) {
      consumer.accept(ref);
    }
  }

  public long orElse(long other) {
    return present ? ref : other;
  }

  public long orElseGet(LongSupplier other) {
    return present ? ref : other.getAsLong();
  }

  public <X extends Throwable> long orElseThrow(Supplier<X> exceptionSupplier) throws X {
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
    if (!(obj instanceof OptionalLong)) {
      return false;
    }
    OptionalLong other = (OptionalLong) obj;
    return present == other.present && Long.compare(ref, other.ref) == 0;
  }

  @Override
  public int hashCode() {
    return present ? Long.hashCode(ref) : 0;
  }

  @Override
  public String toString() {
    return present ? "OptionalLong.of(" + Long.toString(ref) + ")" : "OptionalLong.empty()";
  }
}
