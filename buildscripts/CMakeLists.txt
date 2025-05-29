cmake_minimum_required(VERSION 3.2.0)

# https://github.com/libjpeg-turbo/libjpeg-turbo/releases
set(LIBJPEG_TURBO_VERSION 3.1.0)
set(LIBJPEG_TURBO_HASH SHA256=9564c72b1dfd1d6fe6274c5f95a8d989b59854575d4bbee44ade7bc17aa9bc93)

# http://www.libpng.org/pub/png/libpng.html
set(LIBPNG_VERSION 1.6.48)
set(LIBPNG_HASH SHA256=46fd06ff37db1db64c0dc288d78a3f5efd23ad9ac41561193f983e20937ece03)

# https://www.freetype.org/index.html#news
set(FREETYPE2_VERSION 2.13.3)
set(FREETYPE2_HASH SHA256=5c3a8e78f7b24c20b25b54ee575d6daa40007a5f4eea2845861c3409b3021747)

# https://github.com/kcat/openal-soft/releases
set(OPENAL_VERSION 1.24.3)
set(OPENAL_HASH SHA256=7e1fecdeb45e7f78722b776c5cf30bd33934b961d7fd2a11e0494e064cc631ce)

# http://www.boost.org/users/download/
set(BOOST_VERSION 1.88.0)
set(BOOST_HASH SHA256=8f32d4617390d1c2d16f26a27ab60d97807b35440d45891fa340fc2648b04406)

# https://www.ffmpeg.org/download.html
set(FFMPEG_VERSION 7.1.1)
set(FFMPEG_HASH SHA256=0c8da2f11579a01e014fc007cbacf5bb4da1d06afd0b43c7f8097ec7c0f143ba)

# https://www.libsdl.org/download-2.0.php
set(SDL2_VERSION 2.32.4)
set(SDL2_HASH SHA256=f15b478253e1ff6dac62257ded225ff4e7d0c5230204ac3450f1144ee806f934)

# https://github.com/bulletphysics/bullet3/releases
set(BULLET_VERSION 3.25)
set(BULLET_HASH SHA256=c45afb6399e3f68036ddb641c6bf6f552bf332d5ab6be62f7e6c54eda05ceb77)

# https://github.com/MyGUI/mygui/releases
set(MYGUI_VERSION 3.4.3)
set(MYGUI_HASH SHA256=d1d5f294670ae71f7200ed4b30859018281d8cfd45d6a38d18b97a4aba604c42)

# https://github.com/sisah2/ng_gl4es/releases
set(GL4ES_VERSION 9ce0f6cb8aaca01c697d87c4ccf06a0a014bdbc6)
set(GL4ES_HASH SHA256=b565e717c7d192e936bda25f3cb90ad8db398af56414ec08294b6716574c1a6d)

# https://github.com/openscenegraph/OpenSceneGraph/releases
set(OSG_VERSION 673f30ad3820faf2a5e901ffe403b5246b8918fe)
set(OSG_HASH SHA256=aea196550f02974d6d09291c5d83b51ca6a03b3767e234a8c0e21322927d1e12)

# https://github.com/lz4/lz4/releases
set(LZ4_VERSION 1.9.3)
set(LZ4_HASH SHA256=030644df4611007ff7dc962d981f390361e6c97a34e5cbc393ddfbe019ffe2c1)

# https://luajit.org/download.html
set(LUAJIT_VERSION 505e2c03de35e2718eef0d2d3660712e06dadf1f)
set(LUAJIT_HASH SHA256=1ad2e34b111c802f9d0cdf019e986909123237a28c746b21295b63c9e785d9c3)

set(LIBICU_VERSION 70-1)
set(LIBICU_HASH SHA256=4013d7aff8a34bae410664bdb16554d7eda03654b22627b9284ab55a685492d4)

# https://github.com/OpenMW/openmw/commits/master
set(OPENMW_VERSION 587923f69b73d23c82aa293fbb0bd1129e6a9496)
set(OPENMW_HASH SHA256=44d044ad0a5546216fe611fe92c37065bb07e2efeeeaac4f049dae03df313186)

