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
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/Function.html">
 * the official Java API doc</a> for details.
 *
 * @param <T> type of the argument
 * @param <R> type of the return value
 */
@FunctionalInterface
public interface Function<T, R> {

  static <T> Function<T, T> identity() {
    return t -> t;
  }

  R apply(T t);

  default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
    checkCriticalNotNull(after);
    return t -> after.apply(apply(t));
  }

  default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
    checkCriticalNotNull(before);
    return t -> apply(before.apply(t));
  }
}
