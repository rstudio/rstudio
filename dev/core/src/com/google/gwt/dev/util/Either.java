/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.util;

/**
 * Functional-language like either type. Holds either left or right value.
 * Values must be non-null.
 * 
 * @param <L> left type.
 * @param <R> right type.
 */
public class Either<L, R> {
  public static <L, R> Either<L, R> left(L l) {
    if (l == null) {
      throw new IllegalArgumentException();
    }
    return new Either<L, R>(l, null);
  }

  public static <L, R> Either<L, R> right(R r) {
    if (r == null) {
      throw new IllegalArgumentException();
    }
    return new Either<L, R>(null, r);
  }

  private final L left;

  private final R right;

  private Either(L a, R b) {
    left = a;
    right = b;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Either other = (Either) obj;
    if (left != null && other.left != null) {
      return left.equals(other.left);
    } else if (right != null && other.right != null) {
      return right.equals(other.right);
    }
    return false;
  }

  public L getLeft() {
    Preconditions.checkNotNull(left);
    return left;
  }

  public R getRight() {
    Preconditions.checkNotNull(right);
    return right;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((left == null) ? 0 : left.hashCode());
    result = prime * result + ((right == null) ? 0 : right.hashCode());
    return result;
  }

  public boolean isLeft() {
    return left != null;
  }

  public boolean isRight() {
    return right != null;
  }

  @Override
  public String toString() {
    if (isLeft()) {
      return "L{" + left.toString() + "}";
    }
    return "R{" + right.toString() + "}";
  }
}
