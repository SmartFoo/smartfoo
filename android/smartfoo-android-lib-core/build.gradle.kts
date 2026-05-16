plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
}

base.archivesName = "smartfoo-android-lib-core"
description = "SmartFoo Core Library for Android"
group = "com.smartfoo"
version = providers.gradleProperty("releaseVersion").getOrElse("1.0.0")
val pomName = "SmartFoo Android Core Library"
val pomDeveloperId = "smartfoo"
val pomDeveloperName = "SmartFoo"
val pomDeveloperEmail = "publish@smartfoo.com"
val pomSiteUrl = "https://github.com/SmartFoo/smartfoo"
val pomGitUrl = "${pomSiteUrl.removePrefix("https://")}.git"
val pomLicenseUrl = "https://raw.githubusercontent.com/SmartFoo/smartfoo/master/LICENSE"
val pomLicenseName = "The MIT License"

dependencies {
    //implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.android.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

android {
    namespace = "com.smartfoo.android.core"
    compileSdk = 37
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        buildConfig = true
    }
    /*
    if (getIsLocalDevelopmentBuild()) {
        defaultPublishConfig = "debug"
    }
    */
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

/*
tasks.withType(JavaCompile) {
    options.compilerArgs << "-Xlint:unchecked"
}
*/

// Credentials are read from ~/.gradle/gradle.properties (local) or env vars (CI).
// See publishing setup docs in CONTRIBUTING.md.

val dokkaJavadocJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles a JAR containing Dokka HTML docs (satisfies Maven Central javadoc requirement)"
    val dokkaHtml = tasks.named("dokkaGeneratePublicationHtml")
    dependsOn(dokkaHtml)
    from(dokkaHtml.map { it.outputs.files })
    archiveClassifier = "javadoc"
}

// afterEvaluate required because the Android "release" component isn't available until after evaluation.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(dokkaJavadocJar)
                groupId    = project.group.toString()
                artifactId = base.archivesName.get()
                version    = project.version.toString()
                pom {
                    name        = pomName
                    description = project.description
                    url         = pomSiteUrl
                    licenses {
                        license {
                            name = pomLicenseName
                            url  = pomLicenseUrl
                        }
                    }
                    developers {
                        developer {
                            id    = pomDeveloperId
                            name  = pomDeveloperName
                            email = pomDeveloperEmail
                        }
                    }
                    scm {
                        connection          = "scm:git:git://$pomGitUrl"
                        developerConnection = "scm:git:ssh://$pomGitUrl"
                        url                 = pomSiteUrl
                    }
                }
            }
        }
    }

    signing {
        val signingKey: String?      by project
        val signingPassword: String? by project
        val skipSigning = signingKey == "SKIP" && signingPassword == "SKIP"
        if (!skipSigning)
        {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["release"])
        }
    }
}
