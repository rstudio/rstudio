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
package com.google.gwt.sample.dynatablerf.server;

import com.google.gwt.sample.dynatablerf.domain.Person;
import com.google.gwt.sample.dynatablerf.domain.Professor;
import com.google.gwt.sample.dynatablerf.domain.Schedule;
import com.google.gwt.sample.dynatablerf.domain.Student;
import com.google.gwt.sample.dynatablerf.domain.TimeSlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The server side service class.
 */
public class SchoolCalendarService {
  private static Long serial = 0L;

  private static final String[] FIRST_NAMES = new String[] {
      "Inman", "Sally", "Omar", "Teddy", "Jimmy", "Cathy", "Barney", "Fred",
      "Eddie", "Carlos"};

  private static final String[] LAST_NAMES = new String[] {
      "Smith", "Jones", "Epps", "Gibbs", "Webber", "Blum", "Mendez",
      "Crutcher", "Needler", "Wilson", "Chase", "Edelstein"};

  private static final String[] SUBJECTS = new String[] {
      "Chemistry", "Phrenology", "Geometry", "Underwater Basket Weaving",
      "Basketball", "Computer Science", "Statistics", "Materials Engineering",
      "English Literature", "Geology"};

  private static final int CLASS_LENGTH_MINS = 50;

  private static final int MAX_SCHED_ENTRIES = 5;

  private static final int MIN_SCHED_ENTRIES = 1;

  private static final int MAX_PEOPLE = 100;

  private static final int STUDENTS_PER_PROF = 5;

  private static final Map<Long, Person> people = new LinkedHashMap<Long, Person>(); 

  private static final Random rnd = new Random(3);

  public static Person findPerson(Long id) {
    return people.get(id);
  }

  public static List<Person> getPeople(int startIndex, int maxCount) {
    generateRandomPeople();

    int peopleCount = people.size();

    int start = startIndex;
    if (start >= peopleCount) {
      return Collections.emptyList();
    }

    int end = Math.min(startIndex + maxCount, peopleCount);
    if (start == end) {
      return Collections.emptyList();
    }

    return new ArrayList<Person>(people.values()).subList(startIndex, end);
  }
  
  public static void persist(Person person) {
    if (person.getId() == null) {
      person.setId(++serial);
    }
    person.setVersion(person.getVersion() + 1);
    people.put(person.getId(), person);
  }
  
  private static void generateRandomPeople() {
    if (people.isEmpty())
      for (int i = 0; i < MAX_PEOPLE; ++i) {
        Person person = generateRandomPerson();
        persist(person);
      }
  }

  private static Person generateRandomPerson() {
    // 1 out of every so many people is a prof.
    //
    if (rnd.nextInt(STUDENTS_PER_PROF) == 1) {
      return generateRandomProfessor();
    } else {
      return generateRandomStudent();
    }
  }

  private static Person generateRandomProfessor() {
    Professor prof = new Professor();

    String firstName = pickRandomString(FIRST_NAMES);
    String lastName = pickRandomString(LAST_NAMES);
    prof.setName("Dr. " + firstName + " " + lastName);

    String subject = pickRandomString(SUBJECTS);
    prof.setDescription("Professor of " + subject);

    generateRandomSchedule(prof.getTeachingSchedule());

    return prof;
  }

  private static void generateRandomSchedule(Schedule sched) {
    int range = MAX_SCHED_ENTRIES - MIN_SCHED_ENTRIES + 1;
    int howMany = MIN_SCHED_ENTRIES + rnd.nextInt(range);

    TimeSlot[] timeSlots = new TimeSlot[howMany];

    for (int i = 0; i < howMany; ++i) {
      int startHrs = 8 + rnd.nextInt(9); // 8 am - 5 pm
      int startMins = 15 * rnd.nextInt(4); // on the hour or some quarter
      int dayOfWeek = 1 + rnd.nextInt(5); // Mon - Fri

      int absStartMins = 60 * startHrs + startMins; // convert to minutes
      int absStopMins = absStartMins + CLASS_LENGTH_MINS;

      timeSlots[i] = new TimeSlot(dayOfWeek, absStartMins, absStopMins);
    }

    Arrays.sort(timeSlots);

    for (int i = 0; i < howMany; ++i) {
      sched.addTimeSlot(timeSlots[i]);
    }
  }

  private static Person generateRandomStudent() {
    Student student = new Student();

    String firstName = pickRandomString(FIRST_NAMES);
    String lastName = pickRandomString(LAST_NAMES);
    student.setName(firstName + " " + lastName);

    String subject = pickRandomString(SUBJECTS);
    student.setDescription("Majoring in " + subject);

    generateRandomSchedule(student.getClassSchedule());

    return student;
  }

  private static String pickRandomString(String[] a) {
    int i = rnd.nextInt(a.length);
    return a[i];
  }

  public SchoolCalendarService() {
  }
}
