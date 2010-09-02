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
package com.google.gwt.requestfactory.server;

import com.google.gwt.requestfactory.shared.Id;
import com.google.gwt.requestfactory.shared.SimpleEnum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Domain object for SimpleFooRequest.
 */
public class SimpleFoo {

  static SimpleFoo singleton = new SimpleFoo();

  private static Long nextId = 1L;

  public static Long countSimpleFoo() {
    return 1L;
  }

  public static List<SimpleFoo> findAll() {
    return Collections.singletonList(singleton);
  }

  public static SimpleFoo findSimpleFoo(Long id) {
    return findSimpleFooById(id);
  }

  public static SimpleFoo findSimpleFooById(Long id) {
    singleton.setId(id);
    return singleton;
  }

  public static SimpleFoo getSingleton() {
    return singleton;
  }

  public static void reset() {
    singleton = new SimpleFoo();
  }

  @SuppressWarnings("unused")
  private static Integer privateMethod() {
    return 0;
  }

  @Id
  private Long id = 1L;

  Integer version = 1;

  private String userName;
  private String password;

  private Character charField;
  private Long longField;

  private BigDecimal bigDecimalField;
  
  private BigInteger bigIntField;
  private Integer intId = -1;
  private Short shortField;
  
  private Byte byteField;
  
  private Date created;
  private Double doubleField;
  
  private Float floatField;
  
  private SimpleEnum enumField;
  private Boolean boolField;

  private Boolean otherBoolField;

  private SimpleBar barField;
  private SimpleFoo fooField;

  public SimpleFoo() {
    intId = 42;
    version = 1;
    userName = "GWT";
    longField = 8L;
    enumField = SimpleEnum.FOO;
    created = new Date();
    barField = SimpleBar.getSingleton();
    boolField = true;
  }

  public Long countSimpleFooWithUserNameSideEffect() {
    singleton.setUserName(userName);
    return 1L;
  }

  public SimpleBar getBarField() {
    return barField;
  }

  /**
   * @return the bigDecimalField
   */
  public BigDecimal getBigDecimalField() {
    return bigDecimalField;
  }

  /**
   * @return the bigIntegerField
   */
  public BigInteger getBigIntField() {
    return bigIntField;
  }

  public Boolean getBoolField() {
    return boolField;
  }

  /**
   * @return the byteField
   */
  public Byte getByteField() {
    return byteField;
  }

  /**
   * @return the charField
   */
  public Character getCharField() {
    return charField;
  }

  public Date getCreated() {
    return created;
  }

  /**
   * @return the doubleField
   */
  public Double getDoubleField() {
    return doubleField;
  }

  public SimpleEnum getEnumField() {
    return enumField;
  }

  /**
   * @return the floatField
   */
  public Float getFloatField() {
    return floatField;
  }

  public SimpleFoo getFooField() {
    return fooField;
  }

  public Long getId() {
    return id;
  }

  public Integer getIntId() {
    return intId;
  }

  public Long getLongField() {
    return longField;
  }

  /**
   * @return the otherBoolField
   */
  public Boolean getOtherBoolField() {
    return otherBoolField;
  }
  public String getPassword() {
    return password;
  }

  /**
   * @return the shortField
   */
  public Short getShortField() {
    return shortField;
  }

  public String getUserName() {
    return userName;
  }

  public Integer getVersion() {
    return version;
  }

  public String hello(SimpleBar bar) {
    return "Greetings " + bar.getUserName() + " from " + getUserName();  
  }

  public void persist() {
    setId(nextId++);
  }

  public SimpleFoo persistAndReturnSelf() {
    persist();
    return this;
  }

  public void setBarField(SimpleBar barField) {
    this.barField = barField;
  }

  /**
   * @param bigDecimalField the bigDecimalField to set
   */
  public void setBigDecimalField(BigDecimal bigDecimalField) {
    this.bigDecimalField = bigDecimalField;
  }

  /**
   * @param bigIntegerField the bigIntegerField to set
   */
  public void setBigIntField(BigInteger bigIntegerField) {
    this.bigIntField = bigIntegerField;
  }

  public void setBoolField(Boolean bool) {
    boolField = bool;
  }

  /**
   * @param byteField the byteField to set
   */
  public void setByteField(Byte byteField) {
    this.byteField = byteField;
  }

  /**
   * @param charField the charField to set
   */
  public void setCharField(Character charField) {
    this.charField = charField;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  /**
   * @param doubleField the doubleField to set
   */
  public void setDoubleField(Double doubleField) {
    this.doubleField = doubleField;
  }

  public void setEnumField(SimpleEnum enumField) {
    this.enumField = enumField;
  }

  /**
   * @param floatField the floatField to set
   */
  public void setFloatField(Float floatField) {
    this.floatField = floatField;
  }
  
  public void setFooField(SimpleFoo fooField) {
    this.fooField = fooField;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setIntId(Integer id) {
    this.intId = id;
  }

  public void setLongField(Long longField) {
    this.longField = longField;
  }

  /**
   * @param otherBoolField the otherBoolField to set
   */
  public void setOtherBoolField(Boolean otherBoolField) {
    this.otherBoolField = otherBoolField;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @param shortField the shortField to set
   */
  public void setShortField(Short shortField) {
    this.shortField = shortField;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
