/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.Attribute;
import com.google.gwt.dev.asm.ClassAdapter;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.ClassWriter;
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.Label;
import com.google.gwt.dev.asm.MethodAdapter;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.Util;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.server.RequestFactoryInterfaceValidator.ClassLoaderLoader;
import com.google.web.bindery.requestfactory.server.RequestFactoryInterfaceValidator.ErrorContext;
import com.google.web.bindery.requestfactory.server.RequestFactoryInterfaceValidator.Loader;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.DefaultProxyStore;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyChange;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.JsonRpcContent;
import com.google.web.bindery.requestfactory.shared.JsonRpcProxy;
import com.google.web.bindery.requestfactory.shared.JsonRpcService;
import com.google.web.bindery.requestfactory.shared.JsonRpcWireName;
import com.google.web.bindery.requestfactory.shared.Locator;
import com.google.web.bindery.requestfactory.shared.LoggingRequest;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ProxyForName;
import com.google.web.bindery.requestfactory.shared.ProxySerializer;
import com.google.web.bindery.requestfactory.shared.ProxyStore;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.RequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;
import com.google.web.bindery.requestfactory.shared.ServiceName;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.google.web.bindery.requestfactory.shared.Violation;
import com.google.web.bindery.requestfactory.shared.WriteOperation;
import com.google.web.bindery.requestfactory.vm.RequestFactorySource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

/**
 * Used to extract RequestFactory client jars from {@code gwt-user.jar}.
 */
public class RequestFactoryJarExtractor {
  /*
   * The FooProcessor types are ASM visitors that traverse the bytecode, calling
   * one of the various processFoo() methods. The visitors will also update the
   * bytecode with the rebased type names that are returned from the
   * processFoo() methods.
   */

  /**
   * Describes a way to emit the contents of a classpath, typically into a JAR
   * or filesystem directory.
   */
  public interface Emitter {
    void close() throws IOException;

    void emit(String path, InputStream contents) throws IOException;
  }

  /**
   * An Emitter implementation that creates a jar file.
   */
  public static class JarEmitter implements Emitter {
    private int rawByteSize;
    private final JarOutputStream out;

    public JarEmitter(File outFile) throws IOException {
      Manifest m = new Manifest();
      m.getMainAttributes().putValue("Created-By",
          RequestFactoryJarExtractor.class.getCanonicalName());
      m.getMainAttributes();
      out = new JarOutputStream(new FileOutputStream(outFile), m);
    }

    public void close() throws IOException {
      out.close();
    }

    public void emit(String path, InputStream contents) throws IOException {
      ZipEntry entry = new ZipEntry(path);
      out.putNextEntry(entry);
      byte[] bytes = new byte[4096];
      int read;
      for (;;) {
        read = contents.read(bytes);
        if (read == -1) {
          break;
        }
        rawByteSize += read;
        out.write(bytes, 0, read);
      }
      out.closeEntry();
    }
  }

  /**
   * Controls what is emitted by the tool.
   */
  public enum Mode {
    BOTH(true, true) {
      @Override
      protected boolean matches(String target) {
        return target.endsWith(CODE_AND_SOURCE);
      }
    },
    SOURCE(false, true) {
      @Override
      protected boolean matches(String target) {
        return target.endsWith(SOURCE_ONLY);
      }
    },
    // Order is important, must be last
    CLASSES(true, false) {
      @Override
      protected boolean matches(String target) {
        return true;
      }
    };

    public static Mode match(String target) {
      for (Mode mode : Mode.values()) {
        if (mode.matches(target)) {
          return mode;
        }
      }
      return null;
    }

    private final boolean emitClasses;
    private final boolean emitSource;

    private Mode(boolean emitClasses, boolean emitSource) {
      this.emitClasses = emitClasses;
      this.emitSource = emitSource;
    }

    public boolean isEmitClasses() {
      return emitClasses;
    }

    public boolean isEmitSource() {
      return emitSource;
    }

    protected abstract boolean matches(String target);
  }

  private class AnnotationProcessor implements AnnotationVisitor {
    private final String sourceType;
    private final AnnotationVisitor av;

    public AnnotationProcessor(String sourceType, AnnotationVisitor av) {
      this.sourceType = sourceType;
      this.av = av;
    }

    public void visit(String name, Object value) {
      value = processConstant(sourceType, value);
      av.visit(name, value);
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
      desc = processDescriptor(sourceType, desc);
      return new AnnotationProcessor(desc, av.visitAnnotation(name, desc));
    }

