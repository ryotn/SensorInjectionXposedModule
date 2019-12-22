package jp.RyoTN.sensorinjectionxposedmodule;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView txtV;
    private double[] data = new double[19];
    private UpdateReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.btnStart);
        Button stopButton = findViewById(R.id.btnStop);
        txtV = findViewById(R.id.textView);

        // サービス開始
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
                    Log.d("ServiceName",serviceInfo.service.getClassName());
                    if (Usb9axisReceiverService.class.getName().equals(serviceInfo.service.getClassName())) {
                        // 実行中なら起動しない
                        Log.d("ログ","すでにTestServiceは起動しています。");
                        return;
                    }
                }

                Intent serviceIntent = new Intent(getApplication(), Usb9axisReceiverService.class);
                startService(serviceIntent);
            }
        });

        // サービス停止
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serviceIntent = new Intent(getApplication(), Usb9axisReceiverService.class);
                stopService(serviceIntent);
            }
        });


        // receiver
        receiver = new UpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("Usb9axisSensorReceiver");
        registerReceiver(receiver, filter);
    }

    protected class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            Bundle extras = intent.getExtras();
            data = extras.getDoubleArray("data");

            String recStr = "受信データ\n";
            recStr += "加速度\nx:" + data[0] + "\ny:" + data[1] + "\nz:" + data[2];
            recStr += "\nジャイロ\nx:" + data[3] + "\ny:" + data[4] + "\nz:" + data[5];
            recStr += "\n地磁気\nx:" + data[6] + "\ny:" + data[7] + "\nz:" + data[8];
            recStr += "\nLinearAcc\nx:" + data[9] + "\ny:" + data[10] + "\nz:" + data[11];
            recStr += "\nGravity\nx:" + data[12] + "\ny:" + data[13] + "\nz:" + data[14];
            recStr += "\nクォータニオン\nx:" + data[15] + "\ny:" + data[16] + "\nz:" + data[17] + "\nw:" + data[18];

            txtV.setText(recStr);
        }
    }

    @Override
    public void onDestroy() {
        //アプリ終了時に解除しないとエラーが出る
        unregisterReceiver(receiver);

        super.onDestroy();
    }
}
