package com.example.grid.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grid.view.adapter.PhotoAdapter
import com.example.grid.data.model.Photo
import com.example.grid.databinding.ActivityMainBinding
import com.example.grid.ui.theme.GridTheme
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.grid.data.local.AppDatabase
import com.example.grid.data.local.ImageEntity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding           // ActivityMainBinding 클래스 선언
    private val photoList =
        mutableListOf<ImageEntity>()              // Photo 클래스 리스트 선언
    private lateinit var database: AppDatabase                // AppDatabase 클래스 선언
    private lateinit var photoAdapter: PhotoAdapter           // PhotoAdapter 클래스 선언

    // gallery 에서 이미지를 가져오기 위한 ActivityResultLauncher 초기화
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { // RoomDB에 이미지 저장
            saveImageToDatabase(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DB instance 가져오기 
        database = AppDatabase.getDatabase(this)
        
        // 데이터베이스 경로 확인 (디버깅용)
        val dbPath = database.openHelper.readableDatabase.path
        android.util.Log.d("MainActivity", "Database path: $dbPath")

        photoAdapter = PhotoAdapter(photoList)
        android.util.Log.d("MainActivity", "PhotoAdapter created with ${photoList.size} items")

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // this allocating adpater is call onBindViewHolder onCreateViewHolder at the same time
        binding.recyclerView.adapter = photoAdapter

        // ✅ "사진 추가" 버튼 클릭 시 갤러리 열기
        binding.fabAddPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 권한 확인 및 요청
        checkPermissions()
        
        // 앱 시작 시 데이터베이스에서 이미지 로드
        loadImagesFromDatabase()
        
        // 디버깅용: 데이터베이스 상태 확인
        checkDatabaseStatus()
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }
    }
    
    // 데이터베이스 상태 확인 함수
    private fun checkDatabaseStatus() {
        lifecycleScope.launch {
            try {
                val count = database.imageDao().getAllImages().size
                android.util.Log.d("MainActivity", "Database status: $count images found")
                
                if (count == 0) {
                    android.util.Log.w("MainActivity", "Database is empty - this might be a fresh install or data was cleared")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error checking database status", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 앱이 다시 활성화될 때마다 데이터 새로고침
        loadImagesFromDatabase()
    }


    private fun loadImagesFromDatabase() {
        lifecycleScope.launch { // 코루틴 시작
            try {
                android.util.Log.d("MainActivity", "Loading images from database...")
                val images = database.imageDao().getAllImages()
                android.util.Log.d("MainActivity", "Loaded ${images.size} images from database")
                
                // 데이터베이스가 비어있는 경우 로그
                if (images.isEmpty()) {
                    android.util.Log.w("MainActivity", "Database is empty - no images found")
                } else {
                    // 각 이미지 정보 로그
                    images.forEachIndexed { index, image ->
                        android.util.Log.d("MainActivity", "Image $index: ${image.title} - ${image.imageUri}")
                    }
                }
                
                photoList.clear()
                photoList.addAll(images)
                
                android.util.Log.d("MainActivity", "Updated photoList size: ${photoList.size}")
                
                // UI 스레드에서 어댑터 업데이트
                runOnUiThread {
                    photoAdapter.notifyDataSetChanged()
                    android.util.Log.d("MainActivity", "Adapter notified of data change")
                    
                    // RecyclerView 강제 새로고침
                    binding.recyclerView.invalidate()
                    binding.recyclerView.requestLayout()
                }
            } catch (e: Exception) {
                // 에러 처리
                android.util.Log.e("MainActivity", "Error loading images", e)
                e.printStackTrace()
            }
        }
    }

    // RoomDB에 이미지 저장하는 함수
    private fun saveImageToDatabase(uri: Uri) {
        lifecycleScope.launch {
            try {
                // URI 권한 부여 (Photo Picker URI의 경우 필요)
                contentResolver.takePersistableUriPermission(uri, 
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                // 이미지를 앱 내부 저장소로 복사
                val copiedUri = copyImageToInternalStorage(uri)
                
                val newImage = ImageEntity(
                    title = "새 이미지",
                    imageUri = copiedUri?.toString() ?: uri.toString(),
                    description = "갤러리에서 추가된 사진"
                )
                database.imageDao().insertImage(newImage)

                // DB에 저장 후 목록 다시 로드
                loadImagesFromDatabase()
            } catch (e: Exception) {
                // 에러 처리
                android.util.Log.e("MainActivity", "Error saving image to database", e)
                e.printStackTrace()
            }
        }
    }
    
    // 이미지를 앱 내부 저장소로 복사하는 함수
    private fun copyImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "image_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(filesDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            android.util.Log.d("MainActivity", "Image copied to: ${file.absolutePath}")
            Uri.fromFile(file)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error copying image", e)
            null
        }
    }


    //        enableEdgeToEdge()
    //        setContent {
    //            GridTheme {
    //                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    //                    Greeting(
    //                        name = "Android",
    //                        modifier = Modifier.padding(innerPadding)
    //                    )
    //                }
    //            }
    //        }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GridTheme {
        Greeting("Android")
    }
}