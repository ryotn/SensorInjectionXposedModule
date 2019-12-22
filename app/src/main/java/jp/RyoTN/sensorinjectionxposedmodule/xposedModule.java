package jp.RyoTN.sensorinjectionxposedmodule;

import android.app.ActivityManager;
import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.newInstance;

public class xposedModule implements IXposedHookLoadPackage {

    private Sensor usbGyroSensor,usbMagSensor,usbAccSensor,usbLAccSensor,usbGravSensor,usbQuatSensor;
    private SensorEventListener acc_listener,mag_listener,gyro_listener,lacc_listener,grav_listener,quat_listener;

    private float[] accData = new float[3];
    private float[] gyroData = new float[3];
    private float[] magData = new float[3];
    private float[] laccData = new float[3];
    private float[] gravData = new float[3];
    private float[] quatData = new float[4];


    private UpdateReceiver receiver = null;


    public xposedModule(){

        XposedBridge.log("モジュールが読み込まれた");


        try {
            Class<?> c = Class.forName("android.hardware.Sensor");
            Constructor<?> constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            usbGyroSensor = (Sensor)constructor.newInstance();
            usbMagSensor = (Sensor)constructor.newInstance();
            usbAccSensor = (Sensor)constructor.newInstance();
            usbLAccSensor = (Sensor)constructor.newInstance();
            usbGravSensor = (Sensor)constructor.newInstance();
            usbQuatSensor = (Sensor)constructor.newInstance();
        } catch (ClassNotFoundException e) {
            XposedBridge.log("SensorInjectionModule"+"Error creating USB Sensor: 00");
        }catch (NoSuchMethodException e) {
            XposedBridge.log("SensorInjectionModule"+"Error creating USB Sensor: 01");
        }catch (IllegalAccessException e){
            XposedBridge.log("SensorInjectionModule"+"Error creating USB Sensor: 02");
        }catch(InstantiationException e){
            XposedBridge.log("SensorInjectionModule"+"Error creating USB Sensor: 03");
        }catch(InvocationTargetException e){
            XposedBridge.log("SensorInjectionModule"+"Error creating USB Sensor: 04");
        }
        try {
            //加速度
            Field type_a = usbAccSensor.getClass().getDeclaredField("mType");
            type_a.setAccessible(true);
            type_a.set(usbAccSensor, Sensor.TYPE_ACCELEROMETER);

            Field string_type_a = usbAccSensor.getClass().getDeclaredField("mStringType");
            string_type_a.setAccessible(true);
            string_type_a.set(usbAccSensor, Sensor.STRING_TYPE_ACCELEROMETER);

            Field name_a = usbAccSensor.getClass().getDeclaredField("mName");
            name_a.setAccessible(true);
            name_a.set(usbAccSensor, "USB BNO055 Accelerometer Sensor");

            Field vendor_a = usbAccSensor.getClass().getDeclaredField("mVendor");
            vendor_a.setAccessible(true);
            vendor_a.set(usbAccSensor, "RyoTN");

            Field permission_a = usbAccSensor.getClass().getDeclaredField("mRequiredPermission");
            permission_a.setAccessible(true);
            permission_a.set(usbAccSensor, "android.hardware.sensor.accelerometer");

            Field handle_a = usbAccSensor.getClass().getDeclaredField("mHandle");
            handle_a.setAccessible(true);
            handle_a.set(usbAccSensor, 80);

            //ジャイロ
            Field type_g = usbGyroSensor.getClass().getDeclaredField("mType");
            type_g.setAccessible(true);
            type_g.set(usbGyroSensor, Sensor.TYPE_GYROSCOPE);

            Field string_type_g = usbGyroSensor.getClass().getDeclaredField("mStringType");
            string_type_g.setAccessible(true);
            string_type_g.set(usbGyroSensor, Sensor.STRING_TYPE_GYROSCOPE);

            Field name_g = usbGyroSensor.getClass().getDeclaredField("mName");
            name_g.setAccessible(true);
            name_g.set(usbGyroSensor, "USB BNO055 Gyroscope Sensor");

            Field vendor_g = usbGyroSensor.getClass().getDeclaredField("mVendor");
            vendor_g.setAccessible(true);
            vendor_g.set(usbGyroSensor, "RyoTN");

            Field permission_g = usbGyroSensor.getClass().getDeclaredField("mRequiredPermission");
            permission_g.setAccessible(true);
            permission_g.set(usbGyroSensor, "android.hardware.sensor.gyroscope");

            Field handle_g = usbGyroSensor.getClass().getDeclaredField("mHandle");
            handle_g.setAccessible(true);
            handle_g.set(usbGyroSensor, 81);

            //地磁気
            Field type_m = usbMagSensor.getClass().getDeclaredField("mType");
            type_m.setAccessible(true);
            type_m.set(usbMagSensor, Sensor.TYPE_MAGNETIC_FIELD);

            Field string_type_m = usbMagSensor.getClass().getDeclaredField("mStringType");
            string_type_m.setAccessible(true);
            string_type_m.set(usbMagSensor, Sensor.STRING_TYPE_MAGNETIC_FIELD);

            Field name_m = usbMagSensor.getClass().getDeclaredField("mName");
            name_m.setAccessible(true);
            name_m.set(usbMagSensor, "USB BNO055 Magnetometer Sensor");

            Field vendor_m = usbMagSensor.getClass().getDeclaredField("mVendor");
            vendor_m.setAccessible(true);
            vendor_m.set(usbMagSensor, "RyoTN");

            Field permission_m = usbMagSensor.getClass().getDeclaredField("mRequiredPermission");
            permission_m.setAccessible(true);
            permission_m.set(usbMagSensor, "android.hardware.sensor.magnetometer");

            Field handle_m = usbMagSensor.getClass().getDeclaredField("mHandle");
            handle_m.setAccessible(true);
            handle_m.set(usbMagSensor, 82);

            //LinearAcc
            Field type_la = usbLAccSensor.getClass().getDeclaredField("mType");
            type_la.setAccessible(true);
            type_la.set(usbLAccSensor, Sensor.TYPE_LINEAR_ACCELERATION);

            Field string_type_la = usbLAccSensor.getClass().getDeclaredField("mStringType");
            string_type_la.setAccessible(true);
            string_type_la.set(usbLAccSensor, Sensor.STRING_TYPE_LINEAR_ACCELERATION);

            Field name_la = usbLAccSensor.getClass().getDeclaredField("mName");
            name_la.setAccessible(true);
            name_la.set(usbLAccSensor, "USB BNO055 LinearAccelerometer Sensor");

            Field vendor_la = usbLAccSensor.getClass().getDeclaredField("mVendor");
            vendor_la.setAccessible(true);
            vendor_la.set(usbLAccSensor, "RyoTN");

            Field permission_la = usbLAccSensor.getClass().getDeclaredField("mRequiredPermission");
            permission_la.setAccessible(true);
            permission_la.set(usbLAccSensor, "android.hardware.sensor.linear_accelerometer");

            Field handle_la = usbLAccSensor.getClass().getDeclaredField("mHandle");
            handle_la.setAccessible(true);
            handle_la.set(usbLAccSensor, 83);

            //Gravity
            Field type_grv = usbGravSensor.getClass().getDeclaredField("mType");
            type_grv.setAccessible(true);
            type_grv.set(usbGravSensor, Sensor.TYPE_GRAVITY);

            Field string_type_grv = usbGravSensor.getClass().getDeclaredField("mStringType");
            string_type_grv.setAccessible(true);
            string_type_grv.set(usbGravSensor, Sensor.STRING_TYPE_GRAVITY);

            Field name_grv = usbGravSensor.getClass().getDeclaredField("mName");
            name_grv.setAccessible(true);
            name_grv.set(usbGravSensor, "USB BNO055 Gravity Sensor");

            Field vendor_grv = usbGravSensor.getClass().getDeclaredField("mVendor");
            vendor_grv.setAccessible(true);
            vendor_grv.set(usbGravSensor, "RyoTN");

            Field permission_grv = usbGravSensor.getClass().getDeclaredField("mRequiredPermission");
            permission_grv.setAccessible(true);
            permission_grv.set(usbGravSensor, "android.hardware.sensor.gravity");

            Field handle_grv = usbGravSensor.getClass().getDeclaredField("mHandle");
            handle_grv.setAccessible(true);
            handle_grv.set(usbGravSensor, 84);

            //Quaternion(Rotation Vector)
            Field type_qut = usbQuatSensor.getClass().getDeclaredField("mType");
            type_qut.setAccessible(true);
            type_qut.set(usbQuatSensor, Sensor.TYPE_ROTATION_VECTOR);

            Field string_type_qut = usbQuatSensor.getClass().getDeclaredField("mStringType");
            string_type_qut.setAccessible(true);
            string_type_qut.set(usbQuatSensor, Sensor.STRING_TYPE_ROTATION_VECTOR);

            Field name_qut = usbQuatSensor.getClass().getDeclaredField("mName");
            name_qut.setAccessible(true);
            name_qut.set(usbQuatSensor, "USB BNO055 Quaternion Sensor");

            Field vendor_qut = usbQuatSensor.getClass().getDeclaredField("mVendor");
            vendor_qut.setAccessible(true);
            vendor_qut.set(usbQuatSensor, "RyoTN");

            Field permission_qut = usbQuatSensor.getClass().getDeclaredField("mRequiredPermission");
            permission_qut.setAccessible(true);
            permission_qut.set(usbQuatSensor, "android.hardware.sensor.rotation_vector");

            Field handle_qut = usbQuatSensor.getClass().getDeclaredField("mHandle");
            handle_qut.setAccessible(true);
            handle_qut.set(usbQuatSensor, 85);

        }catch(NoSuchFieldException e){
            XposedBridge.log("SensorInjectionModule:"+"Error creating USB Sensor: 05");
        }catch(IllegalAccessException e){
            XposedBridge.log("SensorInjectionModule:"+"Error creating USB Sensor: 06");
        }




    }
    public void update(){


        //Creating SensorEvent instance and setting its variables, then calling onSensorChanged
        Constructor[] ctors = SensorEvent.class.getDeclaredConstructors();
        Constructor ctor_gyro = null;
        Constructor ctor_mag = null;
        Constructor ctor_acc = null;
        Constructor ctor_lacc = null;
        Constructor ctor_grav = null;
        Constructor ctor_quat = null;
        for (int i = 0; i < ctors.length; i++) {
            ctor_gyro = ctors[i];
            ctor_mag = ctors[i];
            ctor_acc = ctors[i];
            ctor_lacc = ctors[i];
            ctor_grav = ctors[i];
            ctor_quat = ctors[i];
            if (ctor_gyro.getGenericParameterTypes().length == 1)
                break;
        }
        try {
            ctor_gyro.setAccessible(true);
            ctor_mag.setAccessible(true);
            ctor_acc.setAccessible(true);
            ctor_lacc.setAccessible(true);
            ctor_grav.setAccessible(true);
            ctor_quat.setAccessible(true);

            SensorEvent event_gyro = (SensorEvent)ctor_gyro.newInstance(3);
            System.arraycopy(gyroData,0,event_gyro.values,0,event_gyro.values.length);
            event_gyro.timestamp = System.nanoTime();
            event_gyro.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            event_gyro.sensor = usbGyroSensor;
            if(gyro_listener != null)gyro_listener.onSensorChanged(event_gyro);

            SensorEvent event_mag = (SensorEvent)ctor_mag.newInstance(3);
            System.arraycopy(magData,0,event_mag.values,0,event_mag.values.length);
            event_mag.timestamp = System.nanoTime();
            event_mag.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            event_mag.sensor = usbMagSensor;
            if(mag_listener != null)mag_listener.onSensorChanged(event_mag);

            SensorEvent event_acc = (SensorEvent)ctor_acc.newInstance(3);
            System.arraycopy(accData,0,event_acc.values,0,event_acc.values.length);
            event_acc.timestamp = System.nanoTime();
            event_acc.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            event_acc.sensor = usbAccSensor;
            if(acc_listener != null)acc_listener.onSensorChanged(event_acc);

            SensorEvent event_lacc = (SensorEvent)ctor_lacc.newInstance(3);
            System.arraycopy(laccData,0,event_lacc.values,0,event_lacc.values.length);
            event_lacc.timestamp = System.nanoTime();
            event_lacc.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            event_lacc.sensor = usbLAccSensor;
            if(lacc_listener != null)lacc_listener.onSensorChanged(event_lacc);

            SensorEvent event_grav = (SensorEvent)ctor_grav.newInstance(3);
            System.arraycopy(gravData,0,event_grav.values,0,event_grav.values.length);
            event_grav.timestamp = System.nanoTime();
            event_grav.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            event_grav.sensor = usbGravSensor;
            if(grav_listener != null)grav_listener.onSensorChanged(event_grav);

            SensorEvent event_quat = (SensorEvent)ctor_quat.newInstance(4);
            System.arraycopy(quatData,0,event_quat.values,0,event_quat.values.length);
            event_quat.timestamp = System.nanoTime();
            event_quat.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            event_quat.sensor = usbQuatSensor;
            if(quat_listener != null)quat_listener.onSensorChanged(event_quat);

        }catch(IllegalAccessException e){
            Log.e("SensorInjectionModule", "Error updating sensor: 01");
        }catch(InvocationTargetException e){
            Log.e("SensorInjectionModule", "Error updating sensor: 02");
        }catch(InstantiationException e){
            Log.e("SensorInjectionModule", "Error updating sensor: 03");
        }

    }

