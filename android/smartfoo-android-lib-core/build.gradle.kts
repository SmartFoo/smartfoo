plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

base.archivesName = "smartfoo-android-lib-core"
description = "SmartFoo Core Library for Android"
group = "com.smartfoo"
version = "0.1.23"

//def siteUrl = "https://github.com/SmartFoo/smartfoo"
//def gitUrl = "https://github.com/SmartFoo/smartfoo.git"

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.android.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

android {
    namespace = "com.smartfoo.android.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            // NOTE: Setting this to true can mess up debugging by hiding method parameters (especially anonymous methods)
            // If you are seeing weird missing variables when debugging, set this to false.
            // DO NOT CHECK THIS IN AS "false"!
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
    }

    /*
    if (getIsLocalDevelopmentBuild()) {
        defaultPublishConfig = "debug"
    }
    */
}

/*
bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    configurations = ["archives"]

    publish = true

    pkg {
        repo = "maven"
        name = archivesBaseName
        userOrg = "smartfoo"
        websiteUrl = siteUrl
        vcsUrl = gitUrl
        licenses = ["MIT"]
    }
}

install {
    repositories.mavenInstaller {
        pom {
            project {
                packaging "aar"

                // Add your description here
                name description
                url siteUrl

                // Set your license
                licenses {
                    license {
                        name "The MIT License (MIT)"
                        url "https://raw.githubusercontent.com/SmartFoo/smartfoo/master/LICENSE"
                    }
                }
                developers {
                    developer {
                        id "paulpv"
                        name "Paul Peavyhouse"
                        email "pv@swooby.com"
                    }
                }
                scm {
                    connection gitUrl
                    developerConnection gitUrl
                    url siteUrl

                }
            }
        }
    }
}

task sourcesJar(type: Jar) {
    from android.sourceSets.main.java.srcDirs
    classifier = "sources"
}

task javadoc(type: Javadoc) {
    source = android.sourceSets.main.java.srcDirs
    classpath += project.files(android.getBootClasspath().join(File.pathSeparator))
}

// Required to prevent "error: reference not found" for Android OS/API calls
afterEvaluate {
    javadoc.classpath += files(android.libraryVariants.collect { variant ->
        variant.javaCompile.classpath.files
    })
}

task javadocJar(type: Jar, dependsOn: javadoc) {
    classifier = "javadoc"
    from javadoc.destinationDir
}

artifacts {
    archives javadocJar
    archives sourcesJar
}
*/
