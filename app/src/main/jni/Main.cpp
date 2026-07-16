#include <list>
#include <vector>
#include <string.h>
#include <pthread.h>
#include <thread>
#include <cstring>
#include <jni.h>
#include <unistd.h>
#include <fstream>
#include <iostream>
#include <dlfcn.h>
#include "S3HACKS/Tools.h"
using namespace Tools;
#include "Dobby/dobby.h"
#include "ShadowHook/ShadowHook.h"
#include <S3HACKS/il2Cpp.h>
#include "S3HACKS/Vector2.h"
#include "S3HACKS/Vector3.hpp"
#include "Includes/Logger.h"
#include "Includes/obfuscate.h"
#include "Includes/Utils.hpp"
#include "KittyMemory/MemoryPatch.h"
#include "KittyMemory/KittyMemory.h"
#include "KittyMemory/Deps/Keystone/includes/keystone.h"
using namespace KittyMemory;
#include "Menu/Setup.h"
#include "Includes/Macros.h"
#include "S3HACKS/chams.h"
#include "S3HACKS/Quaternion.h"
#include "class.h"
#include "hook.h"
#include <sys/system_properties.h>
JavaVM* publicVM;
JNIEnv* publicEnv;
#include <cstdint>

jobjectArray GetFeatureList(JNIEnv *env, jobject context) {
    jobjectArray ret;

const char *features[] = {
    OBFUSCATE("S3 HACKS"),

	OBFUSCATE("Category_Aim"),
	OBFUSCATE("8_ButtonOnOff_Enable Aim"),
	OBFUSCATE("11_ButtonOnOff_Aim Fire"),
	OBFUSCATE("12_ButtonOnOff_Aim Auto"),
    OBFUSCATE("0_ButtonOnOff_Aim Silent"),
	
	OBFUSCATE("Category_Esp"),
    OBFUSCATE("80_ButtonOnOff_Enable ESP"),
	OBFUSCATE("9_ButtonOnOff_Esp Fire"),
	OBFUSCATE("10_ButtonOnOff_Esp Fire Blue"),
	OBFUSCATE("13_ButtonOnOff_Esp Alert"),

	OBFUSCATE("Category_Chams"),
	OBFUSCATE("1_ButtonOnOff_Enable chams"),
	OBFUSCATE("3_ButtonOnOff_Enable shading"),
	OBFUSCATE("4_ButtonOnOff_Enable wireframe"),
	OBFUSCATE("5_ButtonOnOff_Enable glow"),
	OBFUSCATE("6_ButtonOnOff_Enable outline"),
	OBFUSCATE("7_ButtonOnOff_Enable rainbow"),
	
	OBFUSCATE("Category_Fly Map"),
	OBFUSCATE("14_ButtonOnOff_Fly Map"),
	
	OBFUSCATE("Category_Reset Guest"),
	OBFUSCATE("2_ButtonOnOff_Reset Guest"),
	
};
   
    int Total_Feature = (sizeof features / sizeof features[0]);
    ret = (jobjectArray)
            env->NewObjectArray(Total_Feature, env->FindClass(OBFUSCATE("java/lang/String")),
                                env->NewStringUTF(""));

    for (int i = 0; i < Total_Feature; i++)
        env->SetObjectArrayElement(ret, i, env->NewStringUTF(features[i]));

    return (ret);
}

void Changes(JNIEnv *env, jclass clazz, jobject obj,
                                        jint featNum, jstring featName, jint value,
                                        jboolean boolean, jstring str) {

    LOGD(OBFUSCATE("Feature name: %d - %s | Value: = %d | Bool: = %d | Text: = %s"), featNum,
         env->GetStringUTFChars(featName, 0), value,
         boolean, str != NULL ? env->GetStringUTFChars(str, 0) : "");

    switch (featNum) {

    case 0:
        SilentAimv222 = boolean;
        break;

    case 1:
        chams = boolean;
        break;

    case 2:
        Guest = boolean;
        break;

    case 3:
        shading = boolean;
        break;

    case 4:
        wireframe = boolean;
        break;

    case 5:
       glow = boolean;
        break;
	
	case 6:
       outline = boolean;
        break;
	
	case 7:
       rainbow = boolean;
        break;
		
	case 8:
       EnableAim = boolean;
        break;
	
	case 9:
       EspFire = boolean;
        break;
		
	case 10:
       EspFireBlue = boolean;
        break;
		
	case 11:
       AimFire = boolean;
        break;
		
	case 12:
       AimAuto = boolean;
        break;
		
	case 13:
       EspAlert = boolean;
        break;
	
}
}

int RegisterMenu(JNIEnv *env) {
    JNINativeMethod methods[] = {
            {OBFUSCATE("Icon"), OBFUSCATE("()Ljava/lang/String;"), reinterpret_cast<void *>(Icon)},
            {OBFUSCATE("IconWebViewData"),  OBFUSCATE("()Ljava/lang/String;"), reinterpret_cast<void *>(IconWebViewData)},
            {OBFUSCATE("IsGameLibLoaded"),  OBFUSCATE("()Z"), reinterpret_cast<void *>(isGameLibLoaded)},
            {OBFUSCATE("Init"),  OBFUSCATE("(Landroid/content/Context;Landroid/widget/TextView;Landroid/widget/TextView;)V"), reinterpret_cast<void *>(Init)},
            {OBFUSCATE("SettingsList"),  OBFUSCATE("()[Ljava/lang/String;"), reinterpret_cast<void *>(SettingsList)},
            {OBFUSCATE("GetFeatureList"),  OBFUSCATE("()[Ljava/lang/String;"), reinterpret_cast<void *>(GetFeatureList)},

            
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/android/support/Menu"));
    if (!clazz)
        return JNI_ERR;
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0)
        return JNI_ERR;
    return JNI_OK;
}

int RegisterPreferences(JNIEnv *env) {
    JNINativeMethod methods[] = {
            {OBFUSCATE("Changes"), OBFUSCATE("(Landroid/content/Context;ILjava/lang/String;IZLjava/lang/String;)V"), reinterpret_cast<void *>(Changes)},
    };
    jclass clazz = env->FindClass(OBFUSCATE("com/android/support/Preferences"));
    if (!clazz)
        return JNI_ERR;
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0)
        return JNI_ERR;
    return JNI_OK;
}

int RegisterMain(JNIEnv *env) {
    JNINativeMethod methods[] = {
            {OBFUSCATE("CheckOverlayPermission"), OBFUSCATE("(Landroid/content/Context;)V"), reinterpret_cast<void *>(CheckOverlayPermission)},
    };
    jclass clazz = env->FindClass(OBFUSCATE("com/android/support/Main"));
    if (!clazz)
        return JNI_ERR;
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0)
        return JNI_ERR;

    return JNI_OK;
}

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    publicVM = vm;
    pthread_t ptid;
    pthread_create(&ptid, NULL, hack_thread, NULL);
    if (RegisterMenu(env) != 0)
        return JNI_ERR;
    if (RegisterPreferences(env) != 0)
        return JNI_ERR;
    if (RegisterMain(env) != 0)
        return JNI_ERR;
    return JNI_VERSION_1_6;
}
