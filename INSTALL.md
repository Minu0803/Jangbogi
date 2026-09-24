# 장보기 앱 설치 방법

APK 파일: `C:\Users\minwoo\Documents\Jangbogi\장보기_v1.0.0.apk` (약 6MB)

## 폰에 설치하기
1. APK 파일을 폰으로 전송 — 카카오톡 "나와의 채팅"에 파일 첨부가 가장 간편 (또는 USB, Google Drive)
2. 폰에서 파일을 탭 → 설치
   - "출처를 알 수 없는 앱 설치" 허용을 물으면, 해당 앱(카카오톡/내 파일 등)에 대해 **허용**
   - Play Protect 경고("알 수 없는 개발자")가 뜨면 **자세히 → 무시하고 설치** — 스토어 미등록 자체 서명 앱이라 뜨는 정상 경고
3. 설치 후 홈 화면에서 "장보기" 실행

## 요구사항
- Android 8.0 (API 26) 이상 — 2017년 이후 기기 전부 해당
- 인터넷 불필요 (완전 오프라인, 권한 요구 없음)

## 업데이트
- 이 PC(MINWOO)의 동일 키로 서명하므로, 이후 버전 APK를 같은 방법으로 덮어 설치하면 데이터 유지된 채 업데이트됨
- 다른 PC에서 빌드한 APK는 키가 달라 설치 거부됨 → 이 프로젝트 폴더를 통째로 옮기면 키(`%USERPROFILE%\.android\debug.keystore`)도 함께 옮겨야 함

## 재빌드 방법 (참고)
```powershell
$env:JAVA_HOME = 'C:\Users\minwoo\Documents\Jangbogi\tools\jdk17'
& 'C:\Users\minwoo\Documents\Jangbogi\tools\gradle-8.9\bin\gradle.bat' -p 'C:\Users\minwoo\Documents\Jangbogi' :app:assembleRelease
# 산출물: app\build\outputs\apk\release\app-release.apk
```
