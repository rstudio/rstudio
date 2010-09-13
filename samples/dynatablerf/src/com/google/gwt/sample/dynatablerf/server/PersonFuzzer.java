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

import com.google.gwt.sample.dynatablerf.domain.Person;
import com.google.gwt.sample.dynatablerf.domain.Professor;
import com.google.gwt.sample.dynatablerf.domain.Schedule;
import com.google.gwt.sample.dynatablerf.domain.Student;
import com.google.gwt.sample.dynatablerf.domain.TimeSlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Utility class for creating random people.
 */
class PersonFuzzer {

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

  private static final int MAX_PEOPLE = 100;

  private static final int MAX_SCHED_ENTRIES = 5;

  private static final int MIN_SCHED_ENTRIES = 1;

  private static final int STUDENTS_PER_PROF = 5;

  public static List<Person> generateRandomPeople() {
    List<Person> toReturn = new ArrayList<Person>(MAX_PEOPLE);
    Random rnd = new Random(3);
    for (long i = 0; i < MAX_PEOPLE; i++) {
      Person person = generateRandomPerson(rnd);
      AddressFuzzer.fuzz(rnd, person.getAddress());
      toReturn.add(person);
    }
    return toReturn;
  }

  private static Person generateRandomPerson(Random rnd) {
    // 1 out of every so many people is a prof.
    //
    if (rnd.nextInt(STUDENTS_PER_PROF) == 1) {
      return generateRandomProfessor(rnd);
    } else {
      return generateRandomStudent(rnd);
    }
  }

  private static Person generateRandomProfessor(Random rnd) {
    Professor prof = new Professor();

    String firstName = pickRandomString(rnd, FIRST_NAMES);
    String lastName = pickRandomString(rnd, LAST_NAMES);
    prof.setName("Dr. " + firstName + " " + lastName);

    String subject = pickRandomString(rnd, SUBJECTS);
    prof.setDescription("Professor of " + subject);

    generateRandomSchedule(rnd, prof.getTeachingSchedule());

    return prof;
  }

  private static void generateRandomSchedule(Random rnd, Schedule sched) {
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

  private static Person generateRandomStudent(Random rnd) {
    Student student = new Student();

    String firstName = pickRandomString(rnd, FIRST_NAMES);
    String lastName = pickRandomString(rnd, LAST_NAMES);
    student.setName(firstName + " " + lastName);

    String subject = pickRandomString(rnd, SUBJECTS);
    student.setDescription("Majoring in " + subject);

    generateRandomSchedule(rnd, student.getClassSchedule());

    return student;
  }

  private static String pickRandomString(Random rnd, String[] a) {
    int i = rnd.nextInt(a.length);
    return a[i];
  }

}
