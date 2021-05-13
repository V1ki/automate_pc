package com.yeesotr.auto.android;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;

@Slf4j
public class RMServices {


    static OkHttpClient client = new OkHttpClient() ;

    public static void uploadInfoToRMS(File file){


        String plan = "plan4";
        String caseStr = "T_EM_NA_C12";
        String testKey = "";
        String startTime = "";
        int currentStep = 0 ;
        String imei = null ;
        String json = "" ;
        String msg = "upload androbench info!" ;


        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("image/png"), file))
                .addFormDataPart("Key", testKey)// 测试单信息
                .addFormDataPart("StartTime", startTime)
                .addFormDataPart("Product", "EMMC")
                .addFormDataPart("IMEI", imei)// 当前测试机MEID
                .addFormDataPart("Platform_no", "unknown")
                .addFormDataPart("State", "" + 1)// 当前平台测试状态 0 测试中 1完成测试  非0状态即认为测试平台空闲
                .addFormDataPart("plan",  plan)// 平台当前测试plan
                .addFormDataPart("case",  caseStr)// 当前测试case
                .addFormDataPart("Count", "" + currentStep)// 当前case测试圈数
                .addFormDataPart("datatype", "0")//数据类型  0：测试截图 1：smart信息结构体
                .addFormDataPart("JsonData", json)//测试结果数据 各个case自定义
                .addFormDataPart("DescripInfo", msg)//测试状态描叙 用户自定义描叙

                .build();

        Request request = new Request.Builder()
                .url("http://rms.com/platform")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(final Call call, final IOException e) {
                // Handle the error
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // Handle the error
                }
                // Upload successful
            }
        });

    }

    public static void getTimerTask(String product) {
        RequestBody requestBody = new FormBody.Builder()
                .add("product", product)
                .build();

        Request request = new Request.Builder()
                .post(requestBody)
                .url("http://rms.com/getTestTask/")
                .build();


        try {
            Response resp = client.newCall(request).execute() ;
            log.info("Response: "+resp);
            assert resp.body() != null;
            log.info("body: "+resp.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
