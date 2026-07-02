package com.hanyeong.keyboard.dict

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 온디바이스 학습 사전(SQLite) 저장소입니다.
 *
 * - 네트워크를 전혀 쓰지 않고, 폰 안의 작은 데이터베이스 파일에 단어를 저장합니다.
 * - 앱을 처음 켤 때 SeedData의 단어들을 한 번 넣습니다(onCreate).
 * - 표 구조: 한글, 영어, 발음기호, 뜻, 예문 (+ 내부용 id)
 *
 * 나중에 '단어장/복습' 기능을 붙일 때는 별도의 표를 추가하면 되도록,
 * 단어 표(entries)는 단순하고 독립적으로 두었습니다.
 */
class DictionaryDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

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
        // 한글 단어로 빠르게 찾기 위한 색인
        db.execSQL("CREATE INDEX idx_korean ON entries(korean)")

        // 기본 단어들을 한 번에 넣습니다(트랜잭션으로 빠르게).
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT INTO entries(korean, english, pronunciation, meaning, example) VALUES(?,?,?,?,?)"
            )
            for (e in SeedData.entries) {
                stmt.clearBindings()
                stmt.bindString(1, e.korean)
                stmt.bindString(2, e.english)
                stmt.bindString(3, e.pronunciation)
                stmt.bindString(4, e.meaning)
                stmt.bindString(5, e.example)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 다음 버전에서 사전 구조가 바뀌면 여기서 옮겨 담습니다. (지금은 없음)
    }

    /**
     * 사전 전체를 한글 단어 → 항목들 형태로 메모리에 읽어 옵니다.
     * 사전이 작아서 한 번 읽어 두면 타이핑 중 즉시 조회할 수 있습니다.
     */
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
        private const val DB_VERSION = 1
    }
}
