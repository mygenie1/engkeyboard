# krdict 변환 스크립트

국립국어원 「한국어기초사전」(krdict) 공개 데이터를 우리 학습 사전 2군 CSV로 바꿉니다.

> ⚠️ 이 변환은 **개발 PC에서 한 번만** 돌리는 전처리입니다. 결과 CSV만 앱에 담기며,
> **앱 자체에는 인터넷 권한이 없습니다.** (데이터 내려받기는 개발 단계에서만 합니다.)

## 1) 원본 데이터 내려받기 (LMF XML)

두 가지 방법 중 하나:

- **공식:** https://krdict.korean.go.kr/ 하단 **‘사전 내려받기 → 전체 내려받기’** 에서
  LMF XML ZIP을 받습니다.
- **미러(간편):** https://github.com/spellcheck-ko/korean-dict-nikl-krdict
  의 `5000.xml … 51947.xml` (약 5,000개 단위, 총 ~340MB)

받은 XML 파일들을 한 폴더에 모읍니다. 예: `./xml/`

## 2) 변환 실행

```bash
node convert.js <xml_input_dir> <group1_csv_dir> <output_dir>

# 예시 (이 저장소 기준)
node convert.js ./xml ../../app/src/main/assets/dictionary ./out
```

- `group1_csv_dir` 의 기존 1군 표제어(632개)와 **겹치는 단어는 자동 제외**됩니다(1군 우선).
- 조사·어미·접사, 영어 대응어 없는 항목, 공백/붙임표 포함 표제어는 걸러집니다.
- 결과는 `part_04.csv` 부터 5,000줄씩 나뉘어 저장됩니다. (1군이 `part_01~03`)

## 3) 앱에 반영

`out/part_*.csv` 를 `app/src/main/assets/dictionary/` 로 복사하고,
`DictionaryDb.DB_VERSION` 을 1 올린 뒤 커밋하면 됩니다. (기존 설치본도 다음 실행 때 갱신)

## 출력 형식 (우리 CSV 5칸)

```
"한글","영어","발음기호(빈칸)","뜻(국어정의)","예문(빈칸)"
```

2군은 발음기호·예문이 없으므로 빈 칸이며, 학습 카드가 이를 자동으로 숨깁니다.

## 라이선스

원본은 **CC BY-SA 2.0 KR** 입니다. 변환 결과물(2군)도 동일 라이선스로 배포됩니다.
자세한 내용은 저장소 루트의 [`DICTIONARY_LICENSE.md`](../../DICTIONARY_LICENSE.md) 참고.
