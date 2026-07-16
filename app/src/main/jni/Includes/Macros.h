// thanks to shmoo and joeyjurjens for the usefull stuff under this comment.
#ifndef ANDROID_MOD_MENU_MACROS_H
#define ANDROID_MOD_MENU_MACROS_H

#include "KittyMemory/MemoryPatch.h"
#include "KittyMemory/KittyMemory.h"
#include "KittyMemory/Deps/Keystone/includes/keystone.h"
#include "Dobby/dobby.h"

void DobbyHookWrapper(const char *lib, const char *relative, void* hook_function, void** original_function) {
    void *abs = getAbsoluteAddress(lib, relative);
    // LOGI(OBFUSCATE("Off: 0x%llx, Addr: 0x%llx"), offset, (uintptr_t) abs);

    int res = -1;
    if (original_function != nullptr) {
        res = DobbyHook(abs, (dobby_dummy_func_t)hook_function, (dobby_dummy_func_t*)original_function);
        if (res < 0) LOGE(OBFUSCATE("HOOK FAILED: %s"), relative);
    } else {
        res = DobbyHook(abs, (dobby_dummy_func_t)hook_function, nullptr);
        if (res < 0) LOGE(OBFUSCATE("HOOK_NO_ORIG FAILED: %s"), relative);
    }
}

#define HOOK(offset, ptr, orig) DobbyHookWrapper((void *)getAbsoluteAddress(targetLibName, string2Offset(OBFUSCATE(offset))), (void *)ptr, (void **)&orig)
//#define HOOK_LIB(lib, offset, ptr, orig) DobbyHookWrapper((void *)getAbsoluteAddress(OBFUSCATE(lib), string2Offset(OBFUSCATE(offset))), (void *)ptr, (void **)&orig)
#define HOOK_LIB(lib, off_sym, ptr, orig) DobbyHookWrapper(lib, OBFUSCATE(off_sym), (void*)(ptr), (void**)&(orig))

#define HOOK_NO_ORIG(offset, ptr) DobbyHookWrapper((void *)getAbsoluteAddress(targetLibName, string2Offset(OBFUSCATE(offset))), (void *)ptr, NULL)
#define HOOK_LIB_NO_ORIG(lib, offset, ptr) DobbyHookWrapper((void *)getAbsoluteAddress(OBFUSCATE(lib), string2Offset(OBFUSCATE(offset))), (void *)ptr, NULL)

#define HOOKSYM(sym, ptr, org) hook(dlsym(dlopen(targetLibName, 4), OBFUSCATE(sym)), (void *)ptr, (void **)&org)
#define HOOKSYM_LIB(lib, sym, ptr, org) DobbyHookWrapper(dlsym(dlopen(OBFUSCATE(lib), 4), OBFUSCATE(sym)), (void *)ptr, (void **)&org)

#define HOOKSYM_NO_ORIG(sym, ptr)  hook(dlsym(dlopen(targetLibName, 4), OBFUSCATE(sym)), (void *)ptr, NULL)
#define HOOKSYM_LIB_NO_ORIG(lib, sym, ptr) DobbyHookWrapper(dlsym(dlopen(OBFUSCATE(lib), 4), OBFUSCATE(sym)), (void *)ptr, NULL)

std::vector<MemoryPatch> memoryPatches;
std::vector<uint64_t> offsetVector;
/*
// Patching a offset without switch.
void patchOffset(const char *fileName, uint64_t offset, std::string hexBytes, bool isOn) {

    MemoryPatch patch = MemoryPatch::createWithHex(fileName, offset, hexBytes);

    //Check if offset exists in the offsetVector
    if (std::find(offsetVector.begin(), offsetVector.end(), offset) != offsetVector.end()) {
        //LOGE(OBFUSCATE("Already exists"));
        std::vector<uint64_t>::iterator itr = std::find(offsetVector.begin(), offsetVector.end(), offset);
        patch = memoryPatches[std::distance(offsetVector.begin(), itr)]; //Get index of memoryPatches vector
    } else {
        memoryPatches.push_back(patch);
        offsetVector.push_back(offset);
        //LOGI(OBFUSCATE("Added"));
    }

    if (!patch.isValid()) {
        LOGE(OBFUSCATE("Failing offset: 0x%llu, please re-check the hex"), offset);
        return;
    }
    if (isOn) {
        if (!patch.Modify()) {
            LOGE(OBFUSCATE("Something went wrong while patching this offset: 0x%llu"), offset);
        }
    } else {
        if (!patch.Restore()) {
            LOGE(OBFUSCATE("Something went wrong while restoring this offset: 0x%llu"), offset);
        }
    }
}*/

