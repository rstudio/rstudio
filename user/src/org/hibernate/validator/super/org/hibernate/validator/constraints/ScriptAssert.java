// $Id: ScriptAssert.java 19327 2010-04-30 08:11:15Z hardy.ferentschik $
/*
 * JBoss, Home of Professional Open Source Copyright 2010, Red Hat, Inc. and/or
 * its affiliates, and individual contributors by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of individual
 * contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
// MODIFIED BY GOOGLE
package org.hibernate.validator.constraints;

import org.hibernate.validator.constraints.impl.ScriptAssertValidator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintDeclarationException;
import javax.validation.Payload;

/**
 * <p>
 * A class-level constraint, that evaluates a script expression against the
 * annotated element. This constraint can be used to implement validation
 * routines, that depend on multiple attributes of the annotated element.
 * </p>
 * <p>
 * For evaluation of expressions the Java Scripting API as defined by <a
 * href="http://jcp.org/en/jsr/detail?id=223">JSR 223</a>
 * ("Scripting for the Java<sup>TM</sup> Platform") is used. Therefore an
 * implementation of that API must part of the class path. This is automatically
 * the case when running on Java 6. For older Java versions, the JSR 223 RI can
 * be added manually to the class path.
 * </p>
 * The expressions to be evaluated can be written in any scripting or expression
 * language, for which a JSR 223 compatible engine can be found in the class
 * path. The following listing shows an example using the JavaScript engine,
 * which comes with Java 6: </p>
 * <p/>
 * 
 * <pre>
 * &#064;ScriptAssert(lang = &quot;javascript&quot;, script = &quot;_this.startDate.before(_this.endDate)&quot;)
 * public class CalendarEvent {
 * <p/>
 * 	private Date startDate;
 * <p/>
 * 	private Date endDate;
 * <p/>
 * 	//...
 * <p/>
 * }
 * </pre>
 * <p>
 * Using a real expression language in conjunction with a shorter object alias
 * allows for very compact expressions:
 * </p>
 * 
 * <pre>
 * &#064;ScriptAssert(lang = &quot;jexl&quot;, script = &quot;_.startDate &lt; _.endDate&quot;, alias = &quot;_&quot;)
 * public class CalendarEvent {
 * <p/>
 * 	private Date startDate;
 * <p/>
 * 	private Date endDate;
 * <p/>
 * 	//...
 * <p/>
 * }
 * </pre>
 * <p>
 * Accepts any type.
 * </p>
 * 
 * @author Gunnar Morling
 */
@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ScriptAssertValidator.class)
@Documented
public @interface ScriptAssert {

  String message() default "{org.hibernate.validator.constraints.ScriptAssert.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /**
   * @return The name of the script language used by this constraint as expected
   *         by the JSR 223 {@link javax.script.ScriptEngineManager}. A
   *         {@link ConstraintDeclarationException} will be thrown upon script
   *         evaluation, if no engine for the given language could be found.
   */
  String lang();

  /**
   * @return The script to be executed. The script must return
   *         <code>Boolean.TRUE</code>, if the annotated element could
   *         successfully be validated, otherwise <code>Boolean.FALSE</code>.
   *         Returning null or any type other than Boolean will cause a
   *         {@link ConstraintDeclarationException} upon validation. Any
   *         exception occurring during script evaluation will be wrapped into a
   *         ConstraintDeclarationException, too. Within the script, the
   *         validated object can be accessed from the
   *         {@link javax.script.ScriptContext script context} using the name
   *         specified in the <code>alias</code> attribute.
   */
  String script();

  /**
   * @return The name, under which the annotated element shall be registered
   *         within the script context. Defaults to "_this".
   */
  String alias() default "_this";

  /**
   * Defines several {@code @ScriptAssert} annotations on the same element.
   */
  @Target({TYPE})
  @Retention(RUNTIME)
  @Documented
  public @interface List {
    ScriptAssert[] value();
  }
}
