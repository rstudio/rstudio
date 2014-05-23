/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs.test.gwtincompatible;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple class that is @GwtIncompatible.
 *
 * This class will be seen as a final class that does not extend nor implement anything other
 * than Object an has only one member: its private default constructor.
 */
@GwtIncompatible("incompatible abstract class")
public abstract class AbstractGwtIncompatibleClass extends AbstractCollection<String>
    implements Set<String> {

  public int someIntersetingMethod() {
    return 0;
  }

  @Override
  public Iterator<String> iterator() {
    return null;
  }

  @Override
  public int size() {
    return 0;
  }
}
