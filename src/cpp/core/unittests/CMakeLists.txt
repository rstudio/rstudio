
# Only build GTest-based tests when unit tests are enabled
if (RSTUDIO_UNIT_TESTS_ENABLED)

   set(CORE_TESTS_SOURCE_FILES
      main.cpp
      ThreadsafeQueue_tests.cpp
   )

   add_executable(core_tests
       ${CORE_TESTS_SOURCE_FILES}
   )

   set_target_properties(core_tests PROPERTIES
       CXX_STANDARD 17
       CXX_STANDARD_REQUIRED ON
   )

   target_include_directories(core_tests PRIVATE
      ${CMAKE_CURRENT_SOURCE_DIR}/include
      ${CMAKE_CURRENT_SOURCE_DIR}/../include
      ${CMAKE_CURRENT_SOURCE_DIR}/../../shared_core/include
      ${EXT_SOURCE_DIR}
      ${CMAKE_CURRENT_BINARY_DIR}
   )

   target_link_libraries(core_tests PRIVATE
       rstudio-core
       rstudio-shared-core
       pthread
       gtest_main
       ${Boost_LIBRARIES}
       ${CORE_SYSTEM_LIBRARIES}
   )

   # Enable testing and add test discovery
   enable_testing()
   include(GoogleTest)
   gtest_discover_tests(core_tests)

endif()

