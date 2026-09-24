# 장보기 앱 제작 진행 상태

시작: 2026-07-11 / 목표: 사이드로드 설치용 서명 APK (Kotlin + Jetpack Compose + Room, 화면 2개)
루트: `C:\Users\minwoo\Documents\Jangbogi` (회사 레포와 무관, git 미사용)

## 단계
- [x] P0 환경 점검 — JDK/SDK/Gradle 전무 → 전부 `tools\`에 자체 설치. 디스크 672GB, 네트워크 OK
- [x] P1 스캐폴드(gradle 파일·매니페스트·플레이스홀더) + CONTRACT.md
- [x] P2 에이전트 병렬 작업 완료 — 디자이너(테마+아이콘+DESIGN.md+목업 9파일, 팔레트 L#0F6D40/D#86D9A9), 프론트(화면2+컴포넌트11+내비), 백엔드(Room+도메인+VM+테스트, 키워드 262개). 계약 이탈 0
- [x] P3 툴체인 설치 완료 (tools\jdk17, tools\gradle-8.9, tools\android-sdk) + 스텁 웜업 빌드 통과
- [x] P4 통합 빌드 **한 번에 통과** — testReleaseUnitTest 31개 전부 성공(파서14/분류7/정렬6/공유4), assembleRelease OK. 스플래시 색만 디자이너 확정값(#F6FBF4/#0F1511)으로 반영
- [x] P5 APK 산출: `장보기_v1.0.0.apk` (6MB, debug keystore 서명·minify off) + INSTALL.md. 목업 스크린샷 검수 OK(design\mockup_check.png)
- [x] P6 에뮬레이터 실설치 QA **완료** — Pixel 7 AVD(qa34, API 34, WHPX) 헤드리스 부팅, 배포본 APK 그대로 설치·구동. 검증: 앱 부팅/스플래시, 목록 생성(한글 기본명), "milk 2" 수량 파싱·자동분류(기타), 행 탭 체크→완료 섹션·취소선·"장보기 완료!🎉", 추천 칩(히스토리 학습), 다크모드, IME 위 입력바. 스크린샷 11장 design\qa\
- [x] P6.5 통합 디자인 보정 — DESIGN.md 스펙 반영(surfaceCard 순백 카드+헤어라인, 항목명 titleMedium 18sp, FAB primary, 카운트 primary/완료 tertiary, 추천 칩 secondaryContainer, 빠른추가바 보더+포커스) + 완료 행 반투명이 스와이프 errorContainer 배경 비침 버그 수정(compositeOver+평상시 알파0)
- [x] P7 Minu 볼트 노트 + 최종 보고

## 완료 (2026-07-11)
최종 산출물: `C:\Users\minwoo\Documents\Jangbogi\장보기_v1.0.0.apk` (6MB, 유닛테스트 31/31, 에뮬레이터 E2E 통과)
잔여: 실기기(폰) 설치 확인만 사용자 몫. 다음 버전 아이디어: 홈 위젯, R8 축소, 목록 복제/재사용.

## ⚠ 빌드 함정 기록
- mergeDexRelease "다른 프로세스가 사용 중" 잠금: 그레이들 데몬(+leaked worker)이 classes.dex 핸들 보유 → `gradle --stop` + java 전부 종료 + `app\build\intermediates\dex` 삭제 후 재빌드로 해결. OneDrive는 무관(Documents 미리다이렉트 확인).
- 에뮬레이터 QA 재사용: AVD `qa34` 존재. `emulator -avd qa34 -no-window ...` 부팅 → adb install. adb input text는 ASCII만(한글 입력 불가).

## 핵심 결정
- 스택: Kotlin 2.0.20 / AGP 8.5.2 / compileSdk 34 / Compose BOM 2024.06.00 / Room 2.6.1(KSP) — 검증된 버전 매트릭스로 고정
- 서명: 이 PC의 debug keystore로 release 서명(개인 사이드로드, 동일 키 업데이트 가능). minify off(무기기 검증 리스크 회피)
- 계약 문서(CONTRACT.md)가 단일 원천 — VM 시그니처/도메인 모델/UX/포맷 전부 고정, 에이전트는 소유 파일만 작성

## 재개 프롬프트 (중단 시 다른 채팅에 복붙)
> `C:\Users\minwoo\Documents\Jangbogi\PROGRESS.md`와 `CONTRACT.md`를 읽고 남은 단계를 이어서 진행해줘.
> 빌드는 `$env:JAVA_HOME='C:\Users\minwoo\Documents\Jangbogi\tools\jdk17'` 설정 후
> `& 'C:\Users\minwoo\Documents\Jangbogi\tools\gradle-8.9\bin\gradle.bat' -p 'C:\Users\minwoo\Documents\Jangbogi' :app:assembleRelease` 로.
> 완료 기준: 유닛테스트 통과 + 서명 APK(`장보기_v1.0.0.apk`) + INSTALL.md + Minu 볼트 노트 + 최종 보고.
