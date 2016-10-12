adb shell run-as com.example.grant.wearableclimbtracker chmod 777 /data/data/com.example.grant.wearableclimbtracker/databases/
adb shell run-as com.example.grant.wearableclimbtracker chmod 777 /data/data/com.example.grant.wearableclimbtracker/databases/ClimbEntries.db
adb shell cp /data/data/com.example.grant.wearableclimbtracker/databases/ClimbEntries.db /sdcard/
adb pull /sdcard/ClimbEntries.db