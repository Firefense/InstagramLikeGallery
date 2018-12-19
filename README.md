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
	Copyright (C) 2015 Firefish Communications

	This program is free software: you can redistribute it and/or modify it 
	under the terms of the GNU Lesser General Public License as published by 
	the Free Software Foundation, either version 3 of the License, or 
	(at your option) any later version.

	This program is distributed in the hope that it will be useful, 
	but WITHOUT ANY WARRANTY; without even the implied warranty of 
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
	See the GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along 
	with this program. If not, see http://www.gnu.org/licenses/lgpl.
