package com.hanyeong.keyboard.dict

/**
 * 앱을 처음 켤 때 SQLite 사전에 넣을 기본 단어 목록입니다.
 * (한글, 영어, 발음기호, 뜻, 예문) 순서.
 *
 * 한 한글 단어에 여러 뜻이 있는 경우(배·차·말·눈·다리·밤)는
 * 같은 한글로 여러 줄을 넣었습니다. 추천 바에는 최대 3개까지 보입니다.
 *
 * 나중에 여기에 줄을 추가하기만 하면 사전이 늘어납니다.
 */
object SeedData {
    val entries: List<DictEntry> = listOf(
        // ── 음식 · 음료 ──────────────────────────────────────────────
        DictEntry("사과", "apple", "/ˈæpəl/", "사과", "An apple a day keeps the doctor away."),
        DictEntry("바나나", "banana", "/bəˈnænə/", "바나나", "I ate a banana for breakfast."),
        DictEntry("포도", "grape", "/ɡreɪp/", "포도", "These grapes are very sweet."),
        DictEntry("딸기", "strawberry", "/ˈstrɔːbəri/", "딸기", "She likes strawberry ice cream."),
        DictEntry("수박", "watermelon", "/ˈwɔːtərmelən/", "수박", "Watermelon is perfect in summer."),
        DictEntry("빵", "bread", "/bred/", "빵", "I bought some fresh bread."),
        DictEntry("밥", "rice", "/raɪs/", "밥, 쌀밥", "We eat rice every day."),
        DictEntry("물", "water", "/ˈwɔːtər/", "물", "Can I have some water?"),
        DictEntry("우유", "milk", "/mɪlk/", "우유", "He drinks milk every morning."),
        DictEntry("커피", "coffee", "/ˈkɔːfi/", "커피", "I need a cup of coffee."),
        DictEntry("차", "tea", "/tiː/", "차 (마시는 차)", "Would you like some tea?"),
        DictEntry("차", "car", "/kɑːr/", "차 (자동차)", "My car is parked outside."),
        DictEntry("계란", "egg", "/eɡ/", "계란, 달걀", "I had two eggs for lunch."),
        DictEntry("고기", "meat", "/miːt/", "고기", "This meat is very tender."),
        DictEntry("생선", "fish", "/fɪʃ/", "생선 (음식)", "We had grilled fish for dinner."),
        DictEntry("김치", "kimchi", "/ˈkɪmtʃiː/", "김치", "Kimchi is a Korean side dish."),
        DictEntry("설탕", "sugar", "/ˈʃʊɡər/", "설탕", "Add a little sugar to the tea."),
        DictEntry("소금", "salt", "/sɔːlt/", "소금", "The soup needs more salt."),

        // ── 동물 ────────────────────────────────────────────────────
        DictEntry("개", "dog", "/dɔːɡ/", "개", "The dog is barking loudly."),
        DictEntry("고양이", "cat", "/kæt/", "고양이", "My cat sleeps all day."),
        DictEntry("새", "bird", "/bɜːrd/", "새", "A bird is singing outside."),
        DictEntry("물고기", "fish", "/fɪʃ/", "물고기", "Fish live in the water."),
        DictEntry("말", "horse", "/hɔːrs/", "말 (동물)", "The horse runs very fast."),
        DictEntry("말", "word", "/wɜːrd/", "말 (언어)", "That is a difficult word."),
        DictEntry("소", "cow", "/kaʊ/", "소", "The cow is eating grass."),
        DictEntry("돼지", "pig", "/pɪɡ/", "돼지", "The pig is very big."),
        DictEntry("닭", "chicken", "/ˈtʃɪkɪn/", "닭", "The chicken laid an egg."),
        DictEntry("토끼", "rabbit", "/ˈræbɪt/", "토끼", "The rabbit has long ears."),
        DictEntry("호랑이", "tiger", "/ˈtaɪɡər/", "호랑이", "The tiger is a wild animal."),
        DictEntry("곰", "bear", "/ber/", "곰", "The bear lives in the forest."),

        // ── 자연 ────────────────────────────────────────────────────
        DictEntry("하늘", "sky", "/skaɪ/", "하늘", "The sky is blue today."),
        DictEntry("바다", "sea", "/siː/", "바다", "We swam in the sea."),
        DictEntry("산", "mountain", "/ˈmaʊntən/", "산", "They climbed the mountain."),
        DictEntry("강", "river", "/ˈrɪvər/", "강", "The river flows to the sea."),
        DictEntry("나무", "tree", "/triː/", "나무", "There is a tall tree in the yard."),
        DictEntry("꽃", "flower", "/ˈflaʊər/", "꽃", "She gave me a flower."),
        DictEntry("눈", "snow", "/snoʊ/", "눈 (날씨)", "It is snowing outside."),
        DictEntry("눈", "eye", "/aɪ/", "눈 (신체)", "Please close your eyes."),
        DictEntry("비", "rain", "/reɪn/", "비", "It will rain tomorrow."),
        DictEntry("바람", "wind", "/wɪnd/", "바람", "The wind is strong today."),
        DictEntry("불", "fire", "/ˈfaɪər/", "불", "The fire keeps us warm."),
        DictEntry("돌", "stone", "/stoʊn/", "돌", "He threw a stone into the pond."),
        DictEntry("별", "star", "/stɑːr/", "별", "The stars are bright tonight."),
        DictEntry("달", "moon", "/muːn/", "달", "The moon is full tonight."),
        DictEntry("해", "sun", "/sʌn/", "해, 태양", "The sun rises in the east."),

        // ── 신체 ────────────────────────────────────────────────────
        DictEntry("머리", "head", "/hed/", "머리", "My head hurts a little."),
        DictEntry("손", "hand", "/hænd/", "손", "Please wash your hands."),
        DictEntry("발", "foot", "/fʊt/", "발", "My foot is cold."),
        DictEntry("다리", "leg", "/leɡ/", "다리 (신체)", "She hurt her leg."),
        DictEntry("다리", "bridge", "/brɪdʒ/", "다리 (구조물)", "We crossed the old bridge."),
        DictEntry("입", "mouth", "/maʊθ/", "입", "Open your mouth, please."),
        DictEntry("코", "nose", "/noʊz/", "코", "The dog has a wet nose."),
        DictEntry("귀", "ear", "/ɪr/", "귀", "He whispered in my ear."),
        DictEntry("배", "stomach", "/ˈstʌmək/", "배 (신체)", "My stomach hurts."),
        DictEntry("배", "pear", "/per/", "배 (과일)", "This pear is very juicy."),
        DictEntry("배", "ship", "/ʃɪp/", "배 (선박)", "The ship sailed away slowly."),

        // ── 사람 · 가족 ─────────────────────────────────────────────
        DictEntry("사람", "person", "/ˈpɜːrsən/", "사람", "She is a very kind person."),
        DictEntry("친구", "friend", "/frend/", "친구", "He is my best friend."),
        DictEntry("가족", "family", "/ˈfæməli/", "가족", "I love my family."),
        DictEntry("엄마", "mom", "/mɑːm/", "엄마", "My mom is a teacher."),
        DictEntry("아빠", "dad", "/dæd/", "아빠", "My dad cooks very well."),
        DictEntry("아이", "child", "/tʃaɪld/", "아이", "The child is playing outside."),
        DictEntry("선생님", "teacher", "/ˈtiːtʃər/", "선생님", "Our teacher is very kind."),
        DictEntry("학생", "student", "/ˈstuːdənt/", "학생", "She is a diligent student."),
        DictEntry("의사", "doctor", "/ˈdɑːktər/", "의사", "The doctor helped me a lot."),

        // ── 물건 ────────────────────────────────────────────────────
        DictEntry("책", "book", "/bʊk/", "책", "I am reading an interesting book."),
        DictEntry("집", "house", "/haʊs/", "집", "Their house is very big."),
        DictEntry("문", "door", "/dɔːr/", "문", "Please close the door."),
        DictEntry("창문", "window", "/ˈwɪndoʊ/", "창문", "Open the window, please."),
        DictEntry("의자", "chair", "/tʃer/", "의자", "Sit on the chair, please."),
        DictEntry("책상", "desk", "/desk/", "책상", "My desk is a bit messy."),
        DictEntry("시계", "clock", "/klɑːk/", "시계", "The clock is on the wall."),
        DictEntry("전화", "phone", "/foʊn/", "전화", "Please answer the phone."),
        DictEntry("컴퓨터", "computer", "/kəmˈpjuːtər/", "컴퓨터", "I work on my computer all day."),
        DictEntry("가방", "bag", "/bæɡ/", "가방", "Her bag is very heavy."),
        DictEntry("신발", "shoes", "/ʃuːz/", "신발", "These shoes are brand new."),
        DictEntry("옷", "clothes", "/kloʊz/", "옷", "I need to buy new clothes."),
        DictEntry("돈", "money", "/ˈmʌni/", "돈", "He saved a lot of money."),
        DictEntry("열쇠", "key", "/kiː/", "열쇠", "I lost my house key."),
        DictEntry("우산", "umbrella", "/ʌmˈbrelə/", "우산", "Take an umbrella; it may rain."),

        // ── 장소 ────────────────────────────────────────────────────
        DictEntry("학교", "school", "/skuːl/", "학교", "I go to school by bus."),
        DictEntry("병원", "hospital", "/ˈhɑːspɪtl/", "병원", "She works at a hospital."),
        DictEntry("가게", "store", "/stɔːr/", "가게, 상점", "The store is closed today."),
        DictEntry("시장", "market", "/ˈmɑːrkɪt/", "시장", "We bought fruit at the market."),
        DictEntry("공원", "park", "/pɑːrk/", "공원", "Let us take a walk in the park."),
        DictEntry("회사", "company", "/ˈkʌmpəni/", "회사", "He works for a big company."),
        DictEntry("식당", "restaurant", "/ˈrestrɑːnt/", "식당", "This restaurant is very popular."),
        DictEntry("은행", "bank", "/bæŋk/", "은행", "The bank opens at nine."),
        DictEntry("도시", "city", "/ˈsɪti/", "도시", "Seoul is a very big city."),
        DictEntry("나라", "country", "/ˈkʌntri/", "나라", "Korea is a beautiful country."),

        // ── 시간 ────────────────────────────────────────────────────
        DictEntry("시간", "time", "/taɪm/", "시간", "What time is it now?"),
        DictEntry("오늘", "today", "/təˈdeɪ/", "오늘", "Today is Monday."),
        DictEntry("내일", "tomorrow", "/təˈmɑːroʊ/", "내일", "See you tomorrow."),
        DictEntry("어제", "yesterday", "/ˈjestərdeɪ/", "어제", "I was very busy yesterday."),
        DictEntry("아침", "morning", "/ˈmɔːrnɪŋ/", "아침", "Good morning, everyone!"),
        DictEntry("저녁", "evening", "/ˈiːvnɪŋ/", "저녁", "We met in the evening."),
        DictEntry("밤", "night", "/naɪt/", "밤 (시간)", "Good night, sleep well."),
        DictEntry("밤", "chestnut", "/ˈtʃesnʌt/", "밤 (먹는 밤)", "We roasted chestnuts by the fire."),
        DictEntry("년", "year", "/jɪr/", "년, 해", "Happy New Year!"),
        DictEntry("날", "day", "/deɪ/", "날, 하루", "It was a bright sunny day."),

        // ── 일 · 추상 ───────────────────────────────────────────────
        DictEntry("일", "work", "/wɜːrk/", "일", "I have a lot of work today."),
        DictEntry("회의", "meeting", "/ˈmiːtɪŋ/", "회의, 모임", "We have a meeting at 3."),
        DictEntry("질문", "question", "/ˈkwestʃən/", "질문", "May I ask a question?"),
        DictEntry("문제", "problem", "/ˈprɑːbləm/", "문제", "That is a serious problem."),
        DictEntry("이름", "name", "/neɪm/", "이름", "What is your name?"),
        DictEntry("사랑", "love", "/lʌv/", "사랑", "Love makes life beautiful."),
        DictEntry("행복", "happiness", "/ˈhæpinəs/", "행복", "Happiness comes from within."),
        DictEntry("음악", "music", "/ˈmjuːzɪk/", "음악", "I love listening to music."),
        DictEntry("영화", "movie", "/ˈmuːvi/", "영화", "Let us watch a movie tonight."),
        DictEntry("사진", "photo", "/ˈfoʊtoʊ/", "사진", "Take a photo of us, please."),
        DictEntry("그림", "picture", "/ˈpɪktʃər/", "그림", "She drew a lovely picture."),
        DictEntry("색", "color", "/ˈkʌlər/", "색, 색깔", "What is your favorite color?"),
    )
}
