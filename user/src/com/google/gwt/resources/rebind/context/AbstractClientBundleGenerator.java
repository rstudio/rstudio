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
package com.google.gwt.resources.rebind.context;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.CachedGeneratorResult;
import com.google.gwt.core.ext.CachedPropertyInformation;
import com.google.gwt.core.ext.Generator.RunsLocal;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ClientBundleWithLookup;
import com.google.gwt.resources.client.ResourcePrototype;
import com.google.gwt.resources.ext.ClientBundleFields;
import com.google.gwt.resources.ext.ClientBundleRequirements;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGenerator;
import com.google.gwt.resources.ext.ResourceGeneratorType;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.resources.ext.SupportsGeneratorResultCaching;
import com.google.gwt.resources.rg.BundleResourceGenerator;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.beans.Beans;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The base class for creating new ClientBundle implementations.
 * <p>
 * The general structure of the generated class is as follows:
 *
 * <pre>
 * private void resourceInitializer() {
 *   resource = new Resource();
 * }
 * private static class cellTreeClosedItemInitializer {
 *   // Using a static initializer so the compiler can optimize clinit calls.
 *   // Refers back to an instance method. See comment below.
 *   static {
 *     _instance0.resourceInitializer();
 *   }
 *   static ResourceType get() {
 *     return resource;
 *   }
 * }
 * public ResourceType resource() {
 *   return cellTreeClosedItemInitializer.get();
 * }
 * // Other ResourceGenerator-defined fields
 * private static ResourceType resource;
 * private static HashMap&lt;String, ResourcePrototype&gt; resourceMap;
 * public ResourcePrototype[] getResources() {
 *   return new ResourcePrototype[] { resource() };
 * }
 * public ResourcePrototype getResource(String name) {
 *   if (GWT.isScript()) {
 *     return getResourceNative(name);
 *   } else {
 *     if (resourceMap == null) {
 *       resourceMap = new HashMap<String, ResourcePrototype>();
 *       resourceMap.put("resource", resource());
 *     }
 *     return resourceMap.get(name);
 *   }
 * }
 * private native ResourcePrototype getResourceNative(String name) /-{
 *   switch (name) {
 *     case 'resource': return this.@...::resource()();
 *   }
 *   return null;
 * }-/
 * </pre>
 * The instantiation of the individual ResourcePrototypes is done in the content
 * of an instance of the ClientBundle type so that resources can refer to one
 * another by simply emitting a call to <code>resource()</code>.
 */
@RunsLocal(requiresProperties = RunsLocal.ALL)
public abstract class AbstractClientBundleGenerator extends IncrementalGenerator {
  private static final String CACHED_PROPERTY_INFORMATION = "cached-property-info";
  private static final String CACHED_RESOURCE_INFORMATION = "cached-resource-info";
  private static final String CACHED_TYPE_INFORMATION = "cached-type-info";
  private static final String INSTANCE_NAME = "_instance0";

  /*
   * A version id. Increment this as needed, when structural changes are made to
   * the generated output, specifically with respect to it's effect on the
   * caching and reuse of previous generator results. Previously cached
   * generator results will be invalidated automatically if they were generated
   * by a version of this generator with a different version id.
   */
  private static final long GENERATOR_VERSION_ID = 1L;

  /**
   * An implementation of ClientBundleFields.
   */
  protected static class FieldsImpl implements ClientBundleFields {
    private final NameFactory factory = new NameFactory();
    /**
     * It is necessary to maintain order in case one field refers to another.
     */
    private final Map<String, String> fieldsToDeclarations = new LinkedHashMap<String, String>();
    private final Map<String, String> fieldsToInitializers = new HashMap<String, String>();

    @Override
    public String define(JType type, String name) {
      return define(type, name, null, true, false);
    }

    @Override
    public String define(JType type, String name, String initializer,
        boolean isStatic, boolean isFinal) {

      assert Util.isValidJavaIdent(name) : name
          + " is not a valid Java identifier";

      String ident = factory.createName(name);

      StringBuilder sb = new StringBuilder();
      sb.append("private ");

      if (isStatic) {
        sb.append("static ");
      }

      if (isFinal) {
        sb.append("final ");
      }

      sb.append(type.getParameterizedQualifiedSourceName());
      sb.append(" ");
      sb.append(ident);

      fieldsToDeclarations.put(ident, sb.toString());

      if (initializer != null) {
        fieldsToInitializers.put(ident, initializer);
      }

      return ident;
    }

