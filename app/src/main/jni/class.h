Quaternion GetRotationToLocation(Vector3 targetLocation, float y_bias, Vector3 myLoc){
    return Quaternion::LookRotation((targetLocation + Vector3(0, y_bias, 0)) - myLoc, Vector3(0, 1, 0));
}
template <typename T>
struct UnityArray {
    void* klass;
    void* monitor;
    void* bounds;
    int max_length;
    T vector[64]; 
};

struct DictionaryEntry {
    int hashCode;
    int next;
    void* key;
    void* value;
};

struct MyDictionary {
    void* klass;
    void* monitor;
    void* buckets;
    UnityArray<DictionaryEntry>* entries;
    int count;
};

template <typename T>
struct monoArray
{
    void* klass;
    void* monitor;
    void* bounds;
    int   max_length;
    void* vector [1];
    int getLength()
    {
        return max_length;
    }
    T getPointer()
    {
        return (T)vector;
    }
};

template <typename T>
struct monoList {
    void *unk0;
    void *unk1;
    monoArray<T> *items;
    int size;
    int version;

    T getItems(){
        return items->getPointer();
    }

    int getSize(){
        return size;
    }

    int getVersion(){
        return version;
    }
};

union intfloat {
    int i;
    float f;
};

typedef struct _monoString
{
    void* klass;
    void* monitor;
    int length;    
    char chars[1];   
    int getLength()
    {
      return length;
    }
    char* getChars()
    {
        return chars;
    }
}monoString;

class Vvector3 {
public:
    float X;
    float Y;
    float Z;
    Vvector3() : X(0), Y(0), Z(0) {}
    Vvector3(float x1, float y1, float z1) : X(x1), Y(y1), Z(z1) {}
    Vvector3(const Vvector3 &v);
    ~Vvector3();
};

Vvector3::Vvector3(const Vvector3 &v) : X(v.X), Y(v.Y), Z(v.Z) {}
Vvector3::~Vvector3() {}

#define ListPlayer (uintptr_t) Il2CppGetFieldOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("NFJPHMKKEBF"), OBFUSCATE("NIEBEGJADLC"))

#define offset_GetWeaponOnHand (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("GetWeaponOnHand"), 0)
static void* GetWeaponOnHand(void *local) {
    void *(*_GetWeaponOnHand)(void *local) = (void *(*)(void *))(offset_GetWeaponOnHand);
    return _GetWeaponOnHand(local);
}
#define Match (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW"), OBFUSCATE("GameFacade"), OBFUSCATE("CurrentMatch"), 0)

static void *Curent_Match() {
    void *(*_Curent_Match) (void *nuls) = (void *(*)(void *))(Match);
    return _Curent_Match(NULL);
}

#define Local (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW"), OBFUSCATE("UIHudDetectorController"), OBFUSCATE("GetLocalPlayer"), 0)

static void *GetLocalPlayer(void* Game) {
    void *(*_GetLocalPlayer)(void *match) = (void *(*)(void *))(Local);
    return _GetLocalPlayer(Game);
}
#define Class_Transform__Position (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.CoreModule.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Transform"), OBFUSCATE("get_position"), 0)

 Vector3 get_position(void *player) {
    Vector3 (*_get_position)(void *players) = (Vector3 (*)(void *))(Class_Transform__Position);
    return _get_position(player);
}

#define Head (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("GetHeadTF"), 0)

static void *GetHeadPositions(void *player) {
    void *(*_GetHeadPositions)(void *players) = (void*(*)(void *))(Head);
     return _GetHeadPositions(player);
}
#define Hip (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("GetHipTF"), 0)

static void *GetHipPositions(void *player) {
    void *(*_GetHipPositions)(void *players) = (void*(*)(void *))(Hip);
     return _GetHipPositions(player);
}
static Vector3 GetHipPosition(void* player) {
    return get_position(GetHipPositions(player));
}

static Vector3 GetHeadPosition(void* player) {
    return get_position(GetHeadPositions(player));
}

#define Class_Compent__Transform Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Component"), OBFUSCATE("get_transform"), 0)

static void *Component_GetTransform(void *player) {
    void *(*_Component_GetTransform)(void *component) = (void *(*)(void *))(Class_Compent__Transform);
    return _Component_GetTransform(player);
}
Vector3 getPosition(void *transform) {
    return get_position(Component_GetTransform(transform));
}   

#define offset_IsClientBot (uintptr_t)Il2CppGetFieldOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("IsClientBot"))
bool get_IsClientBot(void* player) {
    if (!player) return false;
    return *(bool*)((uint64_t)player + offset_IsClientBot);
}
#define Team (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("IsLocalTeammate"), 1)

static bool get_isLocalTeam(void *player) {
    bool (*_get_isLocalTeam)(void *players) = (bool (*)(void *))(Team);
    return _get_isLocalTeam(player);
}

static bool get_isLocalTeam(void *player, bool isCheckSocial3pEffect) {
    bool (*_get_isLocalTeam)(void *, bool) = (bool (*)(void *, bool))(Team);
    return _get_isLocalTeam(player, isCheckSocial3pEffect);
}
#define Die (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("get_IsDieing"), 0)

