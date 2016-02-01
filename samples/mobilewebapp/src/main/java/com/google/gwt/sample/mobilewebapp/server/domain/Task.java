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

import static com.google.gwt.sample.mobilewebapp.server.domain.ObjectifyManager.ofy;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.cmd.Query;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A task used in the task list. This is a monolothic implementation of a data object
 * for use with {@code RequestFactory}. Better patterns make use of Locators and
 * ServiceLocators to simplify the boilerplate required to expose a data object.
 * <p>
 * See <a
 * href='http://turbomanage.wordpress.com/2011/03/25/using-gwt-requestfactory-with-objectify/'
 * >this fine blog post</a>,
 * for an example.
 */
@Entity
public class Task {

  /**
   * Find all tasks for the current user.
   */
  @SuppressWarnings("unchecked")
  public static List<Task> findAllTasks() {
    Query<Task> q = ofy().load().type(Task.class).filter("userId", currentUserId());

    List<Task> list = q.list();
    /*
     * If this is the first time running the app, populate the datastore with
     * some default tasks and re-query the datastore for them.
     */
    if (list.size() == 0) {
      populateDatastore();
      q = ofy().load().type(Task.class).filter("userId", currentUserId());
      list = q.list();
    }

    return list;
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

    Task task = ofy().load().type(Task.class).id(id).now();
    if (task != null && task.userId.equals(currentUserId())) {
      return task;
    } else {
      return null;
    }
  }

  private static String currentUserId() {
    return UserServiceWrapper.get().getCurrentUserId();
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
      task0.userId = currentUserId();
      ofy().save().entity(task0);
    }
    {
      // Task 1.
      Task task1 = new Task();
      task1.setName("Make a million dollars");
      task1.setNotes("Then spend it all on Android apps");
      task1.userId = currentUserId();
      ofy().save().entity(task1);
    }
    {
      // Task 2.
      Task task2 = new Task();
      task2.setName("Buy a dozen eggs");
      task2.setNotes("of the chicken variety");
      task2.userId = currentUserId();
      ofy().save().entity(task2);
    }
    {
      // Task 3.
      Task task3 = new Task();
      task3.setName("Complete all tasks");
      task3.userId = currentUserId();
      ofy().save().entity(task3);
    }
  }

  @Id
  Long id;

  private Date dueDate;

  @NotNull(message = "You must specify a name")
  @Size(min = 3, message = "Name must be at least 3 characters long")
  private String name;

  private String notes;

  /**
   * The unique ID of the user who owns this task.
   */
  @Index
  private String userId;

  // TODO: Move this field to a superclass that implements a persistence layer
  private Integer version = 0;

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
    // TODO: Move this method to a superclass that implements a persistence layer
    return version;
  }

  /**
   * Persist this object in the data store.
   */
  public void persist() {
    ++version;

    // Set the user id if this is a new task.
    String curUserId = currentUserId();
    if (userId == null) {
      userId = curUserId;
    }

    // Verify the current user owns the task before updating it.
    if (curUserId.equals(userId)) {
      ofy().save().entity(this);
    }
  }

  /**
   * Remove this object from the data store.
   */
  public void remove() {
    Task task = ofy().load().type(Task.class).id(this.id).now();

    if (currentUserId().equals(task.userId)) {
      ofy().delete().entity(task);
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

  @OnSave
  void onPersist() {
    // TODO: Move this method to a superclass that implements a persistence layer
    ++this.version;
  }
}
