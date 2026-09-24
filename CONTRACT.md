# 장보기(Jangbogi) 앱 — 공용 계약 문서

프로젝트 루트: `C:\Users\minwoo\Documents\Jangbogi` (이하 ROOT)

## 0. 공통 절대 규칙
- 모든 파일 작업은 **절대경로**로 ROOT 아래에서만. `C:\Users\minwoo\Documents\GitHub\*` (회사 레포) 읽기/쓰기 절대 금지.
- **gradle/빌드/설치 실행 금지.** 코드는 이 문서 스펙대로 "컴파일되게" 작성만 한다. 통합 담당(오케스트레이터)이 빌드한다.
- 빌드 파일(`*.gradle.kts`, `libs.versions.toml`, `gradle.properties`, `local.properties`, `AndroidManifest.xml`, `res/values*/themes.xml`)은 통합 담당 소유 — 수정 금지.
- 새 의존성 추가 금지. 아래 2절의 라이브러리만 사용.
- 주석 최소화: 비자명한 의도·함정·계약만. 자명한 코드 설명 주석 금지.
- 사용자 노출 문자열은 전부 한국어. 시크릿/개인정보 기록 금지.
- 이미 존재하는 파일을 덮어쓸 땐 먼저 Read 후 Write.

## 1. 제품 요약
개인용 장보기 리스트 Android 앱. 이름 **"장보기"**.
- 목표: **이쁘다**(모던·감성), **간편하다**(한 손, 최소 탭), **가시성**(마트에서 걸으며 봐도 명확 — 큰 글씨·큰 터치 타깃·고대비).
- 완전 오프라인, 로컬 Room DB. 서버/로그인/네트워크 권한 없음.
- 화면 2개: **홈**(장보기 목록들) + **리스트 상세**(항목 추가/체크).

## 2. 고정 버전과 API 주의사항
Kotlin 2.0.20 / AGP 8.5.2 / compileSdk·targetSdk 34 / minSdk 26
Compose BOM **2024.06.00** → compose ui·foundation 1.6.8, **material3 1.2.1**
navigation-compose 2.7.7 / lifecycle 2.8.4 / activity-compose 1.9.1 / Room 2.6.1(KSP) / core-splashscreen 1.0.1

반드시 지킬 API 명칭 (버전 불일치로 인한 컴파일 실패 방지):
- 스와이프 삭제: `SwipeToDismissBox` + `rememberSwipeToDismissBoxState` + `SwipeToDismissBoxValue` (M3 1.2 명칭. 구 `SwipeToDismiss` 금지)
- LazyColumn 항목 이동 애니메이션: `Modifier.animateItemPlacement()` (신 API `animateItem()` 금지) — `items(..., key = { ... })` 필수
- `ModalBottomSheet`, `TopAppBar`, `LargeTopAppBar` 등: `@OptIn(ExperimentalMaterial3Api::class)`
- `stickyHeader`, `animateItemPlacement`, `FlowRow`: `@OptIn(ExperimentalFoundationApi::class)` / `@OptIn(ExperimentalLayoutApi::class)`
- 상태 수집: `collectAsStateWithLifecycle()` (lifecycle-runtime-compose)
- **material-icons-extended 없음**: 코어 아이콘만 사용 — `Icons.Rounded/Default`의 Add, Check, Close, Clear, Delete, Edit, Share, ArrowBack, MoreVert, ShoppingCart. 그 외 그림은 이모지 텍스트로.
- Hilt / kotlinx.serialization / Coil / accompanist 없음.

## 3. 파일 소유권 (자기 소유 파일만 생성/수정)

| 경로 (ROOT/app/src/main 기준) | 소유 |
|---|---|
| `java/com/minwoo/jangbogi/domain/*` | **백엔드** |
| `java/com/minwoo/jangbogi/data/*` | **백엔드** |
| `java/com/minwoo/jangbogi/ui/viewmodel/*` | **백엔드** |
| `java/com/minwoo/jangbogi/JangbogiApp.kt` | **백엔드** |
| `ROOT/app/src/test/java/com/minwoo/jangbogi/*` | **백엔드** |
| `java/com/minwoo/jangbogi/ui/theme/*` (Color.kt, Type.kt, Theme.kt) | **디자이너** |
| `res/drawable/ic_launcher_foreground.xml`, `res/drawable/ic_launcher_background.xml` (플레이스홀더 덮어쓰기) | **디자이너** |
| `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` (플레이스홀더 덮어쓰기) | **디자이너** |
| `ROOT/design/DESIGN.md`, `ROOT/design/mockup.html` | **디자이너** |
| `java/com/minwoo/jangbogi/MainActivity.kt` (스텁 덮어쓰기) | **프론트** |
| `java/com/minwoo/jangbogi/ui/navigation/*`, `ui/screens/*`, `ui/components/*` | **프론트** |
| `res/values/strings.xml` (덮어쓰되 `app_name`="장보기" 유지) | **프론트** |

