plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // 앱의 코드 묶음 이름(내부 식별자와 동일하게 맞춥니다).
    namespace = "com.hanyeong.keyboard"

    // 앱을 조립할 때 참고하는 안드로이드 버전입니다.
    compileSdk = 34

    defaultConfig {
        // 전 세계에서 안 겹치는 앱 고유 이름.
        applicationId = "com.hanyeong.keyboard"

        // 지원하는 최소 안드로이드 버전 (26 = 안드로이드 8.0).
        minSdk = 26
        targetSdk = 34

        // 앱 버전. 나중에 업데이트할 때 숫자를 올립니다.
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        // debug = 테스트용 설치 파일. 별도 서명 절차 없이 바로 폰에 설치할 수 있습니다.
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 안드로이드 기본 도구 모음입니다.
    implementation("androidx.core:core-ktx:1.13.1")
}
