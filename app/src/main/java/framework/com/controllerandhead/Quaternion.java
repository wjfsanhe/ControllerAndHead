package framework.com.controllerandhead;

import android.util.Log;

/**
 * Created by wangjf on 9/7/17.
 */

public class Quaternion {
    public final float[] x = new float[4]; // w,x,y,z,
    private final boolean Debug = true;
    private final  String TAG = "ControllerAndHead" ;

    public void set(float w, float x, float y, float z) {
        this.x[0] = w;
        this.x[1] = x;
        this.x[2] = y;
        this.x[3] = z;
    }
    private void Log(String str) {
        if (Debug) Log.d(TAG,str);
    }
    public void clone(Quaternion src) {
        System.arraycopy(src.x, 0, x, 0, x.length);
    }
    public void dump(String title){
        Log(title + "[w,x,y,z]: " + x[0] + " ," + x[1] + " ," + x[2] + " ," + x[3]);
    }

    private static float[] cross(float[] a, float[] b) {
        float out0 = a[1] * b[2] - b[1] * a[2];
        float out1 = a[2] * b[0] - b[2] * a[0];
        float out2 = a[0] * b[1] - b[0] * a[1];
        return new float[]{out0, out1, out2};
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float[] normal(float[] a) {
        float norm =(float) Math.sqrt(dot(a, a));
        return new float[]{a[0] / norm, a[1] / norm, a[2] / norm};
    }

    public void set(float[] v1, float[] v2) {
        float[] vec1 = normal(v1);
        float[] vec2 = normal(v2);
        float[] axis = normal(cross(vec1, vec2));
        float angle = (float)Math.acos(dot(vec1, vec2));
        set(angle, axis);
    }
    public static Quaternion identity(){
        return (new Quaternion(1.0f,0.0f,0.0f,0.0f));
    }
    public static float calcAngle(float[] v1, float[] v2) {
        float[] vec1 = normal(v1);
        float[] vec2 = normal(v2);
        return (float)Math.acos(Math.min(dot(vec1, vec2), 1));
    }

    public static float[] calcAxis(float[] v1, float[] v2) {
        float[] vec1 = normal(v1);
        float[] vec2 = normal(v2);
        return normal(cross(vec1, vec2));
    }

    public void set(float angle, float[] axis) {
        x[0] = (float)Math.cos(angle / 2);
        float sin =(float)Math.sin(angle / 2);
        x[1] = axis[0] * sin;
        x[2] = axis[1] * sin;
        x[3] = axis[2] * sin;
    }

    public Quaternion(float x0, float x1, float x2, float x3) {
        x[0] = x0;
        x[1] = x1;
        x[2] = x2;
        x[3] = x3;
    }

    public Quaternion conjugate() {
        return new Quaternion(x[0], -x[1], -x[2], -x[3]);
    }

    public Quaternion plus(Quaternion b) {
        Quaternion a = this;
        return new Quaternion(a.x[0] + b.x[0], a.x[1] + b.x[1], a.x[2] + b.x[2], a.x[3] + b.x[3]);
    }

    public Quaternion times(Quaternion b) {
        Quaternion a = this;
        float y0 = a.x[0] * b.x[0] - a.x[1] * b.x[1] - a.x[2] * b.x[2] - a.x[3] * b.x[3];
        float y1 = a.x[0] * b.x[1] + a.x[1] * b.x[0] + a.x[2] * b.x[3] - a.x[3] * b.x[2];
        float y2 = a.x[0] * b.x[2] - a.x[1] * b.x[3] + a.x[2] * b.x[0] + a.x[3] * b.x[1];
        float y3 = a.x[0] * b.x[3] + a.x[1] * b.x[2] - a.x[2] * b.x[1] + a.x[3] * b.x[0];
        return new Quaternion(y0, y1, y2, y3);
    }

    public Quaternion inverse() {
        float d = x[0] * x[0] + x[1] * x[1] + x[2] * x[2] + x[3] * x[3];
        return new Quaternion(x[0] / d, -x[1] / d, -x[2] / d, -x[3] / d);
    }

    public Quaternion divides(Quaternion b) {
        Quaternion a = this;
        return a.inverse().times(b);
    }


    public float[] rotateVec(float[] v) {

        float v0 = v[0];
        float v1 = v[1];
        float v2 = v[2];

        float s = x[1] * v0 + x[2] * v1 + x[3] * v2;

        float n0 = 2 * (x[0] * (v0 * x[0] - (x[2] * v2 - x[3] * v1)) + s * x[1]) - v0;
        float n1 = 2 * (x[0] * (v1 * x[0] - (x[3] * v0 - x[1] * v2)) + s * x[2]) - v1;
        float n2 = 2 * (x[0] * (v2 * x[0] - (x[1] * v1 - x[2] * v0)) + s * x[3]) - v2;

        return new float[]{n0, n1, n2};

    }

    public float[] matrix() {
        float xx = x[1] * x[1];
        float xy = x[1] * x[2];
        float xz = x[1] * x[3];
        float xw = x[1] * x[0];

        float yy = x[2] * x[2];
        float yz = x[2] * x[3];
        float yw = x[2] * x[0];

        float zz = x[3] * x[3];
        float zw = x[3] * x[0];
        float[] m = new float[16];
        m[0] = 1 - 2 * (yy + zz);
        m[1] = 2 * (xy - zw);
        m[2] = 2 * (xz + yw);

        m[4] = 2 * (xy + zw);
        m[5] = 1 - 2 * (xx + zz);
        m[6] = 2 * (yz - xw);

        m[8] = 2 * (xz - yw);
        m[9] = 2 * (yz + xw);
        m[10] = 1 - 2 * (xx + yy);

        m[3] = m[7] = m[11] = m[12] = m[13] = m[14] = 0;
        m[15] = 1;
        return m;
    }
}
