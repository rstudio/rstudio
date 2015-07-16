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
package java.util;

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkState;

/**
 * Skeletal implementation of the Queue interface. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/AbstractQueue.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {

  // Should not be instantiated directly.
  protected AbstractQueue() {
  }

  @Override
  public boolean add(E o) {
    checkState(offer(o), "Unable to add element to queue");
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    checkNotNull(c);
    checkArgument(c != this, "Can't add a queue to itself");

    return super.addAll(c);
  }

  @Override
  public void clear() {
    while (poll() != null) {
      // empty loop
    }
  }

  @Override
  public E element() {
    E e = peek();
    checkElement(e != null, "Queue is empty");
    return e;
  }

  @Override
  public abstract boolean offer(E o);

  @Override
  public abstract E peek();

  @Override
  public abstract E poll();

  @Override
  public E remove() {
    E e = poll();
    checkElement(e != null, "Queue is empty");
    return e;
  }

}
