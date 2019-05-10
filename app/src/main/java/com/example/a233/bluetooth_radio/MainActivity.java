package com.example.a233.bluetooth_radio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.widget.TextView.BufferType.EDITABLE;


public class MainActivity extends AppCompatActivity {
    public static final int serviceMaxRuntime = 120000;
    static final int BLUETOOTH_DISCOVERABLE_DURATION = 300;
    //Color of button
    static final int colorUnable = 0xFF666666;
    static final int colorNormal = 0xFF3366FF;
    static final int colorWork = 0xFF66FFCC;

    public static final String EXTRA_MESSAGE = "com.example.a233.bluetooth_radio.MESSAGE";
    static final int REQUEST_CODE_SEND_MY_MASSAGE = 1001;
    static final int REQUEST_CODE_SEARCH_MY_MASSAGE = 1002;
    static final int REQUEST_CODE_ASK_Bluetooth_PERMISSION_TO_DISCOVER = 1003;
    private ListView myMainListView;
    private ListView listViewPeopleNearby;
    private MyListViewManager myListViewManager;
    private MyListViewManager mySecondListViewManager;
    private TabLayout myTabLyaout;
    private ViewPager myViewPage;
    private List<View> listViews;
    private List<String> listTitles;
    private View firstPage;
    private View secondPage;
    private viewAdapter myViewAdapter;
    static public final String LocalAction_RefreshUI = "Local_Broadcast_RefreshActivityUI";
    static public final String ServiceOnDestroy = "Local_Broadcast_Service_OnDestroy";
    private Runnable timeRunable;
    Handler mainHandler;
    LocalBroadcastManager myLocalBroadcastManager;
    static BroadcastReceiver myReceiver;
    boolean listViewStarFlag = false;
    boolean secondListViewStarFlag = false;
    boolean isCheckedAddID = false;

    WifiManager wifiManager;