패키지 루트: `com.minwoo.jangbogi`

## 4. 도메인 계약 — 시그니처 그대로 (백엔드 구현, 프론트 사용)

### 4.1 Category (`domain/Category.kt`) — 선언 순서 = 마트 동선 = 화면 표시 순서
```kotlin
enum class Category(val display: String, val emoji: String) {
    VEGETABLE("채소", "🥬"),
    FRUIT("과일", "🍎"),
    MEAT_EGG("정육·계란", "🥩"),
    SEAFOOD("수산", "🐟"),
    DAIRY("유제품", "🥛"),
    FROZEN("냉동·간편식", "🧊"),
    SNACK("과자·간식", "🍪"),
    BEVERAGE("음료", "🧃"),
    SEASONING("조미료·소스", "🧂"),
    HOUSEHOLD("생활용품", "🧻"),
    ETC("기타", "🛒")
}
```

### 4.2 엔티티 (`data/`)
```kotlin
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(tableName = "shopping_items" /* FK: listId → shopping_lists.id, onDelete CASCADE, index(listId) */)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Int = 1,
    val category: Category = Category.ETC,
    val isChecked: Boolean = false,
    val createdAt: Long,
    val checkedAt: Long? = null
)

@Entity(tableName = "item_history")
data class ItemHistory(
    @PrimaryKey val name: String,
    val category: Category,
    val useCount: Int = 1,
    val lastUsedAt: Long
)
```
Room: `JangbogiDatabase`(version 1, `exportSchema = false`, DB 파일명 `"jangbogi.db"`), Category는 TypeConverter(name 문자열, 미지값은 ETC 폴백). DAO는 Flow 반환.

### 4.3 UI 소비용 모델 (`domain/` 또는 `data/` — 아래 이름 그대로)
```kotlin
data class ListWithProgress(val list: ShoppingList, val totalCount: Int, val checkedCount: Int)
data class CategorySection(val category: Category, val items: List<ShoppingItem>)
data class Suggestion(val name: String, val category: Category)

data class ListUiState(
    val listName: String = "",
    val sections: List<CategorySection> = emptyList(),   // 미체크만
    val completedItems: List<ShoppingItem> = emptyList(),
    val totalCount: Int = 0,
    val checkedCount: Int = 0,
    val isLoading: Boolean = true
)
```

### 4.4 순수 로직 (`domain/`, 안드로이드 의존 금지 — 유닛테스트 대상)
```kotlin
object QuantityParser {
    data class Parsed(val name: String, val quantity: Int)
    fun parse(raw: String): Parsed
}
object Categorizer { fun categorize(name: String): Category }
object ItemOrganizer {
    fun sections(items: List<ShoppingItem>): List<CategorySection>
    fun completed(items: List<ShoppingItem>): List<ShoppingItem>
}
object ShareTextBuilder { fun build(listName: String, items: List<ShoppingItem>): String }
```

### 4.5 ViewModel (`ui/viewmodel/`, 백엔드 소유 — 프론트는 이 API만 호출)
```kotlin
class HomeViewModel(/* repo */) : ViewModel() {
    val lists: StateFlow<List<ListWithProgress>>            // createdAt DESC
    fun createList(name: String, onCreated: (Long) -> Unit) // name 공백이면 "장보기 목록" 사용
    fun renameList(listId: Long, newName: String)
    fun deleteList(listId: Long)                            // undo 캐시
    fun undoDeleteList()
}

class ListViewModel(/* repo */, listId: Long) : ViewModel() {
    val uiState: StateFlow<ListUiState>
    val query: StateFlow<String>
    val suggestions: StateFlow<List<Suggestion>>
    fun onQueryChange(newQuery: String)
    fun addItem(rawInput: String)     // 6절 규칙. 성공 시 query를 ""로 리셋
    fun toggleItem(itemId: Long)
    fun deleteItem(itemId: Long)      // undo 캐시
    fun undoDeleteItem()
    fun updateItem(itemId: Long, name: String, quantity: Int, category: Category)
    fun renameList(newName: String)
    fun clearCompleted()              // undo 캐시
    fun undoClearCompleted()
    fun buildShareText(): String
}
```

