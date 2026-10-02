find_program(CCACHE_PROGRAM sccache)
message(STATUS "Looking for SCCACHE...")

if(CCACHE_PROGRAM)
    message(STATUS "Found SCCACHE: ${CCACHE_PROGRAM}")
    # Set the per-language compiler launcher as a CACHE STRING so it survives
    # incremental reconfigures and every add_subdirectory() boundary
    # (including FetchContent subprojects). A plain set() in an include()-d
    # file is scoped to the calling list-file and can be lost by the time a
    # sub-target picks up its properties.
    #
    # Use only this mechanism -- NOT RULE_LAUNCH_COMPILE. On the Ninja
    # generator the two stack additively rather than acting as fallbacks,
    # producing compile rules like `sccache sccache c++ -E ...` and breaking
    # with `Compiler not supported: unexpected argument '-E' found` because
    # sccache calls itself as its own compiler.
    set(CMAKE_C_COMPILER_LAUNCHER   "${CCACHE_PROGRAM}" CACHE STRING "sccache wrapper for C"   FORCE)
    set(CMAKE_CXX_COMPILER_LAUNCHER "${CCACHE_PROGRAM}" CACHE STRING "sccache wrapper for CXX" FORCE)

    # Diagnostic so the cmake configure log makes it obvious that sccache
    # was actually wired. Sccache reports `Compile requests: 0` when the
    # launcher never reaches the compile rules, which is silent unless
    # someone reads --show-stats at the end of the build.
    message(STATUS "sccache CMAKE_C_COMPILER_LAUNCHER:   ${CMAKE_C_COMPILER_LAUNCHER}")
    message(STATUS "sccache CMAKE_CXX_COMPILER_LAUNCHER: ${CMAKE_CXX_COMPILER_LAUNCHER}")

    # sccache + MSVC: parallel cl.exe invocations sharing a PDB file (/Zi) cause
    # C1041 "cannot open program database" errors, so sccache needs embedded
    # debug info (/Z7) -- each object file carries its own symbols, no shared
    # PDB needed. Nothing to do for that here: compiler.cmake selects /Z7 via
    # CMAKE_MSVC_DEBUG_INFORMATION_FORMAT for every build, and CMake adds that
    # flag after a target's other flags, so it also overrides the /Zi that
    # FetchContent subprojects (libgit2) write into their own flags.
endif()

# ccache compatibility with XCode
# see https://stackoverflow.com/a/36515503/1170370
get_property(RULE_LAUNCH_COMPILE GLOBAL PROPERTY RULE_LAUNCH_COMPILE)
if(RULE_LAUNCH_COMPILE AND CMAKE_GENERATOR STREQUAL "Xcode")
    # Set up wrapper scripts
    configure_file(launch-c.in   launch-c)
    configure_file(launch-cxx.in launch-cxx)
    execute_process(COMMAND chmod a+rx
                            "${CMAKE_BINARY_DIR}/launch-c"
                            "${CMAKE_BINARY_DIR}/launch-cxx")

    # Set Xcode project attributes to route compilation through our scripts
    set(CMAKE_XCODE_ATTRIBUTE_CC         "${CMAKE_BINARY_DIR}/launch-c")
    set(CMAKE_XCODE_ATTRIBUTE_CXX        "${CMAKE_BINARY_DIR}/launch-cxx")
    set(CMAKE_XCODE_ATTRIBUTE_LD         "${CMAKE_BINARY_DIR}/launch-c")
    set(CMAKE_XCODE_ATTRIBUTE_LDPLUSPLUS "${CMAKE_BINARY_DIR}/launch-cxx")
endif()
