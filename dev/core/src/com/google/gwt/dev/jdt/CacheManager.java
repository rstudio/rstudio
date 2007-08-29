/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.HasMetaData;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.shell.JavaScriptHost;
import com.google.gwt.dev.shell.ShellGWT;
import com.google.gwt.dev.shell.ShellJavaScriptHost;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * CacheManager manages all the caching used to speed up hosted mode startup and
 * refresh, and manages the invalidations required to ensure that changes are
 * reflected correctly on reload.
 */
public class CacheManager {

  /**
   * Maps SourceTypeBindings to their associated types.
   */
  static class Mapper {
    private final Map<ReferenceBinding, JClassType> map = new IdentityHashMap<ReferenceBinding, JClassType>();

    public JClassType get(ReferenceBinding referenceBinding) {
      JClassType type = map.get(referenceBinding);
      return type;
    }

    public void put(ReferenceBinding binding, JClassType type) {
      boolean firstPut = (null == map.put(binding, type));
      assert (firstPut);
    }

    public void reset() {
      map.clear();
    }
  }

  /**
   * This class is a very simple multi-valued map.
   */
  private static class Dependencies {
    private Map<String, HashSet<String>> map = new HashMap<String, HashSet<String>>();

    /**
     * This method adds <code>item</code> to the list stored under
     * <code>key</code>.
     * 
     * @param key the key used to access the list
     * @param item the item to be added to the list
     */
    private void add(String dependerFilename, String dependeeFilename) {
      if (!map.containsKey(dependeeFilename)) {
        map.put(dependeeFilename, new HashSet<String>());
      }

      get(dependeeFilename).add(dependerFilename);
    }

    /**
     * This method gets the list stored under <code>key</code>.
     * 
     * @param key the key used to access the list.
     * @return the list stored under <code>key</code>
     */
    private Set<String> get(String filename) {
      return map.get(filename);
    }

    private Set<String> transitiveClosure(final String filename) {
      String current = filename;
      TreeSet<String> queue = new TreeSet<String>();
      Set<String> finished = new HashSet<String>();
      queue.add(filename);
      while (true) {
        finished.add(current);
        Set<String> children = get(current);
        if (children != null) {
          for (Iterator<String> iter = children.iterator(); iter.hasNext();) {
            String child = iter.next();
            if (!finished.contains(child)) {
              queue.add(child);
            }
          }
        }
        if (queue.size() == 0) {
          return finished;
        } else {
          current = queue.first();
          queue.remove(current);
        }
      }
    }
  }

  /**
   * Visit all of the CUDs and extract dependencies. This visitor handles
   * explicit TypeRefs via the onTypeRef method AND it also deals with the
   * gwt.typeArgs annotation.
   * 
   * <ol>
   * <li>Extract the list of type names from the gwt.typeArgs annotation</li>
   * <li>For each type name, locate the CUD that defines it</li>
   * <li>Add the referenced CUD as a dependency</li>
   * </ol>
   */
  private final class DependencyVisitor extends TypeRefVisitor {
    private final Dependencies dependencies;

    private DependencyVisitor(Dependencies dependencies) {
      this.dependencies = dependencies;
    }

    @Override
    public void endVisit(FieldDeclaration fieldDeclaration,
        final MethodScope scope) {
      extractDependenciesFromTypeArgs(fieldDeclaration.javadoc,
          scope.referenceContext());
    }

    @Override
    public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
      extractDependenciesFromTypeArgs(methodDeclaration.javadoc,
          scope.referenceContext());
    }

    @Override
    protected void onTypeRef(SourceTypeBinding referencedType,
        CompilationUnitDeclaration unitOfReferrer) {
      // If the referenced type belongs to a compilation unit that
      // was changed, then the unit in which it
      // is referenced must also be treated as changed.
      //
      String dependeeFilename = String.valueOf(referencedType.getFileName());
      String dependerFilename = String.valueOf(unitOfReferrer.getFileName());

      dependencies.add(dependerFilename, dependeeFilename);
    }

    private String combine(String[] strings, int startIndex) {
      StringBuffer sb = new StringBuffer();
      for (int i = startIndex; i < strings.length; i++) {
        String s = strings[i];
        sb.append(s);
      }
      return sb.toString();
    }