### 4.6 DI (`JangbogiApp.kt`, 백엔드 소유 — Hilt 금지, 수동 컨테이너)
```kotlin
class JangbogiApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() { super.onCreate(); container = AppContainer(this) }
}
class AppContainer(context: Context) {
    // Room/Repository 초기화는 백엔드 자유 설계
    fun homeViewModelFactory(): ViewModelProvider.Factory
    fun listViewModelFactory(listId: Long): ViewModelProvider.Factory
}
```
프론트 사용 예 (이 패턴 그대로):
```kotlin
val app = LocalContext.current.applicationContext as JangbogiApp
val vm: HomeViewModel = viewModel(factory = app.container.homeViewModelFactory())
val listVm: ListViewModel = viewModel(key = "list-$listId", factory = app.container.listViewModelFactory(listId))
```
Repository/DAO의 내부 형태는 백엔드 자유. 위 4.1~4.6 심볼 이름·시그니처만 불변.

## 5. 동작 규칙 (백엔드 구현 세부)
- **addItem(raw)**: `QuantityParser.parse` → (name, qty).
  - 같은 리스트에 동일 이름(트림·연속공백 1개로 정규화 후 비교) 항목이 있으면: 미체크 상태면 `quantity += qty`, 체크 상태면 미체크로 복귀(checkedAt=null) + `quantity = qty`. 없으면 insert.
  - 카테고리 결정: ItemHistory에 있으면 그 카테고리 → 없으면 `Categorizer.categorize` → ETC.
  - 이후 ItemHistory upsert: useCount+1, lastUsedAt=now (name 키는 정규화된 이름).
- **updateItem**: 카테고리를 바꾸면 ItemHistory의 해당 이름 카테고리도 갱신(학습).
- **정렬**: sections는 Category 선언 순서, 섹션 내 createdAt ASC. completedItems는 checkedAt DESC(null은 뒤).
- **suggestions**: query 공백 → useCount DESC, lastUsedAt DESC 상위 10개. query 있음 → name LIKE '%query%' 동일 정렬 10개. 두 경우 모두 현재 리스트의 **미체크** 항목 이름은 제외.
- **undo**: deleteItem/deleteList/clearCompleted 각각 마지막 1건을 메모리 캐시, undo 호출 시 원래 id 유지한 채 재삽입.
- **toggleItem**: 체크 시 checkedAt=now, 해제 시 null.

### 5.1 QuantityParser 규칙 (테스트로 고정)
- 입력 트림 + 내부 연속 공백 1개화.
- 끝부분이 `(공백* [xX×] 공백* 숫자)` 또는 `(공백+ 숫자)`이고 숫자가 1~99, 남는 이름이 비어있지 않으면 → 분리.
- 그 외 → 이름 전체, qty=1.

| 입력 | name | qty |
|---|---|---|
| `우유 2` | 우유 | 2 |
| `우유` | 우유 | 1 |
| `콜라x2` | 콜라 | 2 |
| `물 ×3` | 물 | 3 |
| `새우 300` | 새우 300 | 1 (3자리) |
| `라면5` | 라면5 | 1 (구분자 없음) |
| `2` | 2 | 1 (이름이 비게 됨) |

### 5.2 Categorizer 규칙
- 이름 정규화(공백 제거, 라틴 소문자화) 후 **부분 문자열** 매칭, **가장 긴 키워드 우선**, 동률이면 먼저 등록된 것.
- 한국 장보기 필수 키워드 **120개 이상**, 11개 카테고리 전부 커버. 충돌 예시 필수 반영: `아이스크림`(FROZEN)이 `크림`(DAIRY)보다 우선, `새우깡`(SNACK)이 `새우`(SEAFOOD)보다 우선.
- 예: 우유/치즈/요거트→DAIRY, 대파/양파/마늘/상추→VEGETABLE, 사과/바나나→FRUIT, 삼겹살/계란/닭→MEAT_EGG, 고등어/오징어→SEAFOOD, 만두/피자→FROZEN, 과자/초콜릿→SNACK, 콜라/주스/생수→BEVERAGE, 간장/소금/케첩→SEASONING, 휴지/샴푸/세제→HOUSEHOLD.

