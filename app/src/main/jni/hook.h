bool SilentAimv222 = false;
bool ignoreEnemyBot    = false;
bool ignoreKnocked     = true;
static int aimPosition = 0;
const float NECK_OFFSET = -0.15f;
bool checkVisible2 = true;
bool Headshot = false;
bool EnableAim;
float Fov_Aim = 330.0f;
bool AimVisible = true;
bool aimVisible = true;
bool AimFire;
bool AimScope;
bool AimAuto;
bool SpeedHack;
bool AimbotLegit;
float active = 0.0f;
float desactive = 0.0f;
bool saved = false;
bool EspFire = false;
bool EspFireBlue = false;
bool EspCount = false;
bool EspGrenade = false;
bool EspAname = false;
bool EspAlert = false;
bool Guest = false;
float Aimdis = 1000.0f;

bool (*ResetGuest)(void* _this);
bool _ResetGuest(void* _this) {
    if (Guest) {
        return true;
    }
    return ResetGuest(_this);
}

static bool isEnemyInRangeWeapon(void *player, void *enemy, void* weapon) {
    if (player != nullptr && enemy != nullptr && weapon != nullptr) {
        Vector3 EnemyHeadPosition = GetHeadPosition(enemy); 
        Vector3 PlayerHeadPosition = GetHeadPosition(player);       
        float distance = Vector3::Distance(PlayerHeadPosition, EnemyHeadPosition);
        float weaponRange = get_Range(weapon);
        if (distance <= weaponRange) {
            return true;
        }
    }
    return false;
}

bool Visible_Check(void *enemy)  {
    if(enemy != nullptr)  {
        void *hitObj = nullptr;
        auto Camera = Transform_GetPosition(Component_GetTransform(Camera_main()));
        auto Target = Transform_GetPosition(Component_GetTransform(Player_GetHeadCollider(enemy)));
        return !Physics_Raycast(Camera, Target, 12, &hitObj);
    }
    return false;
}
Vector3 GetAdjustedPosition(void* closestEnemy) {
    Vector3 headPos = GetHeadPosition(closestEnemy);
    if (aimPosition == 1) {
        headPos.y += NECK_OFFSET;
    } else if (aimPosition == 2) {
        Vector3 hipPos = GetHipPosition(closestEnemy);
        headPos = Vector3::Lerp(headPos, hipPos, 0.5f);
    } else if (aimPosition == 3) {
        headPos = GetHipPosition(closestEnemy);
    }
    return headPos;
}
void* EnemySlientAim() {
    float shortestDistance = 9999.0f;
    void* closestEnemy = nullptr;

    void* match = Curent_Match();
    if (!match) return nullptr;

    void* localPlayer = GetLocalPlayer(match);
    if (!localPlayer) return nullptr;

    auto players = *(MyDictionary**)((uintptr_t)match + ListPlayer);
    if (!players || !players->entries) return nullptr;

    auto entries = players->entries;
    Vector3 localPos = getPosition(localPlayer);

    for (int i = 0; i < players->count; i++) {
        DictionaryEntry& entry = entries->vector[i];
        if (entry.hashCode < 0) continue;

        void* player = entry.value;
        if (!player) continue;
        if (ignoreEnemyBot && get_IsClientBot(player)) continue;
        if (get_isLocalTeam(player)) continue;
        if (ignoreKnocked && get_IsDieing(player)) continue;
        if (GetHp(player) <= 0) continue;
        if (get_MaxHP(player) <= 0) continue;
        if (!get_isVisible(player)) continue;
        if (checkVisible2 && !Visible_Check(player)) continue;

        Vector3 enemyPos = GetAdjustedPosition(player);
        float dist = Vector3::Distance(localPos, enemyPos);

        if (dist < shortestDistance) {
            shortestDistance = dist;
            closestEnemy = player;
        }
    }

    return closestEnemy;
}

struct COW_GamePlay_IHAAMHPPLMG_o;
struct COW_GamePlay_IHAAMHPPLMG_o {
    uint32_t m_Value;
    uint32_t m_ID;
    uint8_t m_TeamID;
    uint8_t m_ShortID;
    uint64_t m_IDMask;
};

