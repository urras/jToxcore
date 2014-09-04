#Try to find libtoxav for building

include(LibFindMacros)
libfind_pkg_check_modules(libtoxav_PKGCONF libtoxav)

find_path(libtoxav_INCLUDE_DIR NAMES toxav/toxav.h PATHS ${libtoxav_PKGCONF_INCLUDE_DIRS})
find_library(libtoxav_LIBRARY NAMES toxav PATHS ${libtoxav_PKGCONF_LIBRARY_DIRS})

set(libtoxav_PROCESS_INCLUDES libtoxav_INCLUDE_DIR)
set(libtoxav_PROCESS_LIBS libtoxav_LIBRARY)
libfind_process(libtoxav)