    public AnnotationVisitor visitArray(String name) {
      return new AnnotationProcessor(name, av.visitArray(name));
    }

    public void visitEnd() {
      av.visitEnd();
    }

    public void visitEnum(String name, String desc, String value) {
      desc = processDescriptor(sourceType, desc);
      av.visitEnum(name, desc, value);
    }
  }

  private class ClassProcessor extends ClassAdapter {
    private State state;
    private String sourceType;

    public ClassProcessor(String sourceType, ClassVisitor cv, State state) {
      super(cv);
      this.sourceType = sourceType;
      this.state = state;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
        String superName, String[] interfaces) {
      name = processInternalName(sourceType, name);
      superName = processInternalName(sourceType, superName);
      if (interfaces != null) {
        for (int i = 0, j = interfaces.length; i < j; i++) {
          interfaces[i] = processInternalName(sourceType, interfaces[i]);
        }
      }
      super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      desc = processDescriptor(sourceType, desc);
      return new AnnotationProcessor(sourceType, super.visitAnnotation(desc, visible));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
        String signature, Object value) {
      desc = processDescriptor(sourceType, desc);
      return new FieldProcessor(sourceType, super.visitField(access, name, desc, signature,
          value));
    }

    @Override
    public void visitInnerClass(String name, String outerName,
        String innerName, int access) {
      name = processInternalName(sourceType, name);
      outerName = processInternalName(sourceType, outerName);
      super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
        String signature, String[] exceptions) {
      Method method = processMethod(sourceType, name, desc);
      desc = method.getDescriptor();
      if (exceptions != null) {
        for (int i = 0, j = exceptions.length; i < j; i++) {
          exceptions[i] = processInternalName(sourceType, exceptions[i]);
        }
      }
      MethodVisitor mv = super.visitMethod(access, name, desc, signature,
          exceptions);
      if (mv != null) {
        mv = new MethodProcessor(sourceType, mv);
      }
      return mv;
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
      owner = processInternalName(sourceType, owner);
      if (desc != null) {
        desc = processMethod(sourceType, name, desc).getDescriptor();
      }
      super.visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitSource(String source, String debug) {
      if (source != null) {
        state.source = source;
      }
      super.visitSource(source, debug);
    }
  }

  /**
   * A unit of work to write one class and its source file into the Emitter.
   */
  private class EmitOneType implements Callable<Void> {
    private final State state;

    /**
     * @param state
     */
    private EmitOneType(State state) {
      this.state = state;
    }

    public Void call() throws Exception {
      if (mode.isEmitClasses()) {
        String fileName = state.type.getInternalName();
        if (fileName == null) {
          System.err.println("Got null filename from " + state.type);
          return null;
        }
        fileName += ".class";
        emitter.emit(fileName, state.contents);
      }
      if (mode.isEmitSource()) {
        String sourcePath = getPackagePath(state.originalType) + state.source;
        String destPath = getPackagePath(state.type) + state.source;
        if (sources.add(sourcePath) && loader.exists(sourcePath)) {
          String contents = Util.readStreamAsString(loader.getResourceAsStream(sourcePath));
          emitter.emit(destPath,
              new ByteArrayInputStream(Util.getBytes(contents)));
        }
      }
      return null;
    }
  }

  // There is no FieldAdapter type
  private class FieldProcessor implements FieldVisitor {
    private final String sourceType;
    private final FieldVisitor fv;

    public FieldProcessor(String sourceType, FieldVisitor fv) {
      this.sourceType = sourceType;
      this.fv = fv;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return new AnnotationProcessor(sourceType, fv.visitAnnotation(desc, visible));
    }

    public void visitAttribute(Attribute attr) {
      fv.visitAttribute(attr);
    }

    public void visitEnd() {
      fv.visitEnd();
    }
  }

  private class MethodProcessor extends MethodAdapter {
    private final String sourceType;
    
    public MethodProcessor(String sourceType, MethodVisitor mv) {
      super(mv);
      this.sourceType = sourceType;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      desc = processDescriptor(sourceType, desc);
      return super.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
      return new AnnotationProcessor(sourceType, super.visitAnnotationDefault());
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
        String desc) {
      owner = processInternalName(sourceType, owner);
      desc = processDescriptor(sourceType, desc);
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
        Object[] stack) {
      for (int i = 0, j = local.length; i < j; i++) {
        if (local[i] instanceof String) {
          local[i] = processInternalName(sourceType, (String) local[i]);
        }
      }
      for (int i = 0, j = stack.length; i < j; i++) {
        if (stack[i] instanceof String) {
          stack[i] = processInternalName(sourceType, (String) stack[i]);
        }
      }
      super.visitFrame(type, nLocal, local, nStack, stack);
    }

