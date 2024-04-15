package com.example.shiyanone;

import android.os.Bundle;

//import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import android.Manifest;

import android.view.View;
import android.widget.Button;

import androidx.core.app.ActivityCompat;



public class FriActivity extends AppCompatActivity {
    //用于显示信号强度大文本视图
    private TextView signalStrengthTextView;
    //以下两个声明由于不是全局变量，所以可以做局部变量，提高代码的可读性
    //但是我设置在这里是以防在多个方法使用他们，暂时留着，也可能做玩也不会在另一个方法使用

    //用于获取信号强度信息
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    //监测次数计数器，我最多让监测次数为5
    private int monitorCount = 0;

    //初始化界面
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置布局
        setContentView(R.layout.fristlayout);
        //检查控件
        //查找信号强度文本视图
        signalStrengthTextView = findViewById(R.id.signal_strength_textview);
//       监测按钮和清空按钮
        Button monitorButton = findViewById(R.id.monitor_button);
        Button clearButton = findViewById(R.id.clear_button);

        monitorButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (monitorCount < 5) {
                    monitorCount++;
                    //开始监测信号强度
                    monitorSignalStrength();
                } else {
                    Toast.makeText(FriActivity.this, "请清空已监测的数据", Toast.LENGTH_SHORT).show();
                }
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //清空
                clearSignalStrength();
            }
        });
        //检查权限
        if (checkPermissions()) {
            setupTelephonyManager();
        }
    }


    private boolean checkPermissions() {
        //监测安卓的版本是否大于6.0，低于6.0不能使用动态请求权限
        //这里好像提醒了不需要监测，因为minSdkVersion 已经设定为 24 或更高
        //那么检查设备的 Android 版本是否为 6.0 以上的代码确实是多余的
        //但是我第一次做这种东西，以免改完出问题就不删除了
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查应用是否被授予读取手机状态的权限
            if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
                //如果权限没有通过，加申请权限，并返回错误
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_PHONE_STATE},1);
                return false;
            }
        }
        return true;
    }

    private void setupTelephonyManager(){
        //获取对象
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //创建一个 PhoneStateListener对象，在信号强度变化时更新界面上的信号强度文本
        phoneStateListener = new PhoneStateListener(){
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength){
                //获取信号的强度
                super.onSignalStrengthsChanged(signalStrength);
                int strength = getSignalStrength(signalStrength);
                //输出到界面
                //signalStrengthTextView.setText("首次监测的信号强度为：:"+ strength + " dBm");
                String newText = "Signal Strength: " + strength + " dBm";
                if (monitorCount > 0) {
                    newText = "第 " + monitorCount + " 次的信号强度为：" + newText;
                }
                // 更新信号强度文本视图
                signalStrengthTextView.setText(newText);
            }
        };
        //注册监听器，以便监听信号强度大变化
        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    //根据不同的网络类型计算信号强度并返回一个整数
    private int getSignalStrength(SignalStrength signalStrength) {
        // 如果是GSM网络
        if (signalStrength.isGsm()) {
            // 转换单位为dBm
            return signalStrength.getGsmSignalStrength() * 2 - 113;
        } else {
            // 如果不是就获取CDMA 和 EVDO 的信号强度并返回较大值
            // 如果不是GSM网络，计算CDMA 和 EVDO 的信号强度并加权
            int cdmaDbm = signalStrength.getCdmaDbm();
            int evdoDbm = signalStrength.getEvdoDbm();
            // 对 CDMA 和 EVDO 的信号强度进行加权
            // 根据实际情况调整权重
            // 网络类型重要的就分给更高的权重
            // 哪个信号强，加给哪个更高的权重
            double weightedCdmaDbm = cdmaDbm * 0.6;
            double weightedEvdoDbm = evdoDbm * 0.4;
            // 将加权后的值转换为整数
            return (int) (weightedCdmaDbm + weightedEvdoDbm);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);
        if(requestCode == 1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //用户授予权限
                setupTelephonyManager();
            }
            else{
                //用户拒绝了权限,加弹出toast来提醒
                Toast.makeText(this,"Permission denied.",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void monitorSignalStrength() {
        //检查是否初始化
        if (telephonyManager != null && phoneStateListener != null) {
            //监听信号强度变化
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }
    }

    //清空监测次数和信号强度文本
    private void clearSignalStrength() {
        monitorCount = 0;
        signalStrengthTextView.setText("");
    }

    /*WIFI  信号监测函数,但是我这里没做展示
    private int getWifiSignalStrength() {
        int signalStrength = 0;
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                // 获取 Wi-Fi 信号强度
                signalStrength = wifiInfo.getRssi();
            }
        }
        return signalStrength;
    }
     */

}