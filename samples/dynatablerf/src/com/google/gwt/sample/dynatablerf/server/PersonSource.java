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

import static com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory.SchoolCalendarRequest.ALL_DAYS;
import static com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory.SchoolCalendarRequest.NO_DAYS;

import com.google.gwt.sample.dynatablerf.domain.Person;
import com.google.gwt.sample.dynatablerf.domain.Schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a number of Person objects as a demonstration datasource. Many of
 * the operations in this implementation would be much more efficient in a real
 * database, but are implemented is a straightforward fashion because they're
 * not really important for understanding the RequestFactory framework.
 */
public abstract class PersonSource {
  static class Backing extends PersonSource {
    private Long serial = 0L;
    private final ScheduleSource scheduleStore;
    
    public Backing(ScheduleSource scheduleStore) {
      this.scheduleStore = scheduleStore;
    }

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
        List<Boolean> daysFilter) {
      int peopleCount = countPeople();

      int start = startIndex;
      if (start >= peopleCount) {
        return Collections.emptyList();
      }

      int end = Math.min(startIndex + maxCount, peopleCount);
      if (start == end) {
        return Collections.emptyList();
      }

      // If there's a simple filter, use a fast path
      if (ALL_DAYS.equals(daysFilter)) {
        return new ArrayList<Person>(people.values()).subList(start, end);
      } else if (NO_DAYS.equals(daysFilter)) {
        return new ArrayList<Person>();
      }

      /*
       * Otherwise, iterate from the start position until we collect enough
       * People or hit the end of the list.
       */
      Iterator<Person> it = people.values().iterator();
      int skipped = 0;
      List<Person> toReturn = new ArrayList<Person>(maxCount);
      while (toReturn.size() < maxCount && it.hasNext()) {
        Person person = it.next();
        if (person.getScheduleWithFilter(daysFilter).length() > 0) {
          if (skipped++ < startIndex) {
            continue;
          }
          toReturn.add(person);
        }
      }
      return toReturn;
    }

    @Override
    public void persist(Person person) {
      if (person.getId() == null) {
        person.setId(Long.toString(++serial));
      }
      person.setVersion(person.getVersion() + 1);
      if (person.getClassSchedule() != null) {
        scheduleStore.persist(person.getClassSchedule());
      }
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
    private final ScheduleSource scheduleStore;

    public CopyOnRead(PersonSource backingStore, ScheduleSource scheduleStore) {
      this.backingStore = backingStore;
      this.scheduleStore = scheduleStore;
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
          
          Integer scheduleKey = toReturn.getClassSchedule().getKey();
          Schedule scheduleCopy = scheduleStore.find(scheduleKey);
          toReturn.setClassSchedule(scheduleCopy);
        }
        people.put(id, toReturn);
      }
      return toReturn;
    }

    @Override
    public List<Person> getPeople(int startIndex, int maxCount,
        List<Boolean> daysFilter) {
      List<Person> toReturn = new ArrayList<Person>(maxCount);
      for (Person person : backingStore.getPeople(startIndex, maxCount,
          daysFilter)) {
        Person copy = findPerson(person.getId());
        copy.setDaysFilter(daysFilter);
        toReturn.add(copy);
      }
      return toReturn;
    }

    @Override
    public void persist(Person person) {
      backingStore.persist(person);
    }
  }

  /**
   * Create a PersonSource that will act directly on the given list.
   */
  public static PersonSource of(List<Person> people, ScheduleSource schedules) {
    PersonSource backing = new Backing(schedules);
    for (Person person : people) {
      backing.persist(person);
    }
    return backing;
  }

  /**
   * Create a PersonSource that will read through to the given source and make
   * copies of any objects that are requested.
   */
  public static PersonSource of(PersonSource backing, ScheduleSource scheduleBacking) {
    return new CopyOnRead(backing, scheduleBacking);
  }

  final Map<String, Person> people = new LinkedHashMap<String, Person>();

  public abstract int countPeople();

  public abstract Person findPerson(String id);

  public abstract List<Person> getPeople(int startIndex, int maxCount,
      List<Boolean> daysFilter);

  public abstract void persist(Person person);
}
