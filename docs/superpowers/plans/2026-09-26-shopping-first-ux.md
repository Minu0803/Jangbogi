# 물건 중심 장보기 UX·UI 개편 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task if native execution is selected; superpowers:subagent-driven-development is the alternative if explicitly selected. Steps use checkbox syntax for tracking. Do not start implementation until the user reviews the design and this plan.

**Goal:** 앱 실행 직후 물건을 추가·체크하고, 각 목록의 ‘살 것 / 고민 중’을 손쉽게 관리하는 세련된 화면을 만든다.

**Architecture:** 기존 Compose → ViewModel → Repository → Room 구조를 유지한다. ListScreen을 홈에서도 사용하는 상태 기반 콘텐츠로 분리하고, 활성 목록 해석은 새 ShoppingHomeViewModel이 담당한다. 구매 의도는 ShoppingItem에 저장하고 공통 분류·집계로 화면과 공유를 일치시킨다.

**Tech Stack:** Kotlin 2.0.20, Compose BOM 2024.06.00 / Material3 1.2.1, Room 2.6.1, Navigation Compose 2.7.7, JUnit4, Android minSdk 26.

**Spec:** [설계 초안](../specs/2026-09-26-shopping-first-ux-design.md)

**Status:** 추천안 A 기준 구현 계획 초안. 시안·설계 검토 후 필요한 수정을 반영하고 실행한다. 현재 실행 방식은 선택 전이다.

## Global Constraints

- main에서 작업한다. 모든 커밋은 사용자 허락 후 진행한다. 아래 작업 종료마다 자동 커밋하지 않는다.
- Kotlin / Jetpack Compose / Room / Flow의 현 구조, minSdk 26, 오프라인 동작을 유지한다.
- 각 장보기 목록 안에서 ‘살 것’과 ‘고민 중’을 구분한다. 공통 고민함은 만들지 않는다.
- Room v2 → v3에서 purchaseIntent TEXT NOT NULL DEFAULT 'BUY' 추가. 기존 데이터는 보존한다.
- 현재 제품 의존성 대규모 업데이트를 포함하지 않는다. 검증에 필요한 androidTest·coroutines-test 의존성만 최소 추가한다.
- 사용자 문자열은 한국어 리소스로 관리한다. 아이콘·제스처만으로 필수 기능을 숨기지 않는다.
- 문서와 코드에 현재 저장소 경로·Gradle 버전을 사용한다. 과거 CONTRACT.md의 다른 PC 경로와 역할 소유권을 이번 작업의 장벽으로 사용하지 않는다.

## Review Focus

1. 첫 입력을 연속 제출하거나 홈 재생성 시 빈 목록이 여러 개 생기지 않는지 — Task 2의 트랜잭션·ViewModel 테스트.
2. 선택한 목록 삭제·복구·재시작 후 고민 상태와 입력이 다른 목록으로 섞이지 않는지 — Task 2, 5.
3. 고민만 있거나 구매 완료만 있는 목록을 ‘완료’로 잘못 요약하지 않는지 — Task 1, 4.
4. 중복 이름·수량 상한·빠른 이동 후 취소가 다른 품목이나 새 수정을 덮어쓰지 않는지 — Task 1, 3.
5. 기존 데이터 마이그레이션과 계획 전용 항목의 id 충돌이 데이터 손실·화면 충돌을 만들지 않는지 — Task 1, 4.

## 파일 구조

아래 Kotlin 경로는 `app/src/main/java/com/minwoo/jangbogi/` 기준이다.

