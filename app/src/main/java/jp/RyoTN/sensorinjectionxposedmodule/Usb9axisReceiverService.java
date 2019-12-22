package jp.RyoTN.sensorinjectionxposedmodule;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

public class Usb9axisReceiverService extends Service implements SerialInputOutputManager.Listener {

    final Handler handler = new Handler();
    final Runnable r = new Runnable() {
        @Override
        public void run() {
            double[] data = new double[19];
            Random random = new Random();

            for(int i = 0; i < 19 ; i ++){
                data[i] = random.nextDouble();
            }

            sendMessage(data);
            handler.postDelayed(this, 30);
        }
    };

    private UsbManager mUsbManager;
    private UsbSerialDriver mUsbSerialDriver;
    private UsbSerialPort port;
    private SerialInputOutputManager ioManager;
    private static final String ACTION_USB_PERMISSION = "jp.RyoTN.sensorinjectionxposedmodule.USB_PERMISSION";
    private PendingIntent mPermissionIntent;

    private Handler hUsbDeviceSearch ;
    private Runnable rUsbDeviceSearch ;

    private byte[] recBytes = new byte[0];
    private double[] sendData = new double[19];
    private double[] sendData0 = new double[19];

    private boolean isServiceRunning = false;

    private int cntReceive = 0;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                            connectDevice();
                        }
                    }
                    else {
                        Log.d("LOG", "permission denied for device " + device);
                    }
                }
            }
        }
    };

    //起動時に呼ばれるブロードキャストレシーバー
    public static class BootReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {

            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (Usb9axisReceiverService.class.getName().equals(serviceInfo.service.getClassName())) {
                    // 実行中なら起動しない
                    Log.d("ログ","すでにUsb9axisReceiverServiceは起動しています。");
                    return;
                }
            }

            Intent serviceIntent = new Intent(context, Usb9axisReceiverService.class);
            context.startService(serviceIntent);
        }

    }

    @Override
    public void onCreate() {
    super.onCreate();
}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(!isServiceRunning){
            Toast.makeText(this , "サービス開始", Toast.LENGTH_SHORT).show();

            Log.d("test","Start :" + isServiceRunning);

            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(mUsbReceiver, filter);

            String title = "USB 9axis";

            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .build();

            startForeground(R.string.service_start, notification);

            startUsbManager();

            //handler.post(r);
        }

        isServiceRunning = true;



        return START_NOT_STICKY;

    }

    protected void sendMessage(double[] data){
        Intent broadcast = new Intent();
        broadcast.putExtra("data", data);
        broadcast.setAction("Usb9axisSensorReceiver");
        getBaseContext().sendBroadcast(broadcast);
    }


    @Override
    public void onDestroy() {
        //Toast.makeText(this , "停止" , Toast.LENGTH_SHORT).show();
        Log.d("test","Stop");

        //handler.removeCallbacks(r);
        unregisterReceiver(mUsbReceiver);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startUsbManager(){

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if(mUsbManager != null){
            findUsbDevice();
        }

    }
    public void findUsbDevice(){
        Log.d("test ","USB検索開始");
        hUsbDeviceSearch = new Handler();
        rUsbDeviceSearch = new Runnable() {
            @Override
            public void run() {
                ProbeTable customTable = new ProbeTable();
                customTable.addProduct(0x239a, 0x801e, CdcAcmSerialDriver.class);
                UsbSerialProber prober = new UsbSerialProber(customTable);
                List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(mUsbManager);
                if (availableDrivers.isEmpty()) {
                    Log.d("USB","デバイスがない");
                    hUsbDeviceSearch.postDelayed(this, 1000);
                    return;
                }
                for (UsbSerialDriver d : availableDrivers) {
                    Log.d("USBDevice","VID:" + d.getDevice().getVendorId() + "\nPID:"+d.getDevice().getProductId());
                    if (d.getDevice().getVendorId() == 0x239a && d.getDevice().getProductId() == 0x801e) {
                        mUsbSerialDriver = d;

                        if(mUsbManager.hasPermission(mUsbSerialDriver.getDevice())){
                            connectDevice();
                        }else{
                            mUsbManager.requestPermission(mUsbSerialDriver.getDevice(),mPermissionIntent);
                        }

                    }
                }
                if(mUsbSerialDriver == null){
                    Log.d("USB","デバイスがない");
                    hUsbDeviceSearch.postDelayed(this, 1000);
                    return;
                }
            }
        };
        hUsbDeviceSearch.post(rUsbDeviceSearch);

    }

    private void connectDevice() {
        UsbDeviceConnection connection = mUsbManager.openDevice(mUsbSerialDriver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            return;
        }
        for(int i = 0; i < 19 ;i ++){
            sendData0[i] = 0;
        }
        port = mUsbSerialDriver.getPorts().get(0);
        for(UsbSerialPort p : mUsbSerialDriver.getPorts()){
            Log.d("port" , "" + p);
        }
        try {
            port.open(connection);
            port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            port.setDTR(true);
            port.setRTS(true);
            port.purgeHwBuffers(true,true);
        } catch (IOException e) {
            // Deal with error.

        }

        ioManager = new SerialInputOutputManager(port, this);
        Executors.newSingleThreadExecutor().submit(ioManager);


    }

    @Override
    public void onNewData(byte[] data) {


        ByteBuffer byteBuf = ByteBuffer.allocate(data.length + recBytes.length);
        byteBuf.put(recBytes);
        byteBuf.put(data);

        recBytes = byteBuf.array();
        if(recBytes.length >= 39) {

            int chksum = 0;
            for(int i = 0; i < 38 ; i++){
               chksum += recBytes[i];
            }
            chksum = chksum & 0xFF;
            if(chksum != (recBytes[38] & 0xFF)){
                recBytes = new byte[0];
                return;
            }

            int rawAccX = (recBytes[0] & 0xFF) << 8 | (recBytes[1] & 0xFF);
            if (rawAccX > 0x7FFF) rawAccX = rawAccX - 0xFFFF;
            int rawAccY = (recBytes[2] & 0xFF) << 8 | (recBytes[3] & 0xFF);
            if (rawAccY > 0x7FFF) rawAccY = rawAccY - 0xFFFF;
            int rawAccZ = (recBytes[4] & 0xFF) << 8 | (recBytes[5] & 0xFF);
            if (rawAccZ > 0x7FFF) rawAccZ = rawAccZ - 0xFFFF;


            int rawGyroX = (recBytes[6] & 0xFF) << 8 | (recBytes[7] & 0xFF);
            if (rawGyroX > 0x7FFF) rawGyroX = rawGyroX - 0xFFFF;
            int rawGyroY = (recBytes[8] & 0xFF) << 8 | (recBytes[9] & 0xFF);
            if (rawGyroY > 0x7FFF) rawGyroY = rawGyroY - 0xFFFF;
            int rawGyroZ = (recBytes[10] & 0xFF) << 8 | (recBytes[11] & 0xFF);
            if (rawGyroZ > 0x7FFF) rawGyroZ = rawGyroZ - 0xFFFF;



            int rawMagX = (recBytes[12] & 0xFF) << 8 | (recBytes[13] & 0xFF);
            if (rawMagX > 0x7FFF) rawMagX = rawMagX - 0xFFFF;
            int rawMagY = (recBytes[14] & 0xFF) << 8 | (recBytes[15] & 0xFF);
            if (rawMagY > 0x7FFF) rawMagY = rawMagY - 0xFFFF;
            int rawMagZ = (recBytes[16] & 0xFF) << 8 | (recBytes[17] & 0xFF);
            if (rawMagZ > 0x7FFF) rawMagZ = rawMagZ - 0xFFFF;

            int rawLAccX = (recBytes[18] & 0xFF) << 8 | (recBytes[19] & 0xFF);
            if (rawLAccX > 0x7FFF) rawLAccX = rawLAccX - 0xFFFF;
            int rawLAccY = (recBytes[20] & 0xFF) << 8 | (recBytes[21] & 0xFF);
            if (rawLAccY > 0x7FFF) rawLAccY = rawLAccY - 0xFFFF;
            int rawLAccZ = (recBytes[22] & 0xFF) << 8 | (recBytes[23] & 0xFF);
            if (rawLAccZ > 0x7FFF) rawLAccZ = rawLAccZ - 0xFFFF;

            int rawGravX = (recBytes[24] & 0xFF) << 8 | (recBytes[25] & 0xFF);
            if (rawGravX > 0x7FFF) rawGravX = rawGravX - 0xFFFF;
            int rawGravY = (recBytes[26] & 0xFF) << 8 | (recBytes[27] & 0xFF);
            if (rawGravY > 0x7FFF) rawGravY = rawGravY - 0xFFFF;
            int rawGravZ = (recBytes[28] & 0xFF) << 8 | (recBytes[29] & 0xFF);
            if (rawGravZ > 0x7FFF) rawGravZ = rawGravZ - 0xFFFF;

            int rawQuatX = (recBytes[30] & 0xFF) << 8 | (recBytes[31] & 0xFF);
            if (rawQuatX > 0x7FFF) rawQuatX = rawQuatX - 0xFFFF;
            int rawQuatY = (recBytes[32] & 0xFF) << 8 | (recBytes[33] & 0xFF);
            if (rawQuatY > 0x7FFF) rawQuatY = rawQuatY - 0xFFFF;
            int rawQuatZ = (recBytes[34] & 0xFF) << 8 | (recBytes[35] & 0xFF);
            if (rawQuatZ > 0x7FFF) rawQuatZ = rawQuatZ - 0xFFFF;
            int rawQuatW = (recBytes[36] & 0xFF) << 8 | (recBytes[37] & 0xFF);
            if (rawQuatW > 0x7FFF) rawQuatW = rawQuatW - 0xFFFF;

            recBytes = new byte[0];


            double accScale = 100.0;
            sendData0[0] += rawAccX / accScale;
            sendData0[1] += rawAccY / accScale;
            sendData0[2] += rawAccZ / accScale;

            double gyroScale = 900.0;
            sendData0[3] += rawGyroX / gyroScale;
            sendData0[4] += rawGyroY / gyroScale;
            sendData0[5] += rawGyroZ / gyroScale;

            double magScale = 16.0;
            sendData0[6] += rawMagX / magScale;
            sendData0[7] += rawMagY / magScale;
            sendData0[8] += rawMagZ / magScale;

            double laccScale = accScale;
            sendData0[9] += rawLAccX / laccScale;
            sendData0[10] += rawLAccY / laccScale;
            sendData0[11] += rawLAccZ / laccScale;

            double gravScale = accScale;
            sendData0[12] += rawGravX / gravScale;
            sendData0[13] += rawGravY / gravScale;
            sendData0[14] += rawGravZ / gravScale;

            double quatScale = 16384;
            sendData0[15] += rawQuatX / quatScale;
            sendData0[16] += rawQuatY / quatScale;
            sendData0[17] += rawQuatZ / quatScale;
            sendData0[18] += rawQuatW / quatScale;

            cntReceive ++;

            if(cntReceive >= 5){
                for (int i = 0; i < 19 ; i++){
                    sendData[i] = sendData0[i] / 5.0;
                    sendData0[i] = 0;
                }
                sendMessage(sendData);
                cntReceive = 0;
            }

            //Log.d("SensorData","9axisData:" + sendData[0] + " " + sendData[3] + " " + sendData[6]);


        }
    }


    @Override
    public void onRunError(Exception e) {
        mUsbSerialDriver = null;
        recBytes = new byte[0];
        hUsbDeviceSearch.postDelayed(rUsbDeviceSearch,2000);
    }

}
