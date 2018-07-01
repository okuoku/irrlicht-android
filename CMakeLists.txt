#
# INPUTs:
#   BUILD_SHARED_LIB: Build shared library instead

cmake_minimum_required(VERSION 3.6) # Android-studio uses 3.6
project(irr C CXX)

set(CMAKE_ANDROID_ARCH armv7-a)
set(CMAKE_ANDROID_API_MIN 9)
set(CMAKE_ANDROID_API 26)

# Paths
set(SDL2_ROOT "${CMAKE_CURRENT_LIST_DIR}/externals/sdl")
set(IRRLICHT_ROOT "${CMAKE_CURRENT_LIST_DIR}/externals/irrlicht-sdl")

# temp
set(SDL2_CMAKE "sdl2-static-cmake")
set(IRRLICHT_CMAKE "irrlicht-cmake")

# Configure SDL2
add_definitions(
    -DSDL_VIDEO_RENDER_D3D=0
    )

add_subdirectory(${SDL2_CMAKE} sdl2)

# Configure the engine
set(IRRLICHT_USE_SDL2_PATCH ON) # Local patch
include_directories(${SDL2_ROOT}/include)
add_definitions(
    -D_IRR_STATIC_LIB_ # Mandatory (uncool)
    -D_IRR_COMPILE_WITH_SDL2_DEVICE_
    -D_IRR_GENERIC_SDL2_PLATFORM_
    -D_IRR_COMPILE_WITH_EGL_MANAGER_
    #-D_IRR_COMPILE_WITH_JOYSTICK_EVENTS_ # Guessed
    -DNO_IRR_COMPILE_WITH_CONSOLE_DEVICE_
    -DNO_IRR_COMPILE_WITH_X11_
    -DNO_IRR_COMPILE_WITH_DIRECT3D_9_
    -DNO_IRR_COMPILE_WITH_WEBGL1_ # FIXME: Wrong eval. order..?
    -DNO_IRR_COMPILE_WITH_OPENGL_
    #-DNO_IRR_COMPILE_WITH_OGLES2_
    -DNO_IRR_COMPILE_WITH_OGLES1_
    #-DNO_IRR_COMPILE_WITH_X11_DEVICE_
    -DNO_IRR_COMPILE_WITH_ANDROID_DEVICE_
    )

add_subdirectory(${IRRLICHT_CMAKE} irrlicht)

include_directories(${IRRLICHT_ROOT}/include)

# Java Assets
# FIXME: Absorb it into SDL2 side...

set(java_root ${SDL2_ROOT}/android-project/app/src/main/java)
set(java_srcs
    ${java_root}/org/libsdl/app/SDL.java
    ${java_root}/org/libsdl/app/SDLActivity.java
    ${java_root}/org/libsdl/app/SDLAudioManager.java
    ${java_root}/org/libsdl/app/SDLControllerManager.java
    )

# Assets
set(asset_root ${IRRLICHT_ROOT}/media)

message(STATUS "FIXME: Collecting assets using GLOB")
file(GLOB assets
    # Demo Assets
    ${asset_root}/*.b3d
    ${asset_root}/*.irr
    ${asset_root}/*.jpg
    ${asset_root}/*.bmp
    ${asset_root}/*.png
    ${asset_root}/*.mdl
    ${asset_root}/*.md2
    ${asset_root}/*.pk3)

file(GLOB shaders
    # GLES2 Shaders
    ${asset_root}/Shaders/*.fsh
    ${asset_root}/Shaders/*.vsh
    )

# Application package
set(pkg_root ${CMAKE_CURRENT_BINARY_DIR}/pkg)

configure_file(packaging/AndroidManifest.xml.in
    ${pkg_root}/AndroidManifest.xml @ONLY)
configure_file(packaging/strings.xml.in
    ${pkg_root}/res/values/strings.xml)

# App assets
set(icons_root ${CMAKE_CURRENT_LIST_DIR}/packaging/res)
set(icons
    mipmap-hdpi/ic_launcher.png
    mipmap-mdpi/ic_launcher.png
    mipmap-xhdpi/ic_launcher.png
    mipmap-xxhdpi/ic_launcher.png
    mipmap-xxxhdpi/ic_launcher.png
    )

foreach(d hdpi mdpi xhdpi xxhdpi xxxhdpi)
    file(MAKE_DIRECTORY ${pkg_root}/res/mipmap-${d})
endforeach()

set(pkg_assets)
foreach(e ${assets} ${shaders})
    file(RELATIVE_PATH pth ${asset_root} ${e})
    set(output ${pkg_root}/assets/media/${pth})
    add_custom_command(OUTPUT ${output}
        COMMAND ${CMAKE_COMMAND} -E copy_if_different ${e} ${output}
        DEPENDS ${e}
        COMMENT "Copy ${pth}")
    list(APPEND pkg_assets ${output})
endforeach()

foreach(e ${icons})
    set(fil ${icons_root}/${e})
    set(output ${pkg_root}/res/${e})
    add_custom_command(OUTPUT ${output}
        COMMAND ${CMAKE_COMMAND} -E copy_if_different ${fil} ${output}
        DEPENDS ${fil}
        COMMENT "Copy ${e}")
    list(APPEND pkg_assets ${output})
endforeach()

# Add an actual target
if(BUILD_SHARED_LIB)
    add_library(hello SHARED
        main.cpp
        fake-gles-render.c
        $<TARGET_OBJECTS:sdl2_static>
        $<TARGET_OBJECTS:sdl2_main_static>
        $<TARGET_OBJECTS:irrlicht_static>)
else()
    add_executable(hello 
        main.cpp
        fake-gles-render.c
        ${pkg_root}/AndroidManifest.xml
        ${pkg_assets}
        $<TARGET_OBJECTS:sdl2_static>
        $<TARGET_OBJECTS:sdl2_main_static>
        $<TARGET_OBJECTS:irrlicht_static>)
endif()

target_link_libraries(hello
    GLESv2
    EGL
    android
    log
    )

# Configure NSight Tegra
set_target_properties(hello PROPERTIES 
    ANDROID_GUI 1)
set_property(TARGET hello
    PROPERTY ANDROID_ASSETS_DIRECTORIES ${pkg_root}/assets)
set_property(TARGET hello
    PROPERTY ANDROID_NATIVE_LIB_DIRECTORIES $<TARGET_FILE_DIR:hello>)
set_property(TARGET hello
    PROPERTY ANDROID_NATIVE_LIB_DEPENDENCIES hello)

# FIXME: Workaround for 
#  [apkbuilder] Found duplicate file for APK: AndroidManifest.xml
#  [apkbuilder] Origin 1: C:\build\irr-android\hello.dir\Debug\bin\hello.ap_
#  [apkbuilder] Origin 2: C:\build\irr-android\pkg\AndroidManifest.xml
file(MAKE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/empty)
#set_property(TARGET hello
#    PROPERTY ANDROID_JAVA_SOURCE_DIR ${CMAKE_CURRENT_BINARY_DIR}/empty)
set_property(TARGET hello
    PROPERTY ANDROID_JAVA_SOURCE_DIR ${java_root} ${CMAKE_CURRENT_LIST_DIR}/packaging/java)