void patchOffsetSym(uintptr_t absolute_address, std::string hexBytes, bool isOn) {

    MemoryPatch patch = MemoryPatch::createWithHex(absolute_address, hexBytes);

    //Check if offset exists in the offsetVector
    if (std::find(offsetVector.begin(), offsetVector.end(), absolute_address) != offsetVector.end()) {
        //LOGE(OBFUSCATE("Already exists"));
        std::vector<uint64_t>::iterator itr = std::find(offsetVector.begin(), offsetVector.end(), absolute_address);
        patch = memoryPatches[std::distance(offsetVector.begin(), itr)]; //Get index of memoryPatches vector
    } else {
        memoryPatches.push_back(patch);
        offsetVector.push_back(absolute_address);
        //LOGI(OBFUSCATE("Added"));
    }

    if (!patch.isValid()) {
        LOGE(OBFUSCATE("Failing offset: 0x%llu, please re-check the hex"), absolute_address);
        return;
    }
    if (isOn) {
        if (!patch.Modify()) {
            LOGE(OBFUSCATE("Something went wrong while patching this offset: 0x%llu"), absolute_address);
        }
    } else {
        if (!patch.Restore()) {
            LOGE(OBFUSCATE("Something went wrong while restoring this offset: 0x%llu"), absolute_address);
        }
    }
}/*

#define PATCH(offset, hex) patchOffset(targetLibName, string2Offset(OBFUSCATE(offset)), OBFUSCATE(hex), true)
#define PATCH_LIB(lib, offset, hex) patchOffset(OBFUSCATE(lib), string2Offset(OBFUSCATE(offset)), OBFUSCATE(hex), true)

#define PATCH_SYM(sym, hex) patchOffset(dlsym(dlopen(targetLibName, 4), OBFUSCATE(sym)), OBFUSCATE(hex), true)
#define PATCH_LIB_SYM(lib, sym, hex) patchOffset(dlsym(dlopen(lib, 4), OBFUSCATE(sym)), OBFUSCATE(hex), true)

#define PATCH_SWITCH(offset, hex, boolean) patchOffset(targetLibName, string2Offset(OBFUSCATE(offset)), OBFUSCATE(hex), boolean)
#define PATCH_LIB_SWITCH(lib, offset, hex, boolean) patchOffset(OBFUSCATE(lib), string2Offset(OBFUSCATE(offset)), OBFUSCATE(hex), boolean)

#define PATCH_SYM_SWITCH(sym, hex, boolean) patchOffsetSym((uintptr_t)dlsym(dlopen(targetLibName, 4), OBFUSCATE(sym)), OBFUSCATE(hex), boolean)
#define PATCH_LIB_SYM_SWITCH(lib, sym, hex, boolean) patchOffsetSym((uintptr_t)dlsym(dlopen(lib, 4), OBFUSCATE(sym)), OBFUSCATE(hex), boolean)

#define RESTORE(offset) patchOffset(targetLibName, string2Offset(OBFUSCATE(offset)), "", false)
#define RESTORE_LIB(lib, offset) patchOffset(OBFUSCATE(lib), string2Offset(OBFUSCATE(offset)), "", false)

#define RESTORE_SYM(sym) patchOffsetSym((uintptr_t)dlsym(dlopen(targetLibName, 4), OBFUSCATE(sym)), "", false)
#define RESTORE_LIB_SYM(lib, sym) patchOffsetSym((uintptr_t)dlsym(dlopen(lib, 4), OBFUSCATE(sym)), "", false)
*/
#endif //ANDROID_MOD_MENU_MACROS_H
