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
package com.google.gwt.dev.shell;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A class that keeps track of Java objects which have been exposed to the other
 * side and assigns unique ids.
 */
public class ServerObjectsTable {

  /**
   * A type that records the next-available free id slot to use, without itself
   * being a valid return type.
   */
  private static class Tombstone {
    private final int nextFree;

    public Tombstone(int nextFree) {
      this.nextFree = nextFree;
    }
  }

  private int nextFree = -1;
  private int nextId = 0;
  private final Map<Integer, Object> objects = new TreeMap<Integer, Object>();
  private final Map<Object, Integer> refMap = new IdentityHashMap<Object, Integer>();

  public int add(Object obj) {
    int id = find(obj);
    if (id >= 0) {
      return id;
    }
    if (nextFree >= 0) {
      id = nextFree;
      nextFree = ((Tombstone) objects.get(id)).nextFree;
    } else {
      id = nextId++;
    }
    objects.put(id, obj);
    refMap.put(obj, id);
    return id;
  }

  public int find(Object obj) {
    Integer objId = refMap.get(obj);
    return objId != null ? objId.intValue() : -1;
  }

  public void free(int id) {
    Object object = objects.get(id);
    assert object != null : "Trying to free never-used id " + id;
    assert !(object instanceof Tombstone) : "Duplicate free " + id;
    refMap.remove(object);
    objects.put(id, new Tombstone(nextFree));
    nextFree = id;
  }

  public Object get(int id) {
    Object toReturn = objects.get(id);
    assert !(toReturn instanceof Tombstone) : id + " is not an active id";
    return toReturn;
  }
}