    /**
     * This can be called to reset the initializer expression on an
     * already-defined field.
     *
     * @param ident an identifier previously returned by {@link #define}
     * @param initializer a Java expression that will be used to initialize the
     *          field
     */
    public void setInitializer(String ident, String initializer) {
      assert fieldsToDeclarations.containsKey(ident) : ident + " not defined";
      fieldsToInitializers.put(ident, initializer);
    }

    private String getCode() {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, String> entry : fieldsToDeclarations.entrySet()) {
        String ident = entry.getKey();
        sb.append(entry.getValue());

        String initializer = fieldsToInitializers.get(ident);
        if (initializer != null) {
          sb.append(" = ").append(initializer);
        }
        sb.append(";\n");
      }
      return sb.toString();
    }
  }

  private static class RequirementsImpl implements ClientBundleRequirements {
    private final Set<String> axes;
    private boolean axesLocked = false;
    private final boolean canBeCacheable;
    private final Set<String> configProps;
    private final PropertyOracle propertyOracle;
    private final Map<String, URL> resolvedResources;
    private final Set<JClassType> types;

    public RequirementsImpl(PropertyOracle propertyOracle, boolean canBeCacheable) {
      this.propertyOracle = propertyOracle;
      this.canBeCacheable = canBeCacheable;

      // always need to track permuationAxes
      axes = new HashSet<String>();

      // only need to track these if generator caching is a possibility
      if (canBeCacheable) {
        configProps = new HashSet<String>();
        types = new HashSet<JClassType>();
        resolvedResources = new HashMap<String, URL>();
      } else {
        configProps = null;
        types = null;
        resolvedResources = null;
      }
    }

    @Override
    public void addConfigurationProperty(String propertyName)
        throws BadPropertyValueException {

      if (!canBeCacheable) {
        return;
      }
      // Ensure the property exists
      propertyOracle.getConfigurationProperty(propertyName).getValues();
      configProps.add(propertyName);
    }

    @Override
    public void addPermutationAxis(String propertyName)
        throws BadPropertyValueException {

      if (axes.contains(propertyName)) {
        return;
      }

      // Ensure adding of permutationAxes has not been locked
      if (axesLocked) {
        throw new IllegalStateException(
            "addPermutationAxis failed, axes have been locked");
      }

      // Ensure the property exists and add a permutation axis if the
      // property is a deferred binding property.
      try {
        propertyOracle.getSelectionProperty(
            TreeLogger.NULL, propertyName).getCurrentValue();
        axes.add(propertyName);
      } catch (BadPropertyValueException e) {
        addConfigurationProperty(propertyName);
      }
    }

    @Override
    public void addResolvedResource(String partialPath, URL resolvedResourceUrl) {
      if (!canBeCacheable) {
        return;
      }
      resolvedResources.put(partialPath, resolvedResourceUrl);
    }

    @Override
    public void addTypeHierarchy(JClassType type) {
      if (!canBeCacheable) {
        return;
      }
      if (!types.add(type)) {
        return;
      }
      Set<? extends JClassType> superTypes = type.getFlattenedSupertypeHierarchy();
      types.addAll(superTypes);
    }

    public Collection<String> getConfigurationPropertyNames() {
      if (!canBeCacheable) {
        return null;
      }
      return configProps;
    }

    public Collection<String> getPermutationAxes() {
      return axes;
    }

    public Map<String, URL> getResolvedResources() {
      if (!canBeCacheable) {
        return null;
      }
      return resolvedResources;
    }

    public Map<String, Long> getTypeLastModifiedTimes() {
      if (!canBeCacheable) {
        return null;
      }
      Map<String, Long> typeLastModifiedTimeMap = new HashMap<String, Long>();
      for (JClassType type : types) {
        String typeName = type.getQualifiedSourceName();
        assert type instanceof JRealClassType;
        JRealClassType sourceRealType = (JRealClassType) type;
        typeLastModifiedTimeMap.put(typeName, sourceRealType.getLastModifiedTime());
      }
      return typeLastModifiedTimeMap;
    }

    /*
     * No further permutation axes can be added after this is called
     */
    public void lockPermutationAxes() {
      axesLocked = true;
    }
  }

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext generatorContext,
        String typeName) throws UnableToCompleteException {

    /*
     * Do a series of checks to see if we can use a previously cached result,
     * and if so, we can skip further execution and return immediately.
     */
    boolean useCache = false;
    if (checkCachedPropertyInformation(logger, generatorContext)
        && checkCachedSourceTypes(logger, generatorContext)
        && checkCachedDependentResources(logger, generatorContext)) {
      useCache = true;
    }

    if (logger.isLoggable(TreeLogger.TRACE)) {
      if (generatorContext.isGeneratorResultCachingEnabled()) {
        String msg;
        if (useCache) {
          msg = "Reusing cached client bundle for " + typeName;
        } else {
          msg = "Can't use cached client bundle for " + typeName;
        }
        logger.log(TreeLogger.TRACE, msg);
      }
    }

    if (useCache) {
      return new RebindResult(RebindMode.USE_ALL_CACHED, typeName);
    }

    // The TypeOracle knows about all types in the type system
    TypeOracle typeOracle = generatorContext.getTypeOracle();

    // Get a reference to the type that the generator should implement
    JClassType sourceType = typeOracle.findType(typeName);

    // Ensure that the requested type exists
    if (sourceType == null) {
      logger.log(TreeLogger.ERROR, "Could not find requested typeName");
      throw new UnableToCompleteException();
    } else if (sourceType.isInterface() == null) {
      // The incoming type wasn't a plain interface, we don't support
      // abstract base classes
      logger.log(TreeLogger.ERROR, sourceType.getQualifiedSourceName()
          + " is not an interface.", null);
      throw new UnableToCompleteException();
    }

    /*
     * This associates the methods to implement with the ResourceGenerator class
     * that will generate the implementations of those methods.
     */
    Map<Class<? extends ResourceGenerator>, List<JMethod>> taskList = createTaskList(
        logger, typeOracle, sourceType);

    /*
     * Check the resource generators associated with our taskList, and see if
     * they all support generator result caching.
     */
    boolean canBeCacheable = checkResourceGeneratorCacheability(
        generatorContext, taskList);

    /*
     * Additional objects that hold state during the generation process.
     */
    AbstractResourceContext resourceContext = createResourceContext(logger,
        generatorContext, sourceType);
    FieldsImpl fields = new FieldsImpl();
    RequirementsImpl requirements = new RequirementsImpl(
        generatorContext.getPropertyOracle(), canBeCacheable);
    resourceContext.setRequirements(requirements);
    doAddFieldsAndRequirements(logger, generatorContext, fields, requirements);

    /*
     * Add our source type (and it's supertypes) as a requirement.  Note further
     * types may be added during the processing of the taskList.
     */
    requirements.addTypeHierarchy(sourceType);

    /*
     * Initialize the ResourceGenerators and prepare them for subsequent code
     * generation.
     */
    Map<ResourceGenerator, List<JMethod>> generators = initAndPrepare(logger,
        taskList, resourceContext, requirements);

    /*
     * Now that the ResourceGenerators have been initialized and prepared, we
     * can compute the actual name of the implementation class in order to
     * ensure that we use a distinct name between permutations.
     */
    String generatedSimpleSourceName = generateSimpleSourceName(logger,
        resourceContext, requirements);
    String packageName = sourceType.getPackage().getName();
    String createdClassName = packageName + "." + generatedSimpleSourceName;

    PrintWriter out = generatorContext.tryCreate(logger, packageName,
        generatedSimpleSourceName);

    // If an implementation already exists, we don't need to do any work
    if (out != null) {
      // There is actual work to do
      doCreateBundleForPermutation(logger, generatorContext, fields,
          generatedSimpleSourceName);
      // Begin writing the generated source.
      ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
          packageName, generatedSimpleSourceName);

      // The generated class needs to be able to determine the module base URL
      f.addImport(GWT.class.getName());

      // Used by the map methods
      f.addImport(ResourcePrototype.class.getName());

      // The whole point of this exercise
      f.addImplementedInterface(sourceType.getQualifiedSourceName());

      // All source gets written through this Writer
      SourceWriter sw = f.createSourceWriter(generatorContext, out);

      // Set the now-calculated simple source name
      resourceContext.setSimpleSourceName(generatedSimpleSourceName);

      JParameterizedType hashMapStringResource = getHashMapStringResource(typeOracle);
      String resourceMapField = fields.define(hashMapStringResource, "resourceMap");

      // Write a static instance for use by the static initializers.
      sw.print("private static " + generatedSimpleSourceName + " ");
      sw.println(INSTANCE_NAME + " = new " + generatedSimpleSourceName + "();");

      // Write the generated code to disk
      createFieldsAndAssignments(logger, sw, generators, resourceContext,
          fields);

      // Print the accumulated field definitions
      sw.println(fields.getCode());

      /*
       * The map-accessor methods use JSNI and need a fully-qualified class
       * name, but should not include any sub-bundles.
       */
      taskList.remove(BundleResourceGenerator.class);
      writeMapMethods(sw, taskList, hashMapStringResource, resourceMapField);

      sw.commit(logger);
    }

    finish(logger, resourceContext, generators.keySet());
    doFinish(logger);

    if (canBeCacheable) {
      // remember the current set of required properties, and their values
      CachedPropertyInformation cpi = new CachedPropertyInformation(logger,
          generatorContext.getPropertyOracle(),
          requirements.getPermutationAxes(),
          requirements.getConfigurationPropertyNames());

      // remember the last modified times for required source types
      Map<String, Long> cti = requirements.getTypeLastModifiedTimes();

      // remember the required resources
      Map<String, URL> cri = requirements.getResolvedResources();

      // create a new cacheable result
      RebindResult result = new RebindResult(RebindMode.USE_ALL_NEW, createdClassName);

      // add data to be returned the next time the generator is run
      result.putClientData(CACHED_PROPERTY_INFORMATION, cpi);
      result.putClientData(CACHED_RESOURCE_INFORMATION, (Serializable) cri);
      result.putClientData(CACHED_TYPE_INFORMATION, (Serializable) cti);

      return result;
    } else {
      // If we can't be cacheable, don't return a cacheable result
      return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING,
          createdClassName);
    }
  }

  @Override
  public long getVersionId() {
    return GENERATOR_VERSION_ID;
  }

  /**
   * Create the ResourceContext object that will be used by
   * {@link ResourceGenerator} subclasses. This is the primary way to implement
   * custom logic in the resource generation pass.
   */
  protected abstract AbstractResourceContext createResourceContext(
      TreeLogger logger, GeneratorContext context, JClassType resourceBundleType)
      throws UnableToCompleteException;

  /**
   * Provides a hook for subtypes to add additional fields or requirements to
   * the bundle.
   *
   * @param logger a TreeLogger
   * @param context the GeneratorContext
   * @param fields ClentBundle fields
   * @param requirements ClientBundleRequirements
   *
   * @throws UnableToCompleteException if an error occurs.
   */
  protected void doAddFieldsAndRequirements(TreeLogger logger,
      GeneratorContext context, FieldsImpl fields,
      ClientBundleRequirements requirements) throws UnableToCompleteException {
  }

  /**
   * This method is called after the ClientBundleRequirements have been
   * evaluated and a new ClientBundle implementation is being created.
   *
   * @param logger a TreeLogger
   * @param generatorContext the GeneratoContext
   * @param fields ClientBundle fields
   * @param generatedSimpleSourceName a String
   *
   * @throws UnableToCompleteException if an error occurs.
   */
  protected void doCreateBundleForPermutation(TreeLogger logger,
      GeneratorContext generatorContext, FieldsImpl fields,
      String generatedSimpleSourceName) throws UnableToCompleteException {
  }

  /**
   * Provides a hook for finalizing generated resources.
   *
   * @param logger a TreeLogger
   *
   * @throws UnableToCompleteException if an error occurs.
   */
  protected void doFinish(TreeLogger logger) throws UnableToCompleteException {
  }

  /**
   * Check cached dependent resources.
   */
  private boolean checkCachedDependentResources(TreeLogger logger,
      GeneratorContext genContext) {

    CachedGeneratorResult lastRebindResult = genContext.getCachedGeneratorResult();

    if (lastRebindResult == null
        || !genContext.isGeneratorResultCachingEnabled()) {
      return false;
    }
    long lastTimeGenerated = lastRebindResult.getTimeGenerated();

    // check that resource URL's haven't moved, and haven't been modified
    @SuppressWarnings("unchecked")
    Map<String, URL> cachedResolvedResources = (Map<String, URL>)
      lastRebindResult.getClientData(CACHED_RESOURCE_INFORMATION);

    if (cachedResolvedResources == null) {
      return false;
    }

    for (Entry<String, URL> entry : cachedResolvedResources.entrySet()) {
      String resourceName = entry.getKey();
      URL resolvedUrl = entry.getValue();
      URL currentUrl =
          ResourceGeneratorUtil.tryFindResource(logger, genContext, null, resourceName);

      if (resolvedUrl == null) {
        if (currentUrl == null) {
          continue;
        } else {
          logger.log(TreeLogger.TRACE, "Found newly available dependent resource: " + resourceName);
          return false;
        }
      } else if (currentUrl == null
          || !resolvedUrl.toExternalForm().equals(currentUrl.toExternalForm())) {
        logger.log(TreeLogger.TRACE,
            "Found dependent resource that has moved or no longer exists: " + resourceName);
        return false;
      }

      // Check whether the resource referenced by the provided URL is up to date
      long modifiedTime = Util.getResourceModifiedTime(resolvedUrl);
      if (modifiedTime == 0L || modifiedTime > lastTimeGenerated) {
        logger.log(TreeLogger.TRACE, "Found dependent resource that has changed: " + resourceName);
        return false;
      }
    }

    return true;
  }

  /**
   * Check cached properties.
   */
  private boolean checkCachedPropertyInformation(TreeLogger logger, GeneratorContext genContext) {

    CachedGeneratorResult lastRebindResult = genContext.getCachedGeneratorResult();

    if (lastRebindResult == null
        || !genContext.isGeneratorResultCachingEnabled()) {
      return false;
    }

    /*
     * Do a check of deferred-binding and configuration properties, comparing
     * the cached values saved previously with the current properties.
     */
    CachedPropertyInformation cpi = (CachedPropertyInformation)
        lastRebindResult.getClientData(CACHED_PROPERTY_INFORMATION);

    return cpi != null && cpi.checkPropertiesWithPropertyOracle(logger,
        genContext.getPropertyOracle());
  }

  /**
   * Check cached source types.
   */
  private boolean checkCachedSourceTypes(TreeLogger logger, GeneratorContext genContext) {

    CachedGeneratorResult lastRebindResult = genContext.getCachedGeneratorResult();

    if (lastRebindResult == null
        || !genContext.isGeneratorResultCachingEnabled()) {
      return false;
    }

    /*
     * Do a check over the cached list of types that were previously flagged as
     * required.  Check that none of these types has undergone a version change
     * since the previous cached result was generated.
     */
    @SuppressWarnings("unchecked")
    Map<String, Long> cachedTypeLastModifiedTimes = (Map<String, Long>)
      lastRebindResult.getClientData(CACHED_TYPE_INFORMATION);

    return cachedTypeLastModifiedTimes != null
      && checkCachedTypeLastModifiedTimes(logger, genContext, cachedTypeLastModifiedTimes);
  }

  /**
   * Check that the cached last modified times match those from the current
   * typeOracle.
   */
  private boolean checkCachedTypeLastModifiedTimes(TreeLogger logger,
      GeneratorContext generatorContext, Map<String, Long> typeLastModifiedTimes) {

    TypeOracle oracle = generatorContext.getTypeOracle();

    for (String sourceTypeName : typeLastModifiedTimes.keySet()) {
      JClassType sourceType = oracle.findType(sourceTypeName);
      if (sourceType == null) {
        logger.log(TreeLogger.TRACE,
            "Found previously dependent type that's no longer present: " + sourceTypeName);
        return false;
      }
      assert sourceType instanceof JRealClassType;
      JRealClassType sourceRealType = (JRealClassType) sourceType;

      if (sourceRealType.getLastModifiedTime() != typeLastModifiedTimes.get(sourceTypeName)) {
        logger.log(TreeLogger.TRACE, "Found dependent type that has changed: " + sourceTypeName);
        return false;
      }
    }

    return true;
  }

  /**
   * Check cacheability for resource generators in taskList.
   */
  private boolean checkResourceGeneratorCacheability(GeneratorContext genContext,
      Map<Class<? extends ResourceGenerator>, List<JMethod>> taskList) {

    if (!genContext.isGeneratorResultCachingEnabled()) {
      return false;
    }

    /*
     * Loop through each of our ResouceGenerator classes, and check those that
     * implement the SupportsGeneratorResultCaching interface.
     */
    for (Class<? extends ResourceGenerator> rgClass : taskList.keySet()) {
      if (!SupportsGeneratorResultCaching.class.isAssignableFrom(rgClass)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Create fields and assignments for a single ResourceGenerator.
   */
  private boolean createFieldsAndAssignments(TreeLogger logger,
      AbstractResourceContext resourceContext, ResourceGenerator rg,
      List<JMethod> generatorMethods, SourceWriter sw,
      ClientBundleFields fields) {

    // Defer failure until this phase has ended
    boolean fail = false;

    resourceContext.setCurrentResourceGenerator(rg);

    // Write all field values
    try {
      rg.createFields(logger.branch(TreeLogger.DEBUG, "Creating fields"),
          resourceContext, fields);
    } catch (UnableToCompleteException e) {
      return false;
    }

    // Create the instance variables in the IRB subclass by calling
    // writeAssignment() on the ResourceGenerator
    for (JMethod m : generatorMethods) {
      String rhs;

      try {
        rhs = rg.createAssignment(logger.branch(TreeLogger.DEBUG,
            "Creating assignment for " + m.getName() + "()"), resourceContext,
            m);
      } catch (UnableToCompleteException e) {
        fail = true;
        continue;
      }

      // Define a field that will hold the ResourcePrototype
      String ident = fields.define(m.getReturnType().isClassOrInterface(),
          m.getName(), null, true, false);

      /*
       * Create an initializer method in the context of an instance so that
       * resources can refer to one another by simply emitting a call to
       * <code>resource()</code>.
       */
      String initializerName = m.getName() + "Initializer";
      sw.println("private void " + initializerName + "() {");
      sw.indentln(ident + " = " + rhs + ";");
      sw.println("}");

      /*
       * Create a static Initializer class to lazily initialize the field on
       * first access. The compiler can efficiently optimize this static class
       * using clinits.
       */
      sw.println("private static class " + initializerName + " {");

      sw.indent();
      sw.println("static {");
      sw.indentln(INSTANCE_NAME + "." + initializerName + "();");
      sw.println("}");

      sw.print("static ");
      sw.print(m.getReturnType().getParameterizedQualifiedSourceName());
      sw.println(" get() {");
      sw.indentln("return " + ident + ";");
      sw.println("}");

      sw.outdent();
      sw.println("}");

      // Strip off all but the access modifiers
      sw.print(m.getReadableDeclaration(false, true, true, true, true));
      sw.println(" {");
      sw.indentln("return " + initializerName + ".get();");
      sw.println("}");
    }

    if (fail) {
      return false;
    }

    return true;
  }

  /**
   * Create fields and assignments for multiple ResourceGenerators.
   */
  private void createFieldsAndAssignments(TreeLogger logger, SourceWriter sw,
      Map<ResourceGenerator, List<JMethod>> generators,
      AbstractResourceContext resourceContext, ClientBundleFields fields)
      throws UnableToCompleteException {
    // Try to provide as many errors as possible before failing.
    boolean success = true;

    // Run the ResourceGenerators to generate implementations of the methods
    for (Map.Entry<ResourceGenerator, List<JMethod>> entry : generators.entrySet()) {
      success &= createFieldsAndAssignments(logger, resourceContext,
          entry.getKey(), entry.getValue(), sw, fields);
    }

    if (!success) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Given a ClientBundle subtype, compute which ResourceGenerators should
   * implement which methods. The data returned from this method should be
   * stable across identical modules.
   */
  private Map<Class<? extends ResourceGenerator>, List<JMethod>> createTaskList(
      TreeLogger logger, TypeOracle typeOracle, JClassType sourceType)
      throws UnableToCompleteException {

    Map<Class<? extends ResourceGenerator>, List<JMethod>> toReturn = new LinkedHashMap<Class<? extends ResourceGenerator>, List<JMethod>>();

    JClassType bundleType = typeOracle.findType(ClientBundle.class.getName());
    assert bundleType != null;

    JClassType bundleWithLookupType = typeOracle.findType(ClientBundleWithLookup.class.getName());
    assert bundleWithLookupType != null;

    JClassType resourcePrototypeType = typeOracle.findType(ResourcePrototype.class.getName());
    assert resourcePrototypeType != null;

    // Accumulate as many errors as possible before failing
    boolean throwException = false;

    // Using overridable methods allows composition of interface types
    for (JMethod m : sourceType.getOverridableMethods()) {
      JClassType returnType = m.getReturnType().isClassOrInterface();

      if (m.getEnclosingType().equals(bundleType)
          || m.getEnclosingType().equals(bundleWithLookupType)) {
        // Methods that we must generate, but that are not resources
        continue;

      } else if (!m.isAbstract()) {
        // Covers the case of an abstract class base type
        continue;

      } else if (returnType == null
          || !(returnType.isAssignableTo(resourcePrototypeType) || returnType.isAssignableTo(bundleType))) {
        // Primitives and random other abstract methods
        logger.log(TreeLogger.ERROR, "Unable to implement " + m.getName()
            + " because it does not derive from "
            + resourcePrototypeType.getQualifiedSourceName() + " or "
            + bundleType.getQualifiedSourceName());
        throwException = true;
        continue;
      }

      try {
        Class<? extends ResourceGenerator> clazz = findResourceGenerator(
            logger, m);
        List<JMethod> generatorMethods;
        if (toReturn.containsKey(clazz)) {
          generatorMethods = toReturn.get(clazz);
        } else {
          generatorMethods = new ArrayList<JMethod>();
          toReturn.put(clazz, generatorMethods);
        }

        generatorMethods.add(m);
      } catch (UnableToCompleteException e) {
        throwException = true;
      }
    }

    if (throwException) {
      throw new UnableToCompleteException();
    }

    return toReturn;
  }

  /**
   * Given a JMethod, find the a ResourceGenerator class that will be able to
   * provide an implementation of the method.
   */
  private Class<? extends ResourceGenerator> findResourceGenerator(
      TreeLogger logger, JMethod method) throws UnableToCompleteException {
    JClassType resourceType = method.getReturnType().isClassOrInterface();
    assert resourceType != null;

    ResourceGeneratorType annotation = resourceType.findAnnotationInTypeHierarchy(ResourceGeneratorType.class);
    if (annotation == null) {
      logger.log(TreeLogger.ERROR, "No @"
          + ResourceGeneratorType.class.getName() + " was specifed for type "
          + resourceType.getQualifiedSourceName() + " or its supertypes");
      throw new UnableToCompleteException();
    }

    return annotation.value();
  }

  /**
   * Call finish() on several ResourceGenerators.
   */
  private void finish(TreeLogger logger, AbstractResourceContext context,
      Collection<ResourceGenerator> generators)
      throws UnableToCompleteException {
    boolean fail = false;
    // Finalize the ResourceGenerator
    for (ResourceGenerator rg : generators) {
      context.setCurrentResourceGenerator(rg);
      try {
        rg.finish(
            logger.branch(TreeLogger.DEBUG, "Finishing ResourceGenerator"),
            context);
      } catch (UnableToCompleteException e) {
        fail = true;
      }
    }
    if (fail) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Given a user-defined type name, determine the type name for the generated
   * class based on accumulated requirements.
   */
  private String generateSimpleSourceName(TreeLogger logger,
      ResourceContext context, RequirementsImpl requirements) {
    StringBuilder toReturn = new StringBuilder(
        context.getClientBundleType().getName().replaceAll("[.$]", "_"));

    try {
      // always add the locale property
      requirements.addPermutationAxis("locale");
    } catch (BadPropertyValueException e) {
    }

    try {
      // no further additions to the permutation axes allowed after this point
      requirements.lockPermutationAxes();

      PropertyOracle oracle = context.getGeneratorContext().getPropertyOracle();
      for (String property : requirements.getPermutationAxes()) {
        SelectionProperty prop = oracle.getSelectionProperty(logger, property);
        String value = prop.getCurrentValue();
        toReturn.append("_" + value);
      }
    } catch (BadPropertyValueException e) {
    }

    toReturn.append("_" + getClass().getSimpleName());

    // If design time, generate new class each time to allow reloading.
    if (Beans.isDesignTime()) {
      toReturn.append("_designTime" + System.currentTimeMillis());
    }

    return toReturn.toString();
  }

  /**
   * Returns HashMap&lt;String, ResourcePrototype&gt;.
   */
  private JParameterizedType getHashMapStringResource(TypeOracle typeOracle) {
    JGenericType hashMap = (JGenericType) typeOracle.findType(HashMap.class.getName());
    assert hashMap != null;
    JClassType string = typeOracle.findType(String.class.getName());
    assert string != null;
    JClassType resourcePrototype = typeOracle.findType(ResourcePrototype.class.getName());
    assert resourcePrototype != null;
    JParameterizedType mapStringRes = typeOracle.getParameterizedType(hashMap,
        new JClassType[]{string, resourcePrototype});
    return mapStringRes;
  }

  private boolean initAndPrepare(TreeLogger logger,
      AbstractResourceContext resourceContext, ResourceGenerator rg,
      List<JMethod> generatorMethods, ClientBundleRequirements requirements) {
    try {
      resourceContext.setCurrentResourceGenerator(rg);
      rg.init(
          logger.branch(TreeLogger.DEBUG, "Initializing ResourceGenerator"),
          resourceContext);
    } catch (UnableToCompleteException e) {
      return false;
    }

    boolean fail = false;

    // Prepare the ResourceGenerator by telling it all methods that it is
    // expected to produce.
    for (JMethod m : generatorMethods) {
      try {
        rg.prepare(logger.branch(TreeLogger.DEBUG, "Preparing method "
            + m.getName()), resourceContext, requirements, m);
      } catch (UnableToCompleteException e) {
        fail = true;
      }
    }

    return !fail;
  }

  private Map<ResourceGenerator, List<JMethod>> initAndPrepare(
      TreeLogger logger,
      Map<Class<? extends ResourceGenerator>, List<JMethod>> taskList,
      AbstractResourceContext resourceContext,
      ClientBundleRequirements requirements) throws UnableToCompleteException {
    // Try to provide as many errors as possible before failing.
    boolean success = true;
    Map<ResourceGenerator, List<JMethod>> toReturn = new LinkedHashMap<ResourceGenerator, List<JMethod>>();

    // Run the ResourceGenerators to generate implementations of the methods
    for (Map.Entry<Class<? extends ResourceGenerator>, List<JMethod>> entry : taskList.entrySet()) {

      ResourceGenerator rg = instantiateResourceGenerator(logger,
          entry.getKey());
      toReturn.put(rg, entry.getValue());

      success &= initAndPrepare(logger, resourceContext, rg, entry.getValue(),
          requirements);
    }

    if (!success) {
      throw new UnableToCompleteException();
    }

    return toReturn;
  }

  /**
   * Utility method to construct a ResourceGenerator that logs errors.
   */
  private <T extends ResourceGenerator> T instantiateResourceGenerator(
      TreeLogger logger, Class<T> generatorClass)
      throws UnableToCompleteException {
    try {
      return generatorClass.newInstance();
    } catch (InstantiationException e) {
      logger.log(TreeLogger.ERROR, "Unable to initialize ResourceGenerator", e);
    } catch (IllegalAccessException e) {
      logger.log(TreeLogger.ERROR, "Unable to instantiate ResourceGenerator. "
          + "Does it have a public default constructor?", e);
    }
    throw new UnableToCompleteException();
  }

  /**
   * Emits getResources() and getResourceMap() implementations.
   *
   * @param sw the output writer
   * @param taskList the list of methods to map by name
   * @param resourceMapField field containing the Java String to Resource map
   */
  private void writeMapMethods(SourceWriter sw,
      Map<Class<? extends ResourceGenerator>, List<JMethod>> taskList,
      JParameterizedType resourceMapType, String resourceMapField) {

    // Complete the IRB contract
    sw.println("public ResourcePrototype[] getResources() {");
    sw.indent();
    sw.println("return new ResourcePrototype[] {");
    sw.indent();
    for (List<JMethod> methods : taskList.values()) {
      for (JMethod m : methods) {
        sw.println(m.getName() + "(), ");
      }
    }
    sw.outdent();
    sw.println("};");
    sw.outdent();
    sw.println("}");

    // Map implementation for dev mode.
    sw.println("public ResourcePrototype getResource(String name) {");
    sw.indent();
    sw.println("if (GWT.isScript()) {");
    sw.indent();
    sw.println("return getResourceNative(name);");
    sw.outdent();
    sw.println("} else {");
    sw.indent();
    sw.println("if (" + resourceMapField + " == null) {");
    sw.indent();
    sw.println(resourceMapField + " = new "
        + resourceMapType.getParameterizedQualifiedSourceName() + "();");
    for (List<JMethod> list : taskList.values()) {
      for (JMethod m : list) {
        sw.println(resourceMapField + ".put(\"" + m.getName() + "\", "
            + m.getName() + "());");
      }
    }
    sw.outdent();
    sw.println("}");
    sw.println("return resourceMap.get(name);");
    sw.outdent();
    sw.println("}");
    sw.outdent();
    sw.println("}");

    // Use a switch statement as a fast map for script mode.
    sw.println("private native ResourcePrototype "
        + "getResourceNative(String name) /*-{");
    sw.indent();
    sw.println("switch (name) {");
    sw.indent();
    for (List<JMethod> list : taskList.values()) {
      for (JMethod m : list) {
        sw.println("case '" + m.getName() + "': return this."
            + m.getJsniSignature() + "();");
      }
    }
    sw.outdent();
    sw.println("}");
    sw.println("return null;");
    sw.outdent();
    sw.println("}-*/;");
  }
}