| 파일 | 역할 |
|---|---|
| domain/PurchaseIntent.kt | BUY / CONSIDER enum |
| domain/ShoppingSummary.kt | 구매/고민/완료 집계와 화면 요약 |
| domain/UiModels.kt | 고려 항목·개수·목록 상태 추가 |
| data/PurchaseIntentConverter.kt | Room TEXT 변환 |
| data/ShoppingSession.kt, ShoppingSessionDao.kt | 활성 목록 저장 |
| data/ShoppingItem.kt, *Dao.kt, JangbogiDatabase.kt | 의도 저장·쿼리·v3 마이그레이션 |
| data/JangbogiRepository.kt, ItemMutationResult.kt | 입력 결과·원자적 이동·정확한 실행 취소 |
| ui/viewmodel/ShoppingHomeViewModel.kt | 홈 시작, 목록 선택, 초안 관리 |
| ui/viewmodel/ListViewModel.kt | 선택 보기·입력·이동과 UI 이벤트 |
| ui/screens/ShoppingHomeScreen.kt | 새 시작점 |
| ui/screens/ListScreen.kt | 라우트 어댑터, 공유 콘텐츠 호출 |
| ui/components/ShoppingContent.kt | 홈/목록 상세 공용 상태 기반 콘텐츠 |
| ui/components/ListSwitcherSheet.kt | 목록 선택·신규 작성·관리 진입 |
| ui/components/PurchaseIntentSwitch.kt, ConsideringItemRow.kt | 보기 전환과 ‘살래요’ |
| ui/components/{ItemRow,QuickAddBar,EditItemSheet,ProgressHeader,SectionHeader}.kt | 주 동작·입력·편집과 새로운 시각 규칙 |
| ui/theme/{Color,Type,Theme}.kt | 라이트·다크 토큰 |

## Task 1: 구매 의도와 데이터 보존

**Files:** 위 domain/PurchaseIntent.kt, ShoppingSummary.kt, data/PurchaseIntentConverter.kt, ItemMutationResult.kt 생성. ShoppingItem.kt, ShoppingItemDao.kt, ShoppingListDao.kt, ItemHistoryDao.kt, JangbogiRepository.kt, JangbogiDatabase.kt, JangbogiApp.kt, domain/{ItemOrganizer,UiModels}.kt 수정. 테스트 설정은 app/build.gradle.kts와 gradle/libs.versions.toml에 필요한 최소 범위로 추가.

**Interfaces:**
- `enum class PurchaseIntent { BUY, CONSIDER }`
- `ShoppingItem.purchaseIntent: PurchaseIntent = PurchaseIntent.BUY`
- `data class ShoppingSummary(val remainingCount: Int, val completedCount: Int, val consideringCount: Int)`; `val purchaseCount: Int`, `val progress: Float?`; `fun summarize(items: List<ShoppingItem>): ShoppingSummary`
- `ItemOrganizer.considering(items: List<ShoppingItem>): List<ShoppingItem>`; 기존 sections/completed는 BUY만 반환.
- `suspend fun addItem(listId: Long, rawInput: String, intent: PurchaseIntent = BUY): AddItemResult`; 결과 `Added`, `Merged`, `Reopened`, `EmptyInput`, `QuantityLimit`, `OtherIntent(itemId, intent)`. 결과 성공 여부로만 입력을 지운다.
- `ListWithProgress`에 `consideringCount: Int = 0` 추가. 기존 totalCount는 BUY 전체 개수로 정의한다.

- [ ] `app/src/test/java/com/minwoo/jangbogi/ShoppingSummaryTest.kt` 및 ItemOrganizerTest에 실패 테스트를 추가한다: BUY 미완료 4, BUY 완료 2, CONSIDER 3 → `remainingCount=4`, `completedCount=2`, `consideringCount=3`, `purchaseCount=6`, `progress=2f/6f`; 고민만 존재 → `progress=null`.
- [ ] Android 테스트 설정과 `app/src/androidTest/java/com/minwoo/jangbogi/PurchaseIntentMigrationTest.kt`를 만든다. 기존 DDL의 v1/v2 DB를 SQLite로 작성하고 샘플 목록·체크·날짜·재고·독립 계획을 넣은 후 Room v3로 열어 필드 보존과 BUY 기본값을 확인한다. 기존 exportSchema=false 이력 때문에 없는 과거 schema JSON을 전제로 하지 않는다.
- [ ] 위 테스트를 실행해 기능 미구현으로 실패하는지 확인한 뒤 enum·변환기·ALTER 마이그레이션·분류·집계를 구현한다. exportSchema를 활성화하고 신규 스키마를 저장한다.
- [ ] DB 테스트 `PurchaseIntentRepositoryTest`를 추가하고 addItem을 구현한다. 같은 의도 수량 합산, 완료 재추가, 반대 의도 충돌 시 무변경, 99 초과 시 무변경, 다른 목록 같은 이름의 독립성을 검증한다.
- [ ] 추천·목록 집계 쿼리에 의도 필터를 반영한다. 추천의 반대 의도 일치 항목은 위치 정보를 제공한다. 기본 정보 편집의 이름 충돌도 저장 전에 검사한다.
- [ ] 기존 JVM 테스트와 새 DB 테스트를 실행한다. 기존 테스트의 기대값은 고민 항목이 없는 경우 유지한다.