static bool get_IsDieing(void *player) {
    bool (*_get_die)(void *players) = (bool (*)(void *))(Die);
    return _get_die(player);
}
#define CurHP (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("get_CurHP"), 0)

static int GetHp(void* player) {
    int (*_GetHp)(void* players) = (int(*)(void *))(CurHP);
    return _GetHp(player);
}
#define MaxHP (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("get_MaxHP"), 0)

static int get_MaxHP(void* enemy) {
    int (*_get_MaxHP)(void* player) = (int(*)(void *))(MaxHP);
    return _get_MaxHP(enemy);
}
#define Visible (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("IsVisible"), 0)

static bool get_isVisible(void *player) {
    bool (*_get_isVisible)(void *players) = (bool (*)(void *))(Visible);
    return _get_isVisible(player);
}
#define Class_Transform__GetPosition Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Transform"), OBFUSCATE("get_position_Injected"), 1)

static Vector3 Transform_GetPosition(void *player) {
    Vector3 out = Vector3::zero();
    void (*_Transform_GetPosition)(void *transform, Vector3 * out) = (void (*)(void *, Vector3 *))(Class_Transform__GetPosition);
    _Transform_GetPosition(player, &out);
    return out;
}
#define Class_Camera__get_main (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Camera"), OBFUSCATE("get_main"))

static void *Camera_main() {
    void *(*_Camera_main)(void *nuls) = (void *(*)(void *))(Class_Camera__get_main);
    return _Camera_main(nullptr);
}
#define offset_get_Range (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("GPBDEDFKJNA"), OBFUSCATE("JDGGIFMKIKF"), 0)
static float get_Range(void *weapon) {
    if (!weapon) return 0.0f;
    auto _get_Range = (float (*)(void *))(offset_get_Range);
    return _get_Range(weapon);
}
#define HeadColider (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("get_HeadCollider"), 0)

static void *Player_GetHeadCollider(void *player) {
    void *(*_Player_GetHeadCollider)(void *players) = (void *(*)(void *))(HeadColider);
    return _Player_GetHeadCollider(player);
}
#define Raycast (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("JEAGCMACNNC"), OBFUSCATE("PLDCHDBCOBF"), 4)

static bool Physics_Raycast(Vector3 camLocation, Vector3 headLocation, unsigned int LayerID, void* collider) {
    bool (*_Physics_Raycast)(Vector3 camLocation, Vector3 headLocation, unsigned int LayerID, void* collider) = (bool(*)(Vector3, Vector3, unsigned int, void*))(Raycast);
    return _Physics_Raycast(camLocation, headLocation, LayerID, collider);
}
#define MainCam (uintptr_t) Il2CppGetFieldOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("MainCameraTransform"))

static Vector3 CameraMain(void* player){
    return get_position(*(void**) ((uint64_t) player + MainCam));
}
#define GameObject Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Component"), OBFUSCATE("get_gameObject"), 0)
void *get_gameObject(void *player) {
    return ((void *(*)(void *))(GameObject))(player);
}
#define ForWard (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Transform"), OBFUSCATE("get_forward"), 0)

static Vector3 GetForward(void *player) {
    Vector3 (*_GetForward)(void *players) = (Vector3 (*)(void *))(ForWard);
    return _GetForward(player);
}
#define Imo (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("GetActiveWeapon"))
static void* get_imo(void* player) {
    void* (*_GetImo)(void* players) = (void* (*)(void*))Imo;
    return _GetImo(player);
}

#define Esp2 (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("UGCLevelMiniSentry"), OBFUSCATE("CPBCGAKODII"), 2)
static void set_esp2(void* imo, Vector3 x, Vector3 y) {
    void (*_SetEsp2)(void* imo, Vector3 X, Vector3 Y) = (void (*)(void*, Vector3, Vector3))Esp2;
    _SetEsp2(imo, x, y);
}

#define CenterWS1 (uintptr_t)Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("GetAttackableCenterWS"))

static Vector3 GetAttackableCenterWS(void* player) {
    Vector3 (*_GetAttackableCenterWS)(void*) = (Vector3 (*)(void*))CenterWS1;
    return _GetAttackableCenterWS(player);
}
#define offset_isGod (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("get_IsGod"), 0)
static bool get_God(void *player) {
    bool (*_get_God)(void *players) = (bool (*)(void *))(offset_isGod);
    return _get_God(player);
}
#define Aim (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("JPNJCAONHME"), 1)

static void set_aim(void *player, Quaternion look) {
    void (*_set_aim)(void *players, Quaternion lock) = (void (*)(void *, Quaternion))(Aim);
    _set_aim(player, look);
}
#define Scope (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("get_IsSighting"),0 )

#define Fire (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("IsFiring"), 0)

static bool get_IsSighting(void *player) {
    bool (*_get_IsSighting)(void *players) = (bool (*)(void *))(Scope);
    return _get_IsSighting(player);
}

