awt-datatransfer dependencies
=============================

These dependencies allow us to use the java mail api to send e-mails on Android.

The javax.activation and javax.mail libraries rely on some standard java classes
which are not present in Android. In particular, the following classes are required:
* java.awt.datatransfer.DataFlavor
* java.awt.datatransfer.Transferable
* java.awt.datatransfer.UnsupportedFlavorException

One solution exists for this problem: the javamail-android library: 
https://code.google.com/p/javamail-android/source/checkout
This library does not seem to be available on maven central or jcenter 
(or any maven repository). If you know of a place where it is hosted, please let me know!

The javamail-android library repackages the above java.awt.datatransfer classes
into an "additionnal.jar" file, but in a separate package: myjava.awt.datatransfer.
This is to prevent errors when compiling the code, related to using "java" as a 
top-level package.
Since the classes are moved to myjava.awt.datatransfer, the javax.mail and javax.activation
classes must be modified to use the myjava package.  They are repackaged into
mail.jar and activation.jar files.

The approach in this project is different:
* The necessary files from java.awt.datatransfer are included here, in the original java package.
* No customization or repackaging is done for the javax.mail and javax.activation libraries
* The android project depending on this project must therefore include some gradle configuration
  in order to prevent the compilation errors about using the java package:
```
android {
    dexOptions {
        preDexLibraries = false
    }
}
project.tasks.withType(com.android.build.gradle.tasks.Dex) {
    additionalParameters=['--core-library']
}
```

