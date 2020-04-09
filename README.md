# ProgressDownloadManager
## 简述
使用DownloadManager和ContentResolver API完成下载文件和查询更新下载进度
## 如何使用
代码中均使用Android SDK API，建议直接将app/src/main/java/com/labmem005/downloadmanager/ProgressDownloadManager.kt源文件拷贝到项目，方便按需修改。
## 所需权限
- android.permission.INTERNET
- android.permission.WRITE_EXTERNAL_STORAGE 当下载文件放置外部公共存储区域时需要
## 示例
```kotlin
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
            
...
override fun onDestroy() {
    super.onDestroy()
    downloadManager.unSubscribe()
}
```