static bool get_IsFiring(void *player) {
    bool (*_get_IsFiring)(void *players) = (bool (*)(void *))(Fire);
    return _get_IsFiring(player);
}

#define Esp1 (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("GPBDEDFKJNA"), OBFUSCATE("CPBCGAKODII"), 4)
static void set_esp1(void* imo, Vector3 x, Vector3 y, void* obj) {
    void (*_SetEsp1)(void*, Vector3, Vector3, void*) = (void (*)(void*, Vector3, Vector3, void*))Esp1;
    _SetEsp1(imo, x, y, obj);
}

#define PlayGunTrace (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("UGCLevelMiniSentry"), OBFUSCATE("CPBCGAKODII"), 2)
static void set_esp3(void* imo, Vector3 x, Vector3 y) {
    void (*_set_esp3)(void *imo, Vector3 X, Vector3 Y) = (void (*)(void *, Vector3, Vector3))(PlayGunTrace);
    _set_esp3(imo, x, y);
}

#define m_currentUi Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW"), OBFUSCATE("GameFacade"), OBFUSCATE("CurrentInGameUIScene"), 0)
static void* CurrentInGameUIScene() {
    using fnCurrentUIScene = void* (*)();
    auto _CurrentUIScene = reinterpret_cast<fnCurrentUIScene>(m_currentUi);
    return _CurrentUIScene();
}

#define m_addTeamHud Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW"), OBFUSCATE("UIInGameScene"), OBFUSCATE("ShowAssistantText"), 2)
static void ShowAssistantText(void* uiInstance, monoString* playerName, monoString* line) {
    using fnShowAssistantText = void(*)(void*, monoString*, monoString*);
    auto _ShowAssistantText = reinterpret_cast<fnShowAssistantText>(m_addTeamHud);
    if (uiInstance != nullptr) {
        _ShowAssistantText(uiInstance, playerName, line);
    }
}

std::string UTF16ToUTF8(const std::u16string& src) {
    std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> conv;
    return conv.to_bytes(src);
}

std::string GetNameFromPlayer(void* player) {
    if (!player) return "Player";
    auto get_NickName = (void* (*)(void*))Il2CppGetMethodOffset("Assembly-CSharp.dll","COW.GamePlay","Player","get_NickName",0);
    if (!get_NickName) return "Player";
    void* nickNamePtr = get_NickName(player);
    if (!nickNamePtr) return "Player";
    int length = *(int*)((uintptr_t)nickNamePtr + 0x10);
    if (length <= 0) return "Player";
    std::u16string str16((char16_t*)((uintptr_t)nickNamePtr + 0x14), length);
    return UTF16ToUTF8(str16);
}

monoString *il2cpp_string_new (const char *str){
    static const auto __il2cpp_string_new = (monoString*(*)(const char*))dlsym(dlopen("libil2cpp.so", RTLD_NOLOAD), "il2cpp_string_new");
    return __il2cpp_string_new(str);
}

static monoString* U3DStrFormat(float distance, int hp, bool isBot) {
    char buffer[128] = {0};
    sprintf(buffer, OBFUSCATE("DIST %.0f M | %s | %d HP - S3 HACKS"), distance, isBot ? "BOT" : "REAL", hp);
    return il2cpp_string_new(buffer);
}

static monoString* U3DStrPlayer2(float distance, int hp, bool isBot) {
    char buffer[128] = {0};
    sprintf(buffer, OBFUSCATE("DIST %.0f M | %s | %d HP - S3 HACKS"), distance, isBot ? "BOT" : "REAL", hp);
    return il2cpp_string_new(buffer);
}
#define Class_Camera__WorldToScreenPoint (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("UnityEngine.dll"), OBFUSCATE("UnityEngine"), OBFUSCATE("Camera"), OBFUSCATE("WorldToScreenPoint"), 1)

static Vector3 WorldToScreenPoint(void *WorldCam, Vector3 WorldPos) {
    Vector3 (*_WorldToScreenScene)(void* Camera, Vector3 position) = (Vector3 (*)(void*, Vector3)) (Class_Camera__WorldToScreenPoint);
    return _WorldToScreenScene(WorldCam, WorldPos);
}
#define MainCam (uintptr_t) Il2CppGetFieldOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("MainCameraTransform"))
#define Name (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("Assembly-CSharp.dll"), OBFUSCATE("COW.GamePlay"), OBFUSCATE("Player"), OBFUSCATE("get_NickName"), 0)

static monoString* get_NickName(void* player) {
    monoString* (*_get_NickName)(void* players) = (monoString * (*)(void*))(Name);
    return _get_NickName(player);
}

#define CharGet (uintptr_t) Il2CppGetMethodOffset(OBFUSCATE("mscorlib.dll"), OBFUSCATE("System"), OBFUSCATE("String"), OBFUSCATE("get_chars"), 1)
char get_Chars(monoString* str, int index) {
    char (*_get_Chars)(monoString * str, int index) = (char (*)(monoString*, int))(CharGet);
    return _get_Chars(str, index);
}
