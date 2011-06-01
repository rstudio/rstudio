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
package com.google.gwt.sample.mobilewebapp.server.domain;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A task used in the task list.
 */
@Entity
public class Task {

  /**
   * Find all tasks for the current user.
   */
  @SuppressWarnings("unchecked")
  public static List<Task> findAllTasks() {
    EntityManager em = entityManager();
    try {
      Query query = em.createQuery("select o from Task o where o.userId=:userId");
      query.setParameter("userId", UserServiceWrapper.get().getCurrentUserId());
      List<Task> list = query.getResultList();

      /*
       * If this is the first time running the app, populate the datastore with
       * some default tasks and re-query the datastore for them.
       */
      if (list.size() == 0) {
        populateDatastore();
        list = query.getResultList();

        /*
         * Workaround for this issue:
         * http://code.google.com/p/datanucleus-appengine/issues/detail?id=24
         */
        list.size();
      }

      return list;
    } finally {
      em.close();
    }
  }

  /**
   * Find a {@link Task} by id for the current user.
   * 
   * @param id the {@link Task} id
   * @return the associated {@link Task}, or null if not found
   */
  public static Task findTask(Long id) {
    if (id == null) {
      return null;
    }

    EntityManager em = entityManager();
    try {
      Task task = em.find(Task.class, id);
      if (task != null && UserServiceWrapper.get().getCurrentUserId().equals(task.userId)) {
        return task;
      }
      return null;
    } finally {
      em.close();
    }
  }

  /**
   * Create an entity manager to interact with the database.
   * 
   * @return an {@link EntityManager} instance
   */
  private static EntityManager entityManager() {
    return EMF.get().createEntityManager();
  }

  /**
   * Populate the datastore with some default tasks. We do this to make the app
   * more intuitive on first use.
   */
  @SuppressWarnings("deprecation")
  private static void populateDatastore() {
    {
      // Task 0.
      Task task0 = new Task();
      task0.setName("Beat Angry Birds");
      task0.setNotes("This game is impossible!");
      task0.setDueDate(new Date(100, 4, 20));
      task0.persist();
    }
    {
      // Task 1.
      Task task1 = new Task();
      task1.setName("Make a million dollars");
      task1.setNotes("Then spend it all on Android apps");
      task1.persist();
    }
    {
      // Task 2.
      Task task2 = new Task();
      task2.setName("Buy a dozen eggs");
      task2.setNotes("of the chicken variety");
      task2.persist();
    }
    {
      // Task 3.
      Task task3 = new Task();
      task3.setName("Complete all tasks");
      task3.persist();
    }
  }

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * The unique ID of the user who owns this task.
   */
  private String userId;

  @Version
  @Column(name = "version")
  private Integer version;

  private Date dueDate;

  @NotNull(message = "You must specify a name")
  @Size(min = 3, message = "Name must be at least 3 characters long")
  private String name;
  private String notes;

  /**
   * Get the due date of the Task.
   */
  public Date getDueDate() {
    return dueDate;
  }

  /**
   * Get the unique ID of the Task.
   */
  public Long getId() {
    return id;
  }

  /**
   * Get the name of the Task.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the notes associated with the task.
   */
  public String getNotes() {
    return notes;
  }

  /**
   * Get the version of this datastore object.
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Persist this object in the data store.
   */
  public void persist() {
    EntityManager em = entityManager();
    try {
      // Set the user id if this is a new task.
      String curUserId = UserServiceWrapper.get().getCurrentUserId();
      if (userId == null) {
        userId = curUserId;
      }

      // Verify the current user owns the task before updating it.
      if (curUserId.equals(userId)) {
        em.persist(this);
      }
    } finally {
      em.close();
    }
  }

  /**
   * Remove this object from the data store.
   */
  public void remove() {
    EntityManager em = entityManager();
    try {
      Task task = em.find(Task.class, this.id);

      // Verify the current user owns the task before removing it.
      if (UserServiceWrapper.get().getCurrentUserId().equals(task.userId)) {
        em.remove(task);
      }
    } finally {
      em.close();
    }
  }

  /**
   * Set the due date of the task.
   * 
   * @param dueDate the due date, or null if no due date
   */
  public void setDueDate(Date dueDate) {
    this.dueDate = dueDate;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Set the name of the task.
   * 
   * @param name the task name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Set the notes associated with the task.
   * 
   * @param notes the notes
   */
  public void setNotes(String notes) {
    this.notes = notes;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
