package com.example.pgorman.testapplication;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

/**
 * Created by patri_000 on 1/1/2017.
 */

public class TaskUtils {

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
    public static <Params, Progress, Results> void startTask(AsyncTask<Params, Progress, Results> asyncTask, Params ... params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }  else {
            asyncTask.execute(params);
        }
    }
}
