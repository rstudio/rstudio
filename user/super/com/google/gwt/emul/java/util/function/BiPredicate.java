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
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/BiPredicate.html">
 * the official Java API doc</a> for details.
 *
 * @param <T> type of the first argument
 * @param <U> type of the second argument
 */
@FunctionalInterface
public interface BiPredicate<T, U> {

  boolean test(T t, U u);

  default BiPredicate<T, U> negate() {
    return (t, u) -> !test(t, u);
  }

  default BiPredicate<T, U> and(BiPredicate<? super T, ? super U> other) {
    checkCriticalNotNull(other);
    return (t, u) -> test(t, u) && other.test(t, u);
  }

  default BiPredicate<T, U> or(BiPredicate<? super T, ? super U> other) {
    checkCriticalNotNull(other);
    return (t, u) -> test(t, u) || other.test(t, u);
  }
}