## Task 2: 앱을 열자마자 입력하는 홈

**Files:** data/ShoppingSession.kt, ShoppingSessionDao.kt, ui/viewmodel/ShoppingHomeViewModel.kt, ui/screens/ShoppingHomeScreen.kt 생성. JangbogiDatabase.kt(v3에 함께 포함), JangbogiRepository.kt, JangbogiApp.kt, ui/navigation/AppNavHost.kt, ListViewModel.kt 수정.

**Interfaces:**
- `ShoppingSession(id: Int = 1, activeListId: Long?)`; shopping_lists FK, onDelete SET_NULL.
- `suspend fun resolveActiveListId(): Long?`: 저장된 유효 id → 최신 미완료 BUY 포함 목록 → 최신 목록 → null.
- `suspend fun selectList(listId: Long): Boolean`: 존재 확인 후 활성 id 저장.
- `suspend fun addToNewList(rawInput: String, intent: PurchaseIntent, defaultName: String): NewListResult`: 목록+첫 항목+활성 id를 한 트랜잭션으로 저장. `Created(listId)` 또는 `Rejected(reason)`.
- `ShoppingHomeUiState`: Loading / Draft(defaultName) / Active(listId) / Error(message).
- 첫 신규 사용자 기본 이름은 ‘내 장보기’, 사용자가 ‘새 목록’을 택한 경우 날짜형 기본 이름. 날짜는 기기 현지 날짜를 사용한다.

- [ ] `ShoppingSessionRepositoryTest`에 신규/기존/활성 id 삭제/완료 목록 선택/정렬 동률/원자적 생성 테스트를 추가한다.
- [ ] `ShoppingHomeViewModelTest`에 첫 입력 연속 제출 한 번 처리, 빈 입력 무저장, 저장 실패 입력 유지, 재생성 시 선택·초안 복원, 목록별 초안 분리 테스트를 추가한다.
- [ ] 테스트 실패를 확인한 뒤 session DAO·repository 해석·single-flight 저장·SavedStateHandle을 구현한다. Room 트랜잭션 밖에서 목록 생성 후 항목 저장을 따로 호출하지 않는다.
- [ ] AppNavHost의 startDestination을 새 장보기 홈으로 바꾼다. 기존 home 관리 화면은 lists 경로로 옮기고 기존 list/{listId}는 선택 id를 저장한 뒤 공용 콘텐츠로 연결한다.
- [ ] 목록 선택/새 초안/살림 계획/뒤로가기 경로를 연결한다. 새 초안 취소 시 기존 활성 목록을 유지한다. 목록 선택마다 NavBackStack을 누적하지 않는다.
- [ ] 새 설치에서는 시작 화면에 입력창이 있고 목록 생성 다이얼로그가 없는지 확인한다. 관련 단위·DB 테스트를 통과시킨다.

## Task 3: ‘살 것 / 고민 중’ 주 흐름과 새로운 화면

**Files:** ShoppingContent.kt, ListSwitcherSheet.kt, PurchaseIntentSwitch.kt, ConsideringItemRow.kt 생성. ListScreen.kt, ShoppingHomeScreen.kt, ListViewModel.kt, ItemRow.kt, CircleCheck.kt, QuickAddBar.kt, SuggestionChips.kt, ProgressHeader.kt, SectionHeader.kt, EmptyState.kt, theme 파일, strings.xml 수정.

