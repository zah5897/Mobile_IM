package com.mobile.im.location.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestFutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mobile.im.R;
import com.mobile.im.utils.DisplayUtil;
import com.mobile.im.utils.GlideApp;

import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;

public class MarkerUtil {

    public static void initLocationMarkInfos(MapView mMapView, final BaiduMap mBaiduMap, double lat, double lng, String username) {
        String url = "http://app.weimobile.com/nearby/avatar/thumb/8632ed69-93e7-47b0-9ead-e7b2406aaf4d.jpg";
        //判断头像地址是否为空
        //不为空就将地址传递过去加载到布局中
        returnPictureView(mMapView, mBaiduMap, lat, lng, username, url);

    }//将图片加载到布局中

    private static void returnPictureView(final MapView mMapView, final BaiduMap mBaiduMap, final double lat, final double lng, final String username, String imagUrl) {


        int w = DisplayUtil.dip2px(mMapView.getContext(), 60);
        GlideApp.with(mMapView.getContext())
                .load(imagUrl).apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .centerCrop().override(w, w)
                .placeholder(R.drawable.chat_image_selector)
                .into(new SimpleTarget<Drawable>() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        LatLng point = new LatLng(lat, lng);

                        View view = LayoutInflater.from(mMapView.getContext()).inflate(R.layout.circle_imageview, null);
                        CircleImageView imageView = view.findViewById(R.id.profile_image);
                        imageView.setImageAlpha(0);
                        imageView.setImageDrawable(resource);
                        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromView(imageView);
                        MarkerOptions overlayOptions = new MarkerOptions()
                                .position(point)
                                .icon(bitmap)
                                .zIndex(15)
                                .draggable(true);
//                                .animateType(MarkerOptions.MarkerAnimateType.grow);//设置marker从地上生长出来的动画

                        Marker marker = (Marker) mBaiduMap.addOverlay(overlayOptions);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("username", username);
                        marker.setExtraInfo(bundle);//marker点击事件监听时，可以获取到此时设置的数据
                        marker.setToTop();
                    }
                });


    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        // 取 drawable 的长宽
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();

        // 取 drawable 的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        // 建立对应 bitmap
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        // 把 drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }

}
