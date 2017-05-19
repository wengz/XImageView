package com.ooxx.ximageview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Region;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private XImageView xImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xImageView = (XImageView) findViewById(R.id.xiv);
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rec);
        xImageView.setBitmap(createBitmap());

        Region region = new Region(0, 0, 50, 100);
        XImageView.Area area = new XImageView.Area();
        area.mRegion = region;
        area.mOnClickListner = new XImageView.onClickListner() {
            @Override
            public void onClick() {
                Toast.makeText(MainActivity.this, "点击矩形", Toast.LENGTH_SHORT).show();
            }
        };
        xImageView.addArea(area);
    }

    private Bitmap createBitmap (){
        Paint paint = new Paint();
        paint.setColor(Color.RED);

        Bitmap bitmap = Bitmap.createBitmap(100, 200, null);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLUE);
        canvas.drawRect(0, 0, 50, 100, paint);

        return bitmap;
    }
}
