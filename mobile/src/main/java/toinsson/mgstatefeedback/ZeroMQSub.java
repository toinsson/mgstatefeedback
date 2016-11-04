package toinsson.mgstatefeedback;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import org.zeromq.ZMQ;

class ZeroMQSub implements Runnable {
    private final String host;
    private final int port;
    private final Handler uiThreadHandler;

    ZeroMQSub(Handler uiThreadHandler, String host) {
        this.host = "tcp://"+host;
        this.port = 5556;
        this.uiThreadHandler = uiThreadHandler;
    }
    ZeroMQSub(Handler uiThreadHandler, String host, int port) {
        this.host = host;
        this.port = port;
        this.uiThreadHandler = uiThreadHandler;
    }

    @Override
    public void run() {

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.SUB);

        socket.connect(host+":"+port);
        socket.subscribe("".getBytes());

        Log.d("#DEBUG", "before while loop");

        while(!Thread.currentThread().isInterrupted()) {
            String msg = socket.recvStr();
            Log.d("#DEBUG", "msg: "+msg);
            // prepare the message back to UI
            Message m = uiThreadHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString("msg", msg);
            m.setData(b);
            uiThreadHandler.sendMessage(m);
        }

        Log.d("#DEBUG", "Thread exciting");

        socket.close();
        context.term();
    }
}