    /**
     * Extracts additional dependencies based on the gwt.typeArgs annotation.
     * This is not detected by JDT so we need to do it here. We do not perform
     * as strict a parse as the TypeOracle would do.
     * 
     * @param javadoc javadoc text
     * @param scope scope that contains the definition
     * @param isField true if the javadoc is associated with a field
     */
    private void extractDependenciesFromTypeArgs(Javadoc javadoc,
        final ReferenceContext scope) {
      if (javadoc == null) {
        return;
      }
      final char[] source = scope.compilationResult().compilationUnit.getContents();

      TypeOracleBuilder.parseMetaDataTags(source, new HasMetaData() {
        public void addMetaData(String tagName, String[] values) {
          assert (values != null);

          if (!TypeOracle.TAG_TYPEARGS.equals(tagName)) {
            // don't care about non gwt.typeArgs
            return;
          }

          if (values.length == 0) {
            return;
          }

          Set<String> typeNames = new HashSet<String>();

          /*
           * if the first element starts with a "<" then we assume that no
           * parameter name was specified
           */
          int startIndex = 1;
          if (values[0].trim().startsWith("<")) {
            startIndex = 0;
          }

          extractTypeNamesFromTypeArg(combine(values, startIndex), typeNames);

          Iterator<String> it = typeNames.iterator();
          while (it.hasNext()) {
            String typeName = it.next();

            try {
              ICompilationUnit compilationUnit = astCompiler.getCompilationUnitForType(
                  TreeLogger.NULL, typeName);

              String dependeeFilename = String.valueOf(compilationUnit.getFileName());
              String dependerFilename = String.valueOf(scope.compilationResult().compilationUnit.getFileName());

              dependencies.add(dependerFilename, dependeeFilename);

            } catch (UnableToCompleteException e) {
              // Purposely ignored
            }
          }
        }

        public String[][] getMetaData(String tagName) {
          return null;
        }

        public String[] getMetaDataTags() {
          return null;
        }
      }, javadoc);
    }

