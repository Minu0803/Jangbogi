# 장보기 UI 디자인 확정 스펙 (DESIGN.md)

- 작성: UI Designer · 2026-07-11
- 근거: CONTRACT.md 6·7·8·9절. 이 문서의 수치가 **확정값**이다 (9절 기준 메트릭에서 확정한 값 포함).
- 시각 레퍼런스: `design/mockup.html` (홈+상세 × 라이트/다크, 390×844, 1dp=1px)
- 코드: `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` (완성, 그대로 사용)

## 1. 디자인 컨셉 — "청과 코너의 신선함"

| 목표 | 반영 |
|---|---|
| 이쁘다 | 딥 프레시 그린 + 감귤 웜 액센트, 그린 틴트 뉴트럴, 순백/딥 카드, radius 16~28 소프트 지오메트리 |
| 간편하다 | 하단 고정 빠른 추가 바(엄지 존), 행 전체 탭 = 체크, 추천 칩 1탭 추가 |
| 가시성 최우선 | 항목명 18sp SemiBold, 진행 헤더 24sp Bold, 본문 대비 6:1~16:1, 28dp 체크 + 48dp 터치 박스 |

카테고리 이모지가 유일한 "컬러 노이즈"이므로 서피스는 채도를 눌렀고(그린 틴트 그레이), 포인트 컬러는 프라이머리 그린과 터셔리 오렌지 두 개로 제한한다.

## 2. 컬러 팔레트

### 2.1 코어 브랜드 색

| 역할 | 라이트 | 다크 |
|---|---|---|
| 프라이머리(그린) | `#0F6D40` | `#86D9A9` |
| 배경 | `#F6FBF4` | `#0F1511` |
| 액센트(감귤, tertiary) | `#8F4C00` | `#FFB877` |

### 2.2 전체 슬롯 (Color.kt와 1:1 동일)

| ColorScheme 슬롯 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| primary | `#0F6D40` | `#86D9A9` | FAB·추가 버튼·체크 채움·진행바 fill·강조 숫자 |
| onPrimary | `#FFFFFF` | `#00391F` | primary 위 텍스트/아이콘 |
| primaryContainer | `#A8F5C8` | `#00522F` | 선택 상태 컨테이너(카테고리 칩 선택 등) |
| onPrimaryContainer | `#00210F` | `#A8F5C8` | primaryContainer 위 콘텐츠 |
| inversePrimary | `#86D9A9` | `#0F6D40` | 스낵바 "실행 취소" 액션 텍스트 |
| secondary | `#4E6355` | `#B5CCBB` | 보조 강조(거의 미사용, 시스템용) |
| onSecondary | `#FFFFFF` | `#213529` | |
| secondaryContainer | `#D0E8D8` | `#374B3E` | 추천 칩 배경·수량 배지 배경 |
| onSecondaryContainer | `#0A1F12` | `#D0E8D8` | 칩/배지 텍스트 |
| tertiary | `#8F4C00` | `#FFB877` | "완료 🎉" 라벨, 축하 무드 강조 |
| onTertiary | `#FFFFFF` | `#4B2800` | |
| tertiaryContainer | `#FFDCC2` | `#6B3B00` | (예비) 축하 배너 등 |
| onTertiaryContainer | `#2E1500` | `#FFDCC2` | |
| background / surface | `#F6FBF4` | `#0F1511` | 화면 배경, 앱바 기본 |
| onBackground / onSurface | `#171D18` | `#DFE5DD` | 본문 텍스트 |
| surfaceVariant | `#DDE5DC` | `#414942` | 진행바 트랙 |
| onSurfaceVariant | `#414942` | `#C1C9C0` | 보조 텍스트(날짜·섹션 헤더·placeholder) |
| surfaceTint | `#0F6D40` | `#86D9A9` | 엘리베이션 틴트 |
| inverseSurface | `#2C322D` | `#DFE5DD` | 스낵바 배경 |
| inverseOnSurface | `#EDF2EB` | `#2C322D` | 스낵바 텍스트 |
| error | `#BA1A1A` | `#FFB4AB` | 삭제 확인 버튼 텍스트 등 |
| onError | `#FFFFFF` | `#690005` | |
| errorContainer | `#FFDAD6` | `#93000A` | 스와이프 삭제 배경 |
| onErrorContainer | `#410002` | `#FFDAD6` | 스와이프 Delete 아이콘 |
| outline | `#717971` | `#8B938A` | 미체크 원형 체크 테두리 |
| outlineVariant | `#C1C9C0` | `#414942` | 카드 헤어라인 보더·디바이더 |
| scrim | `#000000` | `#000000` | 바텀시트/다이얼로그 스크림 |
| surfaceBright | `#F6FBF4` | `#353B36` | |
| surfaceDim | `#D6DCD5` | `#0F1511` | |
| surfaceContainerLowest | `#FFFFFF` | `#0A0F0B` | (라이트) 카드·행 배경 |
| surfaceContainerLow | `#F0F5EE` | `#171D18` | 바텀시트 기본 배경 |
| surfaceContainer | `#EAF0E8` | `#1B211C` | 앱바 스크롤 시 배경, 메뉴 |
| surfaceContainerHigh | `#E4EBE3` | `#262C26` | (다크) 카드·행 배경, 섹션 개수 배지 |
| surfaceContainerHighest | `#DFE5DD` | `#303631` | 최상위 컨테이너(예비) |

