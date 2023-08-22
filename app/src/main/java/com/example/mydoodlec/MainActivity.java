package com.example.mydoodlec;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private ImageView imageView;
    private View drawingView;

    private Bitmap originalBitmap;
    private Bitmap mutableBitmap;

    private Matrix matrix = new Matrix();//对图片进行移动和缩放变换的矩阵
    private PointF startPoint = new PointF();

    int img_w;
    int img_h;
    int view_w;
    int view_h;
    float init_ratio;//显示图片时，根据图片尺寸、view尺寸，进行缩放，使图片可以完全显示出来
    float total_ratio = 1;//当前缩放的比例
    float current_ratio;//当前缩放的比例
    float distance1;
    float distance2;
    float currentX;//当前x移动的坐标
    float currentY;//当前y移动的坐标
    float nextX;//下一次x移动的坐标
    float nextY;//下一次y移动的坐标
    float disX;//disX = nextX - currentX; 获取移动的X距离
    float disY;//获取移动的Y距离
    float current_disX = 0;//记录当前X移动的距离
    float current_disY = 0;//记录当前Y移动的距离
    boolean move = true;//判断当前是移动还是涂鸦
    private static final int RESULT_LOAD_IMAGE = 1;//判定当前任务是加载图片
    private String mPicturePath = "";//定义文件路径
    boolean two_pointer_down = false;
    private Canvas drawingCanvas;
    private Paint paint;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //从系统相册选择图片 YC：获取选中图片的路径，用于上传
        final Button select = (Button) findViewById(R.id.btn_select_a_image);
        select.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);


                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        Button suoxiao = findViewById(R.id.suoxiao);
        suoxiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suoxiao_img();
            }
        });

        Button fangda = findViewById(R.id.fangda);
        fangda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fangda_img();
            }
        });

        Button Bmove = findViewById(R.id.move);
        Bmove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(move) {
                    move = false;
                }
                else {
                    move = true;
                }
            }
        });

        Button Save = findViewById(R.id.save);
        Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage();
            }
        });

