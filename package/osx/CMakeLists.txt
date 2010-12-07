
# package attributes
set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}")

# create drag and drop installer
set(CPACK_BINARY_DRAGNDROP ON)

include(CPack)
