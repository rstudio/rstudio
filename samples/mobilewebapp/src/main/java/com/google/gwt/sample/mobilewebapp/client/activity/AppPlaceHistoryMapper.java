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
package com.google.gwt.sample.mobilewebapp.client.activity;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskPlace;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListPlace;

/**
 * This interface is the hub of your application's navigation system. It links
 * the {@link com.google.gwt.place.shared.Place Place}s your user navigates to
 * with the browser history system &mdash; that is, it makes the browser's back
 * and forth buttons work for you, and also makes each spot in your app
 * bookmarkable.
 * <p>
 * Its implementation is code generated based on the @WithTokenizers
 * annotation.
 */
@WithTokenizers({TaskListPlace.Tokenizer.class, TaskPlace.Tokenizer.class})
public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}