struct DamageInfo2_o {
    void *klass;
    void *monitor;
    int32_t BaseDamage;
    int32_t HitColliderType;
    monoString* HitColliderName;
    bool isBackArea;
    COW_GamePlay_IHAAMHPPLMG_o Damager;
    void* Weapon;
    int32_t WeaponDataID;
    Vector3 FirePos;
    Vector3 HitPos;
    Vector3 HitNormal;
    uint8_t SpecialHitType;
    bool ForceNoHeadshot;
    int32_t ExtraInfo;
    MyDictionary *SpecialHitDic;
};

struct PlayerID_MKFEKBKJCKE_o;
struct PlayerID {
    uint32_t NBPDJAAAFBH;
    uint32_t JEDDPHIHGKL;
    uint8_t IOICFFEKAIL;
    uint8_t PHAFNFOFFDB;
    uint64_t BNFAIDHEHOM;
};

#define offset_BodyPart (uintptr_t) Il2CppGetFieldOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("ELMGJKHIIAA"), OBFUSCATE("KENBMOOEHBG"))

struct HitObjectInfo {
    void *klass;
    void *monitor;
    bool m_IsInPool;
    void *HitObject;
    void *HitCollider;
    Vector3 HitLocation;
    Vector3 HitNormal;
    Vector3 RayDir;
    Vector3 StartPosition;
    int32_t Damage;
    float Distance;
    int32_t ActorLayer;
    int32_t HitGroup;
    void *HitPhysicMaterial;
    bool IgnoreHappens;
    bool ViewBlocked;
    struct Vector3 OrigStartPosition;
    uint8_t SpecialHitType;
    uint32_t SpecialHitLevelObjID;
};

int (*orig_PlayerNetwork_TakeDamage)(void *ClosestEnemy, int baseDamage, PlayerID damager, DamageInfo2_o *damageInfo, int weaponDataID, Vector3 firePos, Vector3 hitPos, monoList<float> *checkParams, void *damagerWeaponDynamicInfo, int damagerVehicleID);
int hook_PlayerNetwork_TakeDamage(void *ClosestEnemy, int baseDamage, PlayerID damager, DamageInfo2_o *damageInfo, int weaponDataID, Vector3 firePos, Vector3 hitPos, monoList<float> *checkParams, void *damagerWeaponDynamicInfo, int damagerVehicleID) {
    void* CurrentMatch = Curent_Match();
    void* LocalPlayer = GetLocalPlayer(CurrentMatch);
    
    if (LocalPlayer && ClosestEnemy) {
        firePos = GetHeadPosition(LocalPlayer);
        hitPos = GetHeadPosition(ClosestEnemy);
    }
    if (ClosestEnemy != NULL && Headshot) {
        if (damageInfo != NULL) {
            *(int *)((long) damageInfo + offset_BodyPart) = 1;
        }
    }
    return orig_PlayerNetwork_TakeDamage(ClosestEnemy, baseDamage, damager, damageInfo, weaponDataID, firePos, hitPos, checkParams, damagerWeaponDynamicInfo, damagerVehicleID);
}

int (*old_BLAGCMCGEJG1)(void *, HitObjectInfo *);
int BLAGCMCGEJG1(void *ist, HitObjectInfo *HitObject) {
    if (SilentAimv222) {
        if (HitObject != nullptr) {
            void *current_match = Curent_Match();
            if (current_match != NULL) {
                void* local_player = GetLocalPlayer(current_match);
                if (local_player != NULL) {
                    void* WeaponHand = GetWeaponOnHand(local_player);
                    
                    auto *ClosestEnemy = EnemySlientAim();
                    
                    if (ClosestEnemy != nullptr) {
                        if (isEnemyInRangeWeapon(local_player, ClosestEnemy, WeaponHand)) {
                            Vector3 EnemyLocation;
                            
                            if (aimPosition == 0) {
                                EnemyLocation = GetHeadPosition(ClosestEnemy);
                            } else if (aimPosition == 1) {
                                EnemyLocation = GetHipPosition(ClosestEnemy); 
                            } else if (aimPosition == 2) {
                                EnemyLocation = GetHipPosition(ClosestEnemy); 
                            } else if (aimPosition == 3) {
                                EnemyLocation = GetHipPosition(ClosestEnemy); 
                            }                            
                            Vector3 PlayerLocation = CameraMain(local_player);
                            HitObject->HitObject = get_gameObject(Player_GetHeadCollider(ClosestEnemy));
                            HitObject->HitCollider = Player_GetHeadCollider(ClosestEnemy);
                            
                            HitObject->HitLocation = EnemyLocation;
                            HitObject->HitNormal = EnemyLocation;
                            HitObject->RayDir = Vector3::Normalized(EnemyLocation - PlayerLocation);
                            HitObject->StartPosition = PlayerLocation;
                            HitObject->OrigStartPosition = PlayerLocation;
                            HitObject->SpecialHitType = 0;
                            HitObject->HitGroup = 1;
                            HitObject->IgnoreHappens = false;
                            HitObject->ViewBlocked = false;
                        }
                    }
                }
            }
        }
    }
    return old_BLAGCMCGEJG1(ist, HitObject);
}

