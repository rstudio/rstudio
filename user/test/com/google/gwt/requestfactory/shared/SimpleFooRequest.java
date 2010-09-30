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
package com.google.gwt.requestfactory.shared;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Do nothing test interface.
 */
@Service(com.google.gwt.requestfactory.server.SimpleFoo.class)
public interface SimpleFooRequest {
  Request<Long> countSimpleFoo();

  @Instance
  Request<Long> countSimpleFooWithUserNameSideEffect(SimpleFooProxy proxy);

  @Instance
  Request<Void> deleteBar(SimpleFooProxy proxy);

  Request<SimpleFooProxy> echo(SimpleFooProxy proxy);

  Request<SimpleFooProxy> echoComplex(SimpleFooProxy fooProxy, SimpleBarProxy barProxy);

  Request<List<SimpleFooProxy>> findAll();

  Request<SimpleFooProxy> findSimpleFooById(Long id);

  Request<List<Integer>> getNumberList();

  Request<Set<Integer>> getNumberSet();

  @Instance
  Request<String> hello(SimpleFooProxy instance, SimpleBarProxy proxy);

  @Instance
  Request<Void> persist(SimpleFooProxy proxy);

  @Instance
  Request<SimpleFooProxy> persistAndReturnSelf(SimpleFooProxy proxy);

  @Instance
  Request<SimpleFooProxy> persistCascadingAndReturnSelf(SimpleFooProxy proxy);

  Request<Boolean> processBooleanList(List<Boolean> values);

  Request<Date> processDateList(List<Date> values);

  Request<SimpleEnum> processEnumList(List<SimpleEnum> values);

  Request<String> processString(String value);

  @Instance
  Request<String> processList(SimpleFooProxy instance, List<SimpleFooProxy> values);

  @Instance
  Request<Void> receiveNull(SimpleFooProxy instance, String value);

  Request<Void> receiveNullList(List<SimpleFooProxy> value);

  Request<Void> receiveNullSimpleFoo(SimpleFooProxy value);

  Request<Void> receiveNullString(String value);

  Request<Void> receiveNullValueInEntityList(List<SimpleFooProxy> value);

  Request<Void> receiveNullValueInIntegerList(List<Integer> value);

  Request<Void> receiveNullValueInStringList(List<String> value);

  Request<Void> reset();

  Request<List<SimpleFooProxy>> returnNullList();

  Request<SimpleFooProxy> returnNullSimpleFoo();

  Request<String> returnNullString();

  @Instance
  Request<Integer> sum(SimpleFooProxy instance, List<Integer> values);
}