//        imageView = findViewById(R.id.image_view);
//        drawingView = findViewById(R.id.drawing_view);
//
//        // 设置触摸事件监听器
//        imageView.setOnTouchListener(this);
//        drawingView.setOnTouchListener(this);
//
//        // 加载原始图片
//        originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.zhiyin);
//        //originalBitmap = BitmapFactory.decodeFile("/path/to/your/image.png");
//
//        // 创建与原始图片大小相匹配的Mutable Bitmap
//        mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
//
//        // 创建画布并与Mutable Bitmap关联
//        drawingCanvas = new Canvas(mutableBitmap);
//
//        // 初始化画笔
//        paint = new Paint();
//        paint.setColor(Color.RED);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(5f);
//
//        // 在ImageView中显示原始图片
//        imageView.setImageBitmap(originalBitmap);
//        img_w = originalBitmap.getWidth();
//        img_h = originalBitmap.getHeight();
//        Log.d(TAG, "onCreate: img_w = "+img_w+"    img_h = "+img_h);
//        view_w = 1080;//视图 宽
//        view_h = 2124;//视图 高
////        int view_w = imageView.getWidth();
////        int view_h = imageView.getHeight();
//        Log.d(TAG, "onCreate: view_w = "+view_w+"    view_h = "+view_h);
//
////        if(img_w<img_h)
////        {
//            init_ratio = (1080*(1.0f))/(img_w*(1.0f));
//            Log.d(TAG, "aaaaaaaaaaaaaaaaaaaaa init_ratio = "+init_ratio);
//            matrix.reset();
//            matrix.postScale(init_ratio,init_ratio);
//            matrix.postTranslate(0, 0);
//            imageView.setImageMatrix(matrix);
////        }
        img_open(null);
    }

    public void fangda_img(){
        total_ratio = total_ratio*10/9;
        Log.d(TAG, "fangda_img: drawingCanvas w = "+drawingCanvas.getWidth()+"    h = "+drawingCanvas.getHeight());
        matrix.reset();
        matrix.postScale(init_ratio*total_ratio,init_ratio*total_ratio);
        matrix.postTranslate(disX+current_disX, disY+current_disY);
        imageView.setImageMatrix(matrix);
    }

    public void suoxiao_img(){
        total_ratio = total_ratio*9/10;
        matrix.reset();
        matrix.postScale(init_ratio*total_ratio,init_ratio*total_ratio);
        matrix.postTranslate(disX+current_disX, disY+current_disY);
        imageView.setImageMatrix(matrix);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "onTouch: event.getPointerCount() = "+event.getPointerCount());
        Log.d(TAG, "onTouch: event.getAction() = "+event.getAction());
        if (event.getPointerCount() == 2) {
            two_pointer_down = true;
            Log.d(TAG, "onTouch: ~~~~~~~~~~~~~~~~~~~~~~~");
            //处理双指缩放
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if(distance1 ==0)
                    {
                        distance1 = getDistance(event);
                    }
                    current_ratio = getDistance(event)/distance1;
                    Log.d(TAG, "onTouch: distance1 = "+distance1);
                    distance1 = getDistance(event);

                    Log.d(TAG, "onTouch: current_ratio = "+current_ratio);
                    total_ratio = current_ratio*total_ratio;
                    Log.d(TAG, "onTouch: total_ratio = "+total_ratio);

                    float centerX = (event.getX(0)+event.getX(1))/2;
                    float centerY = (event.getY(0)+event.getY(1))/2;
                    Log.d(TAG, "onTouch: centerX = "+centerX);
                    Log.d(TAG, "onTouch: centerY = "+centerY);

//                    disX = centerX/total_ratio;
//                    disY = centerY/total_ratio;

                    matrix.reset();
                    matrix.postScale(init_ratio*total_ratio,init_ratio*total_ratio);
                    float translateX = 0f;
                    float translateY = 0f;
                    translateX = (current_disX*current_ratio)+centerX*(1-current_ratio);
                    translateY = (current_disY*current_ratio)+centerY*(1-current_ratio);
                    Log.d(TAG, "onTouch: translateX = "+translateX);
                    Log.d(TAG, "onTouch: translateY = "+translateY);
                    matrix.postTranslate(translateX, translateY);
//                    matrix.postTranslate((disX+current_disX)+centerX*(total_ratio/2), (disY+current_disY)+centerY*(total_ratio/2));
                    imageView.setImageMatrix(matrix);
//
                    current_disX = translateX;
                    current_disY = translateY;


                    Log.d(TAG, "onTouch: getDistance = "+getDistance(event));
                    break;
                default:
                    break;
            }
        } else if (event.getPointerCount() == 1) {
            if (two_pointer_down == false) {
                if (move == true) {
                    //处理单指移动
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            currentX = event.getX();
                            currentY = event.getY();
                            Log.d(TAG, "onTouch: currentX = " + currentX + "   currentY = " + currentY);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            nextX = event.getX();
                            nextY = event.getY();
                            Log.d(TAG, "onTouch: nextX = " + nextX + "   nextY" + nextY);
                            disX = nextX - currentX;
                            disY = nextY - currentY;
                            Log.d(TAG, "onTouch: disX = " + disX + "   disY" + disY);

                            matrix.reset();
                            matrix.postScale(init_ratio * total_ratio, init_ratio * total_ratio);
                            matrix.postTranslate(disX + current_disX, disY + current_disY);
                            imageView.setImageMatrix(matrix);
                            current_disX = disX + current_disX;
                            current_disY = disY + current_disY;

                            currentX = nextX;
                            currentY = nextY;
                            break;
                        default:
                            break;
                    }
                } else {
                    // 处理单指涂鸦
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startPoint.set((event.getX() - current_disX) / (init_ratio * total_ratio), (event.getY() - current_disY) / (init_ratio * total_ratio));
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float x = (event.getX() - current_disX) / (init_ratio * total_ratio);
                            float y = (event.getY() - current_disY) / (init_ratio * total_ratio);
                            drawingCanvas.drawLine(startPoint.x, startPoint.y, x, y, paint);
                            imageView.setImageBitmap(mutableBitmap);
                            startPoint.set(x, y);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                distance1 = 0;
                Log.d(TAG, "onTouch: 0 = distance1 = "+distance1);
                if(two_pointer_down == true) {
                    two_pointer_down = false;//当双指触摸都弹起的时候，才能进行单指操作
                }
                break;
            default:
                break;

        }
        return true;
    }

    private float getDistance(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float getRotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private PointF getMidPoint(MotionEvent event) {
        float x = (event.getX(0) + event.getX(1)) / 2;
        float y = (event.getY(0) + event.getY(1)) / 2;
        return new PointF(x, y);
    }

    private void saveImage() {
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream("/storage/emulated/0/DCIM/Doodle/final_image.jpg");
            mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            mPicturePath = cursor.getString(columnIndex);
            Log.d("PickPicture", mPicturePath);
            cursor.close();
            img_open(mPicturePath);

        }
    }
    public  void img_open(String path){
        imageView = findViewById(R.id.image_view);
        drawingView = findViewById(R.id.drawing_view);

        // 设置触摸事件监听器
        imageView.setOnTouchListener(this);
        drawingView.setOnTouchListener(this);

        // 加载原始图片
        //originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.zhiyin);
        if(path == null) {
            Log.d(TAG, "img_open: null");
            originalBitmap = BitmapFactory.decodeFile("/storage/emulated/0/DCIM/Doodle/final_image.jpg");
        }else{
            Log.d(TAG, "img_open: "+path);
            originalBitmap = BitmapFactory.decodeFile(path);
        }


        // 创建与原始图片大小相匹配的Mutable Bitmap
        mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 创建画布并与Mutable Bitmap关联
        drawingCanvas = new Canvas(mutableBitmap);

        // 初始化画笔
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        // 在ImageView中显示原始图片
        imageView.setImageBitmap(originalBitmap);
        img_w = originalBitmap.getWidth();
        img_h = originalBitmap.getHeight();
        Log.d(TAG, "onCreate: img_w = "+img_w+"    img_h = "+img_h);
        view_w = 1080;//视图 宽
        view_h = 2124;//视图 高
//        int view_w = imageView.getWidth();
//        int view_h = imageView.getHeight();
        Log.d(TAG, "onCreate: view_w = "+view_w+"    view_h = "+view_h);

//        if(img_w<img_h)
//        {
        init_ratio = (1080*(1.0f))/(img_w*(1.0f));
        Log.d(TAG, "aaaaaaaaaaaaaaaaaaaaa init_ratio = "+init_ratio);
        matrix.reset();
        matrix.postScale(init_ratio,init_ratio);
        matrix.postTranslate(0, 0);
        imageView.setImageMatrix(matrix);
//        }
    }
}