bool isVisible(void * player){
    if(player != NULL) {
        void *hitObj = NULL;
        Vector3 cameraLocation = Transform_GetPosition(Component_GetTransform(Camera_main()));
        Vector3 headLocation = Transform_GetPosition(Component_GetTransform(Player_GetHeadCollider(player)));
            return !Physics_Raycast(cameraLocation, headLocation, 12, &hitObj);
    }
    return false;
}

bool isFov(Vector3 vec1, Vector3 vec2, int radius) {
    int x = vec1.x;
    int y = vec1.y;

    int x0 = vec2.x;
    int y0 = vec2.y;
    if ((pow(x - x0, 2) + pow(y - y0, 2) ) <= pow(radius, 2)) {
        return true;
    } else {
        return false;
    }
}

void* EnemyVisible(void* match) {
    if (!match || !EnableAim || !EnableAim) {
        return nullptr;
    }
    
    float shortestDistance = 99999.0f;
    float maxAngle = Fov_Aim;
    void* closestEnemy = nullptr;
    void* LocalPlayer = GetLocalPlayer(match);
    if (!LocalPlayer) return nullptr;

    auto players = *(MyDictionary**)((uintptr_t)match + ListPlayer);
    if (!players || !players->entries) return nullptr;

    auto entries = players->entries;

    for (int u = 0; u < players->count; u++) {
        DictionaryEntry& entry = entries->vector[u];
        if (entry.hashCode < 0) continue;

        void* Player = entry.value;
        if (!Player) continue;
        

        if (!get_isLocalTeam(Player)
            && (!ignoreKnocked || !get_IsDieing(Player))
            && get_isVisible(Player)
            && get_MaxHP(Player) > 0) {

            Vector3 PlayerPos = GetHeadPosition(Player);
            Vector3 LocalPlayerPos = GetHeadPosition(LocalPlayer);
            Vector3 targetDir = Vector3::Normalized(PlayerPos - LocalPlayerPos);

            float angle = Vector3::Angle(targetDir, GetForward(Component_GetTransform(Camera_main()))) * 100.0f;
            if (!get_God(Player)) {
                if (AimVisible) {
                    if (isVisible(Player) &&
                        angle <= maxAngle &&
                        angle < shortestDistance) {
                        shortestDistance = angle;
                        closestEnemy = Player;
                    }
                } else {
                    if (angle <= maxAngle &&
                        angle < shortestDistance) {
                        shortestDistance = angle;
                        closestEnemy = Player;
                    }
                }
            }
        }
    }
    return closestEnemy;
}

bool ghostmapcs = false;
Vector3 SetMap = Vector3(0, 0, 0);
int HighVp = 0;

