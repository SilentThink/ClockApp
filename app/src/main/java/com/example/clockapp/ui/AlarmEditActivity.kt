package com.example.clockapp.ui

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.clockapp.R
import com.example.clockapp.db.AlarmDatabaseHelper
import com.example.clockapp.model.Alarm
import com.example.clockapp.service.AlarmScheduler
import com.example.clockapp.util.JsonParser
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.iflytek.cloud.ErrorCode
import com.iflytek.cloud.InitListener
import com.iflytek.cloud.RecognizerResult
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechRecognizer
import com.iflytek.cloud.ui.RecognizerDialog
import com.iflytek.cloud.ui.RecognizerDialogListener
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedHashMap
import java.util.Locale

/**
 * 闹钟编辑界面
 */
class AlarmEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val RESULT_ALARM_DELETED = 2
        private const val TAG = "AlarmEditActivity"
    }

    private lateinit var timeText: TextView
    private lateinit var labelEdit: EditText
    private lateinit var repeatLayout: LinearLayout
    private lateinit var repeatText: TextView
    private lateinit var snoozeLayout: LinearLayout
    private lateinit var snoozeText: TextView
    private lateinit var vibrateCheckBox: CheckBox
    private lateinit var voiceButton: FloatingActionButton

    private var alarm: Alarm = Alarm()
    private var isNewAlarm = true

    // 语音识别相关
    private var mIat: SpeechRecognizer? = null
    private var mIatDialog: RecognizerDialog? = null
    private val mIatResults: MutableMap<String, String> = LinkedHashMap()
    private lateinit var mSharedPreferences: SharedPreferences
    private val mEngineType = SpeechConstant.TYPE_CLOUD
    private val language = "zh_cn"
    private val resultType = "json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_edit)

        // 设置标题
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化视图
        timeText = findViewById(R.id.edit_alarm_time)
        labelEdit = findViewById(R.id.edit_alarm_label)
        repeatLayout = findViewById(R.id.repeat_layout)
        repeatText = findViewById(R.id.repeat_text)
        snoozeLayout = findViewById(R.id.snooze_layout)
        snoozeText = findViewById(R.id.snooze_text)
        vibrateCheckBox = findViewById(R.id.vibrate_checkbox)
        voiceButton = findViewById(R.id.voice_button)

        // 获取闹钟ID
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId != null) {
            // 编辑现有闹钟
            isNewAlarm = false
            supportActionBar?.title = "编辑闹钟"

            val dbHelper = AlarmDatabaseHelper(this)
            val existingAlarm = dbHelper.getAlarm(alarmId)
            if (existingAlarm != null) {
                alarm = existingAlarm
            }
        } else {
            // 创建新闹钟
            supportActionBar?.title = "添加闹钟"
            
            // 默认设置为当前时间
            val calendar = Calendar.getInstance()
            alarm.hour = calendar.get(Calendar.HOUR_OF_DAY)
            alarm.minute = calendar.get(Calendar.MINUTE)
        }

        // 设置时间点击事件
        timeText.setOnClickListener {
            showTimePickerDialog()
        }

        // 设置重复点击事件
        repeatLayout.setOnClickListener {
            showRepeatDialog()
        }

        // 设置贪睡点击事件
        snoozeLayout.setOnClickListener {
            showSnoozeDialog()
        }

        // 设置振动点击事件
        vibrateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.vibrate = isChecked
        }

        // 初始化语音识别
        initVoiceRecognition()

        // 设置语音按钮点击事件
        voiceButton.setOnClickListener {
            startVoiceRecognition()
        }

        // 更新UI
        updateUI()
    }

    private fun initVoiceRecognition() {
        // 请求权限
        initPermission()

        // 初始化讯飞语音识别
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener)
        mIatDialog = RecognizerDialog(this, mInitListener)
        mSharedPreferences = getSharedPreferences("ASR", Activity.MODE_PRIVATE)
    }

    /**
     * 初始化监听器
     */
    private val mInitListener = InitListener { code ->
        Log.d(TAG, "SpeechRecognizer init() code = $code")
        if (code != ErrorCode.SUCCESS) {
            showToast("初始化失败，错误码：$code")
        }
    }

    /**
     * 开始语音识别
     */
    private fun startVoiceRecognition() {
        if (mIat == null) {
            showToast("创建对象失败，请确认语音识别组件已正确初始化")
            return
        }

        mIatResults.clear()
        setParam()
        mIatDialog?.setListener(mRecognizerDialogListener)
        mIatDialog?.show()
    }

    /**
     * 听写UI监听器
     */
    private val mRecognizerDialogListener = object : RecognizerDialogListener {
        override fun onResult(results: RecognizerResult, isLast: Boolean) {
            processVoiceResult(results)
        }

        override fun onError(error: SpeechError) {
            showToast(error.getPlainDescription(true))
        }
    }

    /**
     * 处理语音识别结果
     */
    private fun processVoiceResult(results: RecognizerResult) {
        val text = com.example.clockapp.util.JsonParser.parseIatResult(results.resultString)

        var sn: String? = null
        try {
            val resultJson = JSONObject(results.resultString)
            sn = resultJson.optString("sn")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (sn != null) {
            mIatResults[sn] = text
        }

        // 拼接识别结果
        val resultBuffer = StringBuilder()
        for (key in mIatResults.keys) {
            resultBuffer.append(mIatResults[key])
        }

        val recognizedText = resultBuffer.toString()
        Log.d(TAG, "识别结果: $recognizedText")

        // 解析语音命令
        val command = JsonParser.parseAlarmCommand(recognizedText)
        if (command != null) {
            // 设置闹钟
            alarm.hour = command.hour
            alarm.minute = command.minute
            alarm.repeatDays = command.repeatDays
            if (command.label.isNotEmpty()) {
                alarm.label = command.label
            }

            // 更新UI
            updateUI()
            
            // 显示识别成功提示
            showToast("已设置闹钟：${alarm.hour}:${alarm.minute}")
        } else {
            showToast("未能识别有效的闹钟命令，请重试")
        }
    }

    /**
     * 参数设置
     */
    private fun setParam() {
        mIat?.setParameter(SpeechConstant.PARAMS, null)
        mIat?.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType)
        mIat?.setParameter(SpeechConstant.RESULT_TYPE, resultType)

        if (language == "zh_cn") {
            val lag = mSharedPreferences.getString("iat_language_preference", "mandarin")
            mIat?.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
            mIat?.setParameter(SpeechConstant.ACCENT, lag)
        } else {
            mIat?.setParameter(SpeechConstant.LANGUAGE, language)
        }

        // 设置语音前端点:静音超时时间
        mIat?.setParameter(
            SpeechConstant.VAD_BOS,
            mSharedPreferences.getString("iat_vadbos_preference", "4000")
        )

        // 设置语音后端点:后端点静音检测时间
        mIat?.setParameter(
            SpeechConstant.VAD_EOS,
            mSharedPreferences.getString("iat_vadeos_preference", "1000")
        )

        // 设置标点符号
        mIat?.setParameter(
            SpeechConstant.ASR_PTT,
            mSharedPreferences.getString("iat_punc_preference", "1")
        )

        // 设置音频保存路径
        mIat?.setParameter(SpeechConstant.AUDIO_FORMAT, "wav")
        val audioPath = getExternalFilesDir(null)?.absolutePath + "/msc/iat.wav"
        mIat?.setParameter(SpeechConstant.ASR_AUDIO_PATH, audioPath)
    }

    /**
     * 动态申请权限
     */
    private fun initPermission() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val toApplyList = ArrayList<String>()
        for (perm in permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm)
            }
        }

        if (toApplyList.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toTypedArray(), 123)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            
            if (!allPermissionsGranted) {
                showToast("部分权限被拒绝，语音识别功能可能无法使用")
            }
        }
    }

    private fun updateUI() {
        // 更新时间
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeText.text = timeFormat.format(calendar.time)

        // 更新标签
        labelEdit.setText(alarm.label)

        // 更新重复
        repeatText.text = alarm.getRepeatDaysDescription()

        // 更新贪睡
        snoozeText.text = "${alarm.snoozeMinutes} 分钟"

        // 更新振动
        vibrateCheckBox.isChecked = alarm.vibrate
    }

    private fun showTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                alarm.hour = hourOfDay
                alarm.minute = minute
                updateUI()
            },
            alarm.hour,
            alarm.minute,
            true
        )
        timePickerDialog.show()
    }

    private fun showRepeatDialog() {
        val days = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val checkedItems = BooleanArray(7) { i -> alarm.repeatDays[i] }

        AlertDialog.Builder(this)
            .setTitle("重复")
            .setMultiChoiceItems(days, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("确定") { _, _ ->
                alarm.repeatDays = checkedItems
                updateUI()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSnoozeDialog() {
        val options = arrayOf("5 分钟", "10 分钟", "15 分钟", "20 分钟", "30 分钟")
        val values = intArrayOf(5, 10, 15, 20, 30)
        val selectedIndex = values.indexOf(alarm.snoozeMinutes).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("贪睡时长")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                alarm.snoozeMinutes = values[which]
                updateUI()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_alarm_edit, menu)
        // 如果是新闹钟，隐藏删除按钮
        menu.findItem(R.id.action_delete).isVisible = !isNewAlarm
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveAlarm()
                true
            }
            R.id.action_delete -> {
                deleteAlarm()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveAlarm() {
        // 更新标签
        alarm.label = labelEdit.text.toString().trim()

        val dbHelper = AlarmDatabaseHelper(this)
        if (isNewAlarm) {
            dbHelper.addAlarm(alarm)
        } else {
            dbHelper.updateAlarm(alarm)
        }

        // 设置闹钟
        alarm.isEnabled = true
        AlarmScheduler.scheduleAlarm(this, alarm)

        // 返回结果
        val resultIntent = Intent().apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun deleteAlarm() {
        AlertDialog.Builder(this)
            .setTitle("删除闹钟")
            .setMessage("确定要删除这个闹钟吗？")
            .setPositiveButton("删除") { _, _ ->
                val dbHelper = AlarmDatabaseHelper(this)
                dbHelper.deleteAlarm(alarm.id)

                // 取消闹钟
                AlarmScheduler.cancelAlarm(this, alarm)

                // 返回结果
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_ALARM_ID, alarm.id)
                }
                setResult(RESULT_ALARM_DELETED, resultIntent)
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放语音识别资源
        mIat?.cancel()
        mIat?.destroy()
    }
} 