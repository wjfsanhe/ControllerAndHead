package framework.com.controllerandhead;

import javax.microedition.khronos.egl.EGLConfig;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.sql.Struct;

import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements DataTransfer {

    // Used to load the 'native-lib' library on application startup.
    private final  String TAG = "ControllerAndHead" ;
    private boolean Debug = true;
    ControllerAdapter mControllerAdapter;
    private Quaternion mHeadQuat = new Quaternion(0,0,0,0);
    private Quaternion mControllerQuat = new Quaternion(0,0,0,0);;

    //render relative part
    private GLSurfaceView mGLSurfaceView;
    private MyRenderer mRenderer;


    static {
        System.loadLibrary("native-lib");
    }
    private void Log(String str) {
        if (Debug) Log.d(TAG,str);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());*/
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mRenderer = new MyRenderer();
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setRenderer(mRenderer);
        setContentView(mGLSurfaceView);
        mControllerAdapter = new ControllerAdapter(getBaseContext(),this);
    }
    @Override
    public boolean trackControllerData(float x, float y, float z, float w){
        Log("<Controller>[x,y,z,w]:" + x + " ," + y + " ," + w);
        mControllerQuat.set(w,x,y,z);

        return true;
        //return setControllerQuat(x,y,z,w);
    }
    @Override
    public boolean trackHeadData(float x, float y, float z, float w){
        mHeadQuat.set(w,x,y,z);
        mHeadQuat.dump("TrackerH");
        mControllerQuat.dump("TrackerC");
        mRenderer.setMatrixH(mHeadQuat);
        mRenderer.setMatrixC(mControllerQuat);
        return true;
        //return setControllerQuat(x,y,z,w);
    }

    @Override
    public void onResume(){
        super.onResume();
        mControllerAdapter.StartAdapter();
    }
    @Override
    public void onPause(){
        super.onPause();
        mControllerAdapter.StopAdapter();

    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    //render relative part
    class MyRenderer implements GLSurfaceView.Renderer{
        private  float[] mRotationMatrixH ;
        private  float[] mRotationMatrixC ;
        private Cube mCube;
        private Cube mCube2;
        public MyRenderer() {

            // find the rotation-vector sensor
            textureInit();
            mCube = new Cube();
            mCube2 = new Cube();
            // initialize the rotation matrix to identity
            setMatrixH(Quaternion.identity());
            setMatrixC(Quaternion.identity());
        }
        public void setMatrixH(Quaternion quatH){
            mRotationMatrixH = quatH.matrix();
        }
        public void setMatrixC(Quaternion quatC){
            mRotationMatrixC = quatC.matrix();
        }
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // set view-port
            gl.glViewport(0, 0, width, height);
            // set projection matrix
            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
        }
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // dither is enabled by default, we don't need it
            gl.glDisable(GL10.GL_DITHER);
            // clear screen in white
            gl.glClearColor(1,1,1,1);
        }

        public void onDrawFrame(GL10 gl) {
            //drawGrid(gl);

                // clear screen
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            // set-up modelview matrix
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(3, 0, -6f);
            gl.glMultMatrixf(mRotationMatrixH, 0);


            // draw our object
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

            mCube.draw(gl);

            gl.glLoadIdentity();
            gl.glTranslatef(-3, 0, -6f);
            gl.glMultMatrixf(mRotationMatrixC, 0);
            //gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            //gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            mCube2.draw(gl);


        }
        public void drawGrid(GL10 gl) {
            textureId = loadTexture("timg.jpg",gl);
            //定义显示在屏幕上的什么位置(opengl 自动转换)
            gl.glViewport(0, 0, mGLSurfaceView.getWidth(), mGLSurfaceView.getHeight());
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(-160, 160, -240, 240, 1, -1);

            gl.glEnable(GL10.GL_TEXTURE_2D);
            //绑定纹理ID
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertices);

            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texture);
            // gl.glRotatef(1, 0, 1, 0);
            gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 6,
                    GL10.GL_UNSIGNED_SHORT, indices);
        }
        class Cube {
            // initialize our cube
            private FloatBuffer mVertexBuffer;
            private FloatBuffer mColorBuffer;
            private ByteBuffer mIndexBuffer;

            public Cube() {
                final float vertices[] = {
                        -1, -1, -1,		 1, -1, -1,
                        1,  1, -1,	    -1,  1, -1,
                        -1, -1,  1,      1, -1,  1,
                        1,  1,  1,     -1,  1,  1,
                };

                final float colors[] = {
                        0,  0,  0,  1,  1,  0,  0,  1,
                        1,  1,  0,  1,  0,  1,  0,  1,
                        0,  0,  1,  1,  1,  0,  1,  1,
                        1,  1,  1,  1,  0,  1,  1,  1,
                };

                final byte indices[] = {
                        0, 4, 5,    0, 5, 1,
                        1, 5, 6,    1, 6, 2,
                        2, 6, 7,    2, 7, 3,
                        3, 7, 4,    3, 4, 0,
                        4, 7, 6,    4, 6, 5,
                        3, 0, 1,    3, 1, 2
                };

                ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
                vbb.order(ByteOrder.nativeOrder());
                mVertexBuffer = vbb.asFloatBuffer();
                mVertexBuffer.put(vertices);
                mVertexBuffer.position(0);

                ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
                cbb.order(ByteOrder.nativeOrder());
                mColorBuffer = cbb.asFloatBuffer();
                mColorBuffer.put(colors);
                mColorBuffer.position(0);

                mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
                mIndexBuffer.put(indices);
                mIndexBuffer.position(0);
            }

            public void draw(GL10 gl) {
                gl.glEnable(GL10.GL_CULL_FACE);
                gl.glFrontFace(GL10.GL_CW);
                gl.glShadeModel(GL10.GL_SMOOTH);
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
                gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);
                gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
            }

        }
        public int loadTexture(String fileName,GL10 gl) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(getAssets().open(
                        fileName));
                int textureIds[] = new int[1];
                gl.glGenTextures(1, textureIds, 0);
                int textureId = textureIds[0];
                gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
                GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                        GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                        GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
                gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
                bitmap.recycle();
                return textureId;
            } catch (IOException e) {
                Log.d("TexturedRectangleTest",
                        "couldn't load asset 'bobrgb888.png'!");
                throw new RuntimeException("couldn't load asset '" + fileName
                        + "'");
            }
        }
        FloatBuffer vertices;
        FloatBuffer texture;
        ShortBuffer indices;
        int textureId;
        public void textureInit() {

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 2 * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            vertices = byteBuffer.asFloatBuffer();
//            vertices.put( new float[] {  -80f,   -120f,0,1f,
//                                         80f,  -120f, 1f,1f,
//                                         -80f, 120f, 0f,0f,
//                                         80f,120f,   1f,0f});
            vertices.put( new float[] {  -80f,   -120f,
                    80f,  -120f,
                    -80f, 120f,
                    80f,  120f});

            ByteBuffer indicesBuffer = ByteBuffer.allocateDirect(6 * 2);
            indicesBuffer.order(ByteOrder.nativeOrder());
            indices = indicesBuffer.asShortBuffer();
            indices.put(new short[] { 0, 1, 2,1,2,3});

            ByteBuffer textureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4);
            textureBuffer.order(ByteOrder.nativeOrder());
            texture = textureBuffer.asFloatBuffer();
            texture.put( new float[] { 0,1f,
                    1f,1f,
                    0f,0f,
                    1f,0f});

            indices.position(0);
            vertices.position(0);
            texture.position(0);

        }
    }
}
