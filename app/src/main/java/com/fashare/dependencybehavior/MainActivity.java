package com.fashare.dependencybehavior;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableDrag(findViewById(R.id.Object));

        enableDrag(findViewById(R.id.List));
        enableDrag(findViewById(R.id.ArrayList));
        enableDrag(findViewById(R.id.LinkedList));

        enableDrag(findViewById(R.id.Map));
        enableDrag(findViewById(R.id.HashMap));
        enableDrag(findViewById(R.id.TreeMap));
    }

    // 使 view 能拖动
    private void enableDrag(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            Point downP, curP = new Point();

            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                switch (ev.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        downP = new Point((int)ev.getX(), (int)ev.getY());
                        break;

                    case MotionEvent.ACTION_MOVE:
                        curP.x = (int)ev.getX();
                        curP.y = (int)ev.getY();

                        view.setX(view.getX() + curP.x - downP.x);
                        view.setY(view.getY() + curP.y - downP.y);

                        break;
                }

                return true;
            }
        });
    }
}
