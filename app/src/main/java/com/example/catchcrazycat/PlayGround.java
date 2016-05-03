package com.example.catchcrazycat;

import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Vector;

/**
 * Created by carl.ma on 4/29/2016.
 */
public class PlayGround extends SurfaceView implements View.OnTouchListener{


    private static int WIDTH = 100;
    private static final int ROW = 10;
    private static final int COL = 10;
    private static final int BLOCKS = 10; //默认路障数量
    private Dot matrix[][];


    private Dot cat;

    public PlayGround(Context context) {
        super(context);
        getHolder().addCallback(callback);
        matrix = new Dot[ROW][COL];
        for (int i = 0; i < ROW; i++){
            for (int j=0; j < COL; j++){
                matrix[i][j] = new Dot(j,i);
            }
        }
        setOnTouchListener(this);
        initGame();
    }

    private Dot getDot(int x, int y){
        return matrix[y][x];
    }

    private boolean isAtEdge(Dot d){
        if (d.getX()==0 || d.getY()==0 || d.getX()+1 == COL || d.getY()+1 == ROW){
            return true;
        }
        return false;
    }



    private Dot getNeighbour(Dot one, int dir){   //dir是邻居的数量，表示围住的格子的方向
        switch (dir){
            case 1:
                return getDot(one.getX()-1,one.getY());

            case 2:
                if (one.getY()%2 ==0 ){
                    return getDot(one.getX()-1,one.getY()-1);
                }else {
                    return getDot(one.getX(),one.getY()-1);
                }

            case 3:
                if (one.getY()%2 ==0 ){
                    return getDot(one.getX(),one.getY()-1);
                }else {
                    return getDot(one.getX()+1,one.getY()-1);
                }

            case 4:
                return getDot(one.getX()+1,one.getY());

            case 5:
                if (one.getY()%2 ==0 ){
                    return getDot(one.getX(),one.getY()+1);
                }else {
                    return getDot(one.getX()+1,one.getY()+1);
                }

            case 6:
                if (one.getY()%2 ==0 ){
                    return getDot(one.getX()-1,one.getY()+1);
                }else {
                    return getDot(one.getX(),one.getY()+1);
                }
            default:
                break;

        }
        return null;
    }




    private int getDistance(Dot one, int dir){
        int distance = 0;
        boolean toedge = true;
        Dot ori = one,next;
        ori.getStatus();

        if (isAtEdge(one)){
            return 1;
        }
        while (true){
            next = getNeighbour(ori ,dir);

            if (next.getStatus() == Dot.STATUS_ON){
                return distance*-1;
            }

            if (isAtEdge(next)){
                distance++;
                return distance;
            }

            distance++;
            ori = next;
        }

    }

    private void moveTo(Dot one){
        one.setStatus(Dot.STATUS_IN);
        getDot(cat.getX(),cat.getY()).setStatus(Dot.STATUS_OFF);
        cat.setXY(one.getX(),one.getY());
    }


    //决定猫下一步向哪里移动并判断输赢
    private void move(){
        if (isAtEdge(cat)){
            lose();
            return;
        }
        Vector<Dot> available = new Vector<>(); //存储相邻无障碍物的方向的Dot
        Vector<Dot> positive = new Vector<>(); //存储前方无障碍物的方向的Dot
        HashMap<Dot, Integer> al = new HashMap<Dot, Integer>();
        for (int i = 1; i <7 ; i++){
            Dot n = getNeighbour(cat,i);
            if (n.getStatus() == Dot.STATUS_OFF){
                available.add(n); //判断指定方向上没有相邻的障碍，并把Dot加入available
                al.put(n,i); //所有能走的点都记录一份方向
                if (getDistance(n,i)>0){    //判断如果指定方向上，完全没有障碍，则把目的Dot加入positive 和positive list
                    positive.add(n);
                }
            }
        }
        if (available.size()==0){
            win();
        }else if (available.size()==1){   //只有一个位置可以走，那么就走唯一的一个位置。
            moveTo(available.get(0));
        }else {
            Dot best = null;
            if (positive.size()!=0){   //存在可以直接到达屏幕边缘的走向
                int min = 999;
                for (int i = 0; i<positive.size(); i++){
                    int a=getDistance(positive.get(i),al.get(positive.get(i))); //第一个参数，得到前方无障碍物的目的Dot，第二个参数，得到是1-6这六个方向的哪个
                    if (a<min){  //在positive里面选择离边缘最小的，复制给best
                        min = a;
                        best = positive.get(i);
                    }
                }

            }else{ //如果所有方向都存在路障
                int max = 0;    //如果所有方向都存在路障，那么max <0 。
                for (int i = 0; i<available.size(); i++){
                    int k = getDistance(available.get(i),al.get(available.get(i)));   //判断各个available方向的距离
                    if (k < max){    //如果这个路障更远一点，那么就向着路障更远的方向走。
                        max = k;
                        best = available.get(i);
                    }
                }
            }
            moveTo(best);
        }
    }

