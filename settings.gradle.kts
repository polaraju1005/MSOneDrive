import java.net.URI

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = URI("https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")
        }
        maven {
            name = "vsts-maven-adal-android"
            url =  URI("https://identitydivision.pkgs.visualstudio.com/_packaging/AndroidADAL/maven/v1")
            credentials {
                username = System.getenv("ENV_VSTS_MVN_ANDROIDADAL_USERNAME") ?: "defaultUsername"
                password = System.getenv("ENV_VSTS_MVN_ANDROIDADAL_ACCESSTOKEN") ?: "defaultPassword"
            }
        }
        mavenCentral()
    }
}

rootProject.name = "MSOneDrive"
include(":app")