    //button
    static Boolean boolBTN_Send_01;
    static Boolean boolBTN_Cancel_02;
    static Boolean boolBTN_Search_03;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMainActivity();
        initTextView();
        initButtonState();
        initTwitterLogin();
        myMainListView = firstPage.findViewById(R.id.MyContentListView);
        listViewPeopleNearby = secondPage.findViewById(R.id.list_view_people_nearby);
        myLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter= new IntentFilter(LocalAction_RefreshUI);
        filter.addAction(ServiceOnDestroy);
        myLocalBroadcastManager.registerReceiver(new LocalBroadcastReceiver(), filter);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mainHandler = new Handler();
    }

    void initMainActivity() {
        myTabLyaout = findViewById(R.id.myTabLayout);
        myViewPage = findViewById(R.id.myViewPage);
        listViews = new ArrayList<>();
        LayoutInflater mInflater = getLayoutInflater();
        firstPage = mInflater.inflate(R.layout.my_first_page, null);
        secondPage = mInflater.inflate(R.layout.my_second_page, null);
        listViews.add(firstPage);
        listViews.add(secondPage);
        listTitles = new ArrayList<>();
        listTitles.add("Main");
        listTitles.add("People nearby");
        //TabLayout.MODE_FIXED          各tab平分整个工具栏,如果不设置，则默认就是这个值
        //TabLayout.MODE_SCROLLABLE     适用于多tab的，也就是有滚动条的，一行显示不下这些tab可以用这个
        //                              当然了，你要是想做点特别的，像知乎里就使用的这种效果
        myTabLyaout.setTabMode(TabLayout.MODE_FIXED);
        //设置tablayout距离上下左右的距离
        //tab_title.setPadding(20,20,20,20);
        myTabLyaout.addTab(myTabLyaout.newTab().setText(listTitles.get(0)));
        myTabLyaout.addTab(myTabLyaout.newTab().setText(listTitles.get(1)));
        myViewAdapter = new viewAdapter(this, listViews, listTitles, null);
        myViewPage.setAdapter(myViewAdapter);
        myTabLyaout.setupWithViewPager(myViewPage);

        boolBTN_Send_01 = false;
        boolBTN_Cancel_02 = true;
        boolBTN_Search_03= false;
        Button btn_Send_01 = firstPage.findViewById(R.id.button_send);
        Button btn_Cancel_02 = firstPage.findViewById(R.id.button_cancel);
        Button btn_Search_03 = firstPage.findViewById(R.id.button_search);
        btn_Send_01.setBackgroundColor(colorNormal);
        btn_Search_03.setBackgroundColor(colorNormal);
        btn_Cancel_02.setEnabled(false);
        btn_Cancel_02.setVisibility(View.INVISIBLE);
    }

    void initTextView() {

        EditText editText = firstPage.findViewById(R.id.editText);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setGravity(Gravity.TOP);
        editText.setSingleLine(false);
        editText.setHorizontallyScrolling(false);
    }

    void initButtonState() {
        setLogState(false);
        final CheckBox myCheckBox = firstPage.findViewById(R.id.checkBox);
        myCheckBox.setChecked(false);
        myCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isCheckedAddID = true;
                } else {
                    isCheckedAddID = false;
                }
            }
        });
    }

    void initTwitterLogin() {
        final Button logoutButton = firstPage.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sp = getSharedPreferences("TwitterID_Bluetooth", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.clear();
                editor.apply();
                setLogState(false);
                final CheckBox myCheckBox = firstPage.findViewById(R.id.checkBox);
                myCheckBox.setChecked(false);
            }
        });

        final TwitterLoginButton loginButton = firstPage.findViewById(R.id.login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                // Do something with result, which provides a TwitterSession for making API calls
                setLogState(true);
                String twitterID = result.data.getUserName();
                SharedPreferences sp = getSharedPreferences("TwitterID_Bluetooth", Context.MODE_PRIVATE);
                sp.edit().putString("TwitterID", twitterID).apply();
            }

            @Override
            public void failure(TwitterException exception) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Failed").
                        setMessage("It needs twitter application and internet.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
        SharedPreferences sp = getSharedPreferences("TwitterID_Bluetooth", Context.MODE_PRIVATE);
        String twitterID = sp.getString("TwitterID", null);
        if (twitterID != null) {
            setLogState(true);
        }

    }

    void setLogState(boolean isLogin) {
        final TwitterLoginButton loginButton = firstPage.findViewById(R.id.login_button);
        final CheckBox checkBox = firstPage.findViewById(R.id.checkBox);
        final Button logoutButton = firstPage.findViewById(R.id.logout_button);
        if (isLogin) {
            loginButton.setVisibility(View.GONE);
            checkBox.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
        } else {
            loginButton.setVisibility(View.VISIBLE);
            checkBox.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setButtonState(ButtonState.CANCEL);
        if(myReceiver!=null){
            unregisterReceiver(myReceiver);
        }
    }

    //After serviceMaxRuntime seconds stop service
    private void setServiceRuntime() {
        timeRunable = new Runnable() {
            @Override
            public void run() {
                setButtonState(ButtonState.CANCEL);
            }
        };
        mainHandler.postDelayed(timeRunable, serviceMaxRuntime);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void changeTwitterLoginState(View view) {
    }

    //Onclick button Send
    public void sendMessage(View view) {
        hideKeyboard(view);
        if (!wifiManager.isWifiEnabled()) {

            wifiManager.setWifiEnabled(true);
        } else {
            runSendMessage();
        }

    }

    public void runSendMessage() {
        if (wifiManager.isWifiEnabled()&&
                setButtonState(ButtonState.SEND)) {
            Intent intent = new Intent(this, ChangeNameOfWIFI.class);
            EditText editText = firstPage.findViewById(R.id.editText);
            String message = editText.getText().toString();
            SharedPreferences sp = getSharedPreferences("TwitterID_Bluetooth", Context.MODE_PRIVATE);
            String twitterID = sp.getString("TwitterID", null);
            if (isCheckedAddID && twitterID != null) {
                message = " https://twitter.com/" + twitterID + " " + message;
            }
            intent.putExtra(EXTRA_MESSAGE, message);
            startService(intent);
            receiveSystemBroad();
            if(!boolBTN_Search_03) {
                setServiceRuntime();
            }
        }
    }

//    public void requestBluetoothDiscoverable() {
//        Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        requestBluetoothOn.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        requestBluetoothOn.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BLUETOOTH_DISCOVERABLE_DURATION);//BLUETOOTH_DISCOVERABLE_DURATION=300s
//        startActivityForResult(requestBluetoothOn, REQUEST_CODE_SEND_MY_MASSAGE);
//    }

    //Onclick button Search
    public void searchMessage(View view) {
        hideKeyboard(view);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        } else {
            runSearchMessage();
        }
    }

    //TODO:Try BLE
    public void runSearchMessage() {
        if (wifiManager.isWifiEnabled()
//                && checkPermission()
                &&setButtonState(ButtonState.SEARCH)
                ) {
            Intent intent = new Intent(this, ReceiveRadio_BLE.class);
            startService(intent);
            receiveSystemBroad();
            if(!boolBTN_Send_01) {
                setServiceRuntime();
            }
        }
    }

//    public void requestTurnOnBluetooth() {
//        Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        startActivityForResult(requestBluetoothOn, REQUEST_CODE_SEARCH_MY_MASSAGE);
//    }

    //If version Android of target phone more than or equal 6.0 ,request permission
//    public boolean checkPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            String permission = Manifest.permission.ACCESS_COARSE_LOCATION;
//            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{permission}, REQUEST_CODE_ASK_Bluetooth_PERMISSION_TO_DISCOVER);
//                return false;
//            }
//        }
//        return true;
//    }

    //Receive result of request that turn on bluetooth,change to discoverable
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SEND_MY_MASSAGE || requestCode == REQUEST_CODE_SEARCH_MY_MASSAGE) {
            switch (resultCode) {
                case Activity.RESULT_OK: {
                    if (requestCode == REQUEST_CODE_SEARCH_MY_MASSAGE) {
                        runSearchMessage();
                    }
                }
                break;
                case BLUETOOTH_DISCOVERABLE_DURATION: {
                    if (requestCode == REQUEST_CODE_SEND_MY_MASSAGE) {
                        runSendMessage();
                    }
                }
                break;
                case Activity.RESULT_CANCELED: {
                    MainActivityDialog(MainActivity.this, "Can not set bluetooth state!");
                }
                break;
                default:
                    break;
            }
        }
        // Pass the activity result to the login button.
        TwitterLoginButton loginButton = (TwitterLoginButton) firstPage.findViewById(R.id.login_button);
        loginButton.onActivityResult(requestCode, resultCode, data);
    }

