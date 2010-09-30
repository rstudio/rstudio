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

/**
 * Do nothing test interface.
 */
@Service(com.google.gwt.requestfactory.server.SimpleFooString.class)
public interface SimpleFooStringRequest {
  Request<Long> countSimpleFoo();

  @Instance
  Request<Long> countSimpleFooWithUserNameSideEffect(SimpleFooStringProxy proxy);

  Request<List<SimpleFooStringProxy>> findAll();

  Request<SimpleFooStringProxy> findSimpleFooStringById(String id);

  @Instance
  Request<Void> persist(SimpleFooStringProxy proxy);
  
  @Instance
  Request<SimpleFooStringProxy> persistAndReturnSelf(SimpleFooStringProxy proxy);

  Request<Void> reset();

  @Instance
  Request<String> hello(SimpleFooStringProxy instance, SimpleBarProxy proxy);
}