static auto get_position114(void *player) {
    auto (*_get_position114)(void *player) = (Vector3 (*)(void *))Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.CoreModule.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Transform"), OBFUSCATE("get_position"), 0);
    return _get_position114(player);
}
static auto set_position_Injected113(void *player, Vvector3 position) {
    auto (*_set_position_Injected113)(void *player, Vvector3 position) = (void (*)(void *, Vvector3))Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.CoreModule.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Transform"), OBFUSCATE("set_position_Injected"), 1);
    return _set_position_Injected113(player, position);
}
static auto get_transform113(void *player) {
    auto (*_get_transform113)(void *player) = (void *(*)(void *))Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.CoreModule.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Component"), OBFUSCATE("get_transform"), 0);
    return _get_transform113(player);
}
static auto get_position_Injected(void *player) {
    auto zero = Vector3::zero();
    auto (*_get_position_Injected)(void *player, Vector3 *zero) = (void (*)(void *, Vector3 *))Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.CoreModule.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Transform"), OBFUSCATE("get_position_Injected"), 1);
    _get_position_Injected(player, &zero);
    return zero;
}
int (*orig_BEV_Jump_onExecute)(int Player, int a);
int hook_BEV_Jump_onExecute(int Player, int a) {
  if (Player && HighVp < 10) {
    HighVp += 2;
  }
  return orig_BEV_Jump_onExecute(Player,a);
}
int (*orig_isground)(void *Player, void* a);
int hook_isground(void *Player, void* a) {
  if (ghostmapcs) {
    return -1;
  }
  return orig_isground(Player,a);
}
static auto get_Match() {
    auto (*_get_Match)(void *player) = (void *(*)(void *))Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW"), OBFUSCATE("GameFacade"), OBFUSCATE("CurrentMatch"), 0);
    return _get_Match(nullptr);
}
static auto get_HeadTF(void *player) {
    auto (*_get_HeadTF)(void *player) = (void *(*)(void *))Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("GetHeadTF"), 0);
    return _get_HeadTF(player);
}
void* (*old_GarenaMSDKMgr)(void *cmd, void* a);
void* GarenaMSDKMgr(void *cmd, void* a) {
        if (cmd != nullptr) {
        if (ghostmapcs) {
      if (SetMap == Vector3(0, 0, 0)) {      
      void* MatchFC = Curent_Match();
      void* LocalPlayer = GetLocalPlayer(MatchFC);
        SetMap = get_position_Injected(get_transform113(LocalPlayer));
      }
    } else {
      SetMap = Vector3(0, 0, 0);
    }
    if (ghostmapcs) {      
      void *MatchFC = get_Match();
      void *LocalPlayer = GetLocalPlayer(MatchFC);
      set_position_Injected113(get_transform113(LocalPlayer),Vvector3(get_position114(get_transform113(LocalPlayer)).x, SetMap.y + HighVp/5, get_position114(get_transform113(LocalPlayer)).z));
    } else {
      HighVp = 0;
    }
    }
    return old_GarenaMSDKMgr(cmd, a);
}

