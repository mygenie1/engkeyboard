package com.hanyeong.keyboard.dict

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 온디바이스 학습 사전(SQLite) 저장소입니다.
 *
 * - 네트워크를 전혀 쓰지 않습니다.
 * - 단어 원본은 앱 안의 CSV 파일들(assets/dictionary/part_XX.csv)에 있습니다.
 *   앱을 처음 켤 때(또는 사전이 갱신됐을 때) 이 CSV들을 읽어 SQLite로 옮겨 담습니다.
 * - 표 구조: 한글, 영어, 발음기호, 뜻, 예문 (+ 내부용 id)
 *
 * CSV 형식: 한 줄에 한 단어, 각 칸은 큰따옴표로 감쌉니다.
 *   "한글","영어","발음기호","뜻","예문"
 * 칸 안에 큰따옴표가 있으면 두 개("")로 씁니다. 빈 줄은 무시합니다.
 */
class DictionaryDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE entries (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                korean        TEXT NOT NULL,
                english       TEXT NOT NULL,
                pronunciation TEXT NOT NULL,
                meaning       TEXT NOT NULL,
                example       TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_korean ON entries(korean)")
        seedFromAssets(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 사전(CSV)이 늘거나 바뀌면 버전을 올립니다. 그때 표를 새로 채웁니다.
        db.execSQL("DROP TABLE IF EXISTS entries")
        onCreate(db)
    }

    /** assets/dictionary 폴더의 모든 CSV를 읽어 표에 넣습니다. */
    private fun seedFromAssets(db: SQLiteDatabase) {
        val files = try {
            appContext.assets.list(ASSET_DIR)?.filter { it.endsWith(".csv") }?.sorted() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT INTO entries(korean, english, pronunciation, meaning, example) VALUES(?,?,?,?,?)"
            )
            for (file in files) {
                appContext.assets.open("$ASSET_DIR/$file").bufferedReader(Charsets.UTF_8).useLines { lines ->
                    for (raw in lines) {
                        val line = raw.trim()
                        if (line.isEmpty()) continue
                        val cols = parseCsvLine(line)
                        if (cols.size < 5) continue
                        stmt.clearBindings()
                        stmt.bindString(1, cols[0])
                        stmt.bindString(2, cols[1])
                        stmt.bindString(3, cols[2])
                        stmt.bindString(4, cols[3])
                        stmt.bindString(5, cols[4])
                        stmt.executeInsert()
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** CSV 한 줄을 칸 목록으로 나눕니다. (큰따옴표로 감싼 칸 안의 쉼표를 올바르게 처리) */
    private fun parseCsvLine(line: String): List<String> {
        val out = ArrayList<String>(5)
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    sb.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { out.add(sb.toString()); sb.setLength(0) }
                    else -> sb.append(c)
                }
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    /** 사전 전체를 한글 단어 → 항목들 형태로 메모리에 읽어 옵니다. */
    fun loadAll(): Map<String, List<DictEntry>> {
        val map = LinkedHashMap<String, MutableList<DictEntry>>()
        readableDatabase.rawQuery(
            "SELECT korean, english, pronunciation, meaning, example FROM entries ORDER BY id",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val entry = DictEntry(
                    korean = c.getString(0),
                    english = c.getString(1),
                    pronunciation = c.getString(2),
                    meaning = c.getString(3),
                    example = c.getString(4),
                )
                map.getOrPut(entry.korean) { mutableListOf() }.add(entry)
            }
        }
        return map
    }

    companion object {
        private const val DB_NAME = "dictionary.db"
        // 사전 CSV를 추가/수정할 때마다 이 번호를 올립니다. (기존 설치본도 새 단어로 갱신됨)
        private const val DB_VERSION = 2
        private const val ASSET_DIR = "dictionary"
    }
}
