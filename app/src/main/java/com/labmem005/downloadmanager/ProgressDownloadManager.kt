package com.labmem005.downloadmanager

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.annotation.NonNull
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * <pre>
 *     author: labmem005
 *     time  : 2020/4/2
 *     desc  : 有下载进度回调的DownloadManager
 * </pre>
 */
class ProgressDownloadManager constructor(
    private val context: Context
) {

    private lateinit var url: String
    private lateinit var subPath: String

    private val downloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    private var downloadListener: DownloadListener? = null
    private val downloadHandler by lazy {
        DownloadHandler(downloadListener)
    }
    private val scheduledExecutorService by lazy {
        Executors.newSingleThreadScheduledExecutor()
    }

    private val contentObserver by lazy {
        DownloadChangeObserver()
    }
    private var downloadId: Long = 0

    /**
     * @param url 下载地址
     */
    fun setUrl(@NonNull url: String): ProgressDownloadManager{
        this.url = url
        this.subPath = getFileNameByUrl(url)
        return this
    }

    /**
     * @param subPath 下载完成后的文件名#Download/{subPath}
     */
    fun setSubPath(subPath: String): ProgressDownloadManager{
        this.subPath = subPath
        return this
    }

    /**
     * @param downloadListener 下载监听
     */
    fun setListener(downloadListener: DownloadListener?): ProgressDownloadManager {
        this.downloadListener = downloadListener
        return this
    }

    /**
     * 开始下载
     */
    fun download() {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,getFileNameByUrl(url))
        downloadListener?.onPrepare()
        val uri = Uri.parse("content://downloads/all_downloads") // 标识/Download
        context.contentResolver.registerContentObserver(uri,false,contentObserver)
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE).apply {
            addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        })
        downloadId = downloadManager.enqueue(request)

    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, intent.toString())
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1)
            when (intent.action) {
                DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                    //收到ACTION_DOWNLOAD_COMPLETE广播后，此时立刻unSubscribe会导致scheduledExecutorService查询到的已下载文件比例不足100%
                    //可根据具体情况delay几秒调用或主动发送sendMessage(1,1)
                    unSubscribe()
                    if (downloadId == -1L) return
                    val uri = downloadManager.getUriForDownloadedFile(downloadId)
                    if (uri!=null)
                        uri.path?.let {
                            downloadListener?.onSuccess(it)
                        }
                    else
                        downloadListener?.onFailed(Exception("下载失败,id=$downloadId"))
                }
                DownloadManager.ACTION_NOTIFICATION_CLICKED ->{
                    downloadListener?.onNotificationClicked()
                }
            }
        }
    }

    /**
     * 监听下载进度
     */
    private inner class DownloadChangeObserver : ContentObserver(downloadHandler) {
        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         * @param selfChange
         */
        override fun onChange(selfChange: Boolean) {
            Log.d(TAG,"on change:$selfChange")

            scheduledExecutorService.scheduleAtFixedRate(
                {
                    val bytesAndStatus = getBytesAndStatus(downloadId)
                    downloadHandler.sendMessage(
                        downloadHandler.obtainMessage(
                            HANDLE_DOWNLOAD,
                            bytesAndStatus[0],
                            bytesAndStatus[1],
                            bytesAndStatus[2]
                        )
                    )
                },
                0,
                1,
                TimeUnit.SECONDS
            ) //在子线程中查询
        }
    }

    /**
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     *
     * @param downloadId 下载任务id
     * @return 当前任务状态 pos[0]:已下载字节 pos[1]:下载总字节 pos[2]:当前下载状态
     */
    private fun getBytesAndStatus(downloadId: Long): IntArray {
        val bytesAndStatus = intArrayOf(-1, -1, 0)
        val query = DownloadManager.Query().setFilterById(downloadId)
        var cursor: Cursor? = null
        try {
            cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                bytesAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                bytesAndStatus[2] = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            }
        } finally {
            cursor?.close()
        }
        return bytesAndStatus
    }

    /**
     * 关闭定时器，线程等操作
     */
    fun unSubscribe() {
        context.unregisterReceiver(receiver)
        context.contentResolver.unregisterContentObserver(contentObserver)
        if (!scheduledExecutorService.isShutdown) {
            scheduledExecutorService.shutdownNow()
        }
        downloadHandler.removeCallbacksAndMessages(null)
    }

    interface DownloadListener{
        //初始化UI
        fun onPrepare()
        //点击通知栏回调
        fun onNotificationClicked()
        //下载进度回调
        fun onProgress(progress: Float)
        //下载完成
        fun onSuccess(path: String)
        //下载失败
        fun onFailed(throwable: Throwable)
    }

    companion object {
        private val TAG = ProgressDownloadManager::class.java.simpleName
        const val HANDLE_DOWNLOAD = 0x001

        private class DownloadHandler(val downloadListener: DownloadListener?) : Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.what == HANDLE_DOWNLOAD){
                    Log.d(TAG,"arg1=${msg.arg1},arg2=${msg.arg2}")
                    if (msg.arg1 >= 0 && msg.arg2 > 0) {
                        downloadListener?.onProgress(msg.arg1 / msg.arg2.toFloat())
                    }
                }
            }
        }

        /**
         * 通过URL获取文件名
         */
        private fun getFileNameByUrl(url: String): String {
            var filename = url.substring(url.lastIndexOf("/") + 1)
            filename = filename.substring(0, if (filename.indexOf("?") == -1) filename.length else filename.indexOf("?"))
            return filename
        }
    }

}
