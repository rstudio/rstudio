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
package com.google.gwt.sample.expenses.gwt.client;

import com.google.gwt.app.place.PlaceHistoryHandlerWithFactory;

/**
 * This interface is the hub of your application's navigation system. It links
 * the {@link com.google.gwt.app.place.Place Place}s your user navigates to with
 * the browser history system &mdash; that is, it makes the browser's back and
 * forth buttons work for you, and also makes each spot in your app
 * bookmarkable. The instance used by this app is available via
 * {@link ScaffoldFactory#getPlaceHistoryHandler()}.
 * <p>
 * The simplest way to make new {@link com.google.gwt.app.place.Place Place}
 * types available to your app is to uncomment the {@literal @}WithTokenizers
 * annotation below and list their corresponding
 * {@link com.google.gwt.app.place.PlaceTokenizer PlaceTokenizer}s.
 * <p>
 * This code generated object looks to both the {@literal @}WithTokenizers
 * annotation and the factory to infer the types of
 * {@link com.google.gwt.app.place.Place Place}s your app can navigate to. In
 * this case it will find the {@link ScaffoldFactory#getProxyListPlaceTokenizer()} and
 * {@link ScaffoldFactory#getProxyPlaceTokenizer()} methods, and so be able to handle
 * {@link com.google.gwt.app.place.ProxyListPlace ProxyListPlace}s (which show
 * all entities of a particular type) and
 * {@link com.google.gwt.app.place.ProxyPlace ProxyPlace}s (which give access to
 * an individual entity).
 */
// @WithTokenizers({MyNewPlace.Tokenizer, MyOtherNewPlace.Tokenizer})
interface ScaffoldPlaceHistoryHandler extends
    PlaceHistoryHandlerWithFactory<ScaffoldFactory> {
}
