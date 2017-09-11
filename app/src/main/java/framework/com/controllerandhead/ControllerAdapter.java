/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: qiyi_framework
 *
 */
package framework.com.controllerandhead;

import android.app.Service;
import android.content.ComponentName;
import com.android.qiyicontroller.AIDLListener;
import com.android.qiyicontroller.AIDLController;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;


public class ControllerAdapter {
    private final static String TAG = "ControllerAdapter";
    private final boolean Debug = true;
    private AIDLController mController;
    private Context mContext;
    private DataTransfer mDataTransfer;
    private SensorManager mSensorManager;
    private Sensor mRotationVector;
    private Quaternion mHeadQuat = Quaternion.identity();
    private Quaternion mHeadQuatOffset = Quaternion.identity();
    private Quaternion mControllerQuat =  Quaternion.identity() ;
    private Quaternion mControllerQuatOffset =  Quaternion.identity() ;


    public ControllerAdapter(Context ctx,DataTransfer transfer) {
        mContext = ctx;
        mDataTransfer = transfer;
    }
    private void Log(String str) {
        if (Debug) Log.d(TAG,str);
    }

    private AIDLListener mCallbackListener = new AIDLListener.Stub() {
            //here we fill all callback module.
            public void quansDataEvent(float x, float y, float z, float w) throws RemoteException
            {
                    Log("<SensorC>[x,y,z,w]:" + x + " ," + y + " ," + z + " ," + w);
                    float buffer[]= new float[4];
                    mControllerQuat.set(w,x,y,z);
                    Quaternion tmpQuat = mControllerQuat.times(mControllerQuatOffset);

                    mDataTransfer.trackControllerData(tmpQuat.x[1],tmpQuat.x[2],tmpQuat.x[3],tmpQuat.x[0]);
            }
            public void shortClickBackEvent(int state) throws android.os.RemoteException {};
            public void clickAppButtonEvent(int state) throws android.os.RemoteException {};
            public void clickAndTriggerEvent(int state) throws android.os.RemoteException {};
            public void batterLevelEvent(int level) throws android.os.RemoteException {};
            public void shakeEvent(int timeStamp, int event, int eventParameter) throws android.os.RemoteException {};
            public void longClickHomeEvent(int state) throws android.os.RemoteException {
                recenter();
                Log("<Home>" + "longPress");
            };
            public void gyroDataEvent(float x, float y, float z) throws android.os.RemoteException {};
            public void accelDataEvent(float x, float y, float z) throws android.os.RemoteException {};
            public void touchDataEvent(float x, float y) throws android.os.RemoteException {};
            public void handDeviceVersionInfoEvent(int appVersion, int deviceVersion, int deviceType) throws android.os.RemoteException {};

    };
    private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                            IBinder service) {
                    Log("Controller service connected");
                    mController = AIDLController.Stub.asInterface(service);
                    try {
                            //register callback here.
                            mController.registerListener(mCallbackListener);
                    } catch (RemoteException e) {
                            e.printStackTrace() ;
                    }
            }
            public void onServiceDisconnected(ComponentName className) {
                    Log("Controller disconnect service");
                    mController = null;
            }
    };

    public void StopAdapter() {
            Log("Stop Controller");
            mContext.unbindService(mConnection);
        mSensorManager.unregisterListener(mSensorEventListener);
    }
    public void
    StartAdapter() {
            Log("Start Controller");
            float buffer[]= new float[4];

            Bundle args = new Bundle();
            Intent intent = new Intent("com.android.qiyicontroller.IAIDLControllerService");
            intent.setPackage("com.google.vr.vrcore");
            intent.setAction("com.android.qiyicontroller.BIND");
            
            //intent.putExtras(args);
            mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mContext.startService(intent);

        mSensorManager = (SensorManager)mContext.getSystemService("sensor");
        mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(mSensorEventListener, mRotationVector, SensorManager.SENSOR_DELAY_NORMAL);
    }
    private boolean recenter(){
        mHeadQuatOffset.clone(mHeadQuat);
        mControllerQuatOffset.clone(mControllerQuat);
        float[] RH = new float[16];
        float[] RC = new float[16];
        float[] HEula = new float[3];
        float[] CEula = new float[3];

        mSensorManager.getRotationMatrixFromVector(RH,mHeadQuatOffset.x);
        mSensorManager.getRotationMatrixFromVector(RC,mControllerQuatOffset.x);

        mSensorManager.getOrientation(RH,HEula);
        mSensorManager.getOrientation(RC,CEula);
        //recenter offset
        //headset direction
        //z change direction from updown to in out
        //y change direction from in out to updown
        //we care about Y AND X axis
        //Y axis
        float axisY[] = {0.0f,1.0f,0.0f};

        //Quaternion HYaw = new Quaternion(0.0f,0.0f,0.0f,0.0f);
        //HYaw.set(HEula[2],axisY);

        //new test
        Quaternion HYaw = Quaternion.identity();
        HYaw.clone(mHeadQuat);
        Quaternion HYawInverse = HYaw.inverse();
        //mHeadQuatOffset = new Quaternion(0,HYawInverse.x[2],0,HYawInverse.x[0]);
        mHeadQuatOffset = HYawInverse ;

        Quaternion CYaw = Quaternion.identity();
        CYaw.clone(mControllerQuat);
        Quaternion CYawInverse = CYaw.inverse();
        //mControllerQuatOffset = new Quaternion(0,CYawInverse.x[2],0,CYawInverse.x[0]);
        mControllerQuatOffset = CYawInverse;;
        return true;
    }
    SensorEventListener mSensorEventListener = new SensorEventListener(){
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType()== Sensor.TYPE_ROTATION_VECTOR){
                Log.d(TAG, "sensordebug TYPE_ROTATION_VECTOR V[0]:"+String.valueOf(event.values[0])+" V[1]:"+String.valueOf(event.values[1])+" V[2]:"+String.valueOf(event.values[2])+" V[3]:"+String.valueOf(event.values[3]));
                float[] quat = new float[4];
                mSensorManager.getQuaternionFromVector(quat,event.values);
                Log("<SensorH>"+ "[w,x,y,z]: " + quat[0] + " ," + quat[1] + " ," + quat[2] + " ," + quat[3]);
                mHeadQuat.set(quat[0],quat[2],quat[1],quat[3]);
                Quaternion tmpQuat = mHeadQuat.times(mHeadQuatOffset);

                mDataTransfer.trackHeadData(tmpQuat.x[1],tmpQuat.x[2],tmpQuat.x[3],tmpQuat.x[0]);
            }
        }
    };
}


