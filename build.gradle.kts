// 프로젝트 전체에서 쓰는 '조립 도구' 버전을 한곳에 적어 둡니다.
// apply false = 여기서는 버전만 선언하고, 실제 사용은 app 모듈에서 합니다.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
