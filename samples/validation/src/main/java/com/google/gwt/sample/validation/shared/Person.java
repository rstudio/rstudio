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
package com.google.gwt.sample.validation.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A sample bean to show validation on.
 */
@ServerConstraint(groups = ServerGroup.class)
public class Person implements IsSerializable {

  @Valid
  private Address address;

  @Valid
  private Map<String, Address> otherAddresses;

  @NotNull
  @Size(min = 4, message = "{custom.name.size.message}")
  private String name;

  private long ssn;

  public Address getAddress() {
    return address;
  }

  public String getName() {
    return name;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSsn(long ssn) {
    this.ssn = ssn;
  }

  @Max(999999999)
  protected long getSsn() {
    return ssn;
  }
}