### 2.3 `surfaceCard` 별칭 (Color.kt에 구현됨 — 프론트는 이것만 쓰면 됨)

```kotlin
MaterialTheme.colorScheme.surfaceCard
// 라이트 = surfaceContainerLowest(#FFFFFF), 다크 = surfaceContainerHigh(#262C26)
```
적용 대상: **홈 목록 카드, 항목 행, 빠른 추가 바 필**. 라이트는 순백 카드로 배경과 분리, 다크는 밝은 톤으로 띄운다.

### 2.4 대비 검증 (WCAG, 계산값)

| 조합 | 라이트 | 다크 | 판정 |
|---|---|---|---|
| 본문 onSurface / background | 16.3:1 | 14.4:1 | AAA |
| 보조 onSurfaceVariant / background | 8.9:1 | 10.9:1 | AAA |
| primary 텍스트 / background | 6.1:1 | 11.0:1 | AA+ (숫자 강조·링크급) |
| onPrimary / primary (버튼) | 6.4:1 | 7.8:1 | AA+ |
| tertiary "완료 🎉" / 카드 배경 | 6.6:1 | 8.4:1 | AA+ |
| 완료 항목명(0.60 알파 합성) / 행 배경 | ≈4.5:1 | ≈4.6:1 | AA (의도적 감쇠 + 취소선·체크 아이콘 병행) |

## 3. 타이포그래피 (Type.kt와 1:1, FontFamily.Default, letterSpacing 0)

| 슬롯 | 크기/행간/굵기 | 용도 |
|---|---|---|
| displayLarge | 44/52 Bold | (예비) |
| displayMedium | 38/46 Bold | (예비) |
| displaySmall | 32/40 Bold | (예비) |
| headlineLarge | 30/38 Bold | (예비) |
| headlineMedium | 28/36 Bold | 홈 LargeTopAppBar 확장 타이틀 "장보기" (M3 기본 슬롯) |
| headlineSmall | 24/32 Bold | **진행 헤더 "n개 남았어요"** (9절 22~24 중 24 확정) |
| titleLarge | 22/28 Bold | TopAppBar 타이틀(상세 리스트 이름·홈 축소 타이틀), 다이얼로그 제목 |
| titleMedium | 18/24 SemiBold | **홈 카드 이름·항목 행 이름** (9절 17~18 중 18 확정) |
| titleSmall | 15/20 SemiBold | 바텀시트 필드 라벨 |
| bodyLarge | 17/26 Regular | 입력 텍스트·다이얼로그 본문·빈 상태 안내 |
| bodyMedium | 15/22 Regular | 카드 보조 라벨("4개 남음") |
| bodySmall | 13/18 Regular | 캡션 |
| labelLarge | 14/20 SemiBold | 버튼·칩 텍스트·`{checked}/{total}`·섹션 헤더 라벨 |
| labelMedium | 13/18 Medium | 상대 날짜·섹션 개수 배지 |
| labelSmall | 12/16 Medium | 최소 캡션(예비) |

