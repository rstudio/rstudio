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