//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_CODE_ASK_Bluetooth_PERMISSION_TO_DISCOVER) {
//            switch (grantResults[0]) {
//                case PackageManager.PERMISSION_GRANTED: {
//                    runSearchMessage();
//                }
//                break;
//
//                case PackageManager.PERMISSION_DENIED: {
//                    MainActivityDialog(MainActivity.this, "Can not get permission to search bluetooth device!");
//                }
//                break;
//                default:
//                    break;
//            }
//        }
//    }

    public void cancelService(View view) {
        setButtonState(ButtonState.CANCEL);
    }
    //Change color and state of button
    boolean setButtonState(ButtonState state) {
        Button btn_Send_01 = firstPage.findViewById(R.id.button_send);
        Button btn_Search_03 = firstPage.findViewById(R.id.button_search);
        switch (state) {
            case SEND: {
                if(boolBTN_Send_01) {
                    Intent stopIntent = new Intent(this, ChangeNameOfWIFI.class);
                    stopService(stopIntent);
                }
                boolBTN_Send_01=!boolBTN_Send_01;
                btn_Send_01.setBackgroundColor(boolBTN_Send_01?colorWork:colorNormal);
                if (!(boolBTN_Send_01||boolBTN_Search_03)&&timeRunable != null) {
                    mainHandler.removeCallbacks(timeRunable);
                }
                return boolBTN_Send_01;
            }
            case SEARCH: {
                if(boolBTN_Search_03) {
                    Intent stopIntent = new Intent(this, ReceiveRadio_BLE.class);
                    stopService(stopIntent);
                }
                boolBTN_Search_03=!boolBTN_Search_03;
                btn_Search_03.setBackgroundColor(boolBTN_Search_03?colorWork:colorNormal);
                if (!(boolBTN_Send_01||boolBTN_Search_03)&&timeRunable != null) {
                    mainHandler.removeCallbacks(timeRunable);
                }
                return boolBTN_Search_03;
            }
            case CANCEL: {
                if(boolBTN_Send_01) {
                    Intent stopIntent = new Intent(this, ChangeNameOfWIFI.class);
                    stopService(stopIntent);
                    boolBTN_Send_01 = !boolBTN_Send_01;
                    btn_Send_01.setBackgroundColor(boolBTN_Send_01 ? colorWork : colorNormal);
                }
                if(boolBTN_Search_03) {
                    Intent stopIntent = new Intent(this, ReceiveRadio_BLE.class);
                    stopService(stopIntent);
                    boolBTN_Search_03 = !boolBTN_Search_03;
                    btn_Search_03.setBackgroundColor(boolBTN_Search_03 ? colorWork : colorNormal);
                }
                if (!(boolBTN_Send_01||boolBTN_Search_03)&&timeRunable != null) {
                    mainHandler.removeCallbacks(timeRunable);
                }
                return !(boolBTN_Send_01||boolBTN_Search_03);
            }
        }
        return false;
    }

    private void MainActivityDialog(Context context, String msg) {
        new AlertDialog.Builder(context)
                .setTitle("Message")
                .setMessage(msg)
                .show();
    }

    //Listen to System Broadcast, if bluetooth turned off occurred other problem , stop Service
    void receiveSystemBroad() {
        IntentFilter filter = new IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
                        wifiProblemNotification("WIFI Turned Off!");
                        setButtonState(ButtonState.CANCEL);
                    }
               }
            }
        };
        registerReceiver(myReceiver, filter);
    }

    //If occur problem of wifi state,notify user
    void wifiProblemNotification(String errorText) {
        final String channelID = "com.example.a233.bluetooth_radio.unableResolveBluetoothProblem";
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Error")
                .setContentText(errorText)
                .setAutoCancel(true);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        if (notifyManager != null) {
            notifyManager.notify(2222, builder.build());
        }
    }

    //Refresh UI(ListView),  received complete message ,which found in method "ReceiveRadio.MsgBlueRadio.setMessageBody()"
    public class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocalAction_RefreshUI.equals(intent.getAction())) {
                Bundle bundle = intent.getExtras();
                byte[] bis = intent.getByteArrayExtra("picture");
                Bitmap bitmap;
                if (bis == null) {
                    bitmap = null;
                } else {
                    bitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);
                }
                if (bundle != null) {
                    //TODO:Try BLE
                    String url = bundle.getString(ReceiveRadio_BLE.EXTRA_CONTENT_MESSAGE_ADDRESS);
                    String text = bundle.getString(ReceiveRadio_BLE.EXTRA_CONTENT_MESSAGE_TEXT);
                    String username = bundle.getString(ReceiveRadio_BLE.EXTRA_CONTENT_MESSAGE_USERNAME);
                    Log.i("onReceive", "onReceive: " + text);
                    if (!(url == null || url.isEmpty())) {
                        if (secondListViewStarFlag == false) {
                            mySecondListViewManager = new MyListViewManager(username, bitmap, url);
                            mySecondListViewManager.star(listViewPeopleNearby);
                            secondListViewStarFlag = true;
                        } else {
                            mySecondListViewManager.put(username, bitmap, url);
                        }
                    }
//                    if(!(text==null||text.isEmpty())) {
                    if (listViewStarFlag == false) {
                        myListViewManager = new MyListViewManager(username, text, bitmap, url);
                        myListViewManager.star(myMainListView);
                        listViewStarFlag = true;
                    } else {
                        myListViewManager.put(username, text, bitmap, url);
                    }
//                    }
                }
            }
            if (ServiceOnDestroy.equals(intent.getAction())) {
                Toast.makeText(getApplicationContext(),"Do  not exceed 280 bytes",Toast.LENGTH_LONG)
                        .show();
                setButtonState(ButtonState.SEND);
            }
        }
    }
    //    private class MyListViewManager{
