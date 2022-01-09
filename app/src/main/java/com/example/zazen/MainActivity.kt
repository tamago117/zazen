package com.example.zazen

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.widget.Toast
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibratorManager
import android.widget.TextView
import kotlin.math.*
import java.io.IOException
import java.util.Locale
import java.lang.IllegalStateException

class MainActivity : AppCompatActivity() {
    private lateinit var manager : SensorManager
    private lateinit var listener: SensorEventListener
    private lateinit var list : List<Sensor>
    private lateinit var mp: MediaPlayer

    var gx : Float = 0.0f
    var gy : Float = 0.0f
    var gz : Float = 0.0f

    val alpha = 0.2f
    var accel_thre : Float = 0.3f
    val accel_thre_min : Float = 0.15f

    var viv_init : Boolean = true
    var start : Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //音楽プレイヤーの準備
        mp = MediaPlayer.create(this, R.raw.hit)

        //シークバーを取得
        val seek = findViewById<SeekBar>(R.id.seekBar)
        //シークバーに対してイベントリスナーを取得
        seek.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val current : Float = (seek.max - progress) * 0.02f
                if(current<accel_thre_min){
                    accel_thre = accel_thre_min
                }else{
                    accel_thre = current
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        //加速度センサーを取得
        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        list = manager.getSensorList(Sensor.TYPE_ACCELEROMETER)

        //センサー情報が変化した時の処理を記述
        listener = object : SensorEventListener{
            override fun onSensorChanged(event: SensorEvent) {
                //var accel : Float = sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2))
                //accel -= 9.76f

                //low path
                gx = alpha * event.values[0] + (1.0f-alpha) * gx
                gy = alpha * event.values[1] + (1.0f-alpha) * gy
                gz = alpha * event.values[2] + (1.0f-alpha) * gz
                //high path
                var ax : Float = event.values[0] - gx
                var ay : Float = event.values[1] - gy
                var az : Float = event.values[2] - gz
                var g : Float = sqrt(gx.pow(2) + gy.pow(2) + gz.pow(2))
                var accel : Float = sqrt(ax.pow(2) + ay.pow(2) + az.pow(2))

                //Log.d("accel", "accel_x : ${event.values[0]},accel_y : ${event.values[1]},accel_z : ${event.values[2]},")
                Log.d("accel", "accel_norm : ${accel}")

                if(accel > accel_thre){
                    val end : Long = System.currentTimeMillis()
                    if(viv_init){
                        val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        start = System.currentTimeMillis()
                        viv_init = false

                        Toast.makeText(this@MainActivity,
                                        "喝！！！", Toast.LENGTH_SHORT).show()

                        //音楽再生
                        if(!mp.isPlaying){
                            mp.start()
                        }else{
                            try{
                                mp.stop()
                                mp.prepare()
                            }catch (e: IllegalStateException){
                                e.printStackTrace()
                            }catch (e: IOException){
                                e.printStackTrace()
                            }
                        }
                    }

                    if((end - start)>1000){
                        viv_init = true
                    }
                }

            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        }
    }

    override fun onResume() {
        super.onResume()
        if(list.isNotEmpty()){
            manager.registerListener(
                listener, list[0],
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onPause() {
        super.onPause()
        manager.unregisterListener(listener, list[0])
    }
}

//https://blog.shirai.la/wp-content/uploads/downloads/2013/09/VRSJ2013_kitada.pdf
//https://otologic.jp/free/se/hit01.html