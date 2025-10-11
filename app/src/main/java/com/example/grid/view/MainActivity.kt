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

        photoAdapter = PhotoAdapter(photoList)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // this allocating adpater is call onBindViewHolder onCreateViewHolder at the same time
        binding.recyclerView.adapter = photoAdapter

        // ✅ "사진 추가" 버튼 클릭 시 갤러리 열기
        binding.fabAddPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 앱 시작 시 데이터베이스에서 이미지 로드
        loadImagesFromDatabase()
    }

    override fun onResume() {
        super.onResume()
        // 앱이 다시 활성화될 때마다 데이터 새로고침
        loadImagesFromDatabase()
    }


    private fun loadImagesFromDatabase() {
        lifecycleScope.launch { // 코루틴 시작
            try {
                val images = database.imageDao().getAllImages()
                photoList.clear()
                photoList.addAll(images)
                
                // UI 스레드에서 어댑터 업데이트
                runOnUiThread {
                    photoAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                // 에러 처리
                e.printStackTrace()
            }
        }
    }

    // RoomDB에 이미지 저장하는 함수
    private fun saveImageToDatabase(uri: Uri) {
        lifecycleScope.launch {
            try {
                val newImage = ImageEntity(
                    title = "새 이미지",
                    imageUri = uri.toString(),
                    description = "갤러리에서 추가된 사진"
                )
                database.imageDao().insertImage(newImage)

                // DB에 저장 후 목록 다시 로드
                loadImagesFromDatabase()
            } catch (e: Exception) {
                // 에러 처리
                e.printStackTrace()
            }
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