**Interfaces:**
- `ListViewModel.selectIntent(intent: PurchaseIntent)`; `selectedIntent: StateFlow<PurchaseIntent>`; 새 목록 진입은 BUY.
- `suspend fun moveItem(itemId: Long, intent: PurchaseIntent): ItemMutationResult` → Applied(undoToken), NoChange, Missing.
- `suspend fun undoMutation(token: UndoToken): UndoResult` → Restored, Conflict, MissingParent. 토큰은 이전 스냅샷·적용 후 스냅샷을 담고 한 번만 소비한다. 현재 행이 적용 후 값과 다르면 최근 수정을 덮어쓰지 않는다.
- `ShoppingContent(state: ShoppingContentState, onAction: (ShoppingAction) -> Unit)`는 앱 컨테이너·navigation을 직접 참조하지 않는다. 액션은 Add, ChangeQuery, Check, Edit, Delete, Move, SelectIntent, ToggleCompleted, OpenLists로 정의한다.

- [ ] `ItemIntentMutationTest`에서 CONSIDER 직접 체크 무효, BUY→CONSIDER 체크 해제, metadata 보존, 동일 상태 이동 무효, 잘못된 부모 복원 방지, 연속 동작별 정확한 실행 취소를 검증한다.
- [ ] 실패 확인 후 저장·이동·삭제 성공 이벤트를 구현한다. 기존 단일 마지막-item 캐시에 의존하지 않고 각 스낵바가 자신의 토큰을 사용한다.
- [ ] 설계의 라이트·다크 색상, 30/18/16/13sp 글자 위계, 20dp 여백, 64dp 행, 48dp 터치 영역을 적용한다. 변경 대상에서 반복 이모지와 개별 행의 과한 카드 테두리를 줄인다.
- [ ] 메인 제목·작은 진행 요약·두 보기·카테고리 묶음·접힌 구매 완료를 구성한다. 비어 있음/고민만 있음/살 것은 완료됨 상태별 문구를 설계대로 구현한다.
- [ ] 체크 버튼, 이름 편집, 더보기를 독립된 접근성 동작으로 만든다. 고민 행에는 ‘살래요’를 표시하고 이동 후 현재 탭을 유지한다.
- [ ] 하단 입력의 대상 안내·‘담기/보관’·키보드 Done·연속 입력을 연결한다. 추천을 눌러도 현재 의도를 사용한다. Scaffold의 bottomBar와 한 곳의 IME 인셋 처리를 사용한다.
- [ ] `ShoppingContentTest` Compose 테스트: 고민 탭 입력은 CONSIDER, ‘살래요’ 후 살 것 개수 증가, 다른 목록 개수 불변, 완료 접기·펴기·취소, 이름 탭이 구매 상태를 바꾸지 않음.
- [ ] 목록 전환 시트에서 현재 목록, 각 목록의 남은/고민 수, 새 목록, 목록 관리에 접근되는지 확인한다.

## Task 4: 편집·목록 관리·살림 계획·공유 일관성

**Files:** EditItemSheet.kt, ListCard.kt, HomeScreen.kt, HomeViewModel.kt, PlanScreen.kt, JangbogiRepository.kt, domain/ShareTextBuilder.kt, strings.xml 수정. 필요한 경우 편집 폼만 ItemBasicsForm.kt와 PurchasePlanFields.kt로 분리.

**Interfaces:**
- 편집 초안은 이름·수량·카테고리와 기존 계획 필드를 유지한다. 접힌 영역의 값도 그대로 저장한다.
- `ShareTextBuilder.build(listName: String, items: List<ShoppingItem>): String` 기존 시그니처 유지; BUY만 공유.
- `PlannedShoppingItem.stableKey: String`: item:<id> 또는 plan:<id>. 목록별 의도는 ShoppingItem에만 둔다.

