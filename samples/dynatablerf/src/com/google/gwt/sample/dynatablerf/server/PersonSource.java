/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.dynatablerf.server;

import com.google.gwt.sample.dynatablerf.domain.Address;
import com.google.gwt.sample.dynatablerf.domain.Person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public abstract class PersonSource {
  static class Backing extends PersonSource {
    private Long serial = 0L;

    @Override
    public int countPeople() {
      return people.size();
    }

    @Override
    public Person findPerson(String id) {
      return people.get(id);
    }

    @Override
    public List<Person> getPeople(int startIndex, int maxCount,
        boolean[] daysFilter) {
      int peopleCount = countPeople();

      int start = startIndex;
      if (start >= peopleCount) {
        return Collections.emptyList();
      }

      int end = Math.min(startIndex + maxCount, peopleCount);
      if (start == end) {
        return Collections.emptyList();
      }
      // This is ugly, but a real backend would have a skip mechanism
      return new ArrayList<Person>(people.values()).subList(start, end);
    }

    @Override
    public void persist(Address address) {
      address.setVersion(address.getVersion() + 1);
      findPerson(address.getId()).getAddress().copyFrom(address);
    }

    @Override
    public void persist(Person person) {
      if (person.getId() == null) {
        person.setId(Long.toString(++serial));
      }
      person.setVersion(person.getVersion() + 1);
      Person existing = people.get(person.getId());
      if (existing != null) {
        existing.copyFrom(person);
      } else {
        people.put(person.getId(), person);
      }
    }
  }

  static class CopyOnRead extends PersonSource {
    private final PersonSource backingStore;

    public CopyOnRead(PersonSource backingStore) {
      this.backingStore = backingStore;
    }

    @Override
    public int countPeople() {
      return backingStore.countPeople();
    }

    @Override
    public Person findPerson(String id) {
      Person toReturn = people.get(id);
      if (toReturn == null) {
        toReturn = backingStore.findPerson(id);
        if (toReturn != null) {
          toReturn = toReturn.makeCopy();
        }
        people.put(id, toReturn);
      }
      return toReturn;
    }

    @Override
    public List<Person> getPeople(int startIndex, int maxCount,
        boolean[] daysFilter) {
      List<Person> toReturn = new ArrayList<Person>(maxCount);
      for (Person person : backingStore.getPeople(startIndex, maxCount,
          daysFilter)) {
        Person copy = findPerson(person.getId());
        toReturn.add(copy);
        copy.setDaysFilter(daysFilter);
      }
      return toReturn;
    }

    @Override
    public void persist(Address address) {
      backingStore.persist(address);
    }

    @Override
    public void persist(Person person) {
      backingStore.persist(person);
    }
  }

  /**
   * Create a PersonSource that will act directly on the given list.
   */
  public static PersonSource of(List<Person> people) {
    PersonSource backing = new Backing();
    for (Person person : people) {
      backing.persist(person);
    }
    return backing;
  }

  /**
   * Create a PersonSource that will read through to the given source and make
   * copies of any objects that are requested.
   */
  public static PersonSource of(PersonSource backing) {
    return new CopyOnRead(backing);
  }

  final Map<String, Person> people = new LinkedHashMap<String, Person>();

  public abstract int countPeople();

  public abstract Person findPerson(String id);

  public abstract List<Person> getPeople(int startIndex, int maxCount,
      boolean[] daysFilter);

  public abstract void persist(Address address);

  public abstract void persist(Person person);
}
