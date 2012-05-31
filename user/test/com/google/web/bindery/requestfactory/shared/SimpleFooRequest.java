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
package com.google.web.bindery.requestfactory.shared;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Do nothing test interface.
 */
@Service(com.google.web.bindery.requestfactory.server.SimpleFoo.class)
public interface SimpleFooRequest extends RequestContext {
  Request<Double> add(double a, double b);

  // Test overloaded method names
  Request<Integer> add(int a, int b);

  Request<Long> countSimpleFoo();

  InstanceRequest<SimpleFooProxy, Long> countSimpleFooWithUserNameSideEffect();

  InstanceRequest<SimpleFooProxy, Void> deleteBar();

  Request<SimpleFooProxy> echo(SimpleFooProxy proxy);

  Request<SimpleFooProxy> echoComplex(SimpleFooProxy fooProxy, SimpleBarProxy barProxy);

  Request<SimpleFooProxy> fetchDoubleReference();

  Request<List<SimpleFooProxy>> findAll();

  Request<SimpleFooProxy> findSimpleFooById(Long id);

  Request<List<SimpleFooProxy>> getFlattenedTripletReference();

  Request<SimpleFooProxy> getLongChain();

  Request<SimpleFooProxy> getNullInEntityList();

  Request<List<Integer>> getNumberList();
  
  Request<Set<Integer>> getNumberSet();

  Request<SimpleFooProxy> getSimpleFooWithNullRelationship();

  Request<SimpleFooProxy> getSimpleFooWithNullVersion();

  Request<SimpleFooProxy> getSimpleFooWithSubPropertyCollection();

  Request<SimpleFooProxy> getTripletReference();

  Request<SimpleFooProxy> getUnpersistedInstance();

  InstanceRequest<SimpleFooProxy, String> hello(SimpleBarProxy proxy);

  InstanceRequest<SimpleFooProxy, Void> persist();

  InstanceRequest<SimpleFooProxy, SimpleFooProxy> persistAndReturnSelf();

  InstanceRequest<SimpleFooProxy, SimpleFooProxy> persistCascadingAndReturnSelf();

  Request<Void> pleaseCrash(Integer crashIf42or43);

  Request<List<BigDecimal>> processBigDecimalList(List<BigDecimal> values);

  Request<List<BigInteger>> processBigIntegerList(List<BigInteger> values);

  Request<Boolean> processBooleanList(List<Boolean> values);

  Request<List<Date>> processDateList(List<Date> values);

  Request<SimpleEnum> processEnumList(List<SimpleEnum> values);

  InstanceRequest<SimpleFooProxy, String> processList(List<SimpleFooProxy> values);

  Request<String> processString(String value);

  Request<Void> receiveEnum(OnlyUsedByRequestContextMethod value);

  InstanceRequest<SimpleFooProxy, Void> receiveNull(String value);

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

  Request<Void> returnOnlyUsedInParameterization(List<OnlyUsedInListProxy> values);

  Request<SimpleFooProxy> returnSimpleFooSubclass();

  Request<List<SimpleValueProxy>> returnValueProxies();

  Request<SimpleValueProxy> returnValueProxy();

  InstanceRequest<SimpleFooProxy, Integer> sum(List<Integer> values);
}
