adb shell run-as com.example.grant.wearableclimbtracker chmod 777 /data/data/com.example.grant.wearableclimbtracker/files
adb shell run-as com.example.grant.wearableclimbtracker chmod 777 /data/data/com.example.grant.wearableclimbtracker/files/default.realm
adb shell cp /data/data/com.example.grant.wearableclimbtracker/databases/files/default.realm /sdcard/
adb pull /sdcard/default.realm