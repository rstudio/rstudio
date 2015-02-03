// $Id: Configuration.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
// Changed by Google
package javax.validation;


/**
 * Receives configuration information, selects the appropriate
 * Bean Validation provider and builds the appropriate <code>ValidatorFactory</code>.
 * <p/>
 * Usage:
 * <pre>
 * {@code
 * Configuration<?> configuration = //provided by one of the Validation bootstrap methods
 *     ValidatorFactory = configuration
 *         .messageInterpolator( new CustomMessageInterpolator() )
 *         .buildValidatorFactory();}
 * </pre>
 * <p/>
 * By default, the configuration information is retrieved from
 * <i>META-INF/validation.xml</i>.
 * It is possible to override the configuration retrieved from the XML file
 * by using one or more of the <code>Configuration</code> methods.
 * <p/>
 * The {@link ValidationProviderResolver} is specified at configuration time
 * (see {@link javax.validation.spi.ValidationProvider}).
 * If none is explicitly requested, the default <code>ValidationProviderResolver</code> is used.
 * <p/>
 * The provider is selected in the following way:
 * <ul>
 * <li>if a specific provider is requested programmatically using
 * <code>Validation.byProvider(Class)</code>, find the first provider implementing
 * the provider class requested and use it</li>
 * <li>if a specific provider is requested in <i>META-INF/validation.xml</i>,
 * find the first provider implementing the provider class requested and use it</li>
 * <li>otherwise, use the first provider returned by the <code>ValidationProviderResolver<code></li>
 * </ul>
 * <p/>
 * Implementations are not meant to be thread-safe.
 *
 * @author Emmanuel Bernard
 */
public interface Configuration<T extends Configuration<T>> {

  /**
   * Ignore data from the <i>META-INF/validation.xml</i> file if this
   * method is called.
   * This method is typically useful for containers that parse
   * <i>META-INF/validation.xml</i> themselves and pass the information
   * via the <code>Configuration</code> methods.
   *
   * @return <code>this</code> following the chaining method pattern.
   */
  T ignoreXmlConfiguration();

  /**
   * Defines the message interpolator used. Has priority over the configuration
   * based message interpolator.
   * If <code>null</code> is passed, the default message interpolator is used
   * (defined in XML or the specification default).
   *
   * @param interpolator message interpolator implementation.
   *
   * @return <code>this</code> following the chaining method pattern.
   */
  T messageInterpolator(MessageInterpolator interpolator);

  /**
   * Defines the traversable resolver used. Has priority over the configuration
   * based traversable resolver.
   * If <code>null</code> is passed, the default traversable resolver is used
   * (defined in XML or the specification default).
   *
   * @param resolver traversable resolver implementation.
   *
   * @return <code>this</code> following the chaining method pattern.
   */
  T traversableResolver(TraversableResolver resolver);

  /**
   * Defines the constraint validator factory. Has priority over the configuration
   * based constraint factory.
   * If null is passed, the default constraint validator factory is used
   * (defined in XML or the specification default).
   *
   * @param constraintValidatorFactory constraint factory inmplementation.
   *
   * @return <code>this</code> following the chaining method pattern.
   */
  T constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory);

  /**
   * Add a stream describing constraint mapping in the Bean Validation
   * XML format.
   * <p/>
   * The stream should be closed by the client API after the
   * <code>ValidatorFactory</code> has been built. The Bean Validation provider
   * must not close the stream.
   *
   * @param stream XML mapping stream.
   *
   * @return <code>this</code> following the chaining method pattern.
   * @throws IllegalArgumentException if <code>stream</code> is null
   */
   T addMapping(String stream);

  /**
   * Add a provider specific property. This property is equivalent to
   * XML configuration properties.
   * If the underlying provider does not know how to handle the property,
   * it must silently ignore it.
   * <p/>
   * Note: Using this non type-safe method is generally not recommended.
   * <p/>
   * It is more appropriate to use, if available, the type-safe equivalent provided
   * by a specific provider via its <code>Configuration<code> subclass.
   * <code>ValidatorFactory factory = Validation.byProvider(ACMEPrivoder.class)
   * .configure()
   * .providerSpecificProperty(ACMEState.FAST)
   * .buildValidatorFactory();
   * </code>
   * This method is typically used by containers parsing <i>META-INF/validation.xml</i>
   * themselves and injecting the state to the Configuration object.
   * <p/>
   * If a property with a given name is defined both via this method and in the
   * XML configuration, the value set programmatically has priority.
   *
   * If null is passed as a value, the value defined in XML is used. If no value
   * is defined in XML, the property is considered unset.
   *
   * @param name property name.
   * @param value property value.
   * @return <code>this</code> following the chaining method pattern.
   *
   * @throws IllegalArgumentException if <code>name</code> is null
   */
  T addProperty(String name, String value);

  /**
   * Return an implementation of the <code>MessageInterpolator</code> interface
   * following the default <code>MessageInterpolator</code> defined in the
   * specification:
   * <ul>
   * <li>use the ValidationMessages resource bundle to load keys</li>
   * <li>use Locale.getDefault()</li>
   * </ul>
   *
   * @return default MessageInterpolator implementation compliant with the specification
   */
  MessageInterpolator getDefaultMessageInterpolator();

  /**
   * Return an implementation of the <code>TraversableResolver</code> interface
   * following the default <code>TraversableResolver</code> defined in the
   * specification:
   * <ul>
   * <li>if Java Persistence is available in the runtime environment, 
   * a property is considered reachable if Java Persistence considers
   * the property as loaded</li>
   * <li>if Java Persistence is not available in the runtime environment,
   * all properties are considered reachable</li>
   * <li>all properties are considered cascadable.</li>
   * </ul>
   *
   * @return default TraversableResolver implementation compliant with the specification
   */
  TraversableResolver getDefaultTraversableResolver();

  /**
   * Return an implementation of the <code>ConstraintValidatorFactory</code> interface
   * following the default <code>ConstraintValidatorFactory</code> defined in the
   * specification:
   * <ul>
   * <li>uses the public no-arg constructor of the <code>ConstraintValidator</code></li>
   * </ul>
   *
   * @return default ConstraintValidatorFactory implementation compliant with the specification
   */
  ConstraintValidatorFactory getDefaultConstraintValidatorFactory();

  /**
   * Build a <code>ValidatorFactory</code> implementation.
   *
   * @return ValidatorFactory
   * @throws ValidationException if the ValidatorFactory cannot be built
   */
  ValidatorFactory buildValidatorFactory();
}