빈 상태 이모지(🛒)는 텍스트 64sp로 별도 지정.

## 4. 공통 레이아웃 상수

- 화면 좌우 패딩 **16dp** / 홈 카드 간격 **12dp** / 항목 행 간격 **8dp**
- 터치 타깃 최소 **48dp**. 아이콘 버튼 48dp(아이콘 24dp).
- 진행바 공통: 트랙 surfaceVariant, fill primary, **StrokeCap.Round**, 완전 라운드.
- 리스트 하단 contentPadding: 빠른 추가 바에 가리지 않게 **150dp** 확보(칩 유무와 무관하게 고정).

## 5. 컴포넌트 스펙

### 5.1 홈 목록 카드
- 컨테이너: `surfaceCard`, radius **20dp**, 패딩 **18dp(상하 16dp)**. 라이트: 1dp 보더 `outlineVariant@55%` + 그림자(elevation 1~2dp 상당). 다크: 보더 없음, elevation 0(색으로 분리).
- 1행: 이름 titleMedium·onSurface(1줄 ellipsis) — 우측 상대 날짜 labelMedium·onSurfaceVariant.
- 2행(위 10dp): 진행바 높이 **8dp**(flex) + 우측 `{checked}/{total}` labelLarge·primary(tabular-nums, 최소폭 34dp 우측정렬).
- 3행(위 8dp): 보조 라벨 bodyMedium·onSurfaceVariant. 전부 체크 → "완료 🎉" bodyMedium **Bold·tertiary**. 비었으면 "비어 있음" bodyMedium·outline, 진행바 fill 0.
- 상태: pressed = 기본 리플. 롱프레스 → DropdownMenu(containerColor surfaceContainer).

### 5.2 항목 행 (미체크)
- 컨테이너: `surfaceCard`, radius **16dp**, minHeight **56dp**, 패딩 좌14 우16 상하8. 라이트 보더/그림자 = 홈 카드와 동일 규칙.
- 좌: 원형 체크(5.3) → 간격 14dp → 이름 titleMedium·onSurface(flex, 2줄까지) → qty>1이면 수량 배지.
- 수량 배지: `×n` labelMedium Bold, secondaryContainer 배경/onSecondaryContainer 텍스트, radius 완전, 패딩 가로 10dp·세로 3dp.
- 행 전체 탭 = 토글(+햅틱). 리플은 행 radius로 클립.

### 5.3 원형 체크
- 시각 **28dp**, 터치 박스 48dp(행 탭과 동일 동작이므로 시각 요소만 배치해도 무방).
- 미체크: 배경 투명, 보더 **2.5dp** `outline`.
- 체크: 배경 `primary`, 보더 없음, 중앙 Check 아이콘 **18dp** `onPrimary`.
- 전환: 스케일 0.9→1.0 + 색 크로스페이드 150ms(가능하면).

### 5.4 완료(체크됨) 항목 행
- 5.2와 동일 구조, 행 전체 **알파 0.60**, 그림자 제거, 이름 취소선 + FontWeight.Medium으로 감쇠, 체크는 채움 상태.
- 탭 = 미체크 복귀.

