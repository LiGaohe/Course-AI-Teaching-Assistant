# Course TA IntelliJ Plugin - Setup Notes

1. Place the files from this template into your IntelliJ plugin project under `src/main/java` and `src/main/resources/META-INF/plugin.xml`.
2. Add dependencies in `build.gradle.kts`:

```kotlin
dependencies {
implementation("org.apache.tika:tika-core:2.8.0")
implementation("org.apache.tika:tika-parsers-standard-package:2.8.0")
}
```

3. Configure the DeepSeek endpoint and API key in `DeepSeekClient` constructor or provide a small settings UI to hold them.
4. Build and run the plugin using the Gradle IntelliJ run configurations.
