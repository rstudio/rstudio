package com.google.gwt.dev.codeserver;

/**
 * A callback interface that can be used to find out when Super Dev Mode starts and
 * finishes its compiles.
 */
public interface RecompileListener {

  /**
   * Called when starting a compile.
   * @param moduleName The name of the module being compiled, before renaming.
   * @param compileId An id for this compile, starting from 1 for the first compile.
   *                  The combination of (moduleName, compileId) is unique within
   *                  a Code Server process. After the CodeServer is restarted, starts again from 1.
   * @param compileDir The directory used for this compile. Contains subdirectories for
   *                   working files and compiler output.
   */
  void startedCompile(String moduleName, int compileId, CompileDir compileDir);

  /**
   * Called when a compile finishes.
   * @param moduleName The same name passed to startedCompile
   * @param compileId The same id passed to startedCompile
   * @param success True if the compile succeeded.
   */
  void finishedCompile(String moduleName, int compileId, boolean success);

  /**
   * A listener that does nothing.
   */
  RecompileListener NONE = new RecompileListener() {
    @Override
    public void startedCompile(String moduleName, int compileId, CompileDir compileDir) {
    }

    @Override
    public void finishedCompile(String moduleName, int compileId, boolean success) {
    }
  };

}
