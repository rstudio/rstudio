# From http://code.google.com/p/pam-face-authentication/source/browse/branches/pam_face_authentication/cmake/modules/FindPAM.cmake?r=336

# - Try to find the PAM libraries
# Once done this will define
#
#  PAM_FOUND - system has pam
#  PAM_INCLUDE_DIR - the pam include directory
#  PAM_LIBRARIES - libpam library

if (PAM_INCLUDE_DIR AND PAM_LIBRARY)
	# Already in cache, be silent
	set(PAM_FIND_QUIETLY TRUE)
endif (PAM_INCLUDE_DIR AND PAM_LIBRARY)

find_path(PAM_INCLUDE_DIR NAMES security/pam_appl.h pam/pam_appl.h)
find_library(PAM_LIBRARY pam)
find_library(DL_LIBRARY dl)

if (PAM_INCLUDE_DIR AND PAM_LIBRARY)
	set(PAM_FOUND TRUE)
	if (DL_LIBRARY)
		set(PAM_LIBRARIES ${PAM_LIBRARY} ${DL_LIBRARY})
	else (DL_LIBRARY)
		set(PAM_LIBRARIES ${PAM_LIBRARY})
	endif (DL_LIBRARY)

	if (EXISTS ${PAM_INCLUDE_DIR}/pam/pam_appl.h)
		# darwin claims to be something special
		set(HAVE_PAM_PAM_APPL_H 1)
	endif (EXISTS ${PAM_INCLUDE_DIR}/pam/pam_appl.h)

	if (NOT DEFINED PAM_MESSAGE_CONST)
		include(CheckCXXSourceCompiles)
		# XXX does this work with plain c?
		check_cxx_source_compiles("
#if ${HAVE_PAM_PAM_APPL_H}+0
# include <pam/pam_appl.h>
#else
# include <security/pam_appl.h>
#endif

static int PAM_conv(
	int num_msg,
	const struct pam_message **msg, /* this is the culprit */
	struct pam_response **resp,
	void *ctx)
{
	return 0;
}

int main(void)
{
	struct pam_conv PAM_conversation = {
		&PAM_conv, /* this bombs out if the above does not match */
		0
	};

	return 0;
}
" PAM_MESSAGE_CONST)
	endif (NOT DEFINED PAM_MESSAGE_CONST)
	set(PAM_MESSAGE_CONST ${PAM_MESSAGE_CONST} CACHE BOOL "PAM expects a conversation function with const pam_message")

endif (PAM_INCLUDE_DIR AND PAM_LIBRARY)

if (PAM_FOUND)
	if (NOT PAM_FIND_QUIETLY)
		message(STATUS "Found PAM: ${PAM_LIBRARIES}")
	endif (NOT PAM_FIND_QUIETLY)
else (PAM_FOUND)
	if (PAM_FIND_REQUIRED)
		message(FATAL_ERROR "PAM was not found")
	endif(PAM_FIND_REQUIRED)
endif (PAM_FOUND)

mark_as_advanced(PAM_INCLUDE_DIR PAM_LIBRARY DL_LIBRARY PAM_MESSAGE_CONST)