    private void lose(){
        Toast.makeText(getContext(),"lose",Toast.LENGTH_SHORT).show();
    }

    private void win(){
        Toast.makeText(getContext(),"win",Toast.LENGTH_SHORT).show();

    }

    private void redraw(){
        Canvas c = getHolder().lockCanvas();
        c.drawColor(Color.LTGRAY);
        Paint paint = new Paint(); //创建画笔
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i< ROW; i++){           //i是行
            int offset = 0;
            if (i % 2 != 0){
                offset = WIDTH/2;
            }
            for (int j = 0;j< COL; j++){
                Dot one = getDot(j, i);
                switch (one.getStatus()){
                    case Dot.STATUS_OFF:
                        paint.setColor(0xFF7EC0EE); //FF实心，7EC0EE是蓝色
                        break;
                    case Dot.STATUS_ON:
                        paint.setColor(0xFFFFAA00);  //FF实心，FFAA00橙红色
                        break;
                    case Dot.STATUS_IN:
                        paint.setColor(0xFFFF0000); //FF0000红色
                        break;
                    default:
                        break;
                }
                c.drawOval(new RectF(new RectF(one.getX()*WIDTH + offset //椭圆有四个点，左侧点位置
                        ,one.getY()*WIDTH, //上侧点位置
                        (one.getX()+1)*WIDTH + offset,//右侧点位置
                        (one.getY()+1)*WIDTH)) //下侧点位置
                        , paint);
            }
        }
        getHolder().unlockCanvasAndPost(c);
    }

    SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            redraw();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            WIDTH = i1/(COL+1);
            redraw();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    private void initGame(){
        for (int i = 0; i < ROW; i++){
            for (int j=0; j < COL; j++){
                matrix[i][j].setStatus(Dot.STATUS_OFF);
            }
        }
        cat = new Dot(4,5);
        getDot(4,5).setStatus(Dot.STATUS_IN);
        for (int i = 0 ; i< BLOCKS; ){
            int x = (int) (Math.random()*1000)%COL;
            int y = (int) (Math.random()*1000)%ROW;

            if (getDot(x,y).getStatus() == Dot.STATUS_OFF){
                getDot(x,y).setStatus(Dot.STATUS_ON);
                i++;
            }
        }

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP){
//            Toast.makeText(getContext(),motionEvent.getX()+ " "+ motionEvent.getY(), Toast.LENGTH_SHORT).show();
            int x,y;
            y = (int)(motionEvent.getY()/WIDTH);
            if (y%2 == 0){    //y代表第几行。是实际上我们使用时数的坐标
                x = (int)(motionEvent.getX()/WIDTH);
            }else{
                x = (int)((motionEvent.getX()-WIDTH/2)/WIDTH);
            }
            if (x+1>COL || y+1> ROW){
                initGame();
            }else if(getDot(x,y).getStatus() == Dot.STATUS_OFF){
                getDot(x, y).setStatus(Dot.STATUS_ON);
                move();
            }
            redraw();
        }
        return true;
    }
}