void (*LateUpdate)(void* Player);
void _LateUpdate(void* Player) {
    if (Player != NULL) {

void* CurrentMatch = Curent_Match();
void* closestEnemy = EnemyVisible(CurrentMatch);
void* LocalPlayer = GetLocalPlayer(CurrentMatch);

if (EnableAim) {
        if (closestEnemy != NULL && LocalPlayer != NULL && CurrentMatch != NULL) {
            Vector3 EnemyLocation = GetHeadPosition(closestEnemy);
            Vector3 PlayerLocation = CameraMain(LocalPlayer);
			Quaternion PlayerLook = GetRotationToLocation(EnemyLocation, 0.1f, PlayerLocation);
            bool IsScopeOn = get_IsSighting(LocalPlayer);
            bool IsFiring = get_IsFiring(LocalPlayer);
            if (AimFire) {
                if (IsFiring) {
                    set_aim(LocalPlayer, PlayerLook);  
                }
            }
            if (AimScope) {
                if (IsScopeOn) {
                    set_aim(LocalPlayer, PlayerLook);  
                }
            }
            if (AimAuto) {
                set_aim(LocalPlayer, PlayerLook);  
            }
        }
    }
	
if (EnableAim) {
        if (CurrentMatch != nullptr && LocalPlayer != nullptr) {
            MyDictionary* players = *(MyDictionary**)((long)CurrentMatch + ListPlayer);
            if (players && players->entries) {
                int count = players->count;
                for (int i = 0; i < count; i++) {
                    DictionaryEntry entry = players->entries->vector[i];
                    if (entry.hashCode >= 0) {
                        void* enemy = entry.value;
                        if (enemy && !get_isLocalTeam(enemy) && !get_IsDieing(enemy) && GetHp(enemy) > 0 && get_MaxHP(enemy) > 0 && get_isVisible(enemy)) {
                            Vector3 LocalBody = GetAttackableCenterWS(LocalPlayer);
                            Vector3 EnemyBody = GetAttackableCenterWS(enemy);
                            void* imo = get_imo(LocalPlayer);
                            if (imo) {
                                if (EspFire) {
                                    set_esp1(imo, LocalBody, EnemyBody, enemy);
                                }
                                if (EspFireBlue) {
                                    set_esp3(imo, LocalBody, EnemyBody);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
	
	if (EspAlert) {        
        void* uiInstance = CurrentInGameUIScene();          
        if (uiInstance != nullptr) {        
            void* match = Curent_Match();          
            if (match != nullptr) {        
                void* localPlayer = GetLocalPlayer(match);          
                if (localPlayer != nullptr) {        
                    void* enemy = EnemyVisible(match);          
                    if (enemy != nullptr && enemy != localPlayer && !get_isLocalTeam(enemy) && !get_IsDieing(enemy)) {        
                        float distance = Vector3::Distance(getPosition(localPlayer), getPosition(enemy));
                        int curHp = GetHp(enemy);
                        bool isBot = *(bool *)((uintptr_t)enemy + offset_IsClientBot);
                        std::string nameStr = GetNameFromPlayer(enemy);  
                        monoString* nameMono = il2cpp_string_new(nameStr.c_str());  
                        monoString* infoLine = U3DStrFormat(distance, curHp, isBot);
                        ShowAssistantText(uiInstance, nameMono, infoLine);
                    }       
                }        
            }        
        }        
    }

    }
    LateUpdate(Player);
}

#define targetLibName OBFUSCATE("libil2cpp.so")
ProcMap unityMap, anogsMap, anortMap, il2cppMap, ggpMap, FFWebRequestMap, thor_utilsMap;

uintptr_t il2cpp, unity, anogs;
void *hack_thread(void *)
{ 
    while (!il2cpp) {
        il2cpp = GetBaseAddress("libil2cpp.so");
        unity = GetBaseAddress("libunity.so");
        thor_utilsMap = KittyMemory::getLibraryBaseMap("libthor_utils.so");
        anogsMap = KittyMemory::getLibraryBaseMap("libanogs.so");
        anortMap = KittyMemory::getLibraryBaseMap("libanort.so");
        sleep(4);
    }
    Il2CppAttach();
    shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false);

    /*
    if (mlovinit()) {
     setShader("_AlphaMask"); 
     LogShaders();
     Wallhack();
    }
    */
  
shadowhook_hook_func_addr((void*)Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW"), OBFUSCATE("GameConfig"), OBFUSCATE("get_ResetGuest"), 0), (void*)_ResetGuest, (void**)&ResetGuest);                                   
shadowhook_hook_func_addr((void*)Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("GPBDEDFKJNA"), OBFUSCATE("BLAGCMCGEJG"), 1), (void*) BLAGCMCGEJG1,(void**) &old_BLAGCMCGEJG1);            
shadowhook_hook_func_addr((void*)Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("PlayerNetwork"), OBFUSCATE("TakeDamage"), 9), (void*) &hook_PlayerNetwork_TakeDamage, (void**) &orig_PlayerNetwork_TakeDamage);
shadowhook_hook_func_addr((void*)Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("UpdateBehavior"), 2), (void*)_LateUpdate, (void**)&LateUpdate);                                               
uintptr_t OMNMBMOKLOH_onExecute = (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("OMNMBMOKLOH"), OBFUSCATE("onExecute"), 1);
shadowhook_hook_func_addr((void *) OMNMBMOKLOH_onExecute, (void *) hook_BEV_Jump_onExecute, (void **) &orig_BEV_Jump_onExecute);
uintptr_t CharacterController_get_isGrounded = (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.PhysicsModule.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("CharacterController"), OBFUSCATE("get_isGrounded"), 0);
shadowhook_hook_func_addr((void *) CharacterController_get_isGrounded, (void *) hook_isground, (void **) &orig_isground);
uintptr_t GarenaMSDKMgr_Update = (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("GarenaMSDK"), OBFUSCATE("GarenaMSDKMgr"), OBFUSCATE("Update"), 0);
shadowhook_hook_func_addr((void *) GarenaMSDKMgr_Update, (void *)GarenaMSDKMgr, (void **) &old_GarenaMSDKMgr);

    return NULL;
}

