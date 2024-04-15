package com.example.shiyanone;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class FriActivity extends AppCompatActivity {
    private TextView signalStrengthTextView;
    //以下两个声明由于不是全局变量，所以可以做局部变量，提高代码的可读性
    //但是我设置在这里是以防在多个方法使用他们，暂时留着，也可能做玩也不会在另一个方法使用
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private LineChart lineChart;
    private List<Entry> entries;

    //初始化界面
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置布局
        setContentView(R.layout.fristlayout);
        //检查控件
        signalStrengthTextView = findViewById(R.id.signal_strength_textview);

        lineChart = findViewById(R.id.line_chart);
        entries = new ArrayList<>();

        Button btnMonitorSignal = findViewById(R.id.btn_monitor_signal);
        btnMonitorSignal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monitorSignal();
            }
        });

        Button btnClearData = findViewById(R.id.btn_clear_data);
        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearData();
            }
        });

        //检查是否授权
        if (checkPermissions()) {
            //判断高于6.0就设置TelephonyManager
            setupTelephonyManager();
        }
    }

    private void monitorSignal() {
        // 获取当前信号强度并添加到折线图数据中
        int strength = getCurrentSignalStrength();
        entries.add(new Entry(entries.size(), strength));

        // 更新折线图
        updateLineChart();
    }

    private void clearData() {
        // 清空折线图数据
        entries.clear();

        // 更新折线图
        updateLineChart();
    }

    private void updateLineChart() {
        // 创建折线数据集
        LineDataSet dataSet = new LineDataSet(entries, "Signal Strength");
        LineData lineData = new LineData(dataSet);

        // 设置折线图
        lineChart.setData(lineData);
        lineChart.invalidate(); // 刷新图表显示
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
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //创建一个 PhoneStateListener对象，在信号强度变化时更新界面上的信号强度文本
        phoneStateListener = new PhoneStateListener(){
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength){
                //获取信号的强度
                super.onSignalStrengthsChanged(signalStrength);
                int strength = getSignalStrength(signalStrength);
                //输出到界面
                signalStrengthTextView.setText("Signal Strength:"+ strength + " dBm");
            }
        };
        //注册监听器，以便监听信号强度大变化
        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    //根据不同的网络类型计算信号强度并返回一个整数
    private int getSignalStrength (SignalStrength signalStrength){
        //如果是GSM网络
        if(signalStrength.isGsm()){
            //转换单位为dBm
            return signalStrength.getGsmSignalStrength()*2-113;
        }
        //如果不是就获取CDMA 和 EVDO 的信号强度并返回较大值
        else{
            //如果不是GSM网络，计算CDMA 和 EVDO 的信号强度并加权
            int cdmaDbm = signalStrength.getCdmaDbm();
            int evdoDbm = signalStrength.getEvdoDbm();
            //对 CDMA 和 EVDO 的信号强度进行加权
            //根据实际情况调整权重
            //网络类型重要的就分给更高的权重
            //哪个信号强，加给哪个更高的权重
            double weightedCdmaDbm = cdmaDbm * 0.6;
            double weightedEvdoDbm = evdoDbm * 0.4;
            //将加权后的值转换为整数
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





}


