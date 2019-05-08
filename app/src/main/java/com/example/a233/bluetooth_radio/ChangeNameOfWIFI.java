package com.example.a233.bluetooth_radio;



import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ChangeNameOfWIFI extends Service {
    private  boolean workFlagMyThread;
    public static final int baseByteMsg=2;
    public static final int signalZero=0x20;
    private LocalBroadcastManager myLocalBroadcastManager;
    //    public static final byte[] Signal={(byte)0xef ,(byte)0xbf ,(byte)0xbd};
    private static  ParcelUuid MyUuid;

    BroadcastReceiver myP2PReceiver;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public void onCreate(){
        super.onCreate();
        myLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        MyUuid=ParcelUuid.fromString(UUID.randomUUID().toString());
        foregroundNotification();
        final List<byte[]> listBytesMessage = Split(message, 32);
        discoverPeers();
        if(!(listBytesMessage==null)) {
            workFlagMyThread = true;
            Thread myThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (workFlagMyThread) {
                        try {
                            startMyRadio(listBytesMessage);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            myThread.start();
            return START_REDELIVER_INTENT;
        }
        else
        {
            //TODO:限制输入<128*248byte  把分隔程序split放到输入框
            intent = new Intent(MainActivity.ServiceOnDestroy);
            myLocalBroadcastManager.sendBroadcast(intent);
            onDestroy();
            return START_NOT_STICKY;
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        workFlagMyThread=false;
        manager.stopPeerDiscovery(channel, null);
        stopForeground(true);
//        Intent intent = new Intent(MainActivity.ServiceOnDestroy);
//        myLocalBroadcastManager.sendBroadcast(intent);
    }
    //Ever 10 millisecond change the name of Bluetooth
    public void startMyRadio(List<byte[]> listBytesMessage) throws InterruptedException {
        for (int i = 0; (workFlagMyThread) && (i < listBytesMessage.size()); i++) {
            long startSystemTime = System.currentTimeMillis();
            String name = null;
            try {
                name = new String(listBytesMessage.get(i),"utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            setDeviceName(name);
            Thread.sleep(5);
            long endSystemTime = System.currentTimeMillis()-startSystemTime;
            Log.i("Stop: ", Long.toString(endSystemTime));
            Thread.sleep(500);//312.5*4*8=10000 1.28*1000+(10000/1000)=1280+10
            //220
            endSystemTime = System.currentTimeMillis()-startSystemTime;
            Log.i("Stop: ", Long.toString(endSystemTime));
        }
    }


    void discoverPeers() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i("start","discover Peers success");
            }

            @Override
            public void onFailure(int reason) {
                Log.i("start","discover Peers fail"+reason);
            }
        });
    }
    public void setDeviceName(String devName) {
        try {
            Class[] paramTypes = new Class[3];
            paramTypes[0] = WifiP2pManager.Channel.class;
            paramTypes[1] = String.class;
            paramTypes[2] = WifiP2pManager.ActionListener.class;
            Method setDeviceName = manager.getClass().getMethod(
                    "setDeviceName", paramTypes);
            setDeviceName.setAccessible(true);

            Object arglist[] = new Object[3];
            arglist[0] = channel;
            arglist[1] = devName;
            arglist[2] = new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                   Log.i("setname","setDeviceName succeeded");
                }

                @Override
                public void onFailure(int reason) {
                    Log.i("setname","setDeviceName failed "+reason);
                }
            };

            setDeviceName.invoke(manager, arglist);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }
    //Split the "String message" into Several arrays with appropriate length
    //Add  head-message in first 6 byte for each array
    //[0][1][2]{(byte)0xef ,(byte)0xbf ,(byte)0xbd}-----------REPLACEMENT CHARACTER in UTF8
    //[3]serial number,[4]total number and [5]unique ID
    private List<byte[]> Split(String message, int size) {//the max length of Bluetooth name in my phone can be 190 byte
        final int __size = size - baseByteMsg;
        byte[] byteMessage_Be = null;
        try {
            byteMessage_Be = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (byteMessage_Be != null&&byteMessage_Be.length<280) {
            int messageSerial = signalZero;//消息序号
            final int messageID = getMessageID();//消息ID
            final int messageTotal =getMessageTotal(__size,byteMessage_Be);
            List<byte[]> list = new ArrayList<>();
            if (messageTotal <0x80){
                int contentSize=__size;
                int lang =0;
                //If the next character is not first character, the capacity -1 "10xx xxxx"
                while(messageSerial < messageTotal) {
//                    while ((0x80 <= (0xFF & byteMessage_Be[lang + contentSize]) &&
//                            (0xFF & byteMessage_Be[lang + contentSize]) <= 0xBF
//                    ))
                    while (0x80 == (0xC0 & byteMessage_Be[lang + contentSize]))
                    {
                        contentSize--;
                    }
                    byte[] s = new byte[contentSize+baseByteMsg];
                    System.arraycopy(byteMessage_Be, lang, s, baseByteMsg, contentSize);
//                        System.arraycopy(Signal, 0, s, 0, Signal.length);
                    s[baseByteMsg - 2] = (byte)messageSerial;
                    s[baseByteMsg - 1] = (byte)messageTotal;
//                      s[baseByteMsg - 1] = (byte)messageID;
                    list.add(s);
                    lang+=contentSize;
                    messageSerial++;
                    contentSize=__size;
                }
                byte[] s = new byte[byteMessage_Be.length-lang+baseByteMsg];
                System.arraycopy(byteMessage_Be, lang, s, baseByteMsg, byteMessage_Be.length-lang);
//                System.arraycopy(Signal, 0, s, 0, Signal.length);
                s[baseByteMsg - 2] = (byte)messageSerial;
                s[baseByteMsg - 1] = (byte)messageTotal;
//                s[baseByteMsg - 1] = (byte)messageID;
                list.add(s);
                return list;
            }
        }
        return null;
    }
    //If restart service,the messageID will be changed
    //The messageID saved through the method of Android Class SharedPreferences
    //Each turn to call this method,the ID will plus 1
    private int getMessageID() {
        SharedPreferences sp = getSharedPreferences("MessageIDBluetooth", Context.MODE_PRIVATE);
        int intID = sp.getInt("MessageID", 0);
        if (intID == 0) {
            intID=signalZero;
            sp.edit().putInt("MessageID", intID).apply();
        }
        int newID = intID + 1;
        if (newID >= 0x7F) {
            sp.edit().putInt("MessageID", signalZero).apply();
        } else {
            sp.edit().putInt("MessageID", newID).apply();
        }
        return intID;
    }

    private int getMessageTotal(int size,byte[] source){
        int contentSize = size;
        int lang=0;
        int count=0;
        while(lang+size < source.length) {
//        while ((0x80 <= (0xFF & source[lang + contentSize]) &&
//                (0xFF & source[lang + contentSize]) <= 0xBF
//        )) {
//            contentSize--;
//        }
            while (0x80 == (0xC0 & source[lang + contentSize]))
            {
                contentSize--;
            }
            lang+=contentSize;
            count++;
            contentSize=size;
        }
        return count+signalZero;
    }
    //Run service on foreground
    private void foregroundNotification() {
        final String channelID = "com.example.a233.bluetooth_radio.foregroundNotification";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("BluetoothRadio")
                .setContentText("Being broadcast");

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        startForeground(20, builder.build());
    }

}