    @Override
    public void visitLdcInsn(Object cst) {
      cst = processConstant(sourceType, cst);
      super.visitLdcInsn(cst);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature,
        Label start, Label end, int index) {
      desc = processDescriptor(sourceType, desc);
      super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
        String desc) {
      owner = processInternalName(sourceType, owner);
      desc = processMethod(sourceType, name, desc).getDescriptor();
      super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      desc = processDescriptor(sourceType, desc);
      super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter,
        String desc, boolean visible) {
      desc = processDescriptor(sourceType, desc);
      return super.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler,
        String type) {
      type = processInternalName(sourceType, type);
      super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
      type = processInternalName(sourceType, type);
      super.visitTypeInsn(opcode, type);
    }
  }

  /**
   * Replaces native methods with stub implementations that throw an exception.
   * This allows any dangling GWT types to be loaded by a JVM without triggering
   * an {@link UnsatisfiedLinkError}.
   */
  private class NativeMethodDefanger extends ClassAdapter {
    public NativeMethodDefanger(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
        String signature, String[] exceptions) {
      if ((access & Opcodes.ACC_NATIVE) != 0) {
        MethodVisitor mv = super.visitMethod(access & ~Opcodes.ACC_NATIVE,
            name, desc, signature, exceptions);
        if (mv != null) {
          mv.visitCode();
          String exceptionName = Type.getInternalName(RuntimeException.class);
          // <empty>
          mv.visitTypeInsn(Opcodes.NEW, exceptionName);
          // obj
          mv.visitInsn(Opcodes.DUP);
          // obj, obj
          mv.visitLdcInsn(NATIVE_METHOD_ERROR);
          // obj, obj, string
          mv.visitMethodInsn(Opcodes.INVOKESPECIAL, exceptionName, "<init>",
              "(Ljava/lang/String;)V");
          // obj
          mv.visitInsn(Opcodes.ATHROW);

          // Count argument slots - long and double arguments each take up 2 slots
          int numSlots = 0;
          for (Type t : Type.getArgumentTypes(desc)) {
            numSlots += t.getSize();
          }
          if ((access & Opcodes.ACC_STATIC) == 0) {
            // Add one for 'this' reference
            numSlots++;
          }
          mv.visitMaxs(3, numSlots);
          mv.visitEnd();
        }
        return null;
      } else {
        return super.visitMethod(access, name, desc, signature, exceptions);
      }
    }
  }

  /**
   * This is the main bytecode-processing entry point. It will read in one
   * classfile and produce a mutated copy. Any referenced types will be enqueued
   * via {@link RequestFactoryJarExtractor#processType(String, Type)}.
   */
  private class ProcessOneType implements Callable<State> {

    private final State state;
    private final String typeName;

    public ProcessOneType(Type type) {
      state = new State(type);
      typeName = type.getClassName();
    }

    public State call() {
      ClassWriter writer = new ClassWriter(0);
      ClassVisitor cv = writer;
      cv = new ClassProcessor(typeName, cv, state);
      cv = new NativeMethodDefanger(cv);
      visit(logger.setType(state.type), loader, state.type.getInternalName(),
          cv);
      state.contents = new ByteArrayInputStream(writer.toByteArray());
      assert seen.containsKey(state.originalType) : "No type for "
          + state.type.getClassName();
      state.type = seen.get(state.originalType);

      emit(state);
      return state;
    }
  }

  /**
   * Metadata about a single type.
   */
  private static class State {
    boolean containsNativeMethods;
    /**
     * Will contain the data to be written to disk, possibly mutated class data.
     */
    InputStream contents;
    String source;
    /**
     * The possibly rebased type name.
     */
    Type type;
    final Type originalType;

    public State(Type type) {
      this.originalType = this.type = type;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(type.getInternalName());
      if (containsNativeMethods) {
        sb.append(" NATIVE");
      }
      if (source != null) {
        sb.append(" ").append(source);
      }
      return sb.toString();
    }
  }

  /**
   * If true, print a trace of dependencies to System.out.
   */
  private static final boolean VERBOSE = false;

  /**
   * 
   */
  private static final String CODE_AND_SOURCE = "+src";

  /**
   * 
   */
  private static final String SOURCE_ONLY = "-src";

  private static final String NATIVE_METHOD_ERROR = "Cannot call native method";

