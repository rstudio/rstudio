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
    # C1041 "cannot open program database" errors. Use embedded debug info (/Z7)
    # instead -- each object file carries its own symbols, no shared PDB needed.
    #
    # Three layers, most-to-least reliable:
    # 1. add_compile_options(/Z7) -- directory property, inherited by ALL
    #    subdirectories including FetchContent subprojects (libgit2, pcre, ...).
    # 2. CACHE FORCE on per-config flag variables -- strips /Zi so it doesn't
    #    appear alongside /Z7 in the final command line.
    # 3. CMP0141 (CMake 3.25+) -- tells CMake to stop managing debug-info flags
    #    through the flags variables entirely; uses target properties instead.
    if(MSVC)
        add_compile_options(/Z7)
        foreach(_lang C CXX)
            foreach(_config DEBUG RELWITHDEBINFO MINSIZEREL RELEASE)
                get_property(_flags CACHE CMAKE_${_lang}_FLAGS_${_config} PROPERTY VALUE)
                if(_flags)
                    string(REGEX REPLACE "/Z[iI]" "" _flags "${_flags}")
                    set(CMAKE_${_lang}_FLAGS_${_config} "${_flags}" CACHE STRING "" FORCE)
                endif()
            endforeach()
        endforeach()
        if(POLICY CMP0141)
            cmake_policy(SET CMP0141 NEW)
            set(CMAKE_MSVC_DEBUG_INFORMATION_FORMAT "Embedded" CACHE STRING "" FORCE)
        endif()

        # Disable C++20 module-dependency scanning. CMake 3.28+ on MSVC + Ninja
        # with -std:c++20 auto-generates two compile rules per target -- a
        # `_scanned_` rule that adds `@$DYNDEP_MODULE_MAP_FILE` to the cl.exe
        # command line, and a regular `_unscanned_` rule. sccache 0.15.x doesn't
        # understand the dyndep response-file argument and silently bails to
        # direct compilation without registering the request, so every CXX
        # compile bypasses the cache and `sccache --show-stats` reports
        # `Compile requests: 0` despite the launcher being wired correctly.
        # RStudio doesn't actually use C++20 modules, so the scanner only ever
        # confirms "no modules here" -- turning it off has no functional impact
        # but unblocks sccache. macOS is unaffected because it builds with
        # C++17 and never gets the dyndep rules.
        set(CMAKE_CXX_SCAN_FOR_MODULES OFF CACHE BOOL "Disable C++20 module scanning so sccache can wrap cl.exe" FORCE)
        message(STATUS "sccache CMAKE_CXX_SCAN_FOR_MODULES: OFF (avoids @DYNDEP_MODULE_MAP_FILE which sccache 0.15.x can't handle)")
    endif()
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
