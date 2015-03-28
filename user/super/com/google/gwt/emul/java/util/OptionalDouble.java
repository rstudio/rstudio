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

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import static javaemul.internal.InternalPreconditions.checkCriticalElement;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/OptionalDouble.html">
 * the official Java API doc</a> for details.
 */
public final class OptionalDouble {

  public static OptionalDouble empty() {
    return EMPTY;
  }

  public static OptionalDouble of(double value) {
    return new OptionalDouble(value);
  }

  private static final OptionalDouble EMPTY = new OptionalDouble();

  private final double ref;
  private final boolean present;

  private OptionalDouble() {
    ref = 0;
    present = false;
  }

  private OptionalDouble(double value) {
    ref = value;
    present = true;
  }

  public boolean isPresent() {
    return present;
  }

  public double getAsDouble() {
    checkCriticalElement(present);
    return ref;
  }

  public void ifPresent(DoubleConsumer consumer) {
    if (present) {
      consumer.accept(ref);
    }
  }

  public double orElse(double other) {
    return present ? ref : other;
  }

  public double orElseGet(DoubleSupplier other) {
    return present ? ref : other.getAsDouble();
  }

  public <X extends Throwable> double orElseThrow(Supplier<X> exceptionSupplier) throws X {
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
    if (!(obj instanceof OptionalDouble)) {
      return false;
    }
    OptionalDouble other = (OptionalDouble) obj;
    return present == other.present && Double.compare(ref, other.ref) == 0;
  }

  @Override
  public int hashCode() {
    return present ? Double.hashCode(ref) : 0;
  }

  @Override
  public String toString() {
    return present ? "OptionalDouble.of(" + Double.toString(ref) + ")" : "OptionalDouble.empty()";
  }
}
