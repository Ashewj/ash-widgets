package br.com.widgetpoco;
import android.app.job.*;
import android.content.*;

public class RefreshJob extends JobService {
    static void schedule(Context c){
        JobScheduler jobs=(JobScheduler)c.getSystemService(JOB_SCHEDULER_SERVICE);
        JobInfo current=jobs.getPendingJob(701);
        if(current==null||current.getIntervalMillis()!=3600000L)jobs.schedule(new JobInfo.Builder(701,new ComponentName(c,RefreshJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(3600000L,300000L).setPersisted(true).build());
    }
    static void request(Context c){((JobScheduler)c.getSystemService(JOB_SCHEDULER_SERVICE)).schedule(new JobInfo.Builder(702,new ComponentName(c,RefreshJob.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setOverrideDeadline(0).build());}
    public boolean onStartJob(JobParameters p){new Thread(()->{
        boolean ok=Tim.refresh(this);
        if(!ok&&Store.prefs(this).getBoolean("needsLogin",false)&&Store.hasCredentials(this)){
            new android.os.Handler(getMainLooper()).post(()->new TimLogin(this).start(null,success->jobFinished(p,!success)));
        }else jobFinished(p,false);
    }).start();return true;}
    public boolean onStopJob(JobParameters p){return false;}
}
