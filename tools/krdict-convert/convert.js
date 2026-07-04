#!/usr/bin/env node
/*
 * 국립국어원 「한국어기초사전」(krdict) 공개 데이터를
 * 우리 학습 사전(2군) CSV 형식으로 변환하는 스크립트입니다.
 *
 * 데이터 출처: https://krdict.korean.go.kr/ (전체 내려받기 → LMF XML)
 *   미러: https://github.com/spellcheck-ko/korean-dict-nikl-krdict (5000.xml … 51947.xml)
 * 라이선스: CC BY-SA 2.0 KR (출처 표시 + 동일조건변경허락)
 *
 * 하는 일
 *  1) 내려받은 LMF XML(LexicalResource) 들을 훑어 표제어마다
 *     '첫 번째 영어 대응어가 있는 뜻풀이'를 골라냅니다.
 *  2) 우리 CSV 5칸 형식으로 변환합니다:
 *        "한글","영어","발음기호(빈칸)","뜻(국어정의)","예문(빈칸)"
 *     → 2군은 '영어 단어 + 뜻'만 있는 간이 카드용이므로 발음기호·예문은 비웁니다.
 *  3) 1군(직접 만든 632개)과 겹치는 표제어는 제외합니다(1군 우선).
 *  4) 5,000줄씩 part_XX.csv 로 잘라 assets 폴더에 씁니다.
 *
 * 사용법:
 *   node convert.js <xml_input_dir> <group1_csv_dir> <output_dir>
 * 예:
 *   node convert.js ./xml ../../app/src/main/assets/dictionary ./out
 *
 * 인터넷 권한과는 무관합니다 — 이 변환은 '개발 PC에서 한 번' 돌리는 전처리이고,
 * 결과 CSV만 앱에 담깁니다. 앱 자체는 네트워크를 쓰지 않습니다.
 */
'use strict';

const fs = require('fs');
const path = require('path');

// ── 인자 ────────────────────────────────────────────────────────────
const [xmlDir, group1Dir, outDir] = process.argv.slice(2);
if (!xmlDir || !group1Dir || !outDir) {
  console.error('사용법: node convert.js <xml_input_dir> <group1_csv_dir> <output_dir>');
  process.exit(1);
}

const START_INDEX = 4;          // 2군 파일 번호 시작 (1군이 part_01~03을 씀)
const ROWS_PER_FILE = 5000;     // 파일당 줄 수

// ── 1군(기존) 표제어 모으기 → 제외 목록 ──────────────────────────────
function firstCsvField(line) {
  // "한글",... 에서 첫 칸만 뽑음 (칸은 큰따옴표로 감싸여 있음)
  const m = line.match(/^\s*"((?:[^"]|"")*)"/);
  return m ? m[1].replace(/""/g, '"') : null;
}

const group1Words = new Set();
for (const f of fs.readdirSync(group1Dir).filter((f) => f.endsWith('.csv')).sort()) {
  const text = fs.readFileSync(path.join(group1Dir, f), 'utf8');
  for (const raw of text.split(/\r?\n/)) {
    if (!raw.trim()) continue;
    const w = firstCsvField(raw);
    if (w) group1Words.add(w);
  }
}
console.error(`1군 표제어 ${group1Words.size}개 로드 (겹치면 제외)`);

// ── XML 도우미 ──────────────────────────────────────────────────────
// LMF는 <feat att="X" val="Y"/> 꼴로 값을 담습니다.
function featVal(block, att) {
  const m = block.match(new RegExp(`att="${att}"\\s+val="((?:[^"]|"")*)"`));
  return m ? decodeXml(m[1]) : null;
}
function decodeXml(s) {
  return s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim();
}
function csvField(s) {
  return '"' + String(s).replace(/"/g, '""') + '"';
}

// ── 영어 대응어 정리 ────────────────────────────────────────────────
// krdict 영어 lemma는 "edge; verge" 처럼 ; 로 여러 개가 올 수 있습니다.
// 접미 참조표시(¹²)나 대괄호 주석을 걷어내고 정돈합니다.
function cleanEnglish(lemma) {
  return lemma
    .replace(/\[[^\]]*\]/g, ' ')      // [주석] 제거
    .replace(/\s+/g, ' ')
    .replace(/\s*;\s*/g, '; ')
    .trim();
}

// ── 파서 본체 ───────────────────────────────────────────────────────
// 표제어(LexicalEntry) 단위로 잘라, '단어'이면서 영어 대응어가 있는
// 첫 뜻풀이 하나를 뽑습니다. (동음이의어는 각각 별도 표제어로 옵니다)
const entryRe = /<LexicalEntry\b[\s\S]*?<\/LexicalEntry>/g;
const senseRe = /<Sense\b[\s\S]*?<\/Sense>/g;
const equivRe = /<Equivalent\b[\s\S]*?<\/Equivalent>/g;

