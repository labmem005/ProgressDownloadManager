package com.labmem005

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import com.labmem005.downloadmanager.ProgressDownloadManager
import com.labmem005.downloadmanager.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private val URL = "https://github.com/labmem005/ProgressDownloadManager/archive/master.zip"
    private lateinit var downloadManager: ProgressDownloadManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        binding.download.setOnClickListener {
            ProgressDownloadManager(this)
                .setUrl(URL)
                .setListener(object : ProgressDownloadManager.DownloadListener {
                    override fun onPrepare() {
                        Log.d(TAG,"onPrepare")
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
                .download()
        }
        setContentView(binding.root)
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),0x001)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadManager.unSubscribe()
    }

}
