/**
 * AQWJO Ads SDK for Android
 * نسخة 1.0.0
 * 
 * للتكامل مع تطبيقات الأندرويد لعرض الإعلانات من شبكة AQWJO
 */

package com.aqwjo.ads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AqwjoAds {
    
    // ✅ إعدادات الـ SDK
    private static final String API_BASE_URL = "https://aqwjo.com/api/app-ads/";
    private static final String SDK_VERSION = "1.0.0";
    
    private Context context;
    private String apiKey;
    private String packageName;
    private String appVersion;
    private OkHttpClient httpClient;
    private Gson gson;
    private Handler mainHandler;
    
    // ✅ مستمعي الأحداث
    private AdLoadListener adLoadListener;
    private AdClickListener adClickListener;
    
    // ✅ نموذج الإعلان
    public static class AdInfo {
        public int id;
        public String title;
        public String description;
        public String imageUrl;
        public String clickUrl;
        public String deepLink;
        public String adFormat;
        public int width;
        public int height;
    }
    
    // ✅ واجهات المستمعين
    public interface AdLoadListener {
        void onAdLoaded(AdInfo ad);
        void onAdFailed(String error);
    }
    
    public interface AdClickListener {
        void onAdClicked(AdInfo ad);
    }
    
    // ✅ البناء
    public AqwjoAds(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
        this.packageName = context.getPackageName();
        this.appVersion = getAppVersion();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    // ✅ تعيين مستمع تحميل الإعلان
    public void setAdLoadListener(AdLoadListener listener) {
        this.adLoadListener = listener;
    }
    
    // ✅ تعيين مستمع النقر
    public void setAdClickListener(AdClickListener listener) {
        this.adClickListener = listener;
    }
    
    // ✅ تحميل إعلان بانر
    public void loadBanner(final ViewGroup container) {
        loadAd("banner", new AdCallback() {
            @Override
            public void onSuccess(final AdInfo ad) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        displayBannerAd(container, ad);
                        if (adLoadListener != null) {
                            adLoadListener.onAdLoaded(ad);
                        }
                    }
                });
            }
            
            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (adLoadListener != null) {
                            adLoadListener.onAdFailed(error);
                        }
                    }
                });
            }
        });
    }
    
    // ✅ عرض إعلان البانر
    private void displayBannerAd(ViewGroup container, AdInfo ad) {
        container.removeAllViews();
        
        LinearLayout adLayout = new LinearLayout(context);
        adLayout.setOrientation(LinearLayout.VERTICAL);
        adLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        adLayout.setPadding(0, 0, 0, 0);
        
        // صورة الإعلان
        if (ad.imageUrl != null && !ad.imageUrl.isEmpty()) {
            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (ad.height * (context.getResources().getDisplayMetrics().density))
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            Glide.with(context)
                .load(ad.imageUrl)
                .placeholder(android.R.drawable.ic_dialog_info)
                .into(imageView);
            
            adLayout.addView(imageView);
        }
        
        // نص الإعلان
        if (ad.title != null && !ad.title.isEmpty()) {
            TextView titleView = new TextView(context);
            titleView.setText(ad.title);
            titleView.setTextSize(14);
            titleView.setTextColor(android.graphics.Color.BLACK);
            titleView.setPadding(16, 16, 16, 8);
            adLayout.addView(titleView);
        }
        
        // وصف الإعلان
        if (ad.description != null && !ad.description.isEmpty()) {
            TextView descView = new TextView(context);
            descView.setText(ad.description);
            descView.setTextSize(12);
            descView.setTextColor(android.graphics.Color.GRAY);
            descView.setPadding(16, 0, 16, 16);
            adLayout.addView(descView);
        }
        
        // ملصق "إعلان"
        TextView adLabel = new TextView(context);
        adLabel.setText("📢 إعلان");
        adLabel.setTextSize(10);
        adLabel.setTextColor(android.graphics.Color.WHITE);
        adLabel.setBackgroundColor(android.graphics.Color.parseColor("#f59e0b"));
        adLabel.setPadding(8, 4, 8, 4);
        adLabel.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(16, 0, 16, 16);
        adLayout.addView(adLabel, labelParams);
        
        // النقر على الإعلان
        adLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick(ad);
            }
        });
        
        container.addView(adLayout);
        
        // تسجيل الانطباع
        trackImpression(ad.id);
    }
    
    // ✅ تحميل إعلان (عام)
    public void loadAd(final String adFormat, final AdCallback callback) {
        JsonObject requestData = new JsonObject();
        requestData.addProperty("api_key", apiKey);
        requestData.addProperty("package_name", packageName);
        requestData.addProperty("platform", "android");
        requestData.addProperty("app_version", appVersion);
        requestData.addProperty("ad_format", adFormat);
        requestData.addProperty("sdk_version", SDK_VERSION);
        
        RequestBody body = RequestBody.create(
            gson.toJson(requestData),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(API_BASE_URL + "serve-ad.php")
            .post(body)
            .addHeader("X-API-Key", apiKey)
            .addHeader("X-Platform", "android")
            .addHeader("X-Package-Name", packageName)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("فشل الاتصال: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);
                    
                    if (json.has("success") && json.get("success").getAsBoolean()) {
                        AdInfo ad = gson.fromJson(json.getAsJsonObject("ad"), AdInfo.class);
                        callback.onSuccess(ad);
                    } else {
                        callback.onError(json.has("message") ? json.get("message").getAsString() : "لا توجد إعلانات متاحة");
                    }
                } else {
                    callback.onError("خطأ HTTP: " + response.code());
                }
            }
        });
    }
    
    // ✅ معالجة النقر على الإعلان
    private void handleAdClick(AdInfo ad) {
        // تسجيل النقرة
        trackClick(ad.id, ad.clickUrl);
        
        // فتح الرابط
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ad.clickUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            if (adClickListener != null) {
                adClickListener.onAdClicked(ad);
            }
        } catch (Exception e) {
            Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show();
        }
    }
    
    // ✅ تسجيل الانطباع
    private void trackImpression(final int adId) {
        JsonObject requestData = new JsonObject();
        requestData.addProperty("ad_id", adId);
        requestData.addProperty("api_key", apiKey);
        requestData.addProperty("package_name", packageName);
        requestData.addProperty("event_type", "impression");
        
        RequestBody body = RequestBody.create(
            gson.toJson(requestData),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(API_BASE_URL + "track-event.php")
            .post(body)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // فشل صامت لتتبع الانطباعات
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }
    
    // ✅ تسجيل النقرة
    private void trackClick(final int adId, final String redirectUrl) {
        // التتبع يتم عبر رابط النقر مباشرة في API
    }
    
    // ✅ الحصول على نسخة التطبيق
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                .getPackageInfo(packageName, 0)
                .versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }
    
    // ✅ واجهة الاستدعاء
    public interface AdCallback {
        void onSuccess(AdInfo ad);
        void onError(String error);
    }
    
    // ✅ تدمير الـ SDK (تنظيف الموارد)
    public void destroy() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}