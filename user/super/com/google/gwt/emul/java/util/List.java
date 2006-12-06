/*
 * Copyright 2006 Google Inc.
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

/**
 * Represents a sequence of objects.
 */
public interface List extends Collection {

  int size();

  boolean isEmpty();

  boolean contains(Object o);

  Iterator iterator();

  Object[] toArray();

  boolean add(Object o);

  boolean remove(Object o);

  boolean containsAll(Collection c);

  boolean addAll(Collection c);

  boolean addAll(int index, Collection c);

  boolean removeAll(Collection c);

  boolean retainAll(Collection c);

  void clear();

  boolean equals(Object o);

  int hashCode();

  Object get(int index);

  Object set(int index, Object element);

  void add(int index, Object element);

  Object remove(int index);

  int indexOf(Object o);

  int lastIndexOf(Object o);

}