    /**
     * Extracts the type names referenced from a gwt.typeArgs annotation and
     * adds them to the set of type names.
     * 
     * @param typeArg a string containing the type args as the user entered them
     * @param typeNames the set of type names referenced in the typeArgs string
     */
    private void extractTypeNamesFromTypeArg(String typeArg,
        Set<String> typeNames) {
      // Remove all whitespace
      typeArg = typeArg.replaceAll("\\\\s", "");

      // Remove anything that is not a raw type name
      String[] typeArgs = typeArg.split("[\\[\\]<>,]");

      for (int i = 0; i < typeArgs.length; ++i) {
        if (typeArgs[i].length() > 0) {
          typeNames.add(typeArgs[i]);
        }
      }
    }
  }

  /**
   * Caches information using a directory, with an in memory cache to prevent
   * unneeded disk access.
   */
  private static class DiskCache extends AbstractMap<String, Object> {

    private class FileEntry implements Map.Entry<String, Object> {

      private File file;

      private FileEntry(File file) {
        this.file = file;
      }

      private FileEntry(String className) {
        this(new File(directory, possiblyAddTmpExtension(className)));
      }

      private FileEntry(String className, Object o) {
        this(new File(directory, possiblyAddTmpExtension(className)));
        setValue(o);
      }

      public String getKey() {
        return possiblyRemoveTmpExtension(file.getName());
      }

      public Object getValue() {
        if (!file.exists()) {
          return null;
        }
        try {
          FileInputStream fis = new FileInputStream(file);
          ObjectInputStream ois = new ObjectInputStream(fis);
          Object out = ois.readObject();
          ois.close();
          fis.close();
          return out;
        } catch (IOException e) {
          return null;
          // If we can't read the file, we can't get the item from the cache.
        } catch (ClassNotFoundException e) {
          return null;
          // The class does not match because the serialUID is not correct
          // so we don't want this item anyway.
        }
      }

      public void remove() {
        file.delete();
      }

      public Object setValue(Object value) {
        Object o = getValue();
        FileOutputStream fos;
        try {
          fos = new FileOutputStream(file);
          ObjectOutputStream oos = new ObjectOutputStream(fos);
          oos.writeObject(value);
          oos.close();
          fos.close();
        } catch (IOException e) {
          markCacheDirectoryUnusable();
        }
        return o;
      }

      private long lastModified() {
        return file.lastModified();
      }
    }

    private final Map<String, Object> cache = new HashMap<String, Object>();

    // May be set to null after the fact if the cache directory becomes
    // unusable.
    private File directory;

    public DiskCache(File dirName) {
      if (dirName != null) {
        directory = dirName;
        possiblyCreateCacheDirectory();
      } else {
        directory = null;
      }
    }

    @Override
    public void clear() {
      cache.clear();
      if (directory != null) {
        for (Iterator<String> iter = keySet().iterator(); iter.hasNext();) {
          iter.remove();
        }
      }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      Set<Entry<String, Object>> out = new HashSet<Entry<String, Object>>() {
        @Override
        public boolean remove(Object o) {
          Entry<String, Object> entry = (Entry<String, Object>) o;
          boolean removed = (DiskCache.this.remove(entry.getKey())) != null;
          super.remove(o);
          return removed;
        }
      };
      out.addAll(cache.entrySet());
      // No directory means no persistence.
      if (directory != null) {
        possiblyCreateCacheDirectory();
        // Add files not yet loaded into this cache.
        File[] entries = directory.listFiles();
        for (int i = 0; i < entries.length; i++) {
          if (!cache.containsKey(new FileEntry(entries[i]).getKey())) {
            out.add(new FileEntry(entries[i]));
          }
        }
      }
      return out;
    }

    public Object get(String key) {
      if (cache.containsKey(key)) {
        return cache.get(key);
      }
      Object value = null;
      if (directory != null) {
        value = new FileEntry(key).getValue();
        cache.put(key, value);
      }
      return value;
    }

    @Override
    public Set<String> keySet() {
      Set<String> out = new HashSet<String>() {
        @Override
        public boolean remove(Object o) {
          boolean removed = (DiskCache.this.remove(o)) != null;
          super.remove(o);
          return removed;
        }
      };
      out.addAll(cache.keySet());
      // No directory means no persistence.
      if (directory != null) {
        possiblyCreateCacheDirectory();
        // Add files not yet loaded into this cache.
        File[] entries = directory.listFiles();
        for (int i = 0; i < entries.length; i++) {
          out.add(new FileEntry(entries[i].getName()).getKey());
        }
      }
      return out;
    }

    @Override
    public Object put(String key, Object value) {
      return put(key, value, true);
    }

    @Override
    public Object remove(Object key) {
      String fileName = (String) key;
      Object out = get(fileName);
      // No directory means no persistence.
      if (directory != null) {
        possiblyCreateCacheDirectory();
        FileEntry e = new FileEntry(fileName);
        e.remove();
      }
      cache.remove(key);
      return out;
    }

    private long lastModified(String className) {
      if (directory == null) {
        // we have no file on disk to refer to, so should return the same result
        // as if the file did not exist -- namely 0.
        return 0;
      }
      return new FileEntry(className).lastModified();
    }

    /**
     * This method marks the cache directory as being invalid, so we do not try
     * to use it.
     */
    private void markCacheDirectoryUnusable() {
      System.err.println("The directory " + directory.getAbsolutePath()
          + " is not usable as a cache directory");
      directory = null;
    }

    /**
     * This is used to ensure that if something wicked happens to the cache
     * directory while we are running, we do not crash.
     */
    private void possiblyCreateCacheDirectory() {
      directory.mkdirs();
      if (!(directory.exists() && directory.canWrite())) {
        markCacheDirectoryUnusable();
      }
    }

    private Object put(String key, Object value, boolean persist) {
      Object out = get(key);

      // We use toString to match the string value in FileEntry.
      cache.remove(key.toString());

      // Writes the file.
      if (persist && directory != null) {
        // This writes the file to the disk and is all that is needed.
        new FileEntry(key, value);
      }
      cache.put(key, value);
      return out;
    }
  }

  /**
   * The set of all classes whose bytecode needs to exist as bootstrap bytecode
   * to be taken as given by the bytecode compiler.
   */
  public static final Class<?>[] BOOTSTRAP_CLASSES = new Class<?>[] {
      JavaScriptHost.class, ShellJavaScriptHost.class, ShellGWT.class};

  /**
   * The set of bootstrap classes, which are marked transient, but are
   * nevertheless not recompiled each time, as they are bootstrap classes.
   */
  private static final Set<String> TRANSIENT_CLASS_NAMES;

  static {
    TRANSIENT_CLASS_NAMES = new HashSet<String>(BOOTSTRAP_CLASSES.length + 3);
    for (int i = 0; i < BOOTSTRAP_CLASSES.length; i++) {
      TRANSIENT_CLASS_NAMES.add(BOOTSTRAP_CLASSES[i].getName());
    }
  }

  // This method must be outside of DiskCache because of the restriction against
  // defining static methods in inner classes.
  private static String possiblyAddTmpExtension(Object className) {
    String fileName = className.toString();
    if (fileName.indexOf("-") == -1) {
      int hashCode = fileName.hashCode();
      String hashCodeStr = Integer.toHexString(hashCode);
      while (hashCodeStr.length() < 8) {
        hashCodeStr = '0' + hashCodeStr;
      }
      fileName = fileName + "-" + hashCodeStr + ".tmp";
    }
    return fileName;
  }

  // This method must be outside of DiskCache because of the restriction against
  // defining static methods in inner classes.
  private static String possiblyRemoveTmpExtension(Object fileName) {
    String className = fileName.toString();
    if (className.indexOf("-") != -1) {
      className = className.split("-")[0];
    }
    return className;
  }

  private final Set<CompilationUnitProvider> addedCups = new HashSet<CompilationUnitProvider>();

  private final AstCompiler astCompiler;

  private final DiskCache byteCodeCache;

  private final File cacheDir;

  private final Set<String> changedFiles;

  private final Map<String, CompilationUnitDeclaration> cudsByFileName;

  private final Map<String, CompilationUnitProvider> cupsByLocation = new HashMap<String, CompilationUnitProvider>();

  private boolean firstTime = true;

  /**
   * Set of {@link CompilationUnitProvider} locations for all of the compilation
   * units generated by {@link com.google.gwt.core.ext.Generator Generator}s.
   * 
   * TODO: This seems like it should be a Set of CUPs rather than a set of CUP
   * locations.
   */
  private final Set<String> generatedCupLocations = new HashSet<String>();

  private final Mapper identityMapper = new Mapper();

  private final Set<String> invalidatedTypes = new HashSet<String>();

  private final TypeOracle oracle;

  private final Map<String, Long> timesByLocation = new HashMap<String, Long>();

  private boolean typeOracleBuilderFirstTime = true;

  private final Map<String, ICompilationUnitAdapter> unitsByCup = new HashMap<String, ICompilationUnitAdapter>();

  /**
   * Creates a new <code>CacheManager</code>, creating a new
   * <code>TypeOracle</code>. This constructor does not specify a cache
   * directory, and therefore is to be used in unit tests and executables that
   * do not need caching.
   */
  public CacheManager() {
    this(null, null);
  }

  /**
   * Creates a new <code>CacheManager</code>, creating a new
   * <code>TypeOracle</code>. This constructor uses the specified cacheDir,
   * and does cache information across reloads. If the specified cacheDir is
   * null, caching across reloads will be disabled.
   */
  public CacheManager(String cacheDir, TypeOracle oracle) {
    if (oracle == null) {
      this.oracle = new TypeOracle();
    } else {
      this.oracle = oracle;
    }
    changedFiles = new HashSet<String>();
    cudsByFileName = new HashMap<String, CompilationUnitDeclaration>();
    if (cacheDir != null) {
      this.cacheDir = new File(cacheDir);
      this.cacheDir.mkdirs();
      byteCodeCache = new DiskCache(new File(cacheDir, "bytecode"));
    } else {
      this.cacheDir = null;
      byteCodeCache = new DiskCache(null);
    }
    SourceOracleOnTypeOracle sooto = new SourceOracleOnTypeOracle(this.oracle);
    astCompiler = new AstCompiler(sooto);
  }

  /**
   * Creates a new <code>CacheManager</code>, using the supplied
   * <code>TypeOracle</code>. This constructor does not specify a cache
   * directory, and therefore is to be used in unit tests and executables that
   * do not need caching.
   */
  public CacheManager(TypeOracle typeOracle) {
    this(null, typeOracle);
  }

  /**
   * Adds the specified {@link CompilationUnitProvider} to the set of CUPs
   * generated by {@link com.google.gwt.core.ext.Generator Generator}s.
   * Generated <code>CompilationUnitProviders</code> are not cached across
   * reloads.
   */
  public void addGeneratedCup(CompilationUnitProvider generatedCup) {
    assert (generatedCup != null);

    generatedCupLocations.add(generatedCup.getLocation());
  }

  /**
   * This method returns the <code>TypeOracle</code> associated with this
   * <code>CacheManager</code>.
   */
  public TypeOracle getTypeOracle() {
    return oracle;
  }

  /**
   * Ensures that all compilation units generated via generators are removed
   * from the system so that they will be generated again, and thereby take into
   * account input that may have changed since the last reload.
   */
  public void invalidateVolatileFiles() {
    for (Iterator<CompilationUnitProvider> iter = addedCups.iterator(); iter.hasNext();) {
      CompilationUnitProvider cup = iter.next();
      if (isGeneratedCup(cup)) {
        iter.remove();
      }
    }
  }

  /**
   * This method adds byte.
   * 
   * @param logger
   * @param binaryTypeName
   * @param byteCode
   * @return
   */
  boolean acceptIntoCache(TreeLogger logger, String binaryTypeName,
      ByteCode byteCode) {
    synchronized (byteCodeCache) {
      if (getByteCode(binaryTypeName) == null) {
        byteCodeCache.put(binaryTypeName, byteCode, (!byteCode.isTransient()));
        logger.log(TreeLogger.SPAM, "Cached bytecode for " + binaryTypeName,
            null);
        return true;
      } else {
        logger.log(TreeLogger.SPAM, "Bytecode not re-cached for "
            + binaryTypeName, null);
        return false;
      }
    }
  }

  /**
   * Adds this compilation unit if it is not present, or is older. Otherwise
   * does nothing.
   * 
   * @throws UnableToCompleteException thrown if we cannot figure out when this
   *           cup was modified
   */
  void addCompilationUnit(CompilationUnitProvider cup)
      throws UnableToCompleteException {
    Long lastModified = new Long(cup.getLastModified());
    if (isCupUnchanged(cup, lastModified)) {
      return;
    }
    CompilationUnitProvider oldCup = getCup(cup);
    if (oldCup != null) {
      addedCups.remove(oldCup);
      markCupChanged(cup);
    }
    timesByLocation.put(cup.getLocation(), lastModified);
    cupsByLocation.put(cup.getLocation(), cup);
    addedCups.add(cup);
  }

  /**
   * This method modifies the field <code>changedFiles</code> to contain all
   * of the additional files that are capable of reaching any of the files
   * currently contained within <code>changedFiles</code>.
   */
  void addDependentsToChangedFiles() {
    final Dependencies dependencies = new Dependencies();

    DependencyVisitor trv = new DependencyVisitor(dependencies);

    // Find references to type in units that aren't any longer valid.
    //
    for (CompilationUnitDeclaration cud : cudsByFileName.values()) {
      cud.traverse(trv, cud.scope);
    }

    Set<String> toTraverse = new HashSet<String>(changedFiles);
    for (Iterator<String> iter = toTraverse.iterator(); iter.hasNext();) {
      String fileName = iter.next();
      changedFiles.addAll(dependencies.transitiveClosure(fileName));
    }
  }

  ICompilationUnit findUnitForCup(CompilationUnitProvider cup) {
    if (!unitsByCup.containsKey(cup.getLocation())) {
      unitsByCup.put(cup.getLocation(), new ICompilationUnitAdapter(cup));
    }
    return unitsByCup.get(cup.getLocation());
  }

  Set<CompilationUnitProvider> getAddedCups() {
    return addedCups;
  }

  AstCompiler getAstCompiler() {
    return astCompiler;
  }

  /**
   * Gets the bytecode from the cache, rejecting it if an incompatible change
   * occurred since it was cached.
   */
  ByteCode getByteCode(String binaryTypeName) {
    synchronized (byteCodeCache) {
      ByteCode byteCode = (ByteCode) byteCodeCache.get(binaryTypeName);
      // we do not want bytecode created with a different classpath or os or
      // version of GWT.
      if ((byteCode != null)
          && byteCode.getSystemIdentifier() != null
          && (!(byteCode.getSystemIdentifier().equals(ByteCode.getCurrentSystemIdentifier())))) {
        byteCodeCache.remove(binaryTypeName);
        byteCode = null;
      }
      if (byteCode != null) {
        // Found it.
        //
        return byteCode;
      } else {
        // This type has not been compiled before, or we tried but failed.
        //
        return null;
      }
    }
  }

  Set<String> getChangedFiles() {
    return changedFiles;
  }

  Map<String, CompilationUnitDeclaration> getCudsByFileName() {
    return cudsByFileName;
  }

  CompilationUnitProvider getCup(CompilationUnitProvider cup) {
    return getCupsByLocation().get(cup.getLocation());
  }

  Object getCupLastUpdateTime(CompilationUnitProvider cup) {
    return getTimesByLocation().get(cup.getLocation());
  }

  Map<String, CompilationUnitProvider> getCupsByLocation() {
    return cupsByLocation;
  }

  Mapper getIdentityMapper() {
    return identityMapper;
  }

  Map<String, Long> getTimesByLocation() {
    return timesByLocation;
  }

  JType getTypeForBinding(ReferenceBinding referenceBinding) {
    return identityMapper.get(referenceBinding);
  }

  /**
   * This removes all state changed since the last time the typeOracle was run.
   * Since the typeOracle information is not cached on disk, this is not needed
   * the first time.
   * 
   * @param typeOracle
   */
  void invalidateOnRefresh(TypeOracle typeOracle) {
    // If a class is changed, the set of classes in the transitive closure
    // of "refers to" must be marked changed as well.
    // The initial change set is computed in addCompilationUnit.
    // For the first time we do not do this because the compiler
    // has no cached info.
    if (!isTypeOracleBuilderFirstTime()) {
      changedFiles.addAll(generatedCupLocations);
      addDependentsToChangedFiles();

      for (Iterator<String> iter = changedFiles.iterator(); iter.hasNext();) {
        String location = iter.next();
        CompilationUnitProvider cup = getCupsByLocation().get(location);
        unitsByCup.remove(location);
        Util.invokeInaccessableMethod(TypeOracle.class,
            "invalidateTypesInCompilationUnit",
            new Class[] {CompilationUnitProvider.class}, typeOracle,
            new Object[] {cup});
      }
      astCompiler.invalidateChangedFiles(changedFiles, invalidatedTypes);
    } else {
      becomeTypeOracleNotFirstTime();
    }
  }

  /**
   * Was this cup, last modified at time lastModified modified since it was last
   * processed by the system?
   */
  boolean isCupUnchanged(CompilationUnitProvider cup, Long lastModified) {
    Long oldTime = (Long) getCupLastUpdateTime(cup);
    if (oldTime != null) {
      if (oldTime.longValue() >= lastModified.longValue()
          && (!cup.isTransient())) {
        return true;
      }
    }
    return false;
  }

  /**
   * This method is called when a cup is known to have changed. This will ensure
   * that all the types defined in this cup are invalidated.
   * 
   * @param cup the cup modified
   */
  void markCupChanged(CompilationUnitProvider cup) {
    changedFiles.add(String.valueOf(cup.getLocation()));
  }

  boolean removeFromCache(TreeLogger logger, String binaryTypeName) {
    synchronized (byteCodeCache) {
      if (getByteCode(binaryTypeName) == null) {
        logger.log(TreeLogger.SPAM, "Bytecode for " + binaryTypeName
            + " was not cached, so not removing", null);
        return false;
      } else {
        byteCodeCache.remove(binaryTypeName);
        logger.log(TreeLogger.SPAM, "Bytecode not re-cached for "
            + binaryTypeName, null);
        return false;
      }
    }
  }

  /**
   * This method removes all of the bytecode which is out of date from the
   * bytecode cache. The set of files needing to be changed are going to be the
   * set already known to be changed plus those that are out of date in the
   * bytecode cache.
   */
  void removeStaleByteCode(TreeLogger logger, AbstractCompiler compiler) {
    if (cacheDir == null) {
      byteCodeCache.clear();
      return;
    }
    if (isFirstTime()) {
      Set<String> classNames = byteCodeCache.keySet();
      for (Iterator<String> iter = classNames.iterator(); iter.hasNext();) {
        String className = iter.next();
        ByteCode byteCode = ((ByteCode) (byteCodeCache.get(className)));
        if (byteCode == null) {
          iter.remove();
          continue;
        }
        String qname = byteCode.getBinaryTypeName();
        if (TRANSIENT_CLASS_NAMES.contains(qname)) {
          continue;
        }
        String location = byteCode.getLocation();
        if (byteCode.isTransient()) {
          // GWT transient classes; no need to test.
          // Either standardGeneratorContext created it
          // in which case we already know it is invalid
          // or its something like GWT and it lives.
          continue;
        }
        String fileName = Util.findFileName(location);
        CompilationUnitDeclaration compilationUnitDeclaration = cudsByFileName.get(location);
        if (compilationUnitDeclaration == null) {
          changedFiles.add(location);
          continue;
        }
        long srcLastModified = Long.MAX_VALUE;
        File srcLocation = new File(fileName);
        if (srcLocation.exists()) {
          srcLastModified = srcLocation.lastModified();
        }
        long byteCodeLastModified = byteCodeCache.lastModified(className);
        if (srcLastModified >= byteCodeLastModified) {
          changedFiles.add(location);
        }
      }
      addDependentsToChangedFiles();
    }
    becomeNotFirstTime();
    invalidateChangedFiles(logger, compiler);
  }

  void setTypeForBinding(SourceTypeBinding binding, JClassType type) {
    identityMapper.put(binding, type);
  }

  private void becomeNotFirstTime() {
    firstTime = false;
  }

  private void becomeTypeOracleNotFirstTime() {
    typeOracleBuilderFirstTime = false;
  }

  /**
   * Actually performs the work of removing the invalidated data from the
   * system. At this point, changedFiles should be complete. After this method
   * is called, changedFiles should now be empty, since all invalidation that is
   * needed to be done.
   * 
   * @param logger logs the process
   * @param compiler the compiler caches data, so must be invalidated
   */
  private void invalidateChangedFiles(TreeLogger logger,
      AbstractCompiler compiler) {
    Set<String> invalidTypes = new HashSet<String>();
    if (logger.isLoggable(TreeLogger.TRACE)) {
      TreeLogger branch = logger.branch(TreeLogger.TRACE,
          "The following compilation units have changed since "
              + "the last compilation to bytecode", null);
      for (Iterator<String> iter = changedFiles.iterator(); iter.hasNext();) {
        String filename = iter.next();
        branch.log(TreeLogger.TRACE, filename, null);
      }
    }
    for (String key : byteCodeCache.keySet()) {
      ByteCode byteCode = ((ByteCode) (byteCodeCache.get(key)));
      if (byteCode != null) {
        String location = byteCode.getLocation();
        if (changedFiles.contains(location)) {
          String binaryTypeName = byteCode.getBinaryTypeName();
          invalidTypes.add(binaryTypeName);
          removeFromCache(logger, binaryTypeName);
        }
      }
    }
    compiler.invalidateUnitsInFiles(changedFiles, invalidTypes);
    changedFiles.clear();
  }

  private boolean isFirstTime() {
    return firstTime;
  }

  private boolean isGeneratedCup(CompilationUnitProvider cup) {
    return generatedCupLocations.contains(cup.getLocation());
  }

  private boolean isTypeOracleBuilderFirstTime() {
    return typeOracleBuilderFirstTime;
  }
}
