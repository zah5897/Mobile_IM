package com.mobile.im;

import android.app.Service;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.mobile.im.location.service.LocationService;

import net.openmob.mobileimsdk.android.core.LocalUDPDataSender;

import org.json.JSONException;
import org.json.JSONObject;

import im.mobile.IMClientManager;
import im.mobile.callback.Callback;
import im.mobile.http.HttpUtil;
import im.mobile.model.SysType;

public class LocationManger {

    public static LocationManger locationManger;
    public LocationService locationService;
    public Vibrator mVibrator;

    private LocationManger() {
    }

    public static LocationManger getLocationManger() {

        if (locationManger == null) {
            locationManger = new LocationManger();
        }
        return locationManger;
    }


    public void getZoomLeven() {

//        1 首先计算出四个点上、下、左、右
//
//        double maxLat;
//        double minLat;
//        double maxLong;
//        double minLong;
//
//        2 将地图的中间点定位到中间点
//
//        LatLng ll = new LatLng(midlat, midlon);
//
//
//        3 计算地图缩放度
//        int jl = (int) DistanceUtil.getDistance(new LatLng(maxLat, maxLong),
//                new LatLng(minLat, minLong));
//        int i;
//        for (i = 0; i < 17; i++) {
//            if (zoomLevel[i] < jl) {
//                break;
//            }
//        }
//        float zoom = i + 5;
//        4 调整地图
//
//                u = MapStatusUpdateFactory.newLatLngZoom(ll, zoom);
//        baiduMap.setMapStatus(u);
//        baiduMap.animateMapStatus(u);


    }


    public void init(Context context) {
        locationService = new LocationService(context);
        mVibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        SDKInitializer.initialize(context);
        //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.

        //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
        SDKInitializer.setCoordType(CoordType.BD09LL);
    }

    public void loadPublish(int lastId, int pageSize, Callback callback) {
        HttpUtil.loadPublish(IMClientManager.getInstance().getCurrentLoginUsername(), lastId, pageSize, callback);
    }

    public void publishLocationToUser(Context context, BDLocation location, String to) {
        if (null == location || location.getLocType() == BDLocation.TypeServerError) {
            return;
        }
        JSONObject obj = locationTOJson(location);
        try {
            obj.putOpt("type", 1);
            obj.putOpt("to", to);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LocalUDPDataSender.getInstance(context).sendCommonData(obj.toString(), "0", SysType.PUBLISH_LOCATION.getValue());
    }

    public void publishLocationOnly(Context context, BDLocation location) {
        if (null == location || location.getLocType() == BDLocation.TypeServerError) {
            return;
        }
        JSONObject obj = locationTOJson(location);
        try {
            obj.putOpt("type", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LocalUDPDataSender.getInstance(context).sendCommonData(obj.toString(), "0", SysType.PUBLISH_LOCATION.getValue());
    }


    private JSONObject locationTOJson(BDLocation location) {
        String latitude = String.valueOf(location.getLatitude());
        String lontitude = String.valueOf(location.getLongitude());
        String radius = String.valueOf(location.getRadius());
        String CountryCode = location.getCountryCode();
        String Country = location.getCountry();
        String citycode = location.getCityCode();
        String city = location.getCity();
        String District = location.getDistrict();
        String Street = location.getStreet();
        String addr = location.getAddrStr();
        JSONObject obj = new JSONObject();
        try {
            obj.put("lat", latitude);
            obj.put("lng", lontitude);
            obj.put("radius", radius);
            obj.put("countryCode", CountryCode);
            obj.put("country", Country);
            obj.put("cityCode", citycode);
            obj.put("city", city);
            obj.put("district", District);
            obj.put("street", Street);
            obj.put("addr", addr);
        } catch (Exception e) {
        }
        return obj;


//        latitude : 31.297791
//        lontitude : 121.331074
//        radius : 40.0
//        CountryCode : 0
//        Country : 中国
//        citycode : 289
//        city : 上海市
//        District : 嘉定区
//        Street : 众仁路
//        addr : 中国上海市嘉定区众仁路418号
//        UserIndoorState: 1


//        // TODO Auto-generated method stub
//        if (null != location && location.getLocType() != BDLocation.TypeServerError) {
//            StringBuffer sb = new StringBuffer(256);
//            sb.append("time : ");
//            /**
//             * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
//             * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
//             */
//            sb.append(location.getTime());
//            sb.append("\nlocType : ");// 定位类型
//            sb.append(location.getLocType());
//            sb.append("\nlocType description : ");// *****对应的定位类型说明*****
//            sb.append(location.getLocTypeDescription());
//            sb.append("\nlatitude : ");// 纬度
//            sb.append(location.getLatitude());
//            sb.append("\nlontitude : ");// 经度
//            sb.append(location.getLongitude());
//            sb.append("\nradius : ");// 半径
//            sb.append(location.getRadius());
//            sb.append("\nCountryCode : ");// 国家码
//            sb.append(location.getCountryCode());
//            sb.append("\nCountry : ");// 国家名称
//            sb.append(location.getCountry());
//            sb.append("\ncitycode : ");// 城市编码
//            sb.append(location.getCityCode());
//            sb.append("\ncity : ");// 城市
//            sb.append(location.getCity());
//            sb.append("\nDistrict : ");// 区
//            sb.append(location.getDistrict());
//            sb.append("\nStreet : ");// 街道
//            sb.append(location.getStreet());
//            sb.append("\naddr : ");// 地址信息
//            sb.append(location.getAddrStr());
//            sb.append("\nUserIndoorState: ");// *****返回用户室内外判断结果*****
//            sb.append(location.getUserIndoorState());
//            sb.append("\nDirection(not all devices have value): ");
//            sb.append(location.getDirection());// 方向
//            sb.append("\nlocationdescribe: ");
//            sb.append(location.getLocationDescribe());// 位置语义化信息
//            sb.append("\nPoi: ");// POI信息
//            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
//                sb.append("\nspeed : ");
//                sb.append(location.getSpeed());// 速度 单位：km/h
//                sb.append("\nsatellite : ");
//                sb.append(location.getSatelliteNumber());// 卫星数目
//                sb.append("\nheight : ");
//                sb.append(location.getAltitude());// 海拔高度 单位：米
//                sb.append("\ngps status : ");
//                sb.append(location.getGpsAccuracyStatus());// *****gps质量判断*****
//                sb.append("\ndescribe : ");
//                sb.append("gps定位成功");
//            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
//                // 运营商信息
//                if (location.hasAltitude()) {// *****如果有海拔高度*****
//                    sb.append("\nheight : ");
//                    sb.append(location.getAltitude());// 单位：米
//                }
//                sb.append("\noperationers : ");// 运营商信息
//                sb.append(location.getOperators());
//                sb.append("\ndescribe : ");
//                sb.append("网络定位成功");
//            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
//                sb.append("\ndescribe : ");
//                sb.append("离线定位成功，离线定位结果也是有效的");
//            } else if (location.getLocType() == BDLocation.TypeServerError) {
//                sb.append("\ndescribe : ");
//                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
//            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
//                sb.append("\ndescribe : ");
//                sb.append("网络不同导致定位失败，请检查网络是否通畅");
//            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
//                sb.append("\ndescribe : ");
//                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
//            }
//            String msg = sb.toString();
//            Log.DisplayUtil("MapActivity", msg);
//        }
    }
}