### 5.3 ShareTextBuilder 포맷 (문자열 그대로 테스트 고정)
줄 구성(`\n` 결합, 마지막 개행 없음):
1. `🛒 {listName}`
2. 빈 줄
3. 미체크 항목이 있는 카테고리마다 한 줄: `{emoji} {display}: {항목들}` — 항목은 `{name}` 또는 `{name} ×{quantity}`(qty>1), `, ` 결합, 섹션 정렬 규칙 동일
4. (3이 있었으면) 빈 줄
5. (완료 항목 있으면) `✅ 완료: {이름들 ", " 결합}` (checkedAt DESC)
6. `남은 {미체크수}개 · 전체 {전체수}개`

예시 (정확히 이 출력):
```
🛒 주말 장보기

🥬 채소: 대파, 양파 ×2
🥛 유제품: 우유 ×2

✅ 완료: 계란
남은 3개 · 전체 4개
```

## 6. 화면 UX 스펙 (프론트 구현)

### 6.1 공통
- `MainActivity`: `installSplashScreen()` → `enableEdgeToEdge()` → `setContent { JangbogiTheme { AppNavHost() } }`.
- 내비: navigation-compose, 라우트 `"home"` / `"list/{listId}"`(LongType).
- 색·타이포는 `MaterialTheme.colorScheme` / `MaterialTheme.typography` 슬롯만 참조(하드코딩 금지). 투명도 변형(`.copy(alpha=)`)은 허용.
- 터치 타깃 48dp 이상, `contentDescription` 한국어.
- Snackbar는 Scaffold 단위 1개 호스트, undo 액션 "실행 취소".

### 6.2 홈 화면
- `LargeTopAppBar` 타이틀 "장보기" (스크롤 시 축소).
- 목록 카드 LazyColumn: 이름(굵게), `{checked}/{total}` 라벨 + 둥근 LinearProgressIndicator, 보조 라벨(비었으면 "비어 있음", 전부 체크면 "완료 🎉", 아니면 "{남은}개 남음"), 상대 날짜("오늘"/"어제"/"n일 전"/"M월 d일").
- 카드 탭 → 상세 이동. 카드 롱프레스 → 이름 변경 / 삭제 메뉴(DropdownMenu 또는 바텀시트).
- 삭제는 AlertDialog 확인 후 실행, 스낵바 "목록을 삭제했어요" + 실행 취소(undoDeleteList).
- Extended FAB "새 목록" → 다이얼로그(TextField, 기본값 `"M월 d일 장보기"` 오늘 날짜) → createList → onCreated에서 상세로 이동.
- 빈 상태: 큰 🛒 + "아직 목록이 없어요" + "새 목록을 만들어 장보기를 시작하세요".

### 6.3 리스트 상세 화면
- TopAppBar: 뒤로가기, 리스트 이름(탭하면 이름 변경 다이얼로그), 공유 아이콘, MoreVert 메뉴(완료 항목 비우기 — 완료 0개면 비활성).
- 진행 헤더: total>0일 때 "{남은}개 남았어요" 큰 글씨(전부 완료 시 "장보기 완료! 🎉"), 애니메이션되는 LinearProgressIndicator(`animateFloatAsState`).
- 본문 LazyColumn:
  - 카테고리 섹션별 `stickyHeader`(이모지+이름+개수), 항목 행들.
  - 항목 행: **행 전체 탭 = 체크 토글** + 햅틱(`LocalHapticFeedback`, `HapticFeedbackType.LongPress`). 커스텀 원형 체크(≈28dp, 체크 시 채움+체크 아이콘), 이름 큰 글씨, qty>1이면 `×n` 필 배지. 롱프레스 → 편집 바텀시트.
  - `SwipeToDismissBox`(EndToStart만) → deleteItem + 스낵바 "삭제했어요" + 실행 취소. 스와이프 배경은 error 계열 + Delete 아이콘.
  - 완료 섹션: "완료 {n}" 헤더 + 행(취소선, 알파 ~0.55, 체크 채움). 탭하면 복귀.
  - 모든 행 `key = item.id`, `Modifier.animateItemPlacement()`. 헤더 key는 `"h-{category.name}"` 등 유니크.
