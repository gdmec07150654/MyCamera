package com.gdmec.mycamera;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.PixelFormat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener,SurfaceHolder.Callback {

    private SurfaceView mSurfaceView; // 相机视频浏览
    private ImageView mImageView; // 图片
    private SurfaceHolder mSurfaceHolder;
    private ImageView shutter; // 快照按钮
    private android.hardware.Camera mCamera; // 相机
    private boolean mPreViewRunning; // 运行相机浏览
    private static final int MENU_START = 1;
    private static final int MENU_SENSOR = 2;
    private Bitmap bitmap; // 相片bitmap

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.layout_camera);
        mSurfaceView = (SurfaceView) findViewById(R.id.camera);
        mImageView = (ImageView) findViewById(R.id.image);
        shutter = (ImageView) findViewById(R.id.shutter);
        // 设置快照按钮事件
        shutter.setOnClickListener(this);
        mImageView.setVisibility(View.GONE);
        mSurfaceHolder = mSurfaceView.getHolder();
        // 设置SurfaceHolder回调事件
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    // 快照按钮事件
    @Override
    public void onClick(View v) {
        // 判断是否可以进行拍照
        shutter.setEnabled(false);
        // 使用安卓虚拟机调试时，因为没有自动对焦特性，所以需要在这里调用拍照方法。
        // mCamera.takePicture(mShutterCallback, null, mPictureCallback);
        // 设置自动对焦
        mCamera.autoFocus(new android.hardware.Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, android.hardware.Camera camera) {
                // 聚焦后进行拍照
                mCamera.takePicture(mShutterCallback, null, mPictureCallback);
            }
        });
    }

    // 相机图片拍照函数
    android.hardware.Camera.PictureCallback mPictureCallback = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            // 确认相片数据是否不为空
            if(data != null){
                savaAndShow(data);
            }
        }
    };

    // 快照回调函数
    android.hardware.Camera.ShutterCallback mShutterCallback = new android.hardware.Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            System.out.println("快照回调函数");
        }
    };

    // SurfaceView创建时调用
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setCameraParams();
    }

    private void setCameraParams() {
        if(mCamera != null){
            return ;
        }
        // 创建相机
        mCamera = android.hardware.Camera.open();
        // 设置相机参数
        android.hardware.Camera.Parameters params = mCamera.getParameters();
        // 拍照自动对焦
        //params.autoFocus(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
        params.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
        // 设置预览帧速度
        params.setPreviewFrameRate(3);
        // 设置预览格式
        params.setPreviewFormat(PixelFormat.YCbCr_422_SP);
        // 设置图片质量百分比
        params.set("jpeg-quality", 85);
        // 获取相机支持图片分辨率
        List<android.hardware.Camera.Size> list = params.getSupportedPictureSizes();
        android.hardware.Camera.Size size = list.get(0);
        int w = size.width;
        int h = size.height;
        // 设置图片大小
        params.setPictureSize(w, h);
        // 设置自动闪光灯
        params.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_AUTO);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_START, 0, "重拍");
        menu.add(0, MENU_SENSOR, 0, "打开相册");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == MENU_START){
            // 重启相机拍照
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            return true;
        }else if(item.getItemId() == MENU_SENSOR){
            Intent intent = new Intent(this, AlbumActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // SurfaceView改变时调用
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            // 判断是否运行相机，运行就停止
            if (mPreViewRunning) {
                mCamera.stopPreview();
            }
            // 启动相机
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mPreViewRunning = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // SurfaceView消亡时调用
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(mCamera != null){
            // 停止相机预览
            mCamera.stopPreview();
            mPreViewRunning = false;
            // 回收相机
            mCamera.release();
            mCamera = null;
        }
    }

    // 保存和显示图片
    public void savaAndShow(byte[] data){
        try {
            // 图片ID
            String imageId = System.currentTimeMillis() + "";
            // 相片保存路径
            String pathName = android.os.Environment.getExternalStorageDirectory().getPath() + "/mycamera";
            // 创建文件
            File file = new File(pathName);
            if (!file.exists()) {
                file.mkdir();
            }
            // 创建文件
            pathName += "/" + imageId + ".jpeg";
            file = new File(pathName);
            if (!file.exists()) {
                file.createNewFile(); // 文件不存在则新建
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            AlbumActivity album = new AlbumActivity();
            // 读取相片Bitmap
            bitmap = album.loadImage(pathName);
            // 设置到控件上显示
            mImageView.setImageBitmap(bitmap);
            mImageView.setVisibility(View.VISIBLE);
            mSurfaceView.setVisibility(View.GONE);
            // 停止相机预览
            if (mPreViewRunning) {
                mCamera.stopPreview();
                mPreViewRunning = false;
            }
            shutter.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