# End of configurable options

project(openmw-android)

include(ExternalProject)

set(wrapper_command ${CMAKE_BINARY_DIR}/command_wrapper.sh)
set(ndk_triplet ${NDK_TRIPLET})
set(download_dir ${CMAKE_SOURCE_DIR}/downloads/)
set(prefix ${CMAKE_INSTALL_PREFIX})
set(host ${ndk_triplet})
set(android_platform android-${ANDROID_API})
set(app_abi ${ABI})

set(COMMON_CMAKE_ARGS
	-DCMAKE_TOOLCHAIN_FILE=${CMAKE_SOURCE_DIR}/toolchain/ndk/build/cmake/android.toolchain.cmake
	-DANDROID_ABI=${app_abi}
	-DANDROID_PLATFORM=${android_platform}
	-DANDROID_STL=c++_shared
	-DANDROID_LD=deprecated
	-DANDROID_CPP_FEATURES=rtti\ exceptions
	-DANDROID_ALLOW_UNDEFINED_VERSION_SCRIPT_SYMBOLS=ON
	-DCMAKE_C_FLAGS=-I${prefix}/include/\ $ENV{CFLAGS}
	-DCMAKE_CXX_FLAGS=-I${prefix}/include/\ $ENV{CXXFLAGS}
	-DCMAKE_SHARED_LINKER_FLAGS=$ENV{LDFLAGS}
	-DCMAKE_BUILD_TYPE=${BUILD_TYPE}
	-DCMAKE_DEBUG_POSTFIX=
	-DCMAKE_INSTALL_PREFIX=${prefix}
	-DCMAKE_FIND_ROOT_PATH=${prefix}
)

set(COMMON_NDK_BUILD_FLAGS
	NDK_PROJECT_PATH=.
	APP_BUILD_SCRIPT=<SOURCE_DIR>/Android.mk
	APP_PLATFORM=${android_platform}
	APP_ABI=${app_abi}
	APP_LD=deprecated
	LOCAL_LDFLAGS=$ENV{LDFLAGS}
	-j4
)

set(COMMON_AUTOCONF_FLAGS
	--enable-static
	--disable-shared
	--prefix=${prefix}
)

# ffmpeg doesn't like --host, so sandwich it between adding --host to the flags
set(ffmpeg_configure_flags ${COMMON_AUTOCONF_FLAGS})
if (${BUILD_TYPE} STREQUAL "debug")
	# ffmpeg fails to compile if we have asm enabled and optimizations disabled, so disable both
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --disable-asm --disable-optimizations)
endif()

if ((${ARCH} STREQUAL "x86_64") OR (${ARCH} STREQUAL "x86") OR (${ARCH} STREQUAL "arm64"))
	# ffmpeg fails to link with assembly optimizations on x86-64
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --disable-asm)
endif()

set(COMMON_AUTOCONF_FLAGS
	--host=${host}
	${COMMON_AUTOCONF_FLAGS}
)

if (${ARCH} STREQUAL "arm")
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --enable-thumb)
endif()

