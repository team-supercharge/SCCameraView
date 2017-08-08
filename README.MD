# SCCameraView

SCCameraView is a camera preview library, which uses google's Camera1 and Camera2 API respective to the device's operating system version. It also allows you take a picture, record videos with both cameras and to toogle between aspect ratios and preview sizes supported by the given sensor. 

# Download and usage

Get the artifact via gradle

```groovy
compile 'io.supercharge:sccameraview:0.1.0'
```

Add SCCameraView as a child to the root layout in your activity or fragment layout file, and also declare some views 
(e.g. Buttons) for using the functions of the library.

```xml
<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/activity_main"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="@color/colorPrimary"
    tools:context=".MainActivity">

    <io.supercharge.sccameraview.SCCameraView
            android:id="@+id/video_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_centerInParent="true"
            android:background="@color/colorBlack"/>

    <!-- Put your control views here -->
    
</RelativeLayout>
```

In your Activity class call the proper function in your view's OnClickListener

```java
switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.switchCamera();
            }
        });
```

You can find a full example in the demo application

# License

This project is opensource, every contribution and any feedback is more than welcome!

[Apache Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

```
Copyright 2017 Supercharge

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
# Author

[belanagy](https://github.com/belanagy) 
[WonderCsabo](https://github.com/WonderCsabo) 

[![Supercharge](http://s23.postimg.org/gbpv7dwjr/unnamed.png)](http://supercharge.io/)