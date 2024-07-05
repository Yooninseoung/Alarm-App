package com.example.flutter_application_1

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.android.FlutterFragmentActivity

import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.content.Intent

import com.example.flutter_application_1.MainViewModel

import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.tracking.SleepTrackingManager
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.tracking.Reports
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.data.SleepSession
import android.util.Log
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.LocalTime
import kotlin.concurrent.thread
import kotlin.properties.Delegates



@AndroidEntryPoint
class MainActivity: FlutterFragmentActivity() {
    private val CHANNEL = "com.flutter_application_1.app/channel"
    private var asleepConfig: AsleepConfig? = null
    private var userId: String? = null
    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(4)
    private val executorLoopSec: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    private lateinit var viewModel: MainViewModel

    var flag: Boolean by Delegates.observable(false) { prop, old, new ->
        println("flag값이 변경됨")
    }

    //시간은 기본 8:0기상, 11시부터 측정 : 사용자 입력으로 바꿀 수 있음.
    //사용자가 일어나야 하는 시간을 설정(24Hour), ex) 8,0 => 8시에 기상
    var wakeupHour:Int =8
    var wakeupMin:Int =0

    //사용자에게 입력 받으면 자동으로 7시간 전으로 설정됨.
    //StartSleepTracking 자동으로 실행될 시간 설정(24Hour), ex) 11, 0 => 매일 11시 부터 sleep 측정
    var startTrackingHour:Int = 23
    var startTrackingMin:Int = 0

    //기상 시간 기준 7시간(420min) 전에 트랙킹을 시작하겠다.
    var minusTime:Long =420

    //기상 시간 기준, 측정을 시작할 시간을 설정, ex) 20 => wakeupHour = 7, wakeupMin = 40
    val repeatTime:Long = 20

    // repeatReport를 몇초 주기로 검사할지(단위 : sec), ex) 30초 주기로 데이터를 검사하겠다.
    val loopTime:Long = 30

    private var nativeChannel: MethodChannel? = null // android에서 flutter용 MethodChannel

    fun initasleepconfig(){
        Asleep.DeveloperMode.isOn = true

        Asleep.initAsleepConfig(
            context = applicationContext,
            apiKey = "sHDt5WgLsRgU0knI7KnCX7qnAPWzhundB8VMYqgF",
            userId = viewModel.userId,
            baseUrl = null,
            callbackUrl = null,
            service = "[input your AppName]",
            object : Asleep.AsleepConfigListener {
                override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                    viewModel.setUserId(userId)
                    viewModel.setAsleepConfig(asleepConfig)
                    println(viewModel.toString())
                    Log.d(">>>> AsleepConfigListener", "onSuccess: userId - $userId")
                    Log.d(">>>> AsleepConfigListener", "onSuccess: Developer Id - $userId")

                }
                override fun onFail(errorCode: Int, detail: String) {
                    Log.d(">>>> AsleepConfigListener", "onFail: $errorCode - $detail")
                }
            })
    }

    class reportset(
        x_user_id: String?,
        timezone: String,
        session_id: String?,
        start_time: String,
        end_time: String?,
        unexpected_end_time: String?,
        created_timezone: String,
        sleep_time: String?,
        wake_time: String?){

        var x_user_id: String? = x_user_id
        var timezone: String = timezone
        var session_id: String? = session_id
        var start_time: String = start_time
        var end_time: String? = end_time
        var unexpected_end_time: String? = unexpected_end_time
        var created_timezone: String = created_timezone
        var sleep_time: String? = sleep_time
        var wake_time: String? = wake_time

        fun toMap(): Map<String, Any?> {
            return mapOf(
                "x_user_id" to x_user_id,
                "timezone" to timezone,
                "session_id" to session_id,
                "start_time" to start_time,
                "end_time" to end_time,
                "unexpected_end_time" to unexpected_end_time,
                "created_timezone" to created_timezone,
                "sleep_time" to sleep_time,
                "wake_time" to wake_time
            )
        }

    }


    fun MutableList<reportset>.toMapList(): List<Map<String, Any?>> {
        return this.map { it.toMap() }
    }

    fun getreport(multiplereports: List<SleepSession>?): MutableList<reportset> {
        val reportsmap: MutableList<reportset> = mutableListOf()
        var report: Report?
        if (multiplereports != null){
            for(obj in multiplereports) {
                viewModel.getReport(obj.sessionId)
                report = viewModel.reportLiveData.value
                report?.let {
                    reportsmap.add(reportset(
                        x_user_id = viewModel.userId,
                        timezone = it.timezone,
                        session_id = obj.sessionId,
                        start_time = it.session?.startTime ?: "",
                        end_time = it.session?.endTime ?: "",
                        unexpected_end_time = it.session?.unexpectedEndTime ?: "",
                        created_timezone = it.session?.createdTimezone ?: "",
                        sleep_time = it.stat?.sleepTime ?: "",
                        wake_time = it.stat?.wakeTime ?: ""
                    ))
                }
            }
            return reportsmap
        }
        return reportsmap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

    }


    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {

        super.configureFlutterEngine(flutterEngine)
        var date : String
        nativeChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "myChannel")



        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
               "createAsleepInstance" ->  { //자동 실행됨
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        // 권한이 아직 부여되지 않았다면, 권한 요청
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), MY_PERMISSIONS_REQUEST_RECORD_AUDIO)
                    } else {
                        // 권한이 이미 부여되었다면, 마이크 사용 가능
                    }

