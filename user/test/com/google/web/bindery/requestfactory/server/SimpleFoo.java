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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.requestfactory.shared.OnlyUsedByRequestContextMethod;
import com.google.web.bindery.requestfactory.shared.SimpleEnum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
  private static Map<Long, SimpleFoo> jreTestSingleton;

  private static Long nextId = 1L;

  public static Double add(double a, double b) {
    return a + b;
  }

  public static Integer add(int a, int b) {
    return a + b;
  }

  public static Long countSimpleFoo() {
    return (long) get().size();
  }

  public static SimpleFoo echo(SimpleFoo simpleFoo) {
    return simpleFoo;
  }

  public static SimpleFoo echoComplex(SimpleFoo simpleFoo, SimpleBar simpleBar) {
    simpleFoo.setBarField(simpleBar);
    return simpleFoo;
  }

  public static SimpleFoo fetchDoubleReference() {
    SimpleFoo foo = new SimpleFoo();
    SimpleFoo foo2 = new SimpleFoo();
    foo.setFooField(foo2);
    foo.setSelfOneToManyField(Arrays.asList(foo2));
    foo.persist();
    foo2.persist();
    return foo;
  }

  public static List<SimpleFoo> findAll() {
    return new ArrayList<SimpleFoo>(get().values());
  }

  public static SimpleFoo findSimpleFoo(Long id) {
    return findSimpleFooById(id);
  }

  public static SimpleFoo findSimpleFooById(Long id) {
    return get().get(id);
  }

  @SuppressWarnings("unchecked")
  public static synchronized Map<Long, SimpleFoo> get() {
    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req == null) {
      // May be in a JRE test case, use the singleton
      if (jreTestSingleton == null) {
        jreTestSingleton = resetImpl();
      }
      return jreTestSingleton;
    } else {
      /*
       * This will not behave entirely correctly unless we have a servlet filter
       * that doesn't allow any requests to be processed unless they're
       * associated with an existing session.
       */
      Map<Long, SimpleFoo> value =
          (Map<Long, SimpleFoo>) req.getSession().getAttribute(SimpleFoo.class.getCanonicalName());
      if (value == null) {
        value = resetImpl();
      }
      return value;
    }
  }

  public static List<SimpleFoo> getFlattenedTripletReference() {
    SimpleFoo foo1 = new SimpleFoo();
    SimpleFoo foo2 = new SimpleFoo();
    SimpleFoo foo3 = new SimpleFoo();
    foo1.setSelfOneToManyField(Arrays.asList(foo2));
    foo2.setSelfOneToManyField(Arrays.asList(foo3));
    foo1.persist();
    foo2.persist();
    foo3.persist();
    return Arrays.asList(foo1, foo2, foo3);
  }
  
  public static SimpleFoo getLongChain() {
    SimpleFoo foo0 = new SimpleFoo();
    SimpleFoo foo1 = new SimpleFoo();
    SimpleFoo foo2 = new SimpleFoo();
    SimpleFoo foo3 = new SimpleFoo();
    SimpleFoo foo4 = new SimpleFoo();
    SimpleFoo foo5 = new SimpleFoo();
    
    foo0.setSelfOneToManyField(Arrays.asList(foo1, foo2));
    foo0.setFooField(foo1);
    foo1.setFooField(foo2);
    foo2.setFooField(foo3);
    foo3.setFooField(foo4);
    foo4.setFooField(foo5);
    foo5.setFooField(foo5);
    
    foo0.persist();
    foo1.persist();
    foo2.persist();
    foo3.persist();
    foo4.persist();
    foo5.persist();
    
    return foo0;
  }
  
  public static SimpleFoo getNullInEntityList() {
    SimpleFoo foo0 = new SimpleFoo();
    SimpleFoo foo1 = new SimpleFoo();
    SimpleFoo foo2 = new SimpleFoo();
    SimpleFoo foo2FooField = new SimpleFoo();

    foo0.setSelfOneToManyField(Arrays.asList(foo1, null, foo2));

    foo1.setSelfOneToManyField(Arrays.asList(foo0));

    foo2.setSelfOneToManyField(Arrays.asList(foo0));
    foo2.setFooField(foo2FooField);
    foo2FooField.setFooField(foo0);
    
    foo0.persist();
    foo1.persist();
    foo2.persist();
    foo2FooField.persist();
    return foo0;
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

  public static SimpleFoo getSimpleFooWithNullRelationship() {
    SimpleFoo foo = new SimpleFoo();
    foo.persist();
    return foo;
  }

  /**
   * This tests that the server detects and disallows the use of persisted
   * objects with a null version property.
   */
  public static SimpleFoo getSimpleFooWithNullVersion() {
    System.err.println("The following exception about an entity with a null"
        + " version is expected");
    SimpleFoo foo = new SimpleFoo();
    foo.setVersion(null);
    return foo;
  }

  public static SimpleFoo getSimpleFooWithSubPropertyCollection() {
    SimpleFoo foo = new SimpleFoo();
    SimpleFoo subFoo = new SimpleFoo();
    SimpleFoo subSubFoo = new SimpleFoo();
    subFoo.setFooField(subSubFoo);
    subSubFoo.setUserName("I'm here");
    subSubFoo.persist();
    subFoo.persist();
    foo.persist();
    foo.setSelfOneToManyField(Arrays.asList(subFoo));
    return foo;
  }

  public static SimpleFoo getTripletReference() {
    SimpleFoo foo1 = new SimpleFoo();
    SimpleFoo foo2 = new SimpleFoo();
    SimpleFoo foo3 = new SimpleFoo();
    ArrayList<SimpleFoo> foos = new ArrayList<SimpleFoo>();
    foos.add(foo2);
    ArrayList<SimpleFoo> subFoos = new ArrayList<SimpleFoo>();
    subFoos.add(foo3);
    foo1.setSelfOneToManyField(foos);
    foo2.setSelfOneToManyField(subFoos);
    foo3.setFooField(foo2);
    foo1.persist();
    foo2.persist();
    foo3.persist();
    return foo1;
  }

  public static SimpleFoo getUnpersistedInstance() {
    SimpleFoo foo = new SimpleFoo();
    foo.setUnpersisted(true);
    return foo;
  }

  public static void pleaseCrash(Integer crashIf42or43) throws Exception {
    if (crashIf42or43 == 42) {
      throw new UnsupportedOperationException("THIS EXCEPTION IS EXPECTED BY A TEST");
    }
    if (crashIf42or43 == 43) {
      throw new Exception("THIS EXCEPTION IS EXPECTED BY A TEST");
    }
  }

  /**
   * Check client-side upcasting to BigDecimal and return a list of BigDecimals
   * that should be upcast.
   */
  public static List<BigDecimal> processBigDecimalList(List<BigDecimal> values) {
    List<BigDecimal> toReturn = new ArrayList<BigDecimal>();
    toReturn.add(BigDecimal.TEN);
    toReturn.add(new BigDecimal("12345.6789") {
      // This is an anonymous subtype
    });
    if (!toReturn.equals(values)) {
      throw new IllegalArgumentException(toReturn + " != " + values);
    }
    return toReturn;
  }

  /**
   * Check client-side upcasting to BigInteger and return a list of BigIntegers
   * that should be upcast.
   */
  public static List<BigInteger> processBigIntegerList(List<BigInteger> values) {
    List<BigInteger> toReturn = new ArrayList<BigInteger>();
    toReturn.add(BigInteger.TEN);
    toReturn.add(new BigInteger("12345") {
      // This is an anonymous subtype
    });
    if (!toReturn.equals(values)) {
      throw new IllegalArgumentException(toReturn + " != " + values);
    }
    return toReturn;
  }

  public static Boolean processBooleanList(List<Boolean> values) {
    return values.get(0);
  }

  /**
   * Check client-side upcasting to Date and return a list of Dates that should
   * be upcast.
   */
  @SuppressWarnings("deprecation")
  public static List<Date> processDateList(List<Date> values) {
    // Keep these values in sync with SimpleFoo.processDateList
    Date date = new Date(90, 0, 1);
    java.sql.Date sqlDate = new java.sql.Date(90, 0, 2);
    Time sqlTime = new Time(1, 2, 3);
    Timestamp sqlTimestamp = new Timestamp(12345L);
    List<Date> toReturn = Arrays.asList(date, sqlDate, sqlTime, sqlTimestamp);

    if (toReturn.size() != values.size()) {
      throw new IllegalArgumentException("size");
    }

    Iterator<Date> expected = toReturn.iterator();
    Iterator<Date> actual = values.iterator();
    while (expected.hasNext()) {
      Date expectedDate = expected.next();
      long expectedTime = expectedDate.getTime();
      long actualTime = actual.next().getTime();
      if (expectedTime != actualTime) {
        throw new IllegalArgumentException(expectedDate.getClass().getName() + " " + expectedTime
            + " != " + actualTime);
      }
    }

    return toReturn;
  }

  public static SimpleEnum processEnumList(List<SimpleEnum> values) {
    return values.get(0);
  }

  public static String processString(String string) {
    return string;
  }

  public static void receiveEnum(OnlyUsedByRequestContextMethod value) {
    if (value != OnlyUsedByRequestContextMethod.FOO) {
      throw new IllegalArgumentException("Expecting FOO, received " + value);
    }
  }

  public static void receiveNullList(List<SimpleFoo> value) {
    if (value != null) {
      throw new IllegalArgumentException("Expected value to be null. Actual value: \"" + value
          + "\"");
    }
  }

  public static void receiveNullSimpleFoo(SimpleFoo value) {
    if (value != null) {
      throw new IllegalArgumentException("Expected value to be null. Actual value: \"" + value
          + "\"");
    }
  }

  public static void receiveNullString(String value) {
    if (value != null) {
      throw new IllegalArgumentException("Expected value to be null. Actual value: \"" + value
          + "\"");
    }
  }

  public static void receiveNullValueInEntityList(List<SimpleFoo> list) {
    if (list == null) {
      throw new IllegalArgumentException("Expected list to be non null.");
    } else if (list.size() != 2) {
      throw new IllegalArgumentException("Expected list to contain two items.");
    } else if (list.get(0) == null) {
      throw new IllegalArgumentException("Expected list.get(0) to return non null.");
    } else if (list.get(1) != null) {
      throw new IllegalArgumentException("Expected list.get(1) to return null. Actual: "
          + list.get(1));
    }
  }

  public static void receiveNullValueInIntegerList(List<Integer> list) {
    if (list == null) {
      throw new IllegalArgumentException("Expected list to be non null.");
    } else if (list.size() != 3) {
      throw new IllegalArgumentException("Expected list to contain three items.");
    } else if (list.get(0) == null || list.get(1) == null) {
      throw new IllegalArgumentException("Expected list.get(0)/get(1) to return non null.");
    } else if (list.get(2) != null) {
      throw new IllegalArgumentException("Expected list.get(2) to return null. Actual: \""
          + list.get(2) + "\"");
    }
  }

  public static void receiveNullValueInStringList(List<String> list) {
    if (list == null) {
      throw new IllegalArgumentException("Expected list to be non null.");
    } else if (list.size() != 3) {
      throw new IllegalArgumentException("Expected list to contain three items.");
    } else if (list.get(0) == null || list.get(1) == null) {
      throw new IllegalArgumentException("Expected list.get(0)/get(1) to return non null.");
    } else if (list.get(2) != null) {
      throw new IllegalArgumentException("Expected list.get(2) to return null. Actual: \""
          + list.get(2) + "\"");
    }
  }

  public static void reset() {
    resetImpl();
  }

  public static synchronized Map<Long, SimpleFoo> resetImpl() {
    // NOTE: Must be reset before instantiating new SimpleFoos.
    SimpleBar.reset();

    Map<Long, SimpleFoo> instance = new HashMap<Long, SimpleFoo>();
    // fixtures
    SimpleFoo s1 = new SimpleFoo();
    s1.setId(1L);
    s1.isNew = false;
    instance.put(s1.getId(), s1);

    SimpleFoo s2 = new SimpleFoo();
    s2.setId(999L);
    s2.isNew = false;
    instance.put(s2.getId(), s2);

    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req == null) {
      jreTestSingleton = instance;
    } else {
      req.getSession().setAttribute(SimpleFoo.class.getCanonicalName(), instance);
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

  public static void returnOnlyUsedInParameterization(List<SimpleFoo> values) {
  }

  public static SimpleFoo returnSimpleFooSubclass() {
    return new SimpleFoo() {
    };
  }

  public static List<SimpleValue> returnValueProxies() {
    List<SimpleValue> toReturn = new ArrayList<SimpleValue>(2);
    for (int i = 0; i < 2; i++) {
      SimpleValue value = returnValueProxy();
      SimpleFoo foo = new SimpleFoo();
      SimpleFoo subFoo = new SimpleFoo();
      foo.setFooField(subFoo);
      value.setSimpleFoo(foo);
      toReturn.add(value);
      foo.persist();
      subFoo.persist();
    }
    return toReturn;
  }

  public static SimpleValue returnValueProxy() {
    SimpleValue toReturn = new SimpleValue();
    toReturn.setNumber(42);
    toReturn.setString("Hello world!");
    toReturn.setDate(new Date());
    return toReturn;
  }

  @SuppressWarnings("unused")
  private static Integer privateMethod() {
    return 0;
  }

  Integer version = 1;

  private Long id = 1L;
  private boolean isNew = true;

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

  private SimpleValue simpleValueField;
  private List<SimpleValue> simpleValuesField;

  /*
   * isChanged is just a quick-and-dirty way to get version-ing for now.
   * Currently, only set by setUserName and setIntId. TODO for later: Use a
   * cleaner solution to figure out when to increment version numbers.
   */
  boolean isChanged;

  private boolean unpersisted;

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
    isChanged = false;
  }

  public Long countSimpleFooWithUserNameSideEffect() {
    findSimpleFoo(1L).setUserName(userName);
    version++;
    return countSimpleFoo();
  }

  public void deleteBar() {
    if (barField != null) {
      isChanged = true;
      barField.delete();
    }
    barField = null;
    persist();
  }

  public SimpleBar getBarField() {
    return barField;
  }

  public SimpleBar getBarNullField() {
    return barNullField;
  }

  /**
   * Returns the bigDecimalField.
   */
  public BigDecimal getBigDecimalField() {
    return bigDecimalField;
  }

  /**
   * Returns the bigIntegerField.
   */
  public BigInteger getBigIntField() {
    return bigIntField;
  }

  public Boolean getBoolField() {
    return boolField;
  }

  /**
   * Returns the byteField.
   */
  public Byte getByteField() {
    return byteField;
  }

  /**
   * Returns the charField.
   */
  public Character getCharField() {
    return charField;
  }

  public Date getCreated() {
    return created;
  }

  /**
   * Returns the doubleField.
   */
  public Double getDoubleField() {
    return doubleField;
  }

  public SimpleEnum getEnumField() {
    return enumField;
  }

  /**
   * Returns the floatField.
   */
  public Float getFloatField() {
    return floatField;
  }

  public SimpleFoo getFooField() {
    return fooField;
  }

  public Long getId() {
    return unpersisted ? null : id;
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
   * Returns the otherBoolField.
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
   * Returns the shortField.
   */
  public Short getShortField() {
    return shortField;
  }

  public SimpleValue getSimpleValue() {
    return simpleValueField;
  }

  public List<SimpleValue> getSimpleValues() {
    return simpleValuesField;
  }

  public boolean getUnpersisted() {
    return unpersisted;
  }

  public String getUserName() {
    return userName;
  }

  public Integer getVersion() {
    return unpersisted ? null : version;
  }

  public String hello(SimpleBar bar) {
    return "Greetings " + bar.getUserName() + " from " + getUserName();
  }

  public void persist() {
    if (isNew) {
      setId(nextId++);
      isNew = false;
      get().put(getId(), this);
    }
    if (isChanged) {
      version++;
      isChanged = false;
    }
  }

  public SimpleFoo persistAndReturnSelf() {
    persist();
    return this;
  }

  public SimpleFoo persistCascadingAndReturnSelf() {
    persistCascadingAndReturnSelfImpl(new HashSet<SimpleFoo>());
    return this;
  }

  public String processList(List<SimpleFoo> values) {
    String result = "";
    for (SimpleFoo n : values) {
      result += n.getUserName();
    }
    return result;
  }

  public void receiveNull(String value) {
    if (value != null) {
      throw new IllegalArgumentException("Expected value to be null. Actual value: \"" + value
          + "\"");
    }
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
    if (!this.intId.equals(id)) {
      this.intId = id;
      isChanged = true;
    }
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
    pleaseCrash(crashIf42or43);
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

  public void setSimpleValue(SimpleValue simpleValueField) {
    this.simpleValueField = simpleValueField;
  }

  public void setSimpleValues(List<SimpleValue> simpleValueField) {
    this.simpleValuesField = simpleValueField;
  }

  public void setUnpersisted(boolean unpersisted) {
    this.unpersisted = unpersisted;
  }

  public void setUserName(String userName) {
    if (!this.userName.equals(userName)) {
      this.userName = userName;
      isChanged = true;
    }
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

  /**
   * Persist this entity and all child entities. This method can handle loops.
   * 
   * @param processed the entities that have been processed
   */
  private void persistCascadingAndReturnSelfImpl(Set<SimpleFoo> processed) {
    if (processed.contains(this)) {
      return;
    }

    // Persist this entity.
    processed.add(this);
    persist();

    // Persist SimpleBar children.
    // We don't need to keep track of the processed SimpleBars because persist()
    // is a no-op if the SimpleBar has already been persisted.
    if (barField != null) {
      barField.persist();
    }
    if (barNullField != null) {
      barNullField.persist();
    }
    if (oneToManySetField != null) {
      for (SimpleBar child : oneToManySetField) {
        if (child != null) {
          child.persist();
        }
      }
    }
    if (oneToManyField != null) {
      for (SimpleBar child : oneToManyField) {
        if (child != null) {
          child.persist();
        }
      }
    }

    // Persist SimpleFoo children.
    if (fooField != null) {
      fooField.persistCascadingAndReturnSelfImpl(processed);
    }
    if (selfOneToManyField != null) {
      for (SimpleFoo child : selfOneToManyField) {
        if (child != null) {
          child.persistCascadingAndReturnSelfImpl(processed);
        }
      }
    }
  }
}
