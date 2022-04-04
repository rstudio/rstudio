include(FetchContent)
FetchContent_Declare(fmt
   GIT_REPOSITORY https://github.com/fmtlib/fmt
   GIT_TAG b6f4ceaed0a0a24ccf575fab6c56dd50ccf6f1a9 # 8.1.1
   )

FetchContent_MakeAvailable(fmt)