### 5.5 섹션 헤더 (stickyHeader)
- 높이: 콘텐츠 20dp + 패딩 **위 18dp/아래 8dp** (9절 확정). 배경 = background(불투명 — 스크롤 시 행이 비치지 않게).
- 구성: 이모지 16sp → 6dp → 카테고리명 labelLarge·onSurfaceVariant → 6dp → 개수 배지(labelSmall SemiBold·onSurfaceVariant, surfaceContainerHigh 배경, radius 완전, 패딩 8dp×1dp).
- 완료 섹션 헤더: "완료 {n}" 동일 스타일. 위에 1dp `outlineVariant` 디바이더(위 여백 10dp)를 추가해 구역 전환 표시.

### 5.6 진행 헤더 (상세 상단, total>0일 때)
- 패딩: 좌우 20dp, 위 8dp, 아래 14dp.
- 1행: "n개 남았어요" headlineSmall·onBackground — 우측 `{checked}/{total} 완료` labelLarge·primary (baseline 정렬).
- 전부 완료 시: "장보기 완료! 🎉" headlineSmall, 색 tertiary.
- 2행(위 10dp): 진행바 높이 **10dp**, `animateFloatAsState`(300ms, FastOutSlowIn)로 fill 애니메이션.

### 5.7 빠른 추가 바 (하단 고정)
- 컨테이너 영역: 좌우 16dp, 위 10dp, 아래 14dp(+`imePadding().navigationBarsPadding()`), 배경 = background로 위쪽 페이드(그라데이션 background 78%→투명) 또는 불투명 background — 리스트가 밑으로 지나감.
- 필 TextField: 높이 **56dp**, radius **28dp**, 배경 `surfaceCard`, 보더 **1.5dp** `outlineVariant`, 좌우 패딩 20dp. 텍스트 bodyLarge·onSurface, placeholder "무엇을 살까요? (예: 우유 2)" bodyLarge·onSurfaceVariant. 포커스 시 보더 primary 2dp.
- 우측 추가 버튼: **48dp** 원형, `primary` 배경, Add 아이콘 24dp `onPrimary`, 필과 간격 10dp. 비활성(입력 공백) 시에도 색 유지(탭 무시)— 시각 안정 우선.

### 5.8 추천 칩 (빠른 추가 바 위)
- 가로 스크롤 Row(LazyRow), 좌우 16dp, 칩 간격 8dp, 아래 10dp.
- 칩: 높이 **38dp**(터치는 상하 여백 포함 48dp 확보), radius 완전, `secondaryContainer` 배경, 텍스트 `{emoji} {name}` labelLarge·onSecondaryContainer, 좌우 패딩 14dp. 보더 없음.

### 5.9 Extended FAB "새 목록" (홈)
- `primary` 배경 / `onPrimary` 콘텐츠 (기본 primaryContainer 대신 — 가시성 우선 확정).
- 높이 56dp, radius **16dp**, 패딩 좌우 20dp, Add 아이콘 24dp + 8dp + labelLarge "새 목록"(15sp급이면 titleSmall도 허용), elevation 3.
- 위치: 우 20dp·하 32dp(내비 인셋 위).

### 5.10 스와이프 삭제 배경 (EndToStart)
- 배경: `errorContainer`, 행과 동일 radius 16dp. 우측 정렬 Delete 아이콘 24dp `onErrorContainer`, 우 패딩 18dp.
- 스와이프 진행에 따라 아이콘 스케일 0.8→1.0.

### 5.11 편집 바텀시트 (ModalBottomSheet)
- containerColor `surfaceContainerLow`, 상단 radius **28dp**, 드래그 핸들 기본, 콘텐츠 패딩 좌우 20dp·하단 24dp(+navigationBarsPadding).
- 제목 "항목 수정" titleLarge, 아래 16dp.
- 이름 TextField: OutlinedTextField 높이 56dp, radius 14dp, 라벨 "이름" titleSmall·onSurfaceVariant, 포커스 보더 primary.
- 수량 스테퍼(위 16dp): [−] 44dp 원형 톤 버튼(surfaceContainerHigh 배경/onSurface 아이콘) · 수량 titleLarge·tabular(최소폭 48dp 중앙) · [+] 동일. 범위 1~99, 한계값에서 해당 버튼 알파 0.38.
- 카테고리 칩 FlowRow(위 20dp, 간격 8dp): 칩 높이 40dp radius 완전. 미선택 = surfaceContainerHigh 배경·onSurfaceVariant 텍스트. 선택 = **primaryContainer 배경·onPrimaryContainer 텍스트 + 1.5dp primary 보더**. 텍스트 `{emoji} {display}` labelLarge.
- 하단 버튼 행(위 24dp): "삭제" TextButton(error 텍스트) 좌 — "저장" FilledButton(primary/onPrimary, 높이 52dp, radius 완전, flex) 우.

