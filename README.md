[![](https://jitpack.io/v/anton-ff/InstagramLikeGallery.svg)](https://jitpack.io/#anton-ff/InstagramLikeGallery)
# Instagram-Like Gallery Android Library
It is an instagram-like image-picker library that allows you to select images from phone gallery or take pictures from Camera (using Camera2 api)

## Download

### Gradle
Add these lines to your **Project** build.gradle:
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
and add these lines to your **App** build.gradle:
```
	dependencies {
	        implementation 'com.github.anton-ff:InstagramLikeGallery:0.0.4'
	}
```

### Maven
Add these lines to your build file
```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
and add the dependency:
```
	<dependency>
	    <groupId>com.github.anton-ff</groupId>
	    <artifactId>InstagramLikeGallery</artifactId>
	    <version>0.0.4</version>
	</dependency>
```

## ProGuard
If you are using ProGuard you might need to add OkHttp's rules: https://github.com/square/okhttp/#proguard
