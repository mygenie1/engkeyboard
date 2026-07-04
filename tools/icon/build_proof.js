// 아이콘 시안 미리보기 HTML을 만듭니다. PNG를 base64로 박아 넣어 자체 완결형으로.
const fs = require('fs');
const path = require('path');

const outDir = path.join(__dirname, 'out');
const b64 = (n) => 'data:image/png;base64,' +
  fs.readFileSync(path.join(outDir, `icon_${n}.png`)).toString('base64');

const img = { 512: b64(512), 192: b64(192), 96: b64(96), 48: b64(48) };

const html = `<title>앱 아이콘 시안 — 한영 학습 키보드</title>
<style>
  :root{
    --ground:#F6F7F9; --panel:#FFFFFF; --ink:#1B1D21; --sub:#5B6270;
    --line:#E5E8EE; --blue:#1A5FB4; --orange:#E8622C;
    --font:-apple-system,BlinkMacSystemFont,"Segoe UI","Malgun Gothic",Roboto,sans-serif;
  }
  *{box-sizing:border-box}
  body{margin:0;background:var(--ground);color:var(--ink);font-family:var(--font);
    line-height:1.6;-webkit-font-smoothing:antialiased}
  .wrap{max-width:860px;margin:0 auto;padding:48px 24px 80px}
  .eyebrow{text-transform:uppercase;letter-spacing:.14em;font-size:12px;font-weight:700;
    color:var(--blue)}
  h1{font-size:30px;line-height:1.25;margin:8px 0 6px;text-wrap:balance;letter-spacing:-.01em}
  .lede{color:var(--sub);margin:0 0 40px;max-width:56ch}
  .card{background:var(--panel);border:1px solid var(--line);border-radius:18px;
    padding:28px;margin-bottom:22px}
  .card h2{font-size:13px;text-transform:uppercase;letter-spacing:.1em;color:var(--sub);
    margin:0 0 20px;font-weight:700}
  .hero{display:flex;gap:28px;align-items:center;flex-wrap:wrap;justify-content:center}
  .hero img{width:200px;height:200px;border-radius:44px;
    box-shadow:0 12px 34px rgba(20,40,80,.18)}
  .hero .cap{color:var(--sub);font-size:13px;text-align:center;margin-top:10px}
  /* 실제 런처 크기 미리보기: 밝은/어두운 홈 배경 위 */
  .screens{display:grid;grid-template-columns:1fr 1fr;gap:18px}
  @media(max-width:560px){.screens{grid-template-columns:1fr}}
  .screen{border-radius:16px;padding:26px 20px;border:1px solid var(--line)}
  .screen.light{background:linear-gradient(160deg,#DCE6F5,#C4D3EC)}
  .screen.dark{background:linear-gradient(160deg,#20293B,#0E1422);border-color:#20293B}
  .screen .label{font-size:11px;text-transform:uppercase;letter-spacing:.1em;font-weight:700;
    margin-bottom:18px}
  .screen.light .label{color:#39485F}
  .screen.dark .label{color:#9DB0CE}
  .row{display:flex;gap:22px;align-items:flex-end;justify-content:center}
  .row .item{text-align:center}
  .row img{border-radius:22%;display:block;margin:0 auto;
    box-shadow:0 5px 14px rgba(0,0,0,.22)}
  .row .n{font-size:11px;margin-top:9px;font-variant-numeric:tabular-nums}
  .screen.light .n{color:#46556E}
  .screen.dark .n{color:#8698B6}
  .notes{display:grid;grid-template-columns:auto 1fr;gap:12px 18px;font-size:15px}
  .notes dt{color:var(--sub)}
  .notes dd{margin:0}
  .sw{display:inline-block;width:13px;height:13px;border-radius:3px;vertical-align:-1px;
    margin-right:7px;border:1px solid rgba(0,0,0,.08)}
  .confirm{background:#0E4C92;color:#EAF1FB;border-radius:18px;padding:26px 28px}
  .confirm h2{color:#A9C6EC}
  .confirm p{margin:0 0 6px}
  .confirm code{background:rgba(255,255,255,.14);padding:2px 7px;border-radius:6px;
    font-size:14px}
  footer{color:var(--sub);font-size:13px;text-align:center;margin-top:34px}
</style>

<div class="wrap">
  <div class="eyebrow">앱 아이콘 시안</div>
  <h1>말풍선 + 한&thinsp;→&thinsp;A</h1>
  <p class="lede">대화하듯 한글을 치면 영어를 배운다는 앱의 성격을 말풍선에 담았습니다.
    아래에서 큰 화면과 실제 홈 화면 크기(작게)를 모두 확인하고 확정해 주세요.</p>

  <div class="card">
    <h2>마스터 (512px)</h2>
    <div class="hero">
      <div>
        <img src="${img[512]}" alt="아이콘 512px">
        <div class="cap">둥근 사각형 · 파랑 타일</div>
      </div>
    </div>
  </div>

  <div class="card">
    <h2>실제 홈 화면 크기 — 작아도 읽히는지</h2>
    <div class="screens">
      <div class="screen light">
        <div class="label">밝은 배경</div>
        <div class="row">
          <div class="item"><img src="${img[192]}" width="72" height="72" alt=""><div class="n">72dp</div></div>
          <div class="item"><img src="${img[96]}" width="48" height="48" alt=""><div class="n">48dp</div></div>
          <div class="item"><img src="${img[48]}" width="32" height="32" alt=""><div class="n">32dp</div></div>
        </div>
      </div>
      <div class="screen dark">
        <div class="label">어두운 배경</div>
        <div class="row">
          <div class="item"><img src="${img[192]}" width="72" height="72" alt=""><div class="n">72dp</div></div>
          <div class="item"><img src="${img[96]}" width="48" height="48" alt=""><div class="n">48dp</div></div>
          <div class="item"><img src="${img[48]}" width="32" height="32" alt=""><div class="n">32dp</div></div>
        </div>
      </div>
    </div>
  </div>

  <div class="card">
    <h2>디자인 노트</h2>
    <dl class="notes">
      <dt>구성</dt><dd>파랑 말풍선 타일 + 흰 말풍선 안에 "한→A"</dd>
      <dt>의미</dt><dd>'한'(한국어) → 'A'(영어) — 학습 방향을 색으로 구분</dd>
      <dt>색</dt><dd>
        <span class="sw" style="background:#1A5FB4"></span>타일 #1A5FB4 ·
        <span class="sw" style="background:#0E4C92"></span>한·화살표 #0E4C92 ·
        <span class="sw" style="background:#E8622C"></span>A #E8622C</dd>
      <dt>가독성</dt><dd>맑은 고딕 볼드 · 글자를 말풍선 폭에 꽉 채워 작은 크기에서도 뭉개지지 않게</dd>
    </dl>
  </div>

  <div class="confirm">
    <h2>확정하면 이렇게 진행합니다</h2>
    <p>이 시안으로 <code>mipmap</code> 아이콘(48~512dp)과 적응형 아이콘을 만들어 앱에 넣고,</p>
    <p>마일스톤 5를 마무리한 뒤 푸시 명령을 알려드립니다.</p>
    <p style="margin-top:14px;color:#A9C6EC">수정 요청(예: 색·굵기·말풍선 모양)도 편하게 말씀해 주세요.</p>
  </div>

  <footer>한영 학습 키보드 · 아이콘 시안 · 확정 전 미리보기</footer>
</div>`;

const dest = process.argv[2] || path.join(__dirname, 'proof.html');
fs.writeFileSync(dest, html, 'utf8');
console.log('wrote', dest, '(' + Math.round(html.length / 1024) + ' KB)');
