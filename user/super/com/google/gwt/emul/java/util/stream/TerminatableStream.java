/*
 * Copyright 2016 Google Inc.
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

package java.util.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

// package protected, as not part of jre
class TerminatableStream<T extends TerminatableStream<T>> {
  // root-only fields, ignored for non-root instances
  private boolean terminated = false;
  private final List<Runnable> onClose;

  private final TerminatableStream<?> root;

  public TerminatableStream(TerminatableStream<?> previous) {
    if (previous == null) {
      root = null;
      onClose = new ArrayList<>();
    } else {
      root = previous;
      onClose = null;
    }
  }

  void throwIfTerminated() {
    if (root != null) {
      root.throwIfTerminated();
    } else if (terminated) {
      throw new IllegalStateException("Stream already terminated, can't be modified or used");
    }
  }

  // note that not all terminals directly call this, but they must use it indirectly
  void terminate() {
    if (root == null) {
      throwIfTerminated();
      terminated = true;
    } else {
      root.terminate();
    }
  }

  public T onClose(Runnable closeHandler) {
    if (root == null) {
      onClose.add(closeHandler);
    } else {
      root.onClose(closeHandler);
    }

    return (T) this;
  }

  public void close() {
    if (root == null) {
      terminated = true;

      // TODO this might not be quite right, make sure that it behaves the same way when throwing
      //      the same exception multiple times as JDK does.
      LinkedHashSet<Throwable> throwables = new LinkedHashSet<>();
      onClose.forEach((runnable) -> {
        try {
          runnable.run();
        } catch (Throwable e) {
          throwables.add(e);
        }
      });
      onClose.clear();
      if (!throwables.isEmpty()) {
        Iterator<Throwable> iterator = throwables.iterator();
        Throwable outer = iterator.next();
        iterator.forEachRemaining(outer::addSuppressed);

        if (outer instanceof RuntimeException) {
          throw (RuntimeException) outer;
        }
        if (outer instanceof Error) {
          throw (Error) outer;
        }
        assert false : "Couldn't have caught this exception from a Runnable! " + outer;
      }
    } else {
      root.close();
    }
  }
}