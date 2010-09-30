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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Size;

/**
 * Domain object for SimpleFooRequest.
 */
public class SimpleFoo {
  /**
   * DO NOT USE THIS UGLY HACK DIRECTLY! Call {@link #get} instead.
   */
  private static SimpleFoo jreTestSingleton = new SimpleFoo();

  private static Long nextId = 1L;

  public static Long countSimpleFoo() {
    return 1L;
  }

  public static SimpleFoo echo(SimpleFoo simpleFoo) {
    return simpleFoo;
  }

  public static SimpleFoo echoComplex(SimpleFoo simpleFoo, SimpleBar simpleBar) {
    simpleFoo.setBarField(simpleBar);
    return simpleFoo;
  }

  public static List<SimpleFoo> findAll() {
    return Collections.singletonList(get());
  }

  public static SimpleFoo findSimpleFoo(Long id) {
    return findSimpleFooById(id);
  }

  public static SimpleFoo findSimpleFooById(Long id) {
    get().setId(id);
    return get();
  }

  public static synchronized SimpleFoo get() {
    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req == null) {
      // May be in a JRE test case, use the singleton
      return jreTestSingleton;
    } else {
      /*
       * This will not behave entirely correctly unless we have a servlet filter
       * that doesn't allow any requests to be processed unless they're
       * associated with an existing session.
       */
      SimpleFoo value = (SimpleFoo) req.getSession().getAttribute(
          SimpleFoo.class.getCanonicalName());
      if (value == null) {
        value = reset();
      }
      return value;
    }
  }

  public static List<Integer> getNumberList() {
    ArrayList<Integer> list = new ArrayList<Integer>();
    list.add(1);
    list.add(2);
    list.add(3);
    return list;
  }

  public static Set<Integer> getNumberSet() {
    Set<Integer> list = new HashSet<Integer>();
    list.add(1);
    list.add(2);
    list.add(3);
    return list;
  }

  public static SimpleFoo getSingleton() {
    return get();
  }

  public static Boolean processBooleanList(List<Boolean> values) {
    return values.get(0);
  }

  public static Date processDateList(List<Date> values) {
    return values.get(0);
  }

  public static SimpleEnum processEnumList(List<SimpleEnum> values) {
    return values.get(0);
  }

  public static String processString(String string) {
    return string;
  }

  public static synchronized SimpleFoo reset() {
    SimpleFoo instance = new SimpleFoo();
    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req == null) {
      jreTestSingleton = instance;
    } else {
      req.getSession().setAttribute(SimpleFoo.class.getCanonicalName(),
          instance);
    }
    return instance;
  }

  public static List<SimpleFoo> returnNullList() {
    return null;
  }

  public static SimpleFoo returnNullSimpleFoo() {
    return null;
  }

  public static String returnNullString() {
    return null;
  }

  @SuppressWarnings("unused")
  private static Integer privateMethod() {
    return 0;
  }

  Integer version = 1;

  @Id
  private Long id = 1L;

  @Size(min = 3, max = 30)
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
  private Integer pleaseCrash;

  private SimpleBar barField;
  private SimpleFoo fooField;

  private String nullField;
  private SimpleBar barNullField;

  private List<SimpleBar> oneToManyField;
  private List<SimpleFoo> selfOneToManyField;
  private Set<SimpleBar> oneToManySetField;

  private List<Integer> numberListField;

  public SimpleFoo() {
    intId = 42;
    version = 1;
    userName = "GWT";
    longField = 8L;
    enumField = SimpleEnum.FOO;
    created = new Date();
    barField = SimpleBar.getSingleton();
    boolField = true;
    oneToManyField = new ArrayList<SimpleBar>();
    oneToManyField.add(barField);
    oneToManyField.add(barField);
    numberListField = new ArrayList<Integer>();
    numberListField.add(42);
    numberListField.add(99);
    selfOneToManyField = new ArrayList<SimpleFoo>();
    selfOneToManyField.add(this);
    oneToManySetField = new HashSet<SimpleBar>();
    oneToManySetField.add(barField);
    nullField = null;
    barNullField = null;
    pleaseCrash = 0;
  }

  public Long countSimpleFooWithUserNameSideEffect() {
    get().setUserName(userName);
    return 1L;
  }

  public SimpleBar getBarField() {
    return barField;
  }

  public SimpleBar getBarNullField() {
    return barNullField;
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

  public String getNullField() {
    return nullField;
  }

  public List<Integer> getNumberListField() {
    return numberListField;
  }

  public List<SimpleBar> getOneToManyField() {
    return oneToManyField;
  }

  public Set<SimpleBar> getOneToManySetField() {
    return oneToManySetField;
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

  public Integer getPleaseCrash() {
    return pleaseCrash;
  }

  public List<SimpleFoo> getSelfOneToManyField() {
    return selfOneToManyField;
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

  public String processList(List<SimpleFoo> values) {
    String result = "";
    for (SimpleFoo n : values) {
      result += n.getUserName();
    }
    return result;
  }

  public void setBarField(SimpleBar barField) {
    this.barField = barField;
  }

  public void setBarNullField(SimpleBar barNullField) {
    this.barNullField = barNullField;
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

  public void setNullField(String nullField) {
    this.nullField = nullField;
  }

  public void setNumberListField(List<Integer> numberListField) {
    this.numberListField = numberListField;
  }

  public void setOneToManyField(List<SimpleBar> oneToManyField) {
    this.oneToManyField = oneToManyField;
  }

  public void setOneToManySetField(Set<SimpleBar> oneToManySetField) {
    this.oneToManySetField = oneToManySetField;
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

  public void setPleaseCrash(Integer crashIf42or43) throws Exception {
    if (crashIf42or43 == 42) {
      throw new UnsupportedOperationException(
          "THIS EXCEPTION IS EXPECTED BY A TEST");
    }
    if (crashIf42or43 == 43) {
      throw new Exception("THIS EXCEPTION IS EXPECTED BY A TEST");
    }
    pleaseCrash = crashIf42or43;
  }

  public void setSelfOneToManyField(List<SimpleFoo> selfOneToManyField) {
    this.selfOneToManyField = selfOneToManyField;
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

  public Integer sum(List<Integer> values) {
    int sum = 0;
    for (int n : values) {
      sum += n;
    }
    return sum;
  }
}
