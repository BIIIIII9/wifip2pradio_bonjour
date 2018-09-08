package com.example.a233.bluetooth_radio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.InputType;
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

import static android.widget.TextView.BufferType.EDITABLE;


public class MainActivity extends AppCompatActivity {
    public static final int serviceMaxRuntime = 300000;
    static final int BLUETOOTH_DISCOVERABLE_DURATION = 300;
    //Color of button
    static final int colorUnable = 0xFF666666;
    static final int colorNormal = 0xFF3366FF;
    static final int colorWork = 0xFF66FFCC;

    public static final String EXTRA_MESSAGE = "com.example.a233.bluetooth_radio.MESSAGE";
    static final int REQUEST_CODE_SEND_MY_MASSAGE = 1001;
    static final int REQUEST_CODE_SEARCH_MY_MASSAGE = 1002;
    static final int REQUEST_CODE_ASK_Bluetooth_PERMISSION_TO_DISCOVER = 1003;
    private static ButtonState myButtonState;
    private ListView myMainListView;
    private ListView listViewPeopleNearby;
    private MyListViewManager myListViewManager;
    private TabLayout myTabLyaout;
    private ViewPager myViewPage;
    private List<View> listViews;
    private List<String> listTitles;
    private View firstPage;
    private View secondPage;
    private  viewAdapter myViewAdapter;
    static public final String LocalAction_RefreshUI = "Local_Broadcast_RefreshActivityUI";
    static public final String ServiceOnDestroy = "Local_Broadcast_Service_OnDestroy";
    private Runnable timeRunable;
    Handler mainHandler;
    LocalBroadcastManager myLocalBroadcastManager;
    BroadcastReceiver myReceiver;
    boolean listViewStarFlag = false;
    boolean isCheckedAddID = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMainActivity();
        initTextView();
        initButtonState();
        initTwitterLogin();
        myMainListView = firstPage.findViewById(R.id.MyContentListView);
        listViewPeopleNearby=secondPage.findViewById(R.id.list_view_people_nearby);
        myLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        myLocalBroadcastManager.registerReceiver(new LocalBroadcastReceiver(), new IntentFilter(LocalAction_RefreshUI));
        myButtonState = ButtonState.CANCEL;
        setButtonState(myButtonState);
        mainHandler = new Handler();
    }
    void initMainActivity(){
        myTabLyaout=  findViewById(R.id.myTabLayout);
        myViewPage =  findViewById(R.id.myViewPage);
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
        myViewAdapter = new viewAdapter(this,listViews,listTitles,null);
        myViewPage.setAdapter(myViewAdapter);
        myTabLyaout.setupWithViewPager(myViewPage);
    }
    void initTextView(){

        EditText editText = firstPage.findViewById(R.id.editText);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setGravity(Gravity.TOP);
        editText.setSingleLine(false);
        editText.setHorizontallyScrolling(false);
    }
    void initButtonState(){
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
    void initTwitterLogin(){
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
    void setLogState(boolean isLogin){
        final TwitterLoginButton loginButton = firstPage.findViewById(R.id.login_button);
        final CheckBox checkBox = firstPage.findViewById(R.id.checkBox);
        final Button logoutButton = firstPage.findViewById(R.id.logout_button);
        if(isLogin) {
            loginButton.setVisibility(View.GONE);
            checkBox.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
        }
        else{
            loginButton.setVisibility(View.VISIBLE);
            checkBox.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        runCancelService();
    }
    //After serviceMaxRuntime seconds stop service
    private void setServiceRuntime() {
        timeRunable = new Runnable() {
            @Override
            public void run() {
                runCancelService();
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
        if (myButtonState == ButtonState.CANCEL && MyBluetoothMethodManager.isBluetoothSupported()) {
            if (!MyBluetoothMethodManager.isBluetoothEnabled()) {
                requestBluetoothDiscoverable();
            } else {
                runSendMessage();
            }
        }
    }
    public void runSendMessage() {
        if (MyBluetoothMethodManager.isBluetoothEnabled()) {
            Intent intent = new Intent(this, ChangeNameOfBluetooth.class);
            EditText editText = firstPage.findViewById(R.id.editText);
            String message = editText.getText().toString();
            SharedPreferences sp = getSharedPreferences("TwitterID_Bluetooth", Context.MODE_PRIVATE);
            String twitterID = sp.getString("TwitterID", null);
            if (isCheckedAddID && twitterID != null) {
                message = " https://twitter.com/" + twitterID + " " + message;
            }
            intent.putExtra(EXTRA_MESSAGE, message);
            startService(intent);
            myButtonState = ButtonState.SEND;
            setButtonState(myButtonState);
            receiveSystemBroad();
            setServiceRuntime();
        }
    }
    public void requestBluetoothDiscoverable() {
        Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        requestBluetoothOn.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        requestBluetoothOn.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BLUETOOTH_DISCOVERABLE_DURATION);//BLUETOOTH_DISCOVERABLE_DURATION=300s
        startActivityForResult(requestBluetoothOn, REQUEST_CODE_SEND_MY_MASSAGE);
    }
    //Onclick button Search
    public void searchMessage(View view) {
        hideKeyboard(view);
        if (myButtonState == ButtonState.CANCEL && MyBluetoothMethodManager.isBluetoothSupported()) {
            if (!MyBluetoothMethodManager.isBluetoothEnabled()) {
                requestTurnOnBluetooth();
            } else {
                runSearchMessage();
            }
        }
    }
    //TODO:Try BLE
    public void runSearchMessage() {
        if (MyBluetoothMethodManager.isBluetoothEnabled() && checkPermission()) {
            Intent intent = new Intent(this, ReceiveRadio_BLE.class);
            startService(intent);
            myButtonState = ButtonState.SEARCH;
            setButtonState(myButtonState);
            receiveSystemBroad();
            setServiceRuntime();
        }
    }

    public void requestTurnOnBluetooth() {
        Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(requestBluetoothOn, REQUEST_CODE_SEARCH_MY_MASSAGE);
    }

    //If version Android of target phone more than or equal 6.0 ,request permission
    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permission = Manifest.permission.ACCESS_COARSE_LOCATION;
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, REQUEST_CODE_ASK_Bluetooth_PERMISSION_TO_DISCOVER);
                return false;
            }
        }
        return true;
    }

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

    //TODO:BLE
    //Receive result of request that permission for Android 6.0
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_Bluetooth_PERMISSION_TO_DISCOVER) {
            switch (grantResults[0]) {
                case PackageManager.PERMISSION_GRANTED: {
                    runSearchMessage();
                }
                break;

                case PackageManager.PERMISSION_DENIED: {
                    MainActivityDialog(MainActivity.this, "Can not get permission to search bluetooth device!");
                }
                break;
                default:
                    break;
            }
        }
    }

    public void cancelService(View view) {
        runCancelService();
    }

    // Onclick Button Cancel
    public void runCancelService() {
        mainHandler.removeCallbacks(timeRunable);
        if (myReceiver != null) {
            unregisterReceiver(myReceiver);
        }
        if (myButtonState == ButtonState.SEND) {
            Intent stopIntent = new Intent(this, ChangeNameOfBluetooth.class);
            stopService(stopIntent);
            myButtonState = ButtonState.CANCEL;
            setButtonState(myButtonState);
        }
        //TODO:Try BLE
        else if (myButtonState == ButtonState.SEARCH) {
            Intent stopIntent = new Intent(this, ReceiveRadio_BLE.class);
            stopService(stopIntent);
            myButtonState = ButtonState.CANCEL;
            setButtonState(myButtonState);
        }
    }

    //Change color and state of button
    void setButtonState(ButtonState state) {
        Button btn = firstPage.findViewById(R.id.button);
        Button btn2 = firstPage.findViewById(R.id.button2);
        Button btn3 = firstPage.findViewById(R.id.button3);
        Boolean boolBTN1 = false;
        Boolean boolBTN2 = false;
        Boolean boolBTN3 = false;
        switch (state) {
            case SEND: {
                boolBTN2 = true;
                btn.setBackgroundColor(colorWork);
                btn3.setBackgroundColor(colorUnable);
                btn2.setBackgroundColor(colorNormal);
            }
            break;
            case SEARCH: {
                boolBTN2 = true;
                btn.setBackgroundColor(colorUnable);
                btn3.setBackgroundColor(colorWork);
                btn2.setBackgroundColor(colorNormal);
            }
            break;
            case CANCEL: {
                boolBTN1 = true;
                boolBTN3 = true;
                btn.setBackgroundColor(colorNormal);
                btn3.setBackgroundColor(colorNormal);
                btn2.setBackgroundColor(colorUnable);
            }
        }
        btn.setEnabled(boolBTN1);
        btn2.setEnabled(boolBTN2);
        btn3.setEnabled(boolBTN3);
    }

    private void MainActivityDialog(Context context, String msg) {
        new AlertDialog.Builder(context)
                .setTitle("Message")
                .setMessage(msg)
                .show();
    }

    //Listen to System Broadcast, if bluetooth turned off occurred other problem , stop Service
    void receiveSystemBroad() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    int nowStateBluetooth = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    if (nowStateBluetooth == BluetoothAdapter.STATE_OFF) {
                        bluetoothProblemNotification("Bluetooth Turned Off!");
                        runCancelService();
                    }
                } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {
                    int nowDiscoverableBluetooth = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
                    if (nowDiscoverableBluetooth == BluetoothAdapter.SCAN_MODE_CONNECTABLE) {
                        bluetoothProblemNotification("Bluetooth Is Not Discoverable!");
                        runCancelService();
                    }
                }
            }
        };
        registerReceiver(myReceiver, filter);
    }

    //If occur problem of Bluetooth state,notify user
    void bluetoothProblemNotification(String errorText) {
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
                    String address = bundle.getString(ReceiveRadio_BLE.EXTRA_CONTENT_MESSAGE_ADDRESS);
                    String text = bundle.getString(ReceiveRadio_BLE.EXTRA_CONTENT_MESSAGE_TEXT);
                    Log.i("onReceive", "onReceive: " + text);
                    if (listViewStarFlag == false) {
                        myListViewManager = new MyListViewManager(address, text, bitmap);
                        myListViewManager.star(myMainListView);
                        listViewStarFlag = true;
                    } else {
                        myListViewManager.put(address, text, bitmap);
                    }
                }
            }
            if (ServiceOnDestroy.equals(intent.getAction())) {
                myButtonState = ButtonState.CANCEL;
                setButtonState(myButtonState);
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
        MyAdapter mAdapter;
        ListType mListType;

        MyListViewManager(String address, String text, Bitmap img) {
            this.mList = new ArrayList<>();
            this.mImgList=new ArrayList<>();
            Map<String, String> map = new HashMap<>();
            map.put(str_address, address);
            long systemTime = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date(systemTime));
            map.put(str_time, time);
            map.put(str_text, text);
            mImgList.add(img);
            mList.add(map);
            if(mListType==null) {
                mListType = ListType.MAIN_LIST;
            }
            mAdapter = new MyAdapter(mListType,MainActivity.this, mList, mImgList, R.layout.my_listview, new String[]{str_address, str_time, str_text},
                    new int[]{R.id.Bluetooth_item_name, R.id.Bluetooth_item_time, R.id.Bluetooth_item_content});
        }
        MyListViewManager(String text, Bitmap img){
            this("", text, img);
            mListType=ListType.LIST_NEARBY_PEOPLE;
            myMainListView.getR
        }
        void star(ListView mListView) {
            mListView.setAdapter(mAdapter);
        }
        void put(String address, String text, Bitmap img) {
            Map<String, String> map = new HashMap<>();
            map.put(str_address, address);
            long systemTime = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date(systemTime));
            map.put(str_time, time);
            map.put(str_text, text);
            mList.add(0, map);
            mImgList.add(0,img);
            mAdapter.notifyDataSetChanged();
        }
        void put(String text,Bitmap img){
            put("", text, img);
        }
    }
    class MyAdapter extends SimpleAdapter {
        private Context context; /*运行环境*/
        ArrayList<Map<String, String>> listItem;  /*数据源*/
        ArrayList<Bitmap> mImgList;
        private LayoutInflater listContainer; // 视图容器
        String[] mFrom;
        int[] mTo;
        ListType mListType;


        class ListItemView { // 自定义控件集合
            public LinearLayout box;
            public LinearLayout timeAndNameBox;
            public TextView[] textViews;
            ImageView imageView;
            public LinearLayout contentAndButtonBox;
            public Button button_copy;

        }

        /*construction function*/
        public MyAdapter(ListType type,Context context,
                         ArrayList<Map<String, String>> data, ArrayList<Bitmap> imgList, int resource,
                         String[] from, int[] to) {
            super(context, data, resource, from, to);
            this.listContainer = LayoutInflater.from(context); // 创建视图容器并设置上下文
            this.context = context;
            listItem = data;
            mFrom = from;
            mTo = to;
            mImgList = imgList;
            mListType=type;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
           if(?)
            return getViewForMainList();
               else()
                    return getViewForNearbyList;
        }
        private View getViewForMainList(){
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
            listItemView.imageView.setImageBitmap(mImgList.get(mPosition));
            listItemView.button_copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map<String, String> item = listItem.get(mPosition);
                    String text = item.get("Bluetooth_item_content");
                    EditText editText = firstPage.findViewById(R.id.editText);
                    editText.setText(text, EDITABLE);
                    if (myButtonState == ButtonState.SEARCH) {
                        runCancelService();
                        runSendMessage();
                    } else if (myButtonState == ButtonState.SEND) {
                        mainHandler.removeCallbacks(timeRunable);
                        if (myReceiver != null) {
                            unregisterReceiver(myReceiver);
                        }
                        runCancelService();
                    } else {
                        runSendMessage();
                    }

                }
            });
            return convertView;
        }
        private View getViewForNearbyList(){}
    }
    public class viewAdapter extends PagerAdapter {
        public List<View> list_view;
        private List<String> list_Title;                              //tab名的列表
        private int[] tabImg;
        private Context context;

        public viewAdapter(Context context,List<View> list_view,List<String> list_Title,int[] tabImg) {
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
            return view==object;
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
            return  list_Title.get(position % list_Title.size());
        }
    }
}
enum ListType{
    MAIN_LIST,LIST_NEARBY_PEOPLE
}
enum ButtonState{
    SEND,SEARCH,CANCEL
}






