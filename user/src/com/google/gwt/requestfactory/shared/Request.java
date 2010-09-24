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

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Implemented by the request objects created by this factory.
 *
 * @param <T> The return type of objects in the corresponding response.
 */
public interface Request<T> {

  /**
   * @return an editable version of the given {@link EntityProxy}
   */
  <P extends EntityProxy> P edit(P proxy);

  /**
   * Submit this request. Results will be passed to the receiver asynchronously.
   */
  void fire(Receiver<? super T> receiver);

  /**
   * Return true if there are outstanding changes that have not been
   * communicated to the server yet. Note that it is illegal to call this method
   * after a request using it has been fired.
   */
  boolean isChanged();

  Request<T> with(String... propertyRefs);
}