  /**
   * A map of target names to the types that target should use as a base.
   */
  private static final Map<String, List<Class<?>>> SEEDS = new LinkedHashMap<String, List<Class<?>>>();

  /**
   * Server public API classes and interfaces.
   */
  private static final Class<?>[] SERVER_CLASSES = {
      DefaultExceptionHandler.class, ExceptionHandler.class, Logging.class, LoggingRequest.class,
      RequestFactoryServlet.class, ServiceLayer.class, ServiceLayerDecorator.class,
      SimpleRequestProcessor.class
  };

  /**
   * Shared public API classes and interfaces.
   */
  private static final Class<?>[] SHARED_CLASSES = {
      BaseProxy.class, DefaultProxyStore.class, EntityProxy.class, EntityProxyChange.class,
      EntityProxyId.class, InstanceRequest.class, JsonRpcContent.class, JsonRpcProxy.class,
      JsonRpcService.class, JsonRpcWireName.class, Locator.class, ProxyFor.class,
      ProxyForName.class, ProxySerializer.class, ProxyStore.class, Receiver.class, Request.class,
      RequestContext.class, RequestFactory.class, RequestTransport.class, ServerFailure.class,
      Service.class, ServiceLocator.class, ServiceName.class, ValueProxy.class, Violation.class,
      WriteOperation.class, RequestFactorySource.class, SimpleEventBus.class
  };

  static {
    List<Class<?>> sharedClasses = Arrays.<Class<?>> asList(SHARED_CLASSES);

    List<Class<?>> clientClasses = new ArrayList<Class<?>>();
    clientClasses.addAll(sharedClasses);

    List<Class<?>> serverClasses = new ArrayList<Class<?>>();
    serverClasses.addAll(Arrays.<Class<?>> asList(SERVER_CLASSES));
    serverClasses.addAll(sharedClasses);

    SEEDS.put("client", Collections.unmodifiableList(clientClasses));
    SEEDS.put("server", Collections.unmodifiableList(serverClasses));

    Set<Class<?>> all = new LinkedHashSet<Class<?>>();
    for (List<Class<?>> value : SEEDS.values()) {
      all.addAll(value);
    }
    SEEDS.put("all", Collections.unmodifiableList(new ArrayList<Class<?>>(all)));

    for (String target : new ArrayList<String>(SEEDS.keySet())) {
      SEEDS.put(target + SOURCE_ONLY, SEEDS.get(target));
      SEEDS.put(target + CODE_AND_SOURCE, SEEDS.get(target));
    }

    /*
     * Allows the rebased package to be tested. This is done with a by-name
     * lookup, since the gwt-user code is compiled separately from its tests.
     */
    try {
      SEEDS.put(
          "test" + CODE_AND_SOURCE,
          Collections.unmodifiableList(Arrays.<Class<?>> asList(
              Class.forName("com.google.web.bindery.requestfactory.vm.RequestFactoryJreSuite"),
              Class.forName("com.google.web.bindery.requestfactory.server.SimpleBar"))));
    } catch (ClassNotFoundException ignored) {
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: java -cp gwt-dev.jar:gwt-user.jar:json.jar"
          + RequestFactoryJarExtractor.class.getCanonicalName()
          + " <target-name> outfile.jar");
      System.err.println("Valid targets:");
      for (String target : SEEDS.keySet()) {
        System.err.println("  " + target);
      }
      System.exit(1);
    }
    String target = args[0];
    List<Class<?>> seeds = SEEDS.get(target);
    if (seeds == null) {
      System.err.println("Unknown target: " + target);
      System.exit(1);
    }
    Mode mode = Mode.match(target);
    Logger errorContext = Logger.getLogger(RequestFactoryJarExtractor.class.getName());
    ClassLoaderLoader classLoader = new ClassLoaderLoader(
        Thread.currentThread().getContextClassLoader());
    JarEmitter jarEmitter = new JarEmitter(new File(args[1]));
    RequestFactoryJarExtractor extractor = new RequestFactoryJarExtractor(
        errorContext, classLoader, jarEmitter, seeds, mode);
    extractor.run();
    System.exit(0);
  }

  /**
   * Given a Type, return a path-prefix based on the type's package.
   */
  private static String getPackagePath(Type t) {
    String name = t.getInternalName();
    return name.substring(0, name.lastIndexOf('/') + 1);
  }