ExternalProject_Add(libjpeg-turbo
	URL https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/${LIBJPEG_TURBO_VERSION}/libjpeg-turbo-${LIBJPEG_TURBO_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/libjpeg-turbo-${LIBJPEG_TURBO_VERSION}.tar.gz
	URL_HASH ${LIBJPEG_TURBO_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DENABLE_SHARED=false

	BUILD_COMMAND ${wrapper_command} $(MAKE) PROGRAMS=

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(libpng
	URL http://prdownloads.sourceforge.net/libpng/libpng-${LIBPNG_VERSION}.tar.xz
	https://github.com/xyzz/openmw-deps/releases/download/v0/libpng-${LIBPNG_VERSION}.tar.xz
	URL_HASH ${LIBPNG_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE) check_PROGRAMS= bin_PROGRAMS=

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install check_PROGRAMS= bin_PROGRAMS=
)


set(libicu_flags
	--disable-tests
	--disable-samples
	--disable-icuio
	--disable-extras
	--with-cross-build=${CMAKE_SOURCE_DIR}/build/icu-host-build)

ExternalProject_Add(libicu
	URL https://github.com/unicode-org/icu/archive/refs/tags/release-${LIBICU_VERSION}.zip
	URL_HASH ${LIBICU_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/icu4c/source/configure
	${COMMON_AUTOCONF_FLAGS}
	${libicu_flags}

	BUILD_COMMAND ${wrapper_command} $(MAKE) check_PROGRAMS= bin_PROGRAMS=

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install check_PROGRAMS= bin_PROGRAMS=
)

ExternalProject_Add(freetype2
	URL https://download.savannah.gnu.org/releases/freetype/freetype-${FREETYPE2_VERSION}.tar.gz
	http://prdownloads.sourceforge.net/freetype/freetype-${FREETYPE2_VERSION}.tar.gz
	URL_HASH ${FREETYPE2_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}
	--with-png=no

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(openal
	URL https://github.com/kcat/openal-soft/archive/${OPENAL_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/openal-soft-${OPENAL_VERSION}.tar.gz
	URL_HASH ${OPENAL_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DALSOFT_EXAMPLES=OFF
	-DALSOFT_TESTS=OFF
	-DALSOFT_UTILS=OFF
	-DALSOFT_NO_CONFIG_UTIL=ON
	-DALSOFT_BACKEND_OPENSL=ON
	-DALSOFT_BACKEND_WAVE=OFF

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(boost
	URL https://github.com/boostorg/boost/releases/download/boost-1.88.0/boost-1.88.0-cmake.tar.gz
	#URL_HASH ${BOOST_HASH}
	DOWNLOAD_DIR ${download_dir}

#        BUILD_IN_SOURCE 1
	PATCH_COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/system.diff &&
	PATCH_COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/regex.diff

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DCMAKE_CXX_FLAGS=-I<SOURCE_DIR>/libs/core/include/\  $ENV{CXXFLAGS}
	-DBOOST_INCLUDE_LIBRARIES="filesystem;program_options;iostreams;geometry;system;regex"

#FIXME: Doesnt work for some reason, need to compile everything :(, need newer cmake version?
#	-DBOOST_INCLUDE_LIBRARIES=filesystem;program_options;iostreams
#        -DBOOST_RUNTIME_LINK=static
	-DBOOST_EXCLUDE_LIBRARIES=process

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install

	COMMAND llvm-ranlib ${prefix}/lib/libboost_filesystem.a
	COMMAND llvm-ranlib ${prefix}/lib/libboost_program_options.a
	COMMAND llvm-ranlib ${prefix}/lib/libboost_iostreams.a
	COMMAND llvm-ranlib ${prefix}/lib/libboost_system.a
)

ExternalProject_Add(ffmpeg
	URL http://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.bz2
	https://github.com/xyzz/openmw-deps/releases/download/v0/ffmpeg-${FFMPEG_VERSION}.tar.bz2
	URL_HASH ${FFMPEG_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${ffmpeg_configure_flags}
	--target-os=android
	--enable-cross-compile
	--cross-prefix=llvm-
	--cc=${host}-clang
	--arch=${ARCH}
	--cpu=${FFMPEG_CPU}
	--enable-version3
	--enable-pic
	--disable-everything
	--disable-doc
	--disable-programs
	--disable-autodetect
	--disable-iconv
	--enable-decoder=mp3
	--enable-demuxer=mp3
	--enable-decoder=bink
	--enable-decoder=binkaudio_rdft
	--enable-decoder=binkaudio_dct
	--enable-demuxer=bink
	--enable-demuxer=wav
	--enable-decoder=pcm_*
	--enable-decoder=vp8
	--enable-decoder=vp9
	--enable-decoder=opus
	--enable-decoder=vorbis
	--enable-demuxer=matroska
	--enable-demuxer=ogg

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(sdl2
	URL https://www.libsdl.org/release/SDL2-${SDL2_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/SDL2-${SDL2_VERSION}.tar.gz
	#URL_HASH ${SDL2_HASH}
	DOWNLOAD_DIR ${download_dir}

	#PATCH_COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/sdl2-android-120hz-gyroscope.patch
	#COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/sdl2-disable-declaration-after-statement-warning.patch

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
        -DSDL_STATIC=OFF
        -DHAVE_GCC_FVISIBILITY=OFF

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install

	COMMAND cp libSDL2.so ${prefix}/lib/
	COMMAND cp -r <SOURCE_DIR>/include ${prefix}/
)


ExternalProject_Add(bullet
	URL https://github.com/bulletphysics/bullet3/archive/${BULLET_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/bullet3-${BULLET_VERSION}.tar.gz
	URL_HASH ${BULLET_HASH}
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/bullet-fix-cmake.patch

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DBUILD_BULLET2_DEMOS=OFF
	-DBUILD_CPU_DEMOS=OFF
	-DBUILD_UNIT_TESTS=OFF
	-DBUILD_EXTRAS=OFF
	-DUSE_DOUBLE_PRECISION=ON
	-DBULLET2_MULTITHREADING=ON

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND $(MAKE) install
)

ExternalProject_Add(NG-GL4ES
	URL https://github.com/sisah2/NG-GL4ES/archive/${GL4ES_VERSION}.tar.gz
	#URL_HASH ${GL4ES_HASH}
	DOWNLOAD_DIR ${download_dir}

        BUILD_IN_SOURCE 1
	
	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND mkdir -p ${prefix}/lib/
	COMMAND cp libng_gl4es.so ${prefix}/lib/
	COMMAND cp <SOURCE_DIR>/libraries/arm64-v8a/libspirv-cross-c-shared.so ${prefix}/lib/
	COMMAND cp -r <SOURCE_DIR>/include ${prefix}/include/gl4es/
	COMMAND cp -r <SOURCE_DIR>/include ${prefix}/
)

ExternalProject_Add(mygui
	DEPENDS freetype2

	URL https://github.com/MyGUI/mygui/archive/MyGUI${MYGUI_VERSION}.tar.gz
        #URL_HASH ${MYGUI_HASH}
	DOWNLOAD_DIR ${download_dir}

        #PATCH_COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/mygui-dont-set-debug-postfix.patch

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DMYGUI_RENDERSYSTEM=1
	-DMYGUI_BUILD_DEMOS=OFF
	-DMYGUI_BUILD_TOOLS=OFF
	-DMYGUI_BUILD_PLUGINS=OFF
	-DMYGUI_DONT_USE_OBSOLETE=ON
	-DFREETYPE_FT2BUILD_INCLUDE_DIR=${prefix}/include/freetype2/

	-DMYGUI_STATIC=ON

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(lz4
	URL https://github.com/lz4/lz4/archive/v${LZ4_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/v${LZ4_VERSION}.tar.gz

	URL_HASH ${LZ4_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>/build/cmake/
	${COMMON_CMAKE_ARGS}
	-DBUILD_STATIC_LIBS=ON
	-DBUILD_SHARED_LIBS=OFF

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

set(LUAJIT_MAKE_FLAGS
	HOST_CC=${LUAJIT_HOST_CC}
	CFLAGS=
	TARGET_CFLAGS=$ENV{CFLAGS}
	CROSS=llvm-
	STATIC_CC=${host}-clang
	DYNAMIC_CC=${host}-clang\ -fPIC
	TARGET_LD=${host}-clang
	PREFIX=${prefix}
)

ExternalProject_Add(luajit
	URL https://github.com/luaJit/LuaJIT/archive/${LUAJIT_VERSION}.tar.gz
#	URL_HASH ${LUAJIT_HASH}
	DOWNLOAD_DIR ${download_dir}

	BUILD_IN_SOURCE 1

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} $(MAKE) amalg
	${LUAJIT_MAKE_FLAGS}

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
	${LUAJIT_MAKE_FLAGS}
	COMMAND bash -c "rm ${prefix}/lib/libluajit*.so*"
)

ExternalProject_Add(zlib
	URL https://github.com/madler/zlib/archive/refs/tags/v1.2.11.tar.gz
	URL_HASH SHA256=629380c90a77b964d896ed37163f5c3a34f6e6d897311f1df2a7016355c45eff
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)
	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(libxml
	DEPENDS zlib

	URL https://github.com/GNOME/libxml2/archive/refs/tags/v2.9.12.tar.gz
	URL_HASH SHA256=8a4ddd706419c210b30b8978a51388937fd9362c34fc9a3d69e4fcc6f8055ee0
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DBUILD_SHARED_LIBS=OFF
	-DLIBXML2_WITH_CATALOG=OFF
	-DLIBXML2_WITH_ICONV=OFF
	-DLIBXML2_WITH_LZMA=OFF
	-DLIBXML2_WITH_PROGRAMS=OFF
	-DLIBXML2_WITH_PYTHON=OFF
	-DLIBXML2_WITH_TESTS=OFF
	-DLIBXML2_WITH_ZLIB=ON

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

set(LIBCOLLADA_PATCH patch -d <SOURCE_DIR> -p0 -t -N < ${CMAKE_SOURCE_DIR}/patches/libcollada-minizip-fix.patch)

ExternalProject_Add(libcollada
	DEPENDS libxml boost

	URL https://github.com/rdiankov/collada-dom/archive/v2.5.0.tar.gz
	URL_HASH SHA256=3be672407a7aef60b64ce4b39704b32816b0b28f61ebffd4fbd02c8012901e0d
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND ${LIBCOLLADA_PATCH}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DBoost_USE_STATIC_LIBS=ON
	-DBoost_USE_STATIC_RUNTIME=ON
	-DBoost_NO_SYSTEM_PATHS=ON
	-DBoost_INCLUDE_DIR=${prefix}/include
	-DCMAKE_CXX_FLAGS=-Dauto_ptr=unique_ptr\ $ENV{CXXFLAGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

set(OSG_COMMON
	-DOPENGL_PROFILE="GL2"
	-DDYNAMIC_OPENTHREADS=OFF
	-DDYNAMIC_OPENSCENEGRAPH=OFF
	-DBUILD_OSG_PLUGIN_OSG=ON
	-DBUILD_OSG_PLUGIN_DAE=ON
	-DBUILD_OSG_PLUGIN_DDS=ON
	-DBUILD_OSG_PLUGIN_TGA=ON
	-DBUILD_OSG_PLUGIN_BMP=ON
	-DBUILD_OSG_PLUGIN_JPEG=ON
	-DBUILD_OSG_PLUGIN_PNG=ON
	-DBUILD_OSG_PLUGIN_FREETYPE=ON
	-DJPEG_INCLUDE_DIR=${prefix}/include/
	-DPNG_INCLUDE_DIR=${prefix}/include/
	-DFREETYPE_DIR=${prefix}
	-DCOLLADA_INCLUDE_DIR=${prefix}/include/collada-dom2.5
	-DCOLLADA_DIR=${prefix}/include/collada-dom2.5/1.4
	-DOSG_CPP_EXCEPTIONS_AVAILABLE=TRUE
	-DOSG_GL1_AVAILABLE=ON
	-DOSG_GL2_AVAILABLE=ON
	-DOSG_GL3_AVAILABLE=OFF
	-DOSG_GLES1_AVAILABLE=OFF
	-DOSG_GLES3_AVAILABLE=OFF
	-DOSG_GL_LIBRARY_STATIC=OFF
	-DOSG_GL_DISPLAYLISTS_AVAILABLE=ON
	-DOSG_GL_MATRICES_AVAILABLE=ON
	-DOSG_GL_VERTEX_FUNCS_AVAILABLE=ON
	-DOSG_GL_VERTEX_ARRAY_FUNCS_AVAILABLE=ON
	-DOSG_GL_FIXED_FUNCTION_AVAILABLE=ON
	-DBUILD_OSG_APPLICATIONS=OFF
	-DBUILD_OSG_PLUGINS_BY_DEFAULT=OFF
	-DOPENGL_gl_LIBRARY=${prefix}/lib/libng_gl4es.so
	-DCMAKE_CXX_FLAGS=-Dauto_ptr=unique_ptr\ -I${prefix}/include/\ $ENV{CXXFLAGS}
)

set(OSG_PATCH
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/disable-polygon-offset.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/remove-lib-prefix-from-plugins.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/fix-freetype-include-dirs.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/0001-Replace-Atomic-impl-with-std-atomic.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/0002-BufferObject-make-numClients-atomic.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/0004-IncrementalCompileOperation-wrap-some-stuff-in-atomi.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/force-add-plugins.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/dae_collada.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/ng-gl4es.patch &&
#	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/cubic-filtering.patch &&
	patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/0005-CullSettings-make-inheritanceMask-atomic-to-silence-.patch
)

ExternalProject_Add(osg
	DEPENDS libjpeg-turbo libpng NG-GL4ES freetype2 libcollada

	URL https://github.com/openmw/osg/archive/${OSG_VERSION}.tar.gz
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND ${OSG_PATCH}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	${OSG_COMMON}
	-DCMAKE_CXX_FLAGS=-std=gnu++11\ -I${prefix}/include/\ $ENV{CXXFLAGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND mkdir -p <SOURCE_DIR>/3rdparty/build/libpng <SOURCE_DIR>/3rdparty/build/libjpeg <SOURCE_DIR>/3rdparty/build/freetype
	COMMAND $(MAKE) install
)

set(OPENMW_COMMON
	-DBUILD_BSATOOL=0
	-DBUILD_NIFTEST=0
	-DBUILD_ESMTOOL=0
	-DBUILD_LAUNCHER=0
	-DBUILD_MWINIIMPORTER=0
	-DBUILD_ESSIMPORTER=0
	-DBUILD_OPENCS=0
	-DBUILD_NAVMESHTOOL=0
	-DBUILD_WIZARD=0
	-DBUILD_MYGUI_PLUGIN=0
	-DBUILD_BULLETOBJECTTOOL=0
	-DOPENMW_USE_SYSTEM_SQLITE3=OFF
	-DOPENMW_USE_SYSTEM_YAML_CPP=OFF
	-DOPENMW_USE_SYSTEM_ICU=ON
 	#-DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON
 	#-DCMAKE_POLICY_DEFAULT_CMP0069=NEW
	#-DOPENMW_LTO_BUILD=1
	-DOPENGL_gl_LIBRARY=${prefix}/lib/libng_gl4es.so
	-DOPENGL_glx_LIBRARY=${prefix}/lib/libng_gl4es.so
	-DOPENGL_INCLUDE_DIR=${prefix}/include/gl4es/
	#-DOPENMW_GL4ES_MANUAL_INIT=ON
	-DOPENAL_INCLUDE_DIR=${prefix}/include/AL/
	-DBullet_INCLUDE_DIR=${prefix}/include/bullet/
	-DOSG_STATIC=TRUE
	-DCMAKE_CXX_FLAGS=-std=gnu++20\ -I${prefix}/include/\ $ENV{CXXFLAGS}
	-DMyGUI_LIBRARY=${prefix}/lib/libMyGUIEngineStatic.a
)

set(OPENMW_PATCH
        patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/openmw/0001-loadingscreen-disable-for-now.patch &&
        patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/openmw/0009-windowmanagerimp-always-show-mouse-when-possible-pat.patch &&
        patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/openmw/androidPath.patch &&
        #patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/openmw/0010-android-fix-context-being-lost-on-app-minimize.patch &&
	cp ${CMAKE_SOURCE_DIR}/patches/openmw/android_main.cpp <SOURCE_DIR>/apps/openmw/android_main.cpp
)

ExternalProject_Add(openmw
	DEPENDS boost openal osg mygui ffmpeg sdl2 bullet lz4 luajit libicu

	URL https://github.com/sisah2/openmw/archive/${OPENMW_VERSION}.tar.gz
#	URL_HASH ${OPENMW_HASH}
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND ${OPENMW_PATCH}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	${OPENMW_COMMON}
	BUILD_COMMAND $(MAKE)

	INSTALL_COMMAND ""
)