### 5.12 TopAppBar
- 홈: `LargeTopAppBar` — 확장 타이틀 headlineMedium, 축소 titleLarge. containerColor background, scrolledContainerColor `surfaceContainer`.
- 상세: `TopAppBar` — 타이틀 titleLarge(탭 → 이름 변경 다이얼로그), 뒤로/공유/MoreVert 아이콘 onSurface. 같은 container 규칙.

### 5.13 스낵바·다이얼로그·메뉴
- 스낵바: inverseSurface/inverseOnSurface, 액션 "실행 취소" = inversePrimary, radius 12dp, 빠른 추가 바 위에 뜨도록 host를 콘텐츠 하단에 배치.
- AlertDialog: containerColor surfaceContainerHigh, radius 28dp, 제목 titleLarge, 본문 bodyLarge, 확인(파괴적) 텍스트 error, 취소 primary.
- DropdownMenu: surfaceContainer, radius 12dp, 항목 bodyLarge, "삭제" 항목만 error 텍스트.

### 5.14 빈 상태
- 홈: 🛒 64sp → 16dp → "아직 목록이 없어요" titleLarge·onSurface → 6dp → "새 목록을 만들어 장보기를 시작하세요" bodyLarge·onSurfaceVariant. 수직 중앙(위로 8% 올림).
- 상세: "장바구니가 비어 있어요" titleLarge → "아래 입력창에 살 것을 적어보세요 👇" bodyLarge·onSurfaceVariant.

## 6. 스플래시 (통합 담당이 themes.xml에 반영)

| 모드 | `windowSplashScreenBackground` |
|---|---|
| 라이트 (values) | `#F6FBF4` |
| 다크 (values-night) | `#0F1511` |

아이콘은 `@mipmap/ic_launcher` 그대로. 배경을 앱 background와 동일하게 해 스플래시→홈 전환이 끊기지 않게 한다(아이콘 자체가 그린 그라데이션 배경을 가지므로 스플래시 배경은 뉴트럴 유지).

## 7. 앱 아이콘

- **모티프**: 흰 장보기 가방(굵은 반원 핸들 + 라운드 바디) 안에 체크마크가 **컷아웃(구멍)** 으로 뚫려 배경 그린이 비침 — "장 볼 것을 체크한다"는 앱 본질을 1형상으로 표현.
- 전경 `ic_launcher_foreground.xml`: 108dp viewport, 전 요소 중앙 66dp safe zone 내(최외곽점 반경 ≤33dp). 순백 2패스(핸들 / 바디+체크 evenOdd). monochrome 레이어로 재사용(구멍이 유지되어 테마 아이콘에서도 형상 보존).
- 배경 `ic_launcher_background.xml`: 대각 선형 그라데이션 `#23945A → #0A5330`(좌상→우하).
- `mipmap-anydpi-v26/ic_launcher.xml`·`ic_launcher_round.xml`: background/foreground/monochrome 3레이어 adaptive-icon.

## 8. 모션 가이드 (요약)

- 체크 토글: 색/스케일 150ms. 행 이동: `animateItemPlacement()` 기본 스프링.
- 진행바: 300ms FastOutSlowIn. 스낵바·시트: M3 기본.
- 과한 모션 금지 — 마트에서 걸으며 쓰는 앱, 정보 안정성이 우선.