//        private final static String str_address="Bluetooth_name";
//        private final static String str_time="Bluetooth_time";
//        private final static String str_text="Bluetooth_content";
//        List<Map<String , String>> list;
//        SimpleAdapter adapter;
//        MyListViewManager(){
//            this.list=new ArrayList<>();
//            Map<String , String> map=new HashMap<>();
//            map.put(str_address,"");
//            map.put(str_text,"");
//            list.add(map);
//            adapter=new SimpleAdapter(MainActivity.this,list , R.layout.my_listview,new String[]{str_address,str_time,str_text},
//                    new int[]{R.id.Bluetooth_name,R.id.Bluetooth_time,R.id.Bluetooth_content});
//        }
//        void star() {
//            myMainListView.setAdapter(adapter);
//        }
//        void put (String address,String text){
//            Map<String , String> map=new HashMap<>();
//            map.put(str_address,address);
//            long systemTime = System.currentTimeMillis();
//            String time =  new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date(systemTime));
//            map.put(str_time,time);
//            map.put(str_text,text);
//            list.add(0,map);
//            adapter.notifyDataSetChanged();
//        }
//    }
    private class MyListViewManager {
        private final static String str_address = "Bluetooth_item_name";
        private final static String str_time = "Bluetooth_item_time";
        private final static String str_text = "Bluetooth_item_content";
        ArrayList<Map<String, String>> mList;
        ArrayList<Bitmap> mImgList;
        final MyAdapter mAdapter;
        final ListType mListType;
        ArrayList<String> mUrlsList;
        ArrayList<String> mNamesList;

        MyListViewManager(String address, String text, Bitmap img, String url) {
            this.mUrlsList = new ArrayList<>();
            this.mNamesList = new ArrayList<>();
            this.mList = new ArrayList<>();
            this.mImgList = new ArrayList<>();
            Map<String, String> map = new HashMap<>();
            map.put(str_address, address);
            long systemTime = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date(systemTime));
            map.put(str_time, time);
            map.put(str_text, text);
            mImgList.add(img);
            mUrlsList.add(url);
            mList.add(map);
            if (!(url == null || url.isEmpty())
                    && (address == null || address.isEmpty())) {
                mListType = ListType.LIST_NEARBY_PEOPLE;
            } else {
                mListType = ListType.MAIN_LIST;
            }
            mAdapter = new MyAdapter(mListType, MainActivity.this, mUrlsList, mList, mImgList, R.layout.my_listview, new String[]{str_address, str_time, str_text},
                    new int[]{R.id.Bluetooth_item_name, R.id.Bluetooth_item_time, R.id.Bluetooth_item_content});
        }

        MyListViewManager(String text, Bitmap img, String url) {
            this("", text, img, url);
            mNamesList.add(text);
        }

        void star(ListView mListView) {
            mListView.setAdapter(mAdapter);
        }

        void put(String address, String text, Bitmap img, String url) {
            Map<String, String> map = new HashMap<>();
            map.put(str_address, address);
            long systemTime = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date(systemTime));
            map.put(str_time, time);
            map.put(str_text, text);
            mList.add(0, map);
            mImgList.add(0, img);
            mUrlsList.add(0, url);
            mAdapter.notifyDataSetChanged();
        }

        void put(String text, Bitmap img, String url) {
            if (!mNamesList.contains(text)) {
                put("", text, img, url);
                mNamesList.add(text);
            }
        }
    }

    class MyAdapter extends SimpleAdapter {
        private Context context; /*运行环境*/
        ArrayList<Map<String, String>> listItem;  /*数据源*/
        ArrayList<Bitmap> mImgList;
        private LayoutInflater listContainer; // 视图容器
        String[] mFrom;
        int[] mTo;
        final ListType mListType;
        List<String> mUrlList;

        class ListItemView { // 自定义控件集合
            public LinearLayout box;
            public LinearLayout timeAndNameBox;
            public TextView[] textViews;
            public ImageView imageView;
            public LinearLayout contentAndButtonBox;
            public Button button_copy;

        }

        class ListItemView_people_nearby {
            public LinearLayout bar;
            public TextView id;
            public ImageView img;
        }

        /*construction function*/
        public MyAdapter(ListType type, Context context, ArrayList<String> url,
                         ArrayList<Map<String, String>> data, ArrayList<Bitmap> imgList, int resource,
                         String[] from, int[] to) {
            super(context, data, resource, from, to);
            this.listContainer = LayoutInflater.from(context); // 创建视图容器并设置上下文
            this.context = context;
            listItem = data;
            mFrom = from;
            mTo = to;
            mImgList = imgList;
            mListType = type;
            mUrlList = url;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            switch (mListType) {
                case MAIN_LIST:
                    return getViewForMainList(position, convertView, parent);
                case LIST_NEARBY_PEOPLE:
                    return getViewForNearbyList(position, convertView, parent);
                default:
                    return getViewForMainList(position, convertView, parent);
            }
        }

        private View getViewForMainList(final int position, View convertView, ViewGroup parent) {

            final int mPosition = position;
            ListItemView listItemView = null;
            if (convertView == null) {
                convertView = listContainer.inflate(R.layout.my_listview, null);//加载布局
                listItemView = new ListItemView();
                listItemView.box = convertView.findViewById(R.id.Bluetooth_item_box);
                listItemView.timeAndNameBox = convertView.findViewById(R.id.Bluetooth_item_topBox);
                listItemView.contentAndButtonBox = convertView.findViewById(R.id.Bluetooth_item_bottomBox);
                listItemView.button_copy = convertView.findViewById(R.id.Bluetooth_item_button);
                listItemView.imageView = convertView.findViewById(R.id.Bluetooth_item_img);
                listItemView.textViews = new TextView[mTo.length];
                for (int i = 0; i < mTo.length; i++) {
                    listItemView.textViews[i] = convertView.findViewById(mTo[i]);
                }
                // 设置控件集到convertView
                convertView.setTag(listItemView);

            } else {
                listItemView = (ListItemView) convertView.getTag();//利用缓存的View
            }
            Map<String, String> item = listItem.get(mPosition);
            for (int i = 0; i < mFrom.length; i++) {
                listItemView.textViews[i].setText(item.get(mFrom[i]));
            }
            String defaultName = "Not login";
            if (item.get(mFrom[0]) == null || item.get(mFrom[0]).isEmpty()) {
                listItemView.textViews[0].setText(defaultName);
            } else {
                String url = mUrlList.get(position);
                listItemView.textViews[0].setText(getClickableSpan(item.get(mFrom[0]), url));
                listItemView.textViews[0].setMovementMethod(LinkMovementMethod.getInstance());
            }
            listItemView.imageView.setImageBitmap(mImgList.get(mPosition));
            listItemView.button_copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map<String, String> item = listItem.get(mPosition);
                    String text = item.get("Bluetooth_item_content");
                    EditText editText = firstPage.findViewById(R.id.editText);
                    editText.setText(text, EDITABLE);
                    if(boolBTN_Send_01) {
                        setButtonState(ButtonState.SEND);
                    }
                    runSendMessage();
                }
            });
            return convertView;
        }

        private View getViewForNearbyList(final int position, View convertView, ViewGroup parent) {
            final int mPosition = position;
            ListItemView_people_nearby listItemView = null;
            if (convertView == null) {
                convertView = listContainer.inflate(R.layout.my_listview_people_nearby, null);//加载布局
                listItemView = new ListItemView_people_nearby();
                listItemView.bar = convertView.findViewById(R.id.Bluetooth_item_barBox);
                listItemView.id = convertView.findViewById(R.id.id_people_nearby);
                listItemView.img = convertView.findViewById(R.id.img_people_nearby);
                // 设置控件集到convertView
                convertView.setTag(listItemView);
            } else {
                listItemView = (ListItemView_people_nearby) convertView.getTag();//利用缓存的View
            }
            Map<String, String> item = listItem.get(mPosition);
            String url = mUrlList.get(position);
            listItemView.id.setText(getClickableSpan(item.get(mFrom[2]), url));
            listItemView.id.setMovementMethod(LinkMovementMethod.getInstance());
            listItemView.img.setImageBitmap(mImgList.get(mPosition));
            return convertView;
        }

        private SpannableString getClickableSpan(String text, final String url) {
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    PackageManager packageManager = getPackageManager();
                    List activities = packageManager.queryIntentActivities(intent,
                            PackageManager.MATCH_DEFAULT_ONLY);
                    boolean isIntentSafe = activities.size() > 0;
                    if (isIntentSafe) {
                        startActivity(intent);
                    }
                }
            }, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            Pattern name = Pattern.compile("\\(.+?\\)", Pattern.CASE_INSENSITIVE);
            Matcher nameMatcher = name.matcher(text);
            if (nameMatcher.find()) {
                final int firstLength = text.length() - nameMatcher.group().length();
                spannableString.setSpan(new ForegroundColorSpan(Color.GRAY), firstLength, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, firstLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, firstLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new RelativeSizeSpan(1.2f), 0, firstLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            NoUnderlineSpan noUnderlineSpan = new NoUnderlineSpan();
            spannableString.setSpan(noUnderlineSpan, 0, text.length(), Spanned.SPAN_MARK_MARK);
            return spannableString;
        }
    }

    public class NoUnderlineSpan extends UnderlineSpan {
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
        }
    }

    public class viewAdapter extends PagerAdapter {
        public List<View> list_view;
        private List<String> list_Title;                              //tab名的列表
        private int[] tabImg;
        private Context context;

        public viewAdapter(Context context, List<View> list_view, List<String> list_Title, int[] tabImg) {
            this.list_view = list_view;
            this.list_Title = list_Title;
            this.tabImg = tabImg;
            this.context = context;
        }

        @Override
        public int getCount() {
            return list_view.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ((ViewPager) container).addView(list_view.get(position), 0);
            return list_view.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView(list_view.get(position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return list_Title.get(position % list_Title.size());
        }
    }

}
enum ListType{
    MAIN_LIST,LIST_NEARBY_PEOPLE
}
enum ButtonState{
    SEND,SEARCH,CANCEL
}






