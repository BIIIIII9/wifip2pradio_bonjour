/**
 * Created by kami on 2018/4/29.
 */
package com.example.a233.bluetooth_radio;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ViewDebug;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import retrofit2.http.Url;

import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;

public class ReceiveRadio_BLE extends Service {

    private BroadcastReceiver foundReceiver=null;
    Handler myHandler;
    private Looper mLooper;
    static final Map<String,MsgBlueRadio> msgStore=new HashMap<>();
    static final int FOUND_MESSAGE=3001;
    private LocalBroadcastManager myLocalBroadcastManager;
    public static final String EXTRA_CONTENT_MESSAGE_ADDRESS="com.example.a233.bluetooth_radio.EXTRA_CONTENT_MESSAGE_ADDRESS";
    public static final String EXTRA_CONTENT_MESSAGE_TEXT="com.example.a233.bluetooth_radio.EXTRA_CONTENT_MESSAGE_TEXT";
    public static final String EXTRA_CONTENT_MESSAGE_USERNAME="com.example.a233.bluetooth_radio.EXTRA_CONTENT_MESSAGE_USERNAME";
    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult (int callbackType ,ScanResult result) {
                    ScanRecord record = result.getScanRecord();
                   List<ParcelUuid> serviceUuids=record.getServiceUuids();
                   if(record.getDeviceName()!=null) {
                       Log.i("ScanResult result", result.toString());
                   }
                  if (serviceUuids!=null&&serviceUuids.size()==1) {
//                       if (struct.text!=null&&struct.address!=null) {
                      Message msg = new Message();
                      MsgStruct struct = new MsgStruct();
                      struct.text = record.getDeviceName();
                      struct.address = serviceUuids.get(0).toString();
                        msg.obj = struct;
                        msg.what = FOUND_MESSAGE;
                        myHandler.sendMessage(msg);
                        if (struct.text != null) {
                            Log.i("broadcastFoundMessage", struct.text);
                            Log.i("MAC", struct.address);
                        }
                  }
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    public void onCreate(){
        super.onCreate();
        myLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        foregroundNotification();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner=adapter.getBluetoothLeScanner();
        ScanSettings.Builder scanSettingsBuilder= new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY);
        scanner.startScan(null,scanSettingsBuilder.build(),mLeScanCallback);
        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mLooper = Looper.myLooper();
                myHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        if (msg.what == FOUND_MESSAGE) {
                            MsgStruct contentMsg = (MsgStruct) msg.obj;
                            if (contentMsg.text != null) {
                                byte[] msgText = null;
                                try {
                                    msgText = contentMsg.text.getBytes("UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                Log.i("MyBluetoothMethod", msgText.length+"handleMessage: "+contentMsg.text);
                                if (null!=msgText  &&
                                        msgText.length >ChangeNameOfBluetooth.baseByteMsg &&//控制最小长度
                                        isHaveSignal(msgText)
                                        ) {
                                    if (msgStore.containsKey(contentMsg.address)) {
                                        msgStore.get(contentMsg.address).setMessage(msgText);
                                    } else {
                                        MsgBlueRadio item = new MsgBlueRadio(contentMsg.address);
                                        item.setMessage(msgText);
                                        msgStore.put(contentMsg.address, item);
                                    }
                                }
                            }
                        }
                    }

                };
                Looper.loop();
            }
        });
        myThread.start();
        return START_REDELIVER_INTENT;
    }
    //Check the first 3 byte in array ,is it the REPLACEMENT CHARACTER in UTF8{(byte)0xef ,(byte)0xbf ,(byte)0xbd}
    boolean isHaveSignal(byte[] source){
        boolean flag=false;
//        final byte[] array=ChangeNameOfBluetooth.Signal;
//        if(array.length<source.length) {
//            flag=true;
//            for (int i = 0; i <array.length;i++){
//                if(array[i]!=source[i]){
//                    flag=false;
//                }
//            }
//        }
        int base =ChangeNameOfBluetooth.baseByteMsg;
        if(source[base-2]<=source[base-1]
//                &&(0xFF&source[base-3])<0x7F
                ) {
            flag=true;
        }

        return flag;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner=adapter.getBluetoothLeScanner();
        scanner.stopScan(mLeScanCallback);
        stopForeground(true);
        mLooper.quit();
        if(foundReceiver!=null) {
            unregisterReceiver(foundReceiver);
        }
        Intent intent = new Intent(MainActivity.ServiceOnDestroy);
        myLocalBroadcastManager.sendBroadcast(intent);
    }
    //Run service on foreground
    private void foregroundNotification() {
        final String channelID = "com.example.a233.Receive_radio.foregroundNotification";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("ReceiveBluetoothRadio")
                .setContentText("Receive broadcast");

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        startForeground(30, builder.build());
    }
    //Receive System Broadcast, when Bluetooth advice is found,get the name and MAC address of the advice
    //And push it in Loop of this Service


    //Assign one instance for each Mac address
    class MsgBlueRadio {
        private String addressMac;
        private int messageTotal;
        //       private int messageID ;
        private int localMessageTotal;
        private List<byte[]> messageBodyList;

        MsgBlueRadio(String MAC) {
            this.messageTotal = -1;
//            this.messageID=-1;
            this.messageBodyList = null;
            this.localMessageTotal = 0;
            this.addressMac = MAC;
        }

        void setMessage(byte[] msg) {
            final int length = msg.length;
            final int zero = ChangeNameOfBluetooth.signalZero;
            final int base = ChangeNameOfBluetooth.baseByteMsg;
            if (length > base) {
                setMessageBody((msg[base - 2] & 0xFF) - zero, (msg[base - 1] & 0xFF) - zero,
//                        (msg[base-1]&0xFF)-zero,
                        msg);
            }
        }

        //Remove the signal bytes(First 6 byte)
        private byte[] editMessage(byte[] newMessageBody) {
            final int base = ChangeNameOfBluetooth.baseByteMsg;
            byte[] msg = new byte[newMessageBody.length - base];
            System.arraycopy(newMessageBody, base, msg, 0, msg.length);
            return msg;
        }

        //Save pieces of massage in List
        // when collected all pieces,stitch them to one String instance ,and send to MainActivity
        private void setMessageBody(int newSerial, int newTotal,
//                                    int newID,
                                    byte[] newMessageBody) {
            newTotal++;
            if (
//                 newID != this.messageID ||
                    newTotal != this.messageTotal) {
                this.messageTotal = newTotal;
//               this.messageID = newID;
                this.messageBodyList = new ArrayList<>(newTotal);
                for (int i = 0; i < newTotal; i++) {
                    this.messageBodyList.add(null);
                }
                this.localMessageTotal = 0;
            }
            byte[] messageBody = editMessage(newMessageBody);
            if (this.messageBodyList.get(newSerial) == null) {
                this.messageBodyList.set(newSerial, messageBody);
                this.localMessageTotal++;
                if (localMessageTotal == messageTotal) {
                    String text = byteStitching(this.messageBodyList);
                    final String homePageUrl = getHomePageUrl(text);
                    String url=homePageUrl;
                    boolean isGetHomePage = false;
                    boolean isGetImage = false;
                    String homePage = null;
                    if(!(homePageUrl==null||homePageUrl.isEmpty())) {
                        Pattern name = Pattern.compile(homePageUrl, Pattern.CASE_INSENSITIVE);
                        Matcher nameMatcher = name.matcher(text);
                        text=nameMatcher.replaceAll("");
                        url=homePageUrl.replaceAll("\\s+","");
                        try {
                            homePage = getHtml(homePageUrl);
                            isGetHomePage = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            isGetHomePage = false;
                        }
                    }
                    String imgUrl = "";
                    String names="";
                    byte[] img = null;
                    if (isGetHomePage && homePage != null) {
                        imgUrl = getImgUrl(homePage);
                        names=getNames(homePage);
                        try {
                            img = getImage(imgUrl);
                            isGetImage = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            isGetImage = false;
                        }
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString(EXTRA_CONTENT_MESSAGE_ADDRESS, url);
                    bundle.putString(EXTRA_CONTENT_MESSAGE_TEXT, text);
                    bundle.putString(EXTRA_CONTENT_MESSAGE_USERNAME, names);
                    Intent intent = new Intent(MainActivity.LocalAction_RefreshUI);
                    intent.putExtras(bundle);
                    if(isGetImage&&img!=null)
                    intent.putExtra("picture",img);
//                saveData(contentMsg.address,contentMsg.text);TODO:SAVE_DATA
                    myLocalBroadcastManager.sendBroadcast(intent);
                }
            }
        }

        private String byteStitching(List<byte[]> pieceList) {
            int totalLength = 0;
            for (int i = 0; i < pieceList.size(); i++) {
                totalLength += pieceList.get(i).length;
            }
            byte[] bigByteArray = new byte[totalLength];
            int posi = 0;
            for (int i = 0; i < pieceList.size(); i++) {
                System.arraycopy(pieceList.get(i), 0, bigByteArray, posi, pieceList.get(i).length);
                posi += pieceList.get(i).length;
            }
            return new String(bigByteArray);
        }

        //        private void saveData(String addressMac,String text){
//            SharedPreferences sp = getSharedPreferences("MessageBluetooth_SAVE", Context.MODE_PRIVATE);
//            sp.edit().putString(addressMac,text).apply();
//        }
        String getHomePageUrl(String text) {
            Pattern p = Pattern.compile("\\shttps://twitter.com/\\w{1,50}\\s", Pattern.CASE_INSENSITIVE);
            Matcher matcher = p.matcher(text);
            if ((matcher.find())) {
                return matcher.group();
            }
                return null;
        }
        String getImgUrl(String text) {
            Pattern def = Pattern.compile("https://abs.twimg.com/sticky/default_profile_images/default_profile_normal.png", Pattern.CASE_INSENSITIVE);
            Matcher defMatcher = def.matcher(text);
            if (defMatcher.find()) {
                return defMatcher.group();
            }
            else {
                Pattern c = Pattern.compile("https://pbs.twimg.com/profile_images/.+?/.+?.(png|jpg)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = c.matcher(text);
                if (matcher.find())
                    return matcher.group();
            }
                return null;
        }
        String getNames(String text) {
            Pattern def = Pattern.compile("(?<=<title>).+?(?=\\))", Pattern.CASE_INSENSITIVE);
            Matcher defMatcher = def.matcher(text);
            if (defMatcher.find()) {
                text = defMatcher.group();
                return text+")";
            }
            return null;
        }
        byte[] getImage(String path) throws Exception {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 设置连接超时为5秒
            conn.setConnectTimeout(5000);
            // 设置请求类型为Get类型
            conn.setRequestMethod("GET");
            // 判断请求Url是否成功
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("请求url失败");
            }
            InputStream inStream = conn.getInputStream();
            byte[] bt = read(inStream);
            inStream.close();
            return bt;
        }

        // 获取网页的html源代码
        String getHtml(String path) throws Exception {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                InputStream in = conn.getInputStream();
                byte[] data = read(in);
                String html = new String(data, "UTF-8");
                return html;
            }
            return null;
        }

        byte[] read(InputStream inStream) throws Exception {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            inStream.close();
            return outStream.toByteArray();
        }
    }
}
class MsgStruct {
    String text;
    String address;
}




