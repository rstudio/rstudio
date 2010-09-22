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

import java.util.List;
import java.util.Set;

/**
 * Do nothing test interface.
 */
@Service(com.google.gwt.requestfactory.server.SimpleFoo.class)
public interface SimpleFooRequest {
  RequestObject<Long> countSimpleFoo();

  @Instance
  RequestObject<Long> countSimpleFooWithUserNameSideEffect(SimpleFooProxy proxy);

  ProxyListRequest<SimpleFooProxy> findAll();

  ProxyRequest<SimpleFooProxy> findSimpleFooById(Long id);

  RequestObject<Integer> privateMethod();

  RequestObject<List<Integer>> getNumberList();

  RequestObject<Set<Integer>> getNumberSet();

  @Instance
  RequestObject<Void> persist(SimpleFooProxy proxy);
  
  @Instance
  ProxyRequest<SimpleFooProxy> persistAndReturnSelf(SimpleFooProxy proxy);

  RequestObject<Void> reset();

  @Instance
  RequestObject<String> hello(SimpleFooProxy instance, SimpleBarProxy proxy);

  @Instance
  RequestObject<Integer> sum(SimpleFooProxy instance, List<Integer> values);

  @Instance
  RequestObject<String> processList(SimpleFooProxy instance, List<SimpleFooProxy> values);
}