    private static Context getContext() {
        return (Context) AndroidAppHelper.currentApplication();
    }
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("Loaded app: " + lpparam.packageName);

        if(lpparam.packageName.equals("com.google.android.gms"))return;


        try{
            final Class<?> sensorEQ = findClass(
                    "android.hardware.SystemSensorManager$SensorEventQueue",
                    lpparam.classLoader);
            final Class<?> sensorSMGR = findClass(
                    "android.hardware.SystemSensorManager",
                    lpparam.classLoader);
            final Class<?> sensorMGR = findClass(
                    "android.hardware.SensorManager",
                    lpparam.classLoader);
            XposedBridge.hookAllConstructors(sensorSMGR, new
                    XC_MethodHook() {
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws
                                Throwable {
                            try {
                                Field list = param.thisObject.getClass().getDeclaredField("mFullSensorsList");
                                list.setAccessible(true);

                                ArrayList<Sensor> list_inst = new ArrayList<Sensor>();
                                Object list_val = list.get(param.thisObject);
                                Method add_method = list_val.getClass().getDeclaredMethod("add", Object.class);
                                Method set_method = list_val.getClass().getDeclaredMethod("set",int.class, Object.class);
                                Method size_method = list_val.getClass().getDeclaredMethod("size");
                                Method get_method = list_val.getClass().getDeclaredMethod("get",int.class);
                                int size = (int)size_method.invoke(list_val);

                                boolean flg_acc = false;
                                boolean flg_gyro = false;
                                boolean flg_mag = false;
                                boolean flg_lacc = false;
                                boolean flg_grav = false;
                                boolean flg_quat = false;

                                for(int cnt = 0;cnt < size ; cnt ++){
                                    Sensor ss = (Sensor) get_method.invoke(list_val,cnt);
                                    switch (ss.getType()){
                                        case Sensor.TYPE_ACCELEROMETER:
                                        //case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                                            set_method.invoke(list_val,cnt,usbAccSensor);
                                            flg_acc = true;
                                            break;
                                        case Sensor.TYPE_GYROSCOPE:
                                        //case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                                            set_method.invoke(list_val,cnt,usbGyroSensor);
                                            flg_gyro = true;
                                            break;
                                        case Sensor.TYPE_MAGNETIC_FIELD:
                                        //case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                                            set_method.invoke(list_val,cnt,usbMagSensor);
                                            flg_mag = true;
                                            break;
                                        case Sensor.TYPE_LINEAR_ACCELERATION:
                                            //case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                                            set_method.invoke(list_val,cnt,usbLAccSensor);
                                            flg_lacc = true;
                                            break;
                                        case Sensor.TYPE_GRAVITY:
                                            //case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                                            set_method.invoke(list_val,cnt,usbGravSensor);
                                            flg_grav = true;
                                            break;
                                        case Sensor.TYPE_ROTATION_VECTOR:
                                            //case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                                            set_method.invoke(list_val,cnt,usbQuatSensor);
                                            flg_quat = true;
                                            break;
                                    }
                                }

                                if(!flg_acc)add_method.invoke(list_val,usbAccSensor);
                                if(!flg_gyro)add_method.invoke(list_val,usbGyroSensor);
                                if(!flg_mag)add_method.invoke(list_val,usbMagSensor);
                                if(!flg_lacc)add_method.invoke(list_val,usbLAccSensor);
                                if(!flg_grav)add_method.invoke(list_val,usbGravSensor);
                                if(!flg_quat)add_method.invoke(list_val,usbQuatSensor);


                                Log.e("SensorInjectionModule", "USB Sensor injected!");
                            }catch(NoSuchFieldException e){
                                Log.e("SensorInjectionModule", "Error injecting Sensor: 01");
                            }catch(IllegalAccessException e){
                                Log.e("SensorInjectionModule", "Error injecting Sensor: 02");
                            }
                        }
                    });

            XposedBridge.hookAllMethods(sensorMGR, "registerListener", new
                    XC_MethodHook() {
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws
                                Throwable {
                            if(((Sensor)param.args[1]).getType()==Sensor.TYPE_GYROSCOPE||((Sensor)param.args[1]).getType()==Sensor.TYPE_GYROSCOPE_UNCALIBRATED){
                                gyro_listener = (SensorEventListener)param.args[0];
                                param.setResult(true);

                                setUpdateReceiver(true);

                            }
                            if(((Sensor)param.args[1]).getType()==Sensor.TYPE_ACCELEROMETER||((Sensor)param.args[1]).getType()==Sensor.TYPE_ACCELEROMETER_UNCALIBRATED){
                                acc_listener = (SensorEventListener)param.args[0];
                                //usbAccSensor = (Sensor)param.args[1];
                                param.setResult(true);

                                setUpdateReceiver(true);
                            }
                            if(((Sensor)param.args[1]).getType()==Sensor.TYPE_MAGNETIC_FIELD||((Sensor)param.args[1]).getType()==Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED){
                                mag_listener = (SensorEventListener)param.args[0];
                                param.setResult(true);

                                setUpdateReceiver(true);
                            }

                            if(((Sensor)param.args[1]).getType()==Sensor.TYPE_LINEAR_ACCELERATION){
                                lacc_listener = (SensorEventListener)param.args[0];
                                param.setResult(true);

                                setUpdateReceiver(true);
                            }
                            if(((Sensor)param.args[1]).getType()==Sensor.TYPE_GRAVITY){
                                grav_listener = (SensorEventListener)param.args[0];
                                param.setResult(true);

                                setUpdateReceiver(true);
                            }
                            if(((Sensor)param.args[1]).getType()==Sensor.TYPE_ROTATION_VECTOR){
                                quat_listener = (SensorEventListener)param.args[0];
                                param.setResult(true);

                                setUpdateReceiver(true);
                            }
                        }
                    });
            XposedBridge.hookAllMethods(sensorMGR, "unregisterListener", new
                    XC_MethodHook() {
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws
                                Throwable {
                            if(param.args.length==2 && param.args[1] instanceof Sensor && (((Sensor)param.args[1]).getType()==Sensor.TYPE_GYROSCOPE||((Sensor)param.args[1]).getType()==Sensor.TYPE_GYROSCOPE_UNCALIBRATED)){

                                setUpdateReceiver(false);
                            }
                            if(param.args.length==2 && param.args[1] instanceof Sensor && (((Sensor)param.args[1]).getType()==Sensor.TYPE_ACCELEROMETER||((Sensor)param.args[1]).getType()==Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)){

                                setUpdateReceiver(false);
                            }
                            if(param.args.length==2 && param.args[1] instanceof Sensor && (((Sensor)param.args[1]).getType()==Sensor.TYPE_MAGNETIC_FIELD||((Sensor)param.args[1]).getType()==Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)){

                                setUpdateReceiver(false);
                            }

                            if(param.args.length==2 && param.args[1] instanceof Sensor && (((Sensor)param.args[1]).getType()==Sensor.TYPE_LINEAR_ACCELERATION)){

                                setUpdateReceiver(false);
                            }

                            if(param.args.length==2 && param.args[1] instanceof Sensor && (((Sensor)param.args[1]).getType()==Sensor.TYPE_GRAVITY)){

                                setUpdateReceiver(false);
                            }

                            if(param.args.length==2 && param.args[1] instanceof Sensor && (((Sensor)param.args[1]).getType()==Sensor.TYPE_ROTATION_VECTOR)){

                                setUpdateReceiver(false);
                            }
                        }
                    });

        } catch (Throwable t) {
            Log.e("SensorInjectionModule", "Exception in SystemSensorEvent hook: " + t.getMessage());
            // Do nothing
        }
    }

    private void setUpdateReceiver(boolean on){
        Context ctx = getContext();
        if(on){
            if(receiver == null){
                receiver = new UpdateReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction( "Usb9axisSensorReceiver" );
                ctx.registerReceiver(receiver, filter);

                XposedBridge.log("receiver 登録");

                Intent intent = new Intent();
                intent.setAction("START");
                intent.setClassName("jp.RyoTN.sensorinjectionxposedmodule",
                        "jp.RyoTN.sensorinjectionxposedmodule.Usb9axisReceiverService");
                ctx.startService(intent);
            }

        }else{
            if(receiver != null){
                ctx.unregisterReceiver(receiver);
                receiver = null;

                XposedBridge.log("receiver 解除");
            }
        }
    }

    protected class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            Bundle extras = intent.getExtras();
            double[] data = extras.getDoubleArray("data");

            accData[0] = (float)data[0];
            accData[1] = (float)data[1];
            accData[2] = (float)data[2];

            gyroData[0] = (float)data[3];
            gyroData[1] = (float)data[4];
            gyroData[2] = (float)data[5];

            magData[0] = (float)data[6];
            magData[1] = (float)data[7];
            magData[2] = (float)data[8];

            laccData[0] = (float)data[9];
            laccData[1] = (float)data[10];
            laccData[2] = (float)data[11];

            gravData[0] = (float)data[12];
            gravData[1] = (float)data[13];
            gravData[2] = (float)data[14];

            quatData[0] = (float)data[15];
            quatData[1] = (float)data[16];
            quatData[2] = (float)data[17];
            quatData[3] = (float)data[18];

            update();
        }
    }
}
