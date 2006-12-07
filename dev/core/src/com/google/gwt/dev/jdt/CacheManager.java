/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.shell.JavaScriptHost;
import com.google.gwt.dev.shell.ShellGWT;
import com.google.gwt.dev.shell.ShellJavaScriptHost;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
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
    private final Map map = new IdentityHashMap();

    public JClassType get(SourceTypeBinding binding) {
      JClassType type = (JClassType) map.get(binding);
      return type;
    }

    public void put(SourceTypeBinding binding, JClassType type) {
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
    private Map map = new HashMap();

    /**
     * This method adds <code>item</code> to the list stored under
     * <code>key</code>.
     * 
     * @param key the key used to access the list
     * @param item the item to be added to the list
     */
    private void add(String dependerFilename, String dependeeFilename) {
      if (!map.containsKey(dependeeFilename)) {
        map.put(dependeeFilename, new HashSet());
      }
      get(dependeeFilename).add(dependerFilename);
    }

    /**
     * This method gets the list stored under <code>key</code>.
     * 
     * @param key the key used to access the list.
     * @return the list stored under <code>key</code>
     */
    private Set get(String filename) {
      return (Set) map.get(filename);
    }

    private void remove(String filename) {
      map.remove(filename);
    }

    private Set transitiveClosure(final String filename) {
      String current = filename;
      TreeSet queue = new TreeSet();
      Set finished = new HashSet();
      queue.add(filename);
      while (true) {
        finished.add(current);
        Set children = get(filename);
        if (children != null) {
          for (Iterator iter = children.iterator(); iter.hasNext();) {
            String child = (String) iter.next();
            if (!finished.contains(child)) {
              queue.add(child);
            }
          }
        }
        if (queue.size() == 0) {
          return finished;
        } else {
          current = (String) queue.first();
          queue.remove(current);
        }
      }
    }
  }

  /**
   * Caches information using a directory, with an in memory cache to prevent
   * unneeded disk access.
   */
  private static class DiskCache extends AbstractMap {

    private class FileEntry implements Map.Entry {

      private File file;

      private FileEntry(File file) {
        this.file = file;
      }

      private FileEntry(Object name) {
        this(new File(directory, possiblyAddTmpExtension(name)));
      }

      private FileEntry(Object name, Object o) {
        this(new File(directory, possiblyAddTmpExtension(name)));
        setValue(o);
      }

      public Object getKey() {
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

    private final Map cache = new HashMap();

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

    public void clear() {
      cache.clear();
      if (directory != null) {
        for (Iterator iter = keySet().iterator(); iter.hasNext();) {
          iter.remove();
        }
      }
    }

    public Set entrySet() {
      Set out = new HashSet() {
        public boolean remove(Object o) {
          boolean removed = (DiskCache.this.remove(((Entry) o).getKey())) != null;
          super.remove(o);
          return removed;
        }
      };
      out.addAll(cache.entrySet());
      // No directory means no persistance.
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

    public Object get(Object key) {
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

    public Set keySet() {
      Set out = new HashSet() {
        public boolean remove(Object o) {
          boolean removed = (DiskCache.this.remove(o)) != null;
          super.remove(o);
          return removed;
        }
      };
      out.addAll(cache.keySet());
      // No directory means no persistance.
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

    public Object put(Object key, Object value) {
      return put(key, value, true);
    }

    public Object remove(Object key) {
      Object out = get(key);
      // No directory means no persistance.
      if (directory != null) {
        possiblyCreateCacheDirectory();
        FileEntry e = new FileEntry(key);
        e.remove();
      }
      cache.remove(key);
      return out;
    }

    private long lastModified(Object key) {
      if (directory == null) {
        // we have no file on disk to refer to, so should return the same result
        // as if the file did not exist -- namely 0.
        return 0;
      }
      return new FileEntry(key).lastModified();
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

    private Object put(Object key, Object value, boolean persist) {
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
  public static final Class[] BOOTSTRAP_CLASSES = new Class[] {
      JavaScriptHost.class, ShellJavaScriptHost.class, ShellGWT.class};

  /**
   * The set of bootstrap classes, which are marked transient, but are
   * nevertheless not recompiled each time, as they are bootstrap classes.
   */
  private static final Set TRANSIENT_CLASS_NAMES;

  static {
    TRANSIENT_CLASS_NAMES = new HashSet(BOOTSTRAP_CLASSES.length + 3);
    for (int i = 0; i < BOOTSTRAP_CLASSES.length; i++) {
      TRANSIENT_CLASS_NAMES.add(BOOTSTRAP_CLASSES[i].getName());
    }
  }

  // This method must be outside of DiskCache because of the restruction against
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

  // This method must be outside of DiskCache because of the restruction against
  // defining static methods in inner classes.
  private static String possiblyRemoveTmpExtension(Object fileName) {
    String className = fileName.toString();
    if (className.indexOf("-") != -1) {
      className = className.split("-")[0];
    }
    return className;
  }

  private final Set addedCups = new HashSet();

  private final AstCompiler astCompiler;

  private final DiskCache byteCodeCache;

  private final File cacheDir;

  private final Set changedFiles;

  private final Map cudsByFileName;

  private final Map cupsByLocation = new HashMap();

  private boolean firstTime = true;

  private final Mapper identityMapper = new Mapper();

  private final Set invalidatedTypes = new HashSet();

  private final TypeOracle oracle;

  private final Map timesByLocation = new HashMap();

  private boolean typeOracleBuilderFirstTime = true;

  private final Map unitsByCup = new HashMap();

  private final Set volatileFiles = new HashSet();

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
    changedFiles = new HashSet();
    cudsByFileName = new HashMap();
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
    for (Iterator iter = addedCups.iterator(); iter.hasNext();) {
      CompilationUnitProvider cup = (CompilationUnitProvider) iter.next();
      if (isVolatileFile(cup.getLocation())) {
        iter.remove();
      }
    }
  }

  /**
   * This method marks the supplied compilation units as volatile, ensuring that
   * they are not cached across a reload. This is used to ensure
   * generator-created code is not improperly preserved.
   * 
   * @param committedGeneratedCups the set of compilation units to not preserve
   */
  public void markVolatileFiles(Set committedGeneratedCups) {
    for (Iterator iter = committedGeneratedCups.iterator(); iter.hasNext();) {
      CompilationUnitProvider cup = (CompilationUnitProvider) iter.next();
      volatileFiles.add(cup.getLocation());
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
      if (getByteCode(logger, binaryTypeName) == null) {
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

    // induction
    TypeRefVisitor trv = new TypeRefVisitor() {
      protected void onTypeRef(SourceTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer) {
        // If the referenced type belongs to a compilation unit that
        // was changed, then the unit in which it
        // is referenced must also be treated as changed.
        //
        String referencedFn = String.valueOf(referencedType.getFileName());
        CompilationUnitDeclaration referencedCup = (CompilationUnitDeclaration) cudsByFileName.get(referencedFn);
        String fileName = String.valueOf(unitOfReferrer.getFileName());
        dependencies.add(fileName, referencedFn);
      };
    };
    // Find references to type in units that aren't any longer valid.
    //
    for (Iterator iter = cudsByFileName.values().iterator(); iter.hasNext();) {
      CompilationUnitDeclaration cud = (CompilationUnitDeclaration) iter.next();
      cud.traverse(trv, cud.scope);
    }
    Set toTraverse = new HashSet(changedFiles);
    for (Iterator iter = toTraverse.iterator(); iter.hasNext();) {
      String fileName = (String) iter.next();
      changedFiles.addAll(dependencies.transitiveClosure(fileName));
    }
  }

  void addVolatileFiles(Set files) {
    files.addAll(volatileFiles);
  }

  ICompilationUnit findUnitForCup(CompilationUnitProvider cup) {
    if (!unitsByCup.containsKey(cup.getLocation())) {
      unitsByCup.put(cup.getLocation(), new ICompilationUnitAdapter(cup));
    }
    return (ICompilationUnit) unitsByCup.get(cup.getLocation());
  }

  Set getAddedCups() {
    return addedCups;
  }

  AstCompiler getAstCompiler() {
    return astCompiler;
  }

  /**
   * Gets the bytecode from the cache, rejecting it if an incompatible change
   * occured since it was cached.
   */
  ByteCode getByteCode(TreeLogger logger, String binaryTypeName) {
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

  Set getChangedFiles() {
    return changedFiles;
  }

  Map getCudsByFileName() {
    return cudsByFileName;
  }

  CompilationUnitProvider getCup(CompilationUnitProvider cup) {
    return (CompilationUnitProvider) getCupsByLocation().get(cup.getLocation());
  }

  Object getCupLastUpdateTime(CompilationUnitProvider cup) {
    return getTimesByLocation().get(cup.getLocation());
  }

  Map getCupsByLocation() {
    return cupsByLocation;
  }

  Mapper getIdentityMapper() {
    return identityMapper;
  }

  Map getTimesByLocation() {
    return timesByLocation;
  }

  JType getTypeForBinding(SourceTypeBinding sourceTypeBinding) {
    return identityMapper.get(sourceTypeBinding);
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
    // The inital change set is computed in addCompilationUnit.
    // For the first time we do not do this because the compiler
    // has no cached info.
    if (!isTypeOracleBuilderFirstTime()) {
      addVolatileFiles(changedFiles);
      addDependentsToChangedFiles();
      for (Iterator iter = changedFiles.iterator(); iter.hasNext();) {
        String location = (String) iter.next();
        CompilationUnitProvider cup = (CompilationUnitProvider) getCupsByLocation().get(
            location);
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
      if (getByteCode(logger, binaryTypeName) == null) {
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
      Set classNames = byteCodeCache.keySet();
      for (Iterator iter = classNames.iterator(); iter.hasNext();) {
        Object className = iter.next();
        ByteCode byteCode = ((ByteCode) (byteCodeCache.get(className)));
        if (byteCode == null) {
          iter.remove();
          continue;
        }
        String qname = byteCode.getBinaryTypeName();
        if (TRANSIENT_CLASS_NAMES.contains(qname)) {
          continue;
        }
        if (byteCode != null) {
          String location = byteCode.getLocation();
          if (byteCode.isTransient()) {
            // GWT transient classes; no need to test.
            // Either standardGeneratorContext created it
            // in which case we already know it is invalid
            // or its something like GWT and it lives.
            continue;
          }
          String fileName = Util.findFileName(location);
          CompilationUnitDeclaration compilationUnitDeclaration = ((CompilationUnitDeclaration) cudsByFileName.get(location));
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
    Set invalidTypes = new HashSet();
    if (logger.isLoggable(TreeLogger.TRACE)) {
      TreeLogger branch = logger.branch(TreeLogger.TRACE,
          "The following compilation units have changed since "
              + "the last compilation to bytecode", null);
      for (Iterator iter = changedFiles.iterator(); iter.hasNext();) {
        String filename = (String) iter.next();
        branch.log(TreeLogger.TRACE, filename, null);
      }
    }
    for (Iterator iter = byteCodeCache.keySet().iterator(); iter.hasNext();) {
      Object key = iter.next();
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

  private boolean isTypeOracleBuilderFirstTime() {
    return typeOracleBuilderFirstTime;
  }

  private boolean isVolatileFile(String location) {
    if (volatileFiles.contains(location)) {
      return true;
    } else {
      return false;
    }
  }
}
