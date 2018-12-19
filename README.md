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

## How to use
#### To start the Gallery activity:
```
Intent intent = InstagramLikeGallery.getGalleryIntent(context);
startActivityForResult(intent, ILGRequestCode.GALLERY_GET_IMAGES);
```

#### To get the result:
```
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case ILGRequestCode.GALLERY_GET_IMAGES:
                if(resultCode == ILGResultCode.RESULT_OK) {
                    ArrayList<File> images = (ArrayList<File>) data.getSerializableExtra(ILGConstants.EXTRA_GALLERY_IMAGE_FILES);
                }
                break;
        }
    }
```

### To enable multi-select mode:
```
Intent intent = InstagramLikeGallery.getGalleryIntent(context);
intent.putExtra(ILGConstants.EXTRA_ENABLE_MULTI_SELECT_MODE, true);
startActivityForResult(intent, ILGRequestCode.GALLERY_GET_IMAGES);
```

### To limit the number of images:
```
Intent intent = InstagramLikeGallery.getGalleryIntent(context);
intent.putExtra(ILGConstants.EXTRA_GALLERY_MAX_IMAGES, MAX_IMAGES);
startActivityForResult(intent, ILGRequestCode.GALLERY_GET_IMAGES);
```

## ProGuard
If you are using ProGuard you might need to add OkHttp's rules: https://github.com/square/okhttp/#proguard

## License
    Copyright 2018 Firefish Communication

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