//                    Asleep.DeveloperMode.isOn = true
//                    Asleep.initAsleepConfig(
//                        context = applicationContext,
//                        apiKey = "3vocIdIG1j7CBtr2hehvmKfzrrm1p40dtfuocF3t",
//                        userId = null,
//                        baseUrl = null,
//                        callbackUrl = null,
//                        service = "[input your AppName]",
//                        object : Asleep.AsleepConfigListener {
//                            override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
//                                viewModel.setUserId(userId)
//                                viewModel.setAsleepConfig(asleepConfig)
//                                println(viewModel.toString())
//                                Log.d(">>>> AsleepConfigListener", "onSuccess: userId - $userId")
//                                Log.d(">>>> AsleepConfigListener", "onSuccess: Developer Id - $userId")
//
//                            }
//                            override fun onFail(errorCode: Int, detail: String) {
//                                Log.d(">>>> AsleepConfigListener", "onFail: $errorCode - $detail")
//                            }
//                        })
                   initasleepconfig()
                   stopTracking()
                }


                "StartSleepTracking" -> {
                    viewModel.setStartTrackingTime()
                    viewModel.setErrorData(null,null)
                    viewModel.setReport(null)
                    if(viewModel.userId!=null){
                        startService(Intent(this, RecordService::class.java).apply {
                            action = RecordService.ACTION_START_OR_RESUME_SERVICE
                        })
                    } else {
                        initasleepconfig()
                        startService(Intent(this, RecordService::class.java).apply {
                            action = RecordService.ACTION_START_OR_RESUME_SERVICE
                        })
                    }
                }

                "StopSleepTracking" -> {
                    startService(Intent(this, RecordService::class.java).apply {
                        action = RecordService.ACTION_STOP_SERVICE
                    })
                }

                "GetReport" -> {
                    viewModel.getReport()
                    viewModel.reportLiveData.observe(this) { report ->
                        if (report != null) {
                            println(report)
                        } else {
                            println("Report is null")
                        }
                    }
                }

                "ShowCurrent" -> {
                    viewModel.getCurrentAnalysis()
                }

                "GetMutipleReports" -> {
                    viewModel.getMultipleReports("2024-05-24")
                    val reportsList = getreport(viewModel.MultipleReportsLiveData.value)
                    val reportsData = reportsList.toMapList()
                    result.success(reportsData)
                }

                "Wakeup" -> {
                    wakeupHour = Integer.parseInt(call.argument<String>("hour"))
                    wakeupMin = Integer.parseInt(call.argument<String>("min"))

                    println(">>>>>> 받아온 시간 : $wakeupHour : $wakeupMin")

                    val (newStartHour, newStartMin) = findHourAndMin(wakeupHour, wakeupMin, minusTime) //일어날 시간의 420분(7시간) 전의 시간을 구함

                    startTrackingHour = newStartHour
                    startTrackingMin = newStartMin
                    println("사용자 기상 시간 기준 7시 전 startTracking 시작 시간")
                    println("$startTrackingHour:$startTrackingMin") // 계산된 시간


                    println("(기상 시간 - repeatTime)을 구함, sleep stage level를 처음 얻어오는 시간을 알아냄 ")

                    val (newHour, newMin) = findHourAndMin(wakeupHour, wakeupMin, repeatTime) //일어날 시간의 20분 전의 시간을 구함
                    wakeupHour = newHour
                    wakeupMin = newMin

                    println("$wakeupHour:$wakeupMin") // 계산된 시간



                    startTrackingManager()
                    wakeup20()

                    thread {
                        while (!flag) {
                            println("Waiting for flag to become true...")
                            Thread.sleep(10000) // Sleep for 1 second
                        }
                        println("Alarm 호출")
                        runOnUiThread {
                            nativeChannel?.invokeMethod("ring", "ok")
                        }
                    }

                    result.success("Asleep instance created")





                }

                else -> result.notImplemented() //호출한 함수가 없을 때
            }
        }


    }


    fun stopTracking(){
        startService(Intent(this, RecordService::class.java).apply {
            action = RecordService.ACTION_STOP_SERVICE
        })
    }

    // 매일 밤 11시 마다 sleeptracking 자동 실행
    fun startTrackingManager(){
        println("startTrackingManager Start")
        val now = LocalDateTime.now() //현재 시간을 가져와서 지정한 시간까지의 시간을 차를 구함.
        val targetTime = now.withHour(startTrackingHour).withMinute(startTrackingMin).withSecond(0).withNano(0) //자동으로 실행할 시간

        val initialDelay = if (now.isBefore(targetTime)) {
            ChronoUnit.MILLIS.between(now, targetTime)
        } else {
            ChronoUnit.MILLIS.between(now, targetTime.plusDays(1))
        }

        val period = TimeUnit.DAYS.toMillis(1)

        val task2 = Runnable { //원하는 시간에 Tracking Manager가 실행됨.

            println("정해진 시간에 Tracking 시작 실행됨.")
            viewModel.setStartTrackingTime()
            viewModel.setErrorData(null, null)
            viewModel.setReport(null)
            startService(Intent(this, RecordService::class.java).apply {
                action = RecordService.ACTION_START_OR_RESUME_SERVICE
            })



        }

        //원하는 시간에 시작하도록 해주는 스케줄러
        executor.scheduleAtFixedRate(task2, initialDelay, period, TimeUnit.MILLISECONDS)




    }

    fun ringAlarm() {
        //native에서 알람 호출
        println("ringAlarm 실행")
        nativeChannel?.invokeMethod("ring","ok")

    }

    // 30초(loopTime) 마다 sleepLevel를 받음, level 1이 3번 이상이면 알람을 울림.
    fun repeatReport(){
        println("30초(loopTime)마다 SleepLevel 검사")

        var counter:Int = 0 // counter가 3이면 얕은 수면이라고 판단
        var endCounter:Int = 0 // 얕은 수면 관찰이 안되더라도 시간이 되면 종료하기 위한 카운터 : 20분 동안 30초씩 검사. 총 40번 검사...

        var task1 = Runnable { //수면 검사 : 30초에 한번씩 검사
            viewModel.getCurrentAnalysis() //실시간으로 받아옴
            endCounter++
            println(">>>>>>>>>수면 상태 : " + viewModel._sleepLevel) //sleepStage 리스트의 마지막 요소
            if(viewModel._sleepLevel == 1){// level 1이 3번 나오면 얕은 수면 중이라 판단.
                counter++
                print("수면 측정 번호 : ")
                println(endCounter)
                println(">>>>>>>>>수면 상태 : " + viewModel._sleepLevel + "카운터 : "+ counter)
                if(counter == 3){ //카운터 얕은수면(1) 3번 나오면 종료
                    println(">>>>>>>>>>>>카운터 종료")
                    stopTracking()
                    flag = true //알람을 울리기 위한 boolean flag => true : 알람 실행
                    executorLoopSec.shutdownNow()
                    println(">>>>>>>>>>>>측정 종료")
                }else{
                    println(">>>>>>>>>>>>>수면 중")
                }
            }else{ //수면 측정 시 얕은 수면이 아니면..
                print("수면 측정 번호 : ")
                println(endCounter)
                if(endCounter == 40){ // 얕은 수면 관찰안되면 지정된 시간에 알람을 울림.(40번 검사하면  지정된 시간에 종료..)
                    flag = true
                    executorLoopSec.shutdownNow()
                }
                println(">>>>>>>>>>>>>수면 중")
            }

        }
        executorLoopSec.scheduleAtFixedRate(task1, 0, loopTime, TimeUnit.SECONDS)//30초(loopTime) 마다
    }



    fun wakeup20(){ // 기상 시간 20분 전에 실행 설정
        println("20분 전 시간 설정")
        val current_Time = LocalDateTime.now() //현재 시간으로 부터 목표시간을 구해 원하는 시간에 설정
        val target_Time = current_Time.withHour(wakeupHour).withMinute(wakeupMin).withSecond(0).withNano(0) //자동으로 실행할 시간


        val initial_Delay = if (current_Time.isBefore(target_Time)) {
            ChronoUnit.MILLIS.between(current_Time, target_Time)
        } else {
            ChronoUnit.MILLIS.between(current_Time, target_Time.plusDays(1))
        }

        val period = TimeUnit.DAYS.toMillis(1)


        val task3 = Runnable{ //wakeup Hour, Min부터 측정을 시작함
           stopTracking()//이게 있어야 측정가능
            println("Sleep Level 측정 시작")
            repeatReport()
        }

        executor.scheduleAtFixedRate(task3, initial_Delay, period, TimeUnit.MILLISECONDS) //스케줄러 설정

    }






}