- [ ] ShareTextBuilderTest에 고민 항목이 본문·잔여·전체 수에서 제외되고 기존 BUY-only 출력이 유지되는 테스트를 추가한다.
- [ ] 계획 집계 테스트에 고민의 날짜가 확정 기한 개수에서 제외되고 재고·비축 정보는 보존되는지, 동일 숫자 id의 item/plan 키가 다른지 추가한다.
- [ ] 공유 필터와 계획 라벨·집계·키를 구현한다. 기본 공유 안내에 ‘고민 중 제외’를 노출한다.
- [ ] 편집을 기본 정보/‘구매 계획·재고’ 펼치기로 정리한다. 카테고리는 현재 선택값부터 보여주고 선택 시 전체 후보를 연다. 기본 저장 동작은 화면 하단에 접근 가능하게 둔다.
- [ ] HomeScreen(목록 관리)·PlanScreen도 새 색상·글자·여백을 적용한다. 살림 계획에서 ‘미정’ 정보 여러 줄을 상시 반복하지 않고 값이 있는 정보 위주로 요약한다. 편집에서 모든 필드에 접근 가능하게 한다.
- [ ] Compose 테스트로 접기/펼치기 전후 날짜·재고 값 보존, 기본 정보 저장, 고민 상태 라벨, 삭제 안내의 세 상태 포함을 검증한다.

## Task 5: 마이그레이션·실사용 흐름 검증과 문서 반영

**Files:** app/src/androidTest/java/com/minwoo/jangbogi/ShoppingJourneyTest.kt 생성. README.md, design/DESIGN.md, CONTRACT.md 갱신. 검증 스크린샷은 design/qa/에 구별되는 새 파일명으로 저장.

- [ ] JVM 테스트: `.\gradlew.bat :app:testDebugUnitTest` → 실패 0. 기존 수량 파싱·분류·정렬·공유 테스트를 포함한다.
- [ ] 빌드·lint: `.\gradlew.bat :app:assembleDebug :app:lintDebug` → 빌드 성공 및 새 관련 오류 없음. 기존 설정이 lint abortOnError=false이므로 exit code만으로 lint 통과를 주장하지 말고 보고서를 읽는다.
- [ ] 연결 기기/에뮬레이터에서 `.\gradlew.bat :app:connectedDebugAndroidTest` → migration/repository/Compose 테스트 실패 0. 기기가 없으면 미실행으로 기록하고 통과로 보고하지 않는다.
- [ ] 새 설치: 실행 → 물건 입력 → 고민 보관 → 살래요 → 체크 → 취소 → 종료/재시작 흐름을 확인한다.
- [ ] 기존 데이터: 여러 목록·미완료·완료·계획 전용 항목·날짜·재고가 있는 v1/v2 테스트 DB 업데이트 후 값과 화면을 확인한다. 실제 사용자 DB를 초기화해서 검증하지 않는다.
- [ ] 활성 목록 삭제→대체 목록, 삭제 취소→복원, 마지막 목록 삭제→빈 입력 화면, 새 초안 취소→원래 목록을 확인한다.
- [ ] 320/360dp 소형 화면, 큰 화면, 키보드 표시, 글자 200%, 라이트/다크, 긴 한글 이름, TalkBack을 점검한다. 색상에 의존하지 않는 상태와 터치 영역을 확인한다.
- [ ] 새 UX 설명·스크린샷·검증 결과와 미실행 항목을 문서에 기록한다. git diff를 검토해 의도하지 않은 데이터/빌드 산출물 변경이 없는지 확인한다.
- [ ] 완료 결과를 사용자에게 제시한다. 커밋을 원하면 변경 요약과 검증 결과를 제시한 뒤 명시적 허락을 받는다.

## 계획 자체 검토

- [x] 첫 입력/목록 전환/고민/완료/수정/공유/계획/디자인 요구에 대응하는 작업이 있다.
- [x] 새 데이터와 기존 데이터, 오류·중복·취소·화면 크기까지 검증에 포함했다.
- [x] 커밋 허락 규칙과 main 작업 조건을 보존했다.
- [x] 실행 시 결정해야 할 불명확한 대규모 라이브러리 교체·추가 기능을 제외했다.
- [ ] 사용자 시안·설계·계획 검토 후 실행 방식 확정. 추천은 이 세션에서 순서대로 직접 구현하는 방식이다. 데이터·홈·화면이 서로 연결되어 있으므로 인터페이스를 단계별로 맞추기 쉽다.
