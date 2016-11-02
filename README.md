# mgstatefeedback

Feedback the state of the shared control module onto an Android Wear watch.

The repo comes with a stub for generating the message, `bbt_stub.py`. It spawns a 0MQ publisher on port `5556`, 
which the app on the Android phone will subscribe to. Any update is then propagated through the app and the watch.
