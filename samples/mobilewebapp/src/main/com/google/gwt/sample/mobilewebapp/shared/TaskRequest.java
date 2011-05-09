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
package com.google.gwt.sample.mobilewebapp.shared;

import com.google.gwt.sample.mobilewebapp.server.domain.Task;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;

import java.util.List;

/**
 * Remote request for {@link Task}.
 */
@Service(Task.class)
public interface TaskRequest extends RequestContext {

  /**
   * Create a {@link Request} for all tasks.
   * 
   * @return a {@link Request}
   */
  Request<List<TaskProxy>> findAllTasks();

  /**
   * Create a {@link Request} to find a Task by id.
   * 
   * @param id the task id
   * @return a {@link Request}
   */
  Request<TaskProxy> findTask(Long id);

  /**
   * Persist a Task instance in the datastore.
   * 
   * @return an {@link InstanceRequest}
   */
  InstanceRequest<TaskProxy, Void> persist();

  /**
   * Remove a Task instance from the datastore.
   * 
   * @return an {@link InstanceRequest}
   */
  InstanceRequest<TaskProxy, Void> remove();
}
