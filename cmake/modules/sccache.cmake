find_program(CCACHE_PROGRAM sccache)
message(STATUS "Looking for SCCACHE...")

if(CCACHE_PROGRAM)
    message(STATUS "Found SCCACHE: ${CCACHE_PROGRAM}")
    # Support Unix Makefiles and Ninja. See https://stackoverflow.com/a/24305849/1170370
    # set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE "${CCACHE_PROGRAM}")
    set(CMAKE_C_COMPILER_LAUNCHER ${CCACHE_PROGRAM})
    set(CMAKE_CXX_COMPILER_LAUNCHER ${CCACHE_PROGRAM})

    # sccache + MSVC: parallel cl.exe invocations sharing a PDB file (/Zi) cause
    # C1041 "cannot open program database" errors. Use embedded debug info (/Z7)
    # instead -- each object file carries its own symbols, no shared PDB needed.
    #
    # CMP0141 (CMake 3.25+) is the authoritative way to set this for ALL targets
    # including FetchContent subprojects (e.g. libgit2). When the policy is
    # available we set CMAKE_MSVC_DEBUG_INFORMATION_FORMAT so CMake stops
    # injecting /Zi from the per-config flag strings entirely.
    # For older CMake we fall back to replacing /Zi in the flag variables, which
    # covers the main project but may not reach every subproject.
    if(MSVC)
        if(POLICY CMP0141)
            cmake_policy(SET CMP0141 NEW)
            set(CMAKE_MSVC_DEBUG_INFORMATION_FORMAT "Embedded" CACHE STRING
                "Force /Z7 (embedded debug info) so sccache can cache MSVC compilations" FORCE)
        endif()
        foreach(_lang C CXX)
            foreach(_config DEBUG RELWITHDEBINFO MINSIZEREL RELEASE)
                string(REGEX REPLACE "/Z[iI]" "/Z7"
                    CMAKE_${_lang}_FLAGS_${_config}
                    "${CMAKE_${_lang}_FLAGS_${_config}}")
            endforeach()
        endforeach()
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
