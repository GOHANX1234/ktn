LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

KITTYMEMORY_PATH = KittyMemory
include $(CLEAR_VARS)
LOCAL_MODULE    := Keystone
LOCAL_SRC_FILES := $(KITTYMEMORY_PATH)/Deps/Keystone/libs-android/$(TARGET_ARCH_ABI)/libkeystone.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := Shadowhook
LOCAL_SRC_FILES := Shadowhook/libraries/$(TARGET_ARCH_ABI)/libShadowhook.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/Shadowhook/libraries/
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := Dobby
LOCAL_SRC_FILES := Dobby/${TARGET_ARCH_ABI}/libdobby.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

# Here is the name of your lib.
# When you change the lib name, change also on System.loadLibrary("") under OnCreate method on StaticActivity.java
# Both must have same name
LOCAL_MODULE    := MyLibName

# -std=c++17 is required to support AIDE app with NDK
LOCAL_CFLAGS := -w -s -Wno-error=format-security -fvisibility=hidden -fpermissive -fexceptions
LOCAL_CPPFLAGS := -w -s -Wno-error=format-security -fvisibility=hidden -Werror -std=c++17
LOCAL_CPPFLAGS += -Wno-error=c++11-narrowing -fpermissive -Wall -fexceptions
LOCAL_LDFLAGS += -Wl,--gc-sections,--strip-all,-llog
LOCAL_LDLIBS := -llog -landroid -lEGL -lGLESv2
LOCAL_ARM_MODE := arm

LOCAL_STATIC_LIBRARIES := Keystone Dobby Shadowhook 

LOCAL_C_INCLUDES += $(LOCAL_PATH) \
                    $(LOCAL_PATH)/ImGui \
                    $(LOCAL_PATH)/ImGui/backends \

# Here you add the cpp file to compile
LOCAL_SRC_FILES := Main.cpp \
    Includes/Utils.cpp \
	KittyMemory/KittyArm64.cpp \
    KittyMemory/KittyMemory.cpp \
    KittyMemory/KittyScanner.cpp \
    KittyMemory/KittyUtils.cpp \
    KittyMemory/MemoryBackup.cpp \
    KittyMemory/MemoryPatch.cpp \
    S3HACKS/Tools.cpp \
    S3HACKS/Il2Cpp.cpp \
	
include $(BUILD_SHARED_LIBRARY)
