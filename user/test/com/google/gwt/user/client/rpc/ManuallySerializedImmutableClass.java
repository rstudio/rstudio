/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.rpc;

import java.util.Date;

/**
 * This class is defined outside of the CustomFieldSerializerTestSetFactory
 * because of a bug where custom field serializers cannot be inner classes and
 * custom field serializers must be in the same package as the class that they
 * serializer. Once we fix this bug we can move this class into the test set
 * factory.
 */
public class ManuallySerializedImmutableClass {
  /**
   * Ensures that RPC will consider Date to be exposed. It will be pruned by
   * dead code elimination.
   */
  @SuppressWarnings("unused")
  private Date dateTypeExposeToSerialization;

  /**
   * Transient to avoid 'cannot serialize final field' RPC warning.
   * 
   * FIXME: when final fields are supported, or don't generate warnings for
   * custom serialized classes.
   */
  private final transient Date endDate;

  /**
   * Transient to avoid 'cannot serialize final field' RPC warning.
   * 
   * FIXME: when final fields are supported, or don't generate warnings for
   * custom serialized classes.
   */
  private final transient Date startDate;

  public ManuallySerializedImmutableClass(Date startDate, Date endDate) {
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public Date getStartDate() {
    return startDate;
  }
}
