# (보관용) 나중에 AdMob 배너를 붙일 때 필요한 작업 목록

> 현재 방침: **당분간 무료, 광고 없음.** 학습 카드 하단에 '광고 자리'(50dp)만
> 코드로 예약해 두었고 기본은 접힘(높이 0). `KoreanKeyboardView`의
> `AD_PLACEHOLDER_ENABLED = false`. **광고 SDK·인터넷 권한은 넣지 않은 상태.**
> 아래는 실제로 배너를 붙이기로 결정했을 때의 체크리스트다(그 전까지 착수 금지).

## A. 권한·네트워크 (핵심 제약 변경 — 먼저 결정 필요)
1. `AndroidManifest.xml`에 `INTERNET`(필요 시 `ACCESS_NETWORK_STATE`) 권한 추가.
   → **"인터넷 권한 없음"이라는 이 앱의 핵심 원칙이 깨진다.** 광고는 본질적으로
   네트워크가 필요하므로 이 결정을 가장 먼저 승인해야 함.

## B. SDK·빌드
2. `build.gradle`에 `play-services-ads` 의존성 추가.
3. `AndroidManifest.xml`에 AdMob **앱 ID** 메타데이터
   (`com.google.android.gms.ads.APPLICATION_ID`) 등록.
4. 광고 로딩 코드: 예약해 둔 `adSlot` 영역에 `AdView`(배너)를 넣고 `loadAd()`.
   IME(키보드)는 액티비티와 생명주기가 달라 `onCreateInputView`/카드 열림 시점에
   로드·해제 처리 필요. 테스트는 반드시 **테스트 광고 ID**로.

## C. 계정·정책·심사
5. **AdMob 계정** 생성 → 앱 등록 → 배너 광고 단위 생성해 단위 ID 발급.
6. **개인정보처리방침 갱신**: 광고 SDK가 광고 ID(AAID)·기기정보를 수집하므로,
   ㉠방침 문서에 데이터 수집·제3자(Google) 제공 명시, ㉡Play Console
   **데이터 안전(Data safety)** 설문 갱신, ㉢앱 내 설정 '정보'에 방침 링크 노출.
7. **동의 관리(UMP)**: Google User Messaging Platform SDK로 맞춤형 광고 동의
   팝업 구현(특히 EU/UK). 만 14세 미만 대상이 아님을 확인.
8. **Play Console 재심사**: 권한·SDK·데이터 안전 변경으로 새 버전 심사 필요.
   키보드(IME)는 입력 데이터 취급 심사가 민감 → "광고에 입력 내용은 사용하지
   않음"을 방침에 명확히.

## 참고
- 지금 커밋 상태는 위 A~C를 **하나도 건드리지 않고** UI 자리(50dp)만 예약.
- 실제 적용은 **A(인터넷 권한)** 승인 이후 착수 권장.
