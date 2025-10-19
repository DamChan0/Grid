package com.example.grid.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grid.view.adapter.PhotoAdapter
import com.example.grid.R
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
import android.app.AlertDialog
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding           // ActivityMainBinding 클래스 선언
    private val photoList =
        mutableListOf<ImageEntity>()              // Photo 클래스 리스트 선언
    private lateinit var database: AppDatabase                // AppDatabase 클래스 선언
    private lateinit var photoAdapter: PhotoAdapter           // PhotoAdapter 클래스 선언
    private val selectedPositions = mutableSetOf<Int>()
    private var actionMode: ActionMode? = null

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

        photoAdapter = PhotoAdapter(
            photoList,
            onItemLongClick = { pos -> // 롱클릭은 포지션(Int)을 받습니다.
                val entity = photoList.getOrNull(pos)
                android.util.Log.d(
                    "MainActivity", "Item long-clicked: ${entity?.id}"
                ) // 롱클릭 시 선택 모드 시작
                startSelectionMode(pos)
            },
            onItemClick = { pos -> // 클릭 콜백도 포지션을 받습니다.
                val entity = photoList.getOrNull(pos)
                android.util.Log.d(
                    "MainActivity", "Item clicked: ${entity?.id}"
                ) // 필요 시 클릭 시 동작 추가 (예: 토글 선택)
                // toggleSelection(pos)
            })
        android.util.Log.d(
            "MainActivity", "PhotoAdapter created with ${photoList.size} items"
        )
        android.util.Log.d(
            "MainActivity", "PhotoAdapter created with ${photoList.size} items"
        )

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

    private fun startSelectionMode(position: Int) {
        if (actionMode != null) return
        photoAdapter.multiSelectMode = true
        photoAdapter.toggleSelection(position)

        actionMode = startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.selection_menu, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

            override fun onActionItemClicked(
                mode: ActionMode, item: MenuItem
            ): Boolean {
                return when (item.itemId) {
                    R.id.action_delete -> {
                        confirmDelete()
                        true
                    }

                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                photoAdapter.multiSelectMode = false
                photoAdapter.clearSelection()
                actionMode = null
            }
        })
    }

    private fun toggleSelection(pos: Int) {
        if (selectedPositions.contains(pos)) {
            selectedPositions.remove(pos)
        } else {
            selectedPositions.add(pos)
        }
        photoAdapter.notifyItemChanged(pos)
    }

    private fun confirmDelete() {
        val selectedPos = photoAdapter.getSelectedItems()
        if (selectedPos.isEmpty()) {
            return
        }

        android.app.AlertDialog.Builder(this).setTitle("삭제 확인")
                .setMessage("선택한 사진을 삭제하시겠습니까?").setPositiveButton("삭제") { _, _ ->
                    deleteSelectedPhotos(selectedPos)
                }.setNegativeButton("취소", null).show()
    }

    private fun deleteSelectedPhotos(selectedPositions: Set<Int>) {
        if (selectedPositions.isEmpty()) return

        // 삭제할 엔티티 ID를 먼저 수집합니다 (DB 삭제를 위해 필요)
        val idsToDelete =
            selectedPositions.mapNotNull { idx -> photoList.getOrNull(idx)?.id }
                    .toList()

        if (idsToDelete.isEmpty()) return

        // DB 삭제를 먼저 수행한 뒤 UI를 갱신합니다.
        lifecycleScope.launch {
            try { // DAO의 deleteByIds는 suspend이므로 코루틴에서 호출합니다.
                database.imageDao().deleteByIds(idsToDelete)

                // DB 삭제 성공 시 UI 갱신은 메인 스레드에서 수행
                runOnUiThread {
                    photoAdapter.removeItemsAtPositions(selectedPositions)
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "삭제되었습니다",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to delete items", e)
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "삭제에 실패했습니다",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
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
                android.util.Log.d(
                    "MainActivity", "Database status: $count images found"
                )

                if (count == 0) {
                    android.util.Log.w(
                        "MainActivity",
                        "Database is empty - this might be a fresh install or data was cleared"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainActivity", "Error checking database status", e
                )
            }
        }
    }

    override fun onResume() {
        super.onResume() // 앱이 다시 활성화될 때마다 데이터 새로고침
        loadImagesFromDatabase()
    }


    private fun loadImagesFromDatabase() {
        lifecycleScope.launch { // 코루틴 시작
            try {
                android.util.Log.d("MainActivity", "Loading images from database...")
                val images = database.imageDao().getAllImages()
                android.util.Log.d(
                    "MainActivity", "Loaded ${images.size} images from database"
                )

                // 데이터베이스가 비어있는 경우 로그
                if (images.isEmpty()) {
                    android.util.Log.w(
                        "MainActivity", "Database is empty - no images found"
                    )
                } else { // 각 이미지 정보 로그
                    images.forEachIndexed { index, image ->
                        android.util.Log.d(
                            "MainActivity",
                            "Image $index: ${image.title} - ${image.imageUri}"
                        )
                    }
                }

                photoList.clear()
                photoList.addAll(images)

                android.util.Log.d(
                    "MainActivity", "Updated photoList size: ${photoList.size}"
                )

                // UI 스레드에서 어댑터 업데이트
                runOnUiThread {
                    photoAdapter.notifyDataSetChanged()
                    android.util.Log.d(
                        "MainActivity", "Adapter notified of data change"
                    )

                    // RecyclerView 강제 새로고침
                    binding.recyclerView.invalidate()
                    binding.recyclerView.requestLayout()
                }
            } catch (e: Exception) { // 에러 처리
                android.util.Log.e("MainActivity", "Error loading images", e)
                e.printStackTrace()
            }
        }
    }

    // RoomDB에 이미지 저장하는 함수
    private fun saveImageToDatabase(uri: Uri) {
        lifecycleScope.launch {
            try { // URI 권한 부여 (Photo Picker URI의 경우 필요)
                contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

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
            } catch (e: Exception) { // 에러 처리
                android.util.Log.e(
                    "MainActivity", "Error saving image to database", e
                )
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

            android.util.Log.d(
                "MainActivity", "Image copied to: ${file.absolutePath}"
            )
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