- **빠른 추가 바** (화면 하단 고정, `imePadding().navigationBarsPadding()`): 필 형태 TextField placeholder "무엇을 살까요? (예: 우유 2)" + 48dp 채움 Add 버튼. `ImeAction.Done`/버튼 → `addItem(query)`. 추가 후 포커스 유지(연속 입력), 텍스트는 VM이 리셋.
- **추천 칩**: suggestions 비어있지 않으면 빠른 추가 바 위에 가로 스크롤 칩(`{emoji} {name}`), 탭 → `addItem(name)`.
- 편집 바텀시트(`ModalBottomSheet`): 이름 TextField, 수량 스테퍼(− 1~99 +), 카테고리 `FlowRow` 칩 11개(이모지+이름, 선택 강조), 저장/삭제.
- 공유: `buildShareText()` → `Intent.ACTION_SEND`(text/plain) chooser "목록 공유".
- 빈 리스트 상태: "장바구니가 비어 있어요" + "아래 입력창에 살 것을 적어보세요 👇".
- 완료 항목 비우기 → 확인 다이얼로그 → clearCompleted + 스낵바 + 실행 취소.

## 7. 테마 계약 (디자이너 구현, 프론트 소비)
```kotlin
// ui/theme/Theme.kt
@Composable
fun JangbogiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```
- 내부: `MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = AppTypography, content = content)`
- `Color.kt`: `LightColors`/`DarkColors` = `lightColorScheme()`/`darkColorScheme()` — 주요 슬롯 전부 지정(primary/onPrimary/primaryContainer/onPrimaryContainer/secondary(Container)/tertiary(Container)/background/onBackground/surface/onSurface/surfaceVariant/onSurfaceVariant/surfaceContainer류/outline/outlineVariant/error류).
- `Type.kt`: `AppTypography` = `Typography(...)` 시스템 폰트(`FontFamily.Default`) 기반, 9절 기준 반영.
- Dynamic color 미사용. 컴파일되는 유효 Kotlin이어야 함(`Color(0xFF......)`).

## 8. 디자인 방향 (디자이너)
- 무드: "신선한 장보기" — M3 기본 보라 금지, 자체 팔레트. 라이트/다크 모두 설계. 마트 형광등·이동 중 사용 고려한 **고대비**(본문 텍스트 WCAG AA 이상).
- 산출물: 7절 테마 코드 3파일 + 앱 아이콘(adaptive: 장바구니 모티프 벡터, safe zone 66/108 중심, monochrome 포함, 플레이스홀더 4파일 덮어쓰기) + `design/DESIGN.md`(컴포넌트별 확정 스펙: dp/sp/radius/색 슬롯 매핑 — 통합 담당이 그대로 적용 가능하게) + `design/mockup.html`.
- mockup.html: **완전 자급자족**(외부 요청 0 — CDN/웹폰트/이미지 금지, `system-ui` 폰트 스택), 홈+리스트 두 화면 × 라이트/다크 = 4개의 390×844 프레임 나란히, 실감나는 한국 장보기 예시 데이터(6.2/6.3 구조 반영), 정적.

## 9. 기준 메트릭 (디자이너·프론트 공통 시작점, 디자이너가 DESIGN.md에서 확정)
- 화면 좌우 패딩 16dp, 카드 radius 20dp, 항목 행 radius 16dp·최소높이 56dp
- 원형 체크 28dp(터치 박스 48dp), 항목 이름 17~18sp(Medium+), 진행 헤더 22sp(Bold)
- 섹션 헤더: 이모지+14sp 라벨+개수, 위 18dp/아래 8dp
- 빠른 추가 바: 높이 56dp 필(radius 28dp), 카드 간격 10~12dp
- 타이포 스케일: display/headline(진행헤더 22~24sp) > title(18sp) > body(17sp) > label(13~14sp), 행간 넉넉히

## 10. 백엔드 유닛테스트 범위 (JUnit4, 안드로이드 의존 금지)
- `QuantityParserTest`: 5.1 표 전체 + 경계(99, 100, 0)
- `CategorizerTest`: 카테고리별 대표 + 최장매칭 충돌 2건(아이스크림/새우깡) + 미지→ETC
- `ItemOrganizerTest`: 섹션 순서/섹션 내 정렬/완료 정렬/빈 카테고리 제외
- `ShareTextBuilderTest`: 5.3 예시 문자열 정확 일치 + 빈 리스트 + 완료 없음 케이스
