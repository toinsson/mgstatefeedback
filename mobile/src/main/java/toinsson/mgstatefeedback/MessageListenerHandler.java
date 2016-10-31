package toinsson.mgstatefeedback;

import android.os.Handler;
import android.os.Message;

class MessageListenerHandler extends Handler {
    private final IMessageListener messageListener;
 
    MessageListenerHandler(IMessageListener messageListener) {
        this.messageListener = messageListener;
    }
 
    @Override
    public void handleMessage(Message msg) {
        messageListener.messageReceived(msg.getData());
    }
}