const stats = {
  entries: 0, words: 0, withEnglish: 0,
  skippedNotWord: 0, skippedNoEnglish: 0, skippedGroup1: 0, skippedShape: 0,
};

const rows = [];               // {korean, english, meaning}
const seen = new Set();        // 2군 내부 중복 제거 (koreanenglish)

function processXmlFile(file) {
  const xml = fs.readFileSync(file, 'utf8');
  let em;
  while ((em = entryRe.exec(xml)) !== null) {
    stats.entries++;
    const block = em[0];

    const unit = featVal(block, 'lexicalUnit');
    if (unit !== '단어') { stats.skippedNotWord++; continue; }

    // 조사·어미·접사(순수 문법 형태소)는 자판으로 홀로 칠 '학습 단어'가 아니므로 제외
    const pos = featVal(block, 'partOfSpeech');
    if (pos === '조사' || pos === '어미' || pos === '접사') { stats.skippedNotWord++; continue; }

    // 표제어(첫 <Lemma> 안의 writtenForm)
    const lemmaBlock = (block.match(/<Lemma\b[\s\S]*?<\/Lemma>/) || [''])[0];
    const korean = featVal(lemmaBlock, 'writtenForm');
    if (!korean) { stats.skippedShape++; continue; }
    // 공백/붙임표(접사·어미) 포함 표제어는 자판으로 칠 단어가 아니므로 제외
    if (/[\s\-]/.test(korean)) { stats.skippedShape++; continue; }
    stats.words++;

    // 뜻풀이(Sense)들 중 영어 대응어가 있는 첫 번째를 채택
    let english = null, meaning = null;
    let sm;
    senseRe.lastIndex = 0;
    while ((sm = senseRe.exec(block)) !== null) {
      const sBlock = sm[0];
      let eng = null;
      let qm;
      equivRe.lastIndex = 0;
      while ((qm = equivRe.exec(sBlock)) !== null) {
        if (featVal(qm[0], 'language') === '영어') {
          eng = featVal(qm[0], 'lemma');
          break;
        }
      }
      if (eng) {
        english = cleanEnglish(eng);
        meaning = featVal(sBlock, 'definition') || '';
        break;
      }
    }

    if (!english) { stats.skippedNoEnglish++; continue; }
    stats.withEnglish++;

    if (group1Words.has(korean)) { stats.skippedGroup1++; continue; }

    const key = korean + '' + english;
    if (seen.has(key)) continue;
    seen.add(key);
    rows.push({ korean, english, meaning: meaning || '' });
  }
}

// ── 실행 ────────────────────────────────────────────────────────────
const xmlFiles = fs.readdirSync(xmlDir).filter((f) => f.endsWith('.xml')).sort();
if (xmlFiles.length === 0) {
  console.error(`XML 파일이 없습니다: ${xmlDir}`);
  process.exit(1);
}
for (const f of xmlFiles) {
  processXmlFile(path.join(xmlDir, f));
  console.error(`  처리: ${f}  (누적 2군 ${rows.length}개)`);
}

// 한글 표제어 기준 정렬 (파일이 안정적으로 나오도록)
rows.sort((a, b) => a.korean.localeCompare(b.korean, 'ko') || a.english.localeCompare(b.english));

// ── CSV로 쓰기 ──────────────────────────────────────────────────────
fs.mkdirSync(outDir, { recursive: true });
let fileNo = START_INDEX;
for (let i = 0; i < rows.length; i += ROWS_PER_FILE) {
  const chunk = rows.slice(i, i + ROWS_PER_FILE);
  const name = `part_${String(fileNo).padStart(2, '0')}.csv`;
  const body = chunk
    .map((r) => [r.korean, r.english, '', r.meaning, ''].map(csvField).join(','))
    .join('\n') + '\n';
  fs.writeFileSync(path.join(outDir, name), body, 'utf8');
  console.error(`  씀: ${name}  (${chunk.length}줄)`);
  fileNo++;
}

// ── 통계 출력 ───────────────────────────────────────────────────────
console.error('\n──── 변환 통계 ────');
console.error(`총 표제어(LexicalEntry): ${stats.entries}`);
console.error(`  · '단어'      : ${stats.words}`);
console.error(`  · 영어 대응 有 : ${stats.withEnglish}`);
console.error(`제외: 단어아님 ${stats.skippedNotWord}, 영어없음 ${stats.skippedNoEnglish}, 모양(공백/붙임표) ${stats.skippedShape}, 1군중복 ${stats.skippedGroup1}`);
console.error(`최종 2군 행수: ${rows.length}`);
