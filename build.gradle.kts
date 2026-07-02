buildscript {
    dependencies {
        classpath("com.android.tools:r8:8.8.34")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}
