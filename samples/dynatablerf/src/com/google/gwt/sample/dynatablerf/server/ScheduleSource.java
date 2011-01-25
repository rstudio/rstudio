/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.sample.dynatablerf.domain.Schedule;
import com.google.gwt.sample.dynatablerf.domain.TimeSlot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a number of Schedule objects as a demonstration datasource. Many of
 * the operations in this implementation would be much more efficient in a real
 * database, but are implemented is a straightforward fashion because they're
 * not really important for understanding the RequestFactory framework.
 */
public abstract class ScheduleSource {

  private static int serial = 0;

  /**
   * Backing store of schedule entities. 
   */
  static class Backing extends ScheduleSource {

    @Override
    public Schedule create(Class<? extends Schedule> clazz) {
      return new Schedule();
    }

    @Override
    public Schedule find(Class<? extends Schedule> clazz, String id) {
      if (!Schedule.class.equals(clazz)) {
        return null;
      }
      return schedules.get(makeKey(id));
    }

    @Override
    public Class<Schedule> getDomainType() {
      return Schedule.class;
    }

    @Override
    public String getId(Schedule domainObject) {
      return Integer.toString(domainObject.getKey());
    }

    @Override
    public Class<String> getIdType() {
      return String.class;
    }

    @Override
    public Object getVersion(Schedule domainObject) {
      return domainObject.getRevision();
    }

    @Override
    public void persist(Schedule domainObject) {
      if (domainObject.getKey() == null) {
        domainObject.setKey(newSerial());
      }
      domainObject.setRevision(newSerial());
      Schedule existing = schedules.get(domainObject.getKey());
      if (existing == null) {
        schedules.put(domainObject.getKey(), domainObject);
      } else {
        copyScheduleFields(domainObject, existing);
      }
    }    
  }

  /**
   * Provides copy-on-read access to a ScheduleLocator.
   */
  public static class CopyOnRead extends Backing {
    private final ScheduleSource backingStore;
    
    public CopyOnRead(ScheduleSource backingStore) {
      this.backingStore = backingStore;
    }

    @Override
    public Schedule find(Class<? extends Schedule> clazz, String id) {
      if (!Schedule.class.equals(clazz)) {
        return null;
      }
      Integer key = makeKey(id);
      Schedule toReturn = schedules.get(key);
      if (toReturn == null) {
        Schedule original = backingStore.find(clazz, id);
        if (original != null) {
          toReturn = makeCopy(original);
        }
        schedules.put(key, toReturn);
      }
      return toReturn;
    }

    @Override
    public void persist(Schedule domainObject) {
      backingStore.persist(domainObject);
    }

    private Schedule makeCopy(Schedule source) {
      Schedule destination = new Schedule();
      copyScheduleFields(source, destination);
      return destination;
    }
  }

  static void copyScheduleFields(Schedule source, Schedule destination) {
    destination.setKey(source.getKey());
    destination.setRevision(source.getRevision());
    destination.setTimeSlots(new ArrayList<TimeSlot>(source.getTimeSlots()));
  }

  public static Backing createBacking() {
    return new Backing();
  }

  public static ScheduleSource getThreadLocalObject() {
    return SchoolCalendarService.SCHEDULE_SOURCE.get();
  }

  /**
   * Create a ScheduleLocator that will act directly on the given list.
   */
  public static ScheduleSource of(List<Schedule> schedules) {
    ScheduleSource backing = createBacking();
    for (Schedule schedule : schedules) {
      backing.persist(schedule);
    }
    return backing;
  }

  /**
   * Create a ScheduleLocator that will read through to the given source and make
   * copies of any objects that are requested.
   */
  public static ScheduleSource of(ScheduleSource backing) {
    return new CopyOnRead(backing);
  }

  private static Integer makeKey(String id) {
    return Integer.valueOf(id);
  }
  
  private static int newSerial() {
    return ++serial;
  }

  final Map<Integer, Schedule> schedules = new LinkedHashMap<Integer, Schedule>();

  public abstract Schedule create(Class<? extends Schedule> clazz);

  public Schedule find(Integer key) {
    return find(Schedule.class, key.toString());
  }
  
  public abstract Schedule find(Class<? extends Schedule> clazz, String id);

  public abstract Class<Schedule> getDomainType();

  public abstract String getId(Schedule domainObject);

  public abstract Class<String> getIdType();

  public abstract Object getVersion(Schedule domainObject);
  
  public abstract void persist(Schedule domainObject);

}
