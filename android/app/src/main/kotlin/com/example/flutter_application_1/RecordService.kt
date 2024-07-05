package com.example.flutter_application_1

import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Session
import ai.asleep.asleepsdk.tracking.SleepTrackingManager
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import com.example.flutter_application_1.MainViewModel
import dagger.hilt.android.AndroidEntryPoint


//foregroundservice에서 asleeptrackingmanager 생성
const val TAG = ">>>>> RecordService"

@AndroidEntryPoint
class RecordService : LifecycleService() {

//    var sleepTrackingManager:  SleepTrackingManager? = null
//    private var asleepConfig: AsleepConfig? = null
//    var sessionIdLiveData =  MutableLiveData ("") //세션 id 저장 어케함
//    private var userId: String? = null
    @Inject
    lateinit var viewModel: MainViewModel

    companion object {
        private const val FOREGROUND_SERVICE_ID = 1000
        private const val RECORD_NOTIFICATION_CHANNEL_ID = "12344321"

        const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_ERR_EXIT = "ACTION_ERR_EXIT"

        fun isRecordServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (RecordService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override fun onCreate() { //서비스가 생성될 때 한번만 실행
        super.onCreate()
        Log.d(TAG, "onCreate: ")
        createNotificationChannel()
        startForegroundService()
        viewModel.setSequence(null)

        viewModel.asleepConfig.observe(this) {
            createSleepTrackingManager()
            viewModel.sleepTrackingManager?.startSleepTracking()
            //viewModel.sleepTrackingManager?.requestAnalysis() // 실시간 분석을 위해 추가
            Log.d(TAG, "starttracking")

        }

        val sessionIdObserver = Observer<String> { sessionId ->
            if (sessionId != "") { viewModel.getReport() }
        }
        viewModel.sessionIdLiveData.observe(this, sessionIdObserver)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == null || intent.action == ACTION_START_OR_RESUME_SERVICE) {
            Log.d(">>>>> onStartCommand", "Intent: ${intent?.action}")
            if (viewModel.asleepConfig.value == null) {
                if (Asleep.hasUnfinishedSession(applicationContext)) {
                    Log.d(">>>>> onStartCommand", ": hasUnfinishedSession")
                    viewModel.setAsleepConfig(Asleep.getSavedAsleepConfig(applicationContext, "3vocIdIG1j7CBtr2hehvmKfzrrm1p40dtfuocF3t"))
                }
            }
        }
        if (intent?.action == ACTION_STOP_SERVICE) {
            viewModel.sleepTrackingManager?.stopSleepTracking()
            Log.d(TAG, "stoptracking ")

            stopSelf()
        }
        if (intent?.action == ACTION_ERR_EXIT) {
            if (viewModel.sleepTrackingManager?.getTrackingStatus()?.sessionId != null) {
                viewModel.sleepTrackingManager?.stopSleepTracking()
            }
            stopSelf()
        }
        return START_STICKY
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(
                    RECORD_NOTIFICATION_CHANNEL_ID,
                    "Recording In Progress",
                    NotificationManager.IMPORTANCE_HIGH
                )
            notificationChannel.setSound(null, null)
            NotificationManagerCompat.from(applicationContext)
                .createNotificationChannel(notificationChannel)
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, RECORD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Tracking Sleep")
                .setContentText("Running ForegroundService")
                .setContentIntent(pendingIntent)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Tracking Sleep")
                .setContentText("Running ForegroundService")
                .setContentIntent(pendingIntent)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(FOREGROUND_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }

        Log.d(">>>>> ", "startForeground()")
    }


    private fun createSleepTrackingManager(){
        val asleepConfig = viewModel.asleepConfig.value

        viewModel.sleepTrackingManager = Asleep.createSleepTrackingManager(asleepConfig, object : SleepTrackingManager.TrackingListener {
            override fun onCreate() {
                Log.d(">>>>> sleepTrackingManager - ", "onCreate: start tracking")
                Log.d(">>>>> RecordService", "onCreate) TrackingStatus.sessionId: ${viewModel.sleepTrackingManager?.getTrackingStatus()?.sessionId}")
                //Toast.makeText(applicationContext, "Create Session: ${viewModel.sleepTrackingManager?.getTrackingStatus()?.sessionId}", Toast.LENGTH_SHORT).show()
            }

            override fun onUpload(sequence: Int) {
                Log.d(">>>>> sleepTrackingManager - ", "onUpload: $sequence")
                viewModel.setSequence(sequence) //viewmodel 리팩터링
            }

            override fun onClose(sessionId: String) {
                Log.d(">>>>> sleepTrackingManager - ", "onClose: $sessionId")
                Log.d(">>>>> RecordService", "onClose) TrackingStatus.sessionId: ${viewModel.sleepTrackingManager?.getTrackingStatus()?.sessionId}")
                viewModel.sessionIdLiveData.postValue(sessionId)
                //Toast.makeText(applicationContext, "Close: ${viewModel.sleepTrackingManager?.getTrackingStatus()?.sessionId}", Toast.LENGTH_SHORT).show()
            }

            override fun onFail(errorCode: Int, detail: String) {
                Log.d(">>>>> sleepTrackingManager - ", "onFail: $errorCode - $detail")
                viewModel.setErrorData(errorCode, detail)
            }
        })
    }





}