  /**
   * Load the classfile for the given binary name and apply the provided
   * visitor.
   * 
   * @return <code>true</code> if the visitor was successfully visited
   */
  private static boolean visit(ErrorContext logger, Loader loader,
      String internalName, ClassVisitor visitor) {
    assert Name.isInternalName(internalName) : "internalName";
    logger.spam("Visiting " + internalName);
    InputStream inputStream = null;
    try {
      inputStream = loader.getResourceAsStream(internalName + ".class");
      if (inputStream == null) {
        System.err.println("Could not find class file for " + internalName);
        logger.poison("Could not find class file for " + internalName);
        return false;
      }
      ClassReader reader = new ClassReader(inputStream);
      reader.accept(visitor, 0);
      return true;
    } catch (IOException e) {
      logger.poison("Unable to open " + internalName, e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException ignored) {
        }
      }
    }
    return false;
  }

  private final Emitter emitter;
  private final ExecutorService ex;
  private final BlockingQueue<Future<?>> inProcess = new LinkedBlockingQueue<Future<?>>();
  private final ErrorContext logger;
  private final Loader loader;
  private final Mode mode;
  private final List<Class<?>> seeds;
  private final Map<Type, Type> seen = new HashMap<Type, Type>();
  private final Set<String> sources = new HashSet<String>();
  private final ExecutorService writerService;

  public RequestFactoryJarExtractor(Logger logger, Loader loader,
      Emitter emitter, List<Class<?>> seeds, Mode mode) {
    this.logger = new ErrorContext(logger);
    this.loader = loader;
    this.emitter = emitter;
    this.seeds = seeds;
    this.mode = mode;

    ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    writerService = Executors.newSingleThreadExecutor();
  }

  /**
   * Blocks until all work has been finished.
   */
  public void run() throws IOException {
    for (Class<?> seed : seeds) {
      processType("seeds", Type.getType(seed));
    }
    // Wait for all tasks to be completed
    while (!inProcess.isEmpty()) {
      try {
        Future<?> task = inProcess.take();
        task.get();
      } catch (InterruptedException retry) {
      } catch (ExecutionException e) {
        e.getCause().printStackTrace();
      }
    }
    emitter.close();
  }

  /**
   * Write one type into the output.
   */
  private void emit(final State state) {
    inProcess.add(writerService.submit(new EmitOneType(state)));
  }

  /**
   * Look at constant values from the bytecode, processing referenced types.
   */
  private Object processConstant(String sourceType, Object value) {
    if (value instanceof Type) {
      value = processType(sourceType, (Type) value);
    }
    return value;
  }

  /**
   * Process the type represented by the descriptor, possibly returning a
   * rebased descriptor.
   */
  private String processDescriptor(String sourceType, String desc) {
    if (desc == null) {
      return null;
    }
    return processType(sourceType, Type.getType(desc)).getDescriptor();
  }

  /**
   * Process the type represented by the name, possibly returning a rebased
   * name.
   */
  private String processInternalName(String sourceType, String internalName) {
    if (internalName == null) {
      return null;
    }
    return processType(sourceType, Type.getObjectType(internalName)).getInternalName();
  }

  /**
   * Produce a rebased method declaration, also visiting referenced types.
   */
  private Method processMethod(String sourceType, String name, String desc) {
    Method method = new Method(name, desc);
    Type[] argumentTypes = method.getArgumentTypes();
    for (int i = 0, j = argumentTypes.length; i < j; i++) {
      argumentTypes[i] = processType(sourceType, argumentTypes[i]);
    }
    method = new Method(name, processType(sourceType, method.getReturnType()),
        argumentTypes);
    return method;
  }

  /**
   * Process a type, possibly returning a rebased type.
   * @param sourceType TODO
   */
  private Type processType(String sourceType, Type type) {
    Type toReturn;
    synchronized (seen) {
      toReturn = seen.get(type);
      if (toReturn != null) {
        return toReturn;
      }
      toReturn = Type.getType(type.getDescriptor());
      seen.put(type, toReturn);
    }
    int sort = type.getSort();
    if (sort != Type.OBJECT && sort != Type.ARRAY) {
      return toReturn;
    }
    if (sort == Type.ARRAY) {
      processType(sourceType, type.getElementType());
      return toReturn;
    }
    assert type.getInternalName().charAt(0) != 'L';
    if (type.getInternalName().startsWith("java/")
        || type.getInternalName().startsWith("javax/")) {
      return toReturn;
    }
    if (VERBOSE) {
      System.out.println(sourceType + " -> " + type.getClassName());
    }
    Future<State> future = ex.submit(new ProcessOneType(type));
    inProcess.add(future);
    return toReturn;
  }
}
