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
package java.util.function;

import static javaemul.internal.InternalPreconditions.checkCriticalNotNull;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/DoubleUnaryOperator.html">
 * the official Java API doc</a> for details.
 */
@FunctionalInterface
public interface DoubleUnaryOperator {

  static DoubleUnaryOperator identity() {
    return operand -> operand;
  }

  double applyAsDouble(double operand);

  default DoubleUnaryOperator andThen(DoubleUnaryOperator after) {
    checkCriticalNotNull(after);
    return operand -> after.applyAsDouble(applyAsDouble(operand));
  }

  default DoubleUnaryOperator compose(DoubleUnaryOperator before) {
    checkCriticalNotNull(before);
    return operand -> applyAsDouble(before.applyAsDouble(operand));
  }
}
