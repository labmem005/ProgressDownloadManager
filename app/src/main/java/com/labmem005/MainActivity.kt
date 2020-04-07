package com.labmem005

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import com.labmem005.downloadmanager.ProgressDownloadManager
import com.labmem005.downloadmanager.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private val URL = "https://github.com/labmem005/dcs-flutter-plugin/archive/master.zip"
    private lateinit var downloadManager: ProgressDownloadManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        downloadManager = ProgressDownloadManager(this)
            .setUrl(URL)
            .setListener(object : ProgressDownloadManager.DownloadListener {
                override fun onPrepare() {
                    Log.d(TAG,"onPrepare")
                }

                override fun onUnknownTotalSize() {
                    Log.d(TAG,"onUnknownTotalSize")
                }

                override fun onNotificationClicked() {
                    Log.d(TAG,"onNotificationClicked")
                }

                override fun onProgress(progress: Float) {
                    Log.d(TAG,"onProgress $progress")
                    progressBar.progress = (progress * 100).toInt()
                }

                override fun onSuccess(path: String) {
                    Log.d(TAG,"onSuccess $path")
                }

                override fun onFailed(throwable: Throwable) {
                    Log.d(TAG,"onFailed $throwable")
                }
            })
        binding.download.setOnClickListener {
            downloadManager.download()
            Toast.makeText(this,"开始下载",Toast.LENGTH_SHORT).show()
        }
        binding.cancel.setOnClickListener {
            Toast.makeText(this,"下载取消",Toast.LENGTH_SHORT).show()
            downloadManager.cancel()
        }
        setContentView(binding.root)
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),0x001)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadManager.unSubscribe()
    }

}
