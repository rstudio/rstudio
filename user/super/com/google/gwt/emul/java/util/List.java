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

  void add(int index, Object element);

  boolean add(Object o);

  boolean addAll(Collection c);

  boolean addAll(int index, Collection c);

  void clear();

  boolean contains(Object o);

  boolean containsAll(Collection c);

  boolean equals(Object o);

  Object get(int index);

  int hashCode();

  int indexOf(Object o);

  boolean isEmpty();

  Iterator iterator();

  int lastIndexOf(Object o);

  Object remove(int index);

  boolean remove(Object o);

  boolean removeAll(Collection c);

  boolean retainAll(Collection c);

  Object set(int index, Object element);

  int size();

  Object[] toArray();

}
