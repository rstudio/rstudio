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
package com.google.gwt.sample.mobilewebapp.client;

import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxyImpl;
import com.google.gwt.storage.client.Storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the storage and retrieval of local tasks.
 */
public class TaskProxyLocalStorage {

  private static final String TASKLIST_SAVE_KEY = "TASKLIST";
  private static final String TASKSEP = "&&";
  private static final String FIELDSEP = "@@";
  private static final String FIELDEMPTY = "***";

  /**
   * Convert a task proxy list into a string.
   */
  private static String getStringFromTaskProxy(List<TaskProxy> list) {
    StringBuilder sb = new StringBuilder();
    for (TaskProxy proxy : list) {
      sb.append(proxy.getDueDate() != null ? proxy.getDueDate().getTime() : FIELDEMPTY);
      sb.append(FIELDSEP);
      sb.append(proxy.getId() != null ? proxy.getId() : "");
      sb.append(FIELDSEP);
      String name = proxy.getName();
      sb.append(name != null && name.length() > 0 ? proxy.getName() : FIELDEMPTY);
      sb.append(FIELDSEP);
      String notes = proxy.getNotes();
      sb.append(notes != null && notes.length() > 0 ? proxy.getNotes() : FIELDEMPTY);
      sb.append(TASKSEP);
    }
    return sb.toString();
  }

  /**
   * Parse a task proxy list from a string.
   */
  private static List<TaskProxy> getTaskProxyFromString(String taskProxyList) {
    ArrayList<TaskProxy> list = new ArrayList<TaskProxy>(0);
    if (taskProxyList == null) {
      return list;
    }
    // taskproxy1&&taskproxy2&&taskproxy3&&...
    String taskProxyStrings[] = taskProxyList.split(TASKSEP);
    for (String taskProxyString : taskProxyStrings) {
      if (taskProxyString == null) {
        continue;
      }
      // date@@id@@name@@notes
      String taskProxyStringData[] = taskProxyString.split(FIELDSEP);
      if (taskProxyStringData.length >= 4) {
        // collect the fields
        String dateString = taskProxyStringData[0];
        String idString = taskProxyStringData[1];
        String nameString = taskProxyStringData[2];
        if (FIELDEMPTY.equals(nameString)) {
          nameString = null;
        }
        String notesString = taskProxyStringData[3];
        if (FIELDEMPTY.equals(notesString)) {
          notesString = null;
        }
        // parse the numerical fields
        Date dueDate = null;
        try {
          dueDate = new Date(Long.parseLong(dateString));
        } catch (NumberFormatException nfe) {
        }
        Long idLong = 0L;
        try {
          idLong = Long.parseLong(idString);
        } catch (NumberFormatException nfe) {
        }
        // create and populate the TaskProxy
        TaskProxyImpl taskProxy = new TaskProxyImpl();
        taskProxy.setDueDate(dueDate);
        taskProxy.setId(idLong);
        taskProxy.setName(nameString);
        taskProxy.setNotes(notesString);
        list.add(taskProxy);
      }
    }
    return list;
  }

  private final Storage storage;
  private List<TaskProxy> tasks;
  private Map<Long, TaskProxy> taskMap;

  public TaskProxyLocalStorage(Storage storage) {
    this.storage = storage;
  }

  /**
   * Get a task by its ID.
   * 
   * @param id the task id
   * @return the task, or null if it isn't in local storage
   */
  public TaskProxy getTask(Long id) {
    // Create the map of tasks.
    if (taskMap == null) {
      taskMap = new HashMap<Long, TaskProxy>();
      for (TaskProxy task : getTasks()) {
        taskMap.put(task.getId(), task);
      }
    }

    return taskMap.get(id);
  }

  /**
   * Get a list of all tasks in local storage.
   */
  public List<TaskProxy> getTasks() {
    if (tasks == null) {
      // Load the saved task list from storage
      if (storage != null) { // if storage is supported
        String taskString = storage.getItem(TASKLIST_SAVE_KEY);
        tasks = getTaskProxyFromString(taskString);
      } else {
        tasks = new ArrayList<TaskProxy>();
      }
    }

    return tasks;
  }

  /**
   * Save a list of tasks to local storage.
   */
  public void setTasks(List<TaskProxy> tasks) {
    this.tasks = tasks;

    // Save the response to storage
    if (storage != null) { // if storage is supported
      String responseString = getStringFromTaskProxy(tasks);
      storage.setItem(TASKLIST_SAVE_KEY, responseString);
    }
  }
}