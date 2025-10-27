package com.example.grid.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.grid.view.adapter.PhotoAdapter
import com.example.grid.view.adapter.FolderAdapter
import com.example.grid.R
import com.example.grid.databinding.ActivityMainBinding
import com.example.grid.ui.theme.GridTheme
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.grid.data.local.AppDatabase
import com.example.grid.data.local.ImageEntity
import com.example.grid.data.local.FolderEntity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.app.AlertDialog
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private val photoList = mutableListOf<ImageEntity>()
    private val folderList = mutableListOf<FolderEntity>()  // ✅ 폴더 리스트 추가
    private lateinit var database: AppDatabase
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var folderAdapter: FolderAdapter  // ✅ 폴더 어댑터 추가
    private val selectedPositions = mutableSetOf<Int>()
    private var actionMode: ActionMode? = null
    private var currentFolderPosition: Int? = null

    // ✅ 뷰 모드 관리
    private var viewMode: ViewMode = ViewMode.FOLDERS
    private var currentFolderId: Int? = null

    enum class ViewMode {
        FOLDERS,  // 폴더 목록 보기
        IMAGES    // 이미지 목록 보기
    }

    // gallery 에서 이미지를 가져오기 위한 ActivityResultLauncher 초기화
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { // ✅ 폴더 선택 다이얼로그 표시로 변경k
            showFolderSelectionDialog(it, currentFolderPosition)
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

        // ✅ FolderAdapter 초기화
        folderAdapter =
            FolderAdapter(folderList, mutableMapOf(), onFolderClick = { folder ->
                openFolder(folder)
            }, onFolderLongClick = { folder ->
                android.util.Log.d(
                    "MainActivity", "Folder long-clicked: ${folder.name}"
                )
            })

        // ✅ 초기에는 GridLayout (2열)으로 폴더 표시
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = folderAdapter

        // ✅ FAB 버튼 동작 - 뷰 모드에 따라 다른 동작
        binding.fabAddPhoto.setOnClickListener {
            if (viewMode == ViewMode.FOLDERS) {
                showCreateFolderDialog()  // 폴더 생성
            } else {
                pickImageLauncher.launch("image/*")  // 이미지 추가
            }
        }

        // 권한 확인 및 요청
        checkPermissions()

        // ✅ 앱 시작 시 폴더 목록 로드
        loadFolders()

        // 디버깅용: 데이터베이스 상태 확인
        checkDatabaseStatus()

        // ✅ 뒤로가기 처리 (OnBackPressedDispatcher 사용)
        onBackPressedDispatcher.addCallback(
            this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewMode == ViewMode.IMAGES) { // 이미지 뷰에서 폴더 뷰로 돌아가기
                        viewMode = ViewMode.FOLDERS
                        currentFolderId = null

                        binding.recyclerView.layoutManager =
                            GridLayoutManager(this@MainActivity, 2)
                        binding.recyclerView.adapter = folderAdapter

                        loadFolders()

                        // 상단 텍스트를 "갤러리"로 변경
                        binding.tvCurrentFolder.text = "/home"
                    } else { // 기본 뒤로가기 동작
                        isEnabled = false
                        this@MainActivity.onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
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
        super.onResume()

        // ✅ 현재 뷰 모드에 따라 적절한 데이터 로드
        when (viewMode) {
            ViewMode.FOLDERS -> { // 폴더 목록 뷰: 폴더 목록 새로고침
                loadFolders()
            }

            ViewMode.IMAGES -> { // 이미지 목록 뷰: 현재 폴더의 이미지만 새로고침
                currentFolderId?.let { folderId ->
                    loadImagesFromFolder(folderId)
                }
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

    // ==================== 폴더 관련 메서드 ====================

    /**
     * 폴더 목록을 데이터베이스에서 로드합니다
     */
    private fun loadFolders() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "loadFolders() 시작")

                val folders = database.folderDao().getAllFolders()
                android.util.Log.d("MainActivity", "DB에서 가져온 폴더 개수: ${folders.size}")

                folderList.clear()
                folderList.addAll(folders)

                // 각 폴더의 이미지 개수 가져오기
                folders.forEach { folder ->
                    val count = database.folderDao().getImageCountInFolder(folder.id)
                    folderAdapter.updateImageCount(folder.id, count)
                }

                folderAdapter.notifyDataSetChanged()
                android.util.Log.d("MainActivity", "Loaded ${folders.size} folders")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error loading folders", e)
            }
        }
    }

    /**
     * 폴더를 열어 해당 폴더의 이미지 목록을 표시합니다
     */
    private fun openFolder(folder: FolderEntity) {
        viewMode = ViewMode.IMAGES
        currentFolderId = folder.id
        currentFolderPosition = folderList.indexOf(folder)

        // RecyclerView를 LinearLayout으로 변경
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = photoAdapter

        // 해당 폴더의 이미지들 로드
        loadImagesFromFolder(folder.id)

        // 상단에 현재 경로를 Linux 스타일로 표시
        binding.tvCurrentFolder.text = "/home/${folder.name}"

        android.util.Log.d("MainActivity", "Opened folder: ${folder.name}")
    }

    /**
     * 특정 폴더의 이미지들을 로드합니다
     */
    private fun loadImagesFromFolder(folderId: Int) {
        lifecycleScope.launch {
            try {
                val images = database.imageDao().getImagesByFolder(folderId)
                photoList.clear()
                photoList.addAll(images)
                photoAdapter.notifyDataSetChanged()

                android.util.Log.d(
                    "MainActivity",
                    "Loaded ${images.size} images from folder $folderId"
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainActivity", "Error loading images from folder", e
                )
            }
        }
    }

    /**
     * 새 폴더 생성 다이얼로그를 표시합니다
     */
    private fun showCreateFolderDialog() {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_create_folder, null)
        val etFolderName = dialogView.findViewById<EditText>(R.id.etFolderName)

        AlertDialog.Builder(this).setView(dialogView)
                .setPositiveButton("생성") { _, _ ->
                    val folderName = etFolderName.text.toString().trim()
                    if (folderName.isNotEmpty()) {
                        createFolder(folderName)
                    } else {
                        Toast.makeText(this, "폴더 이름을 입력하세요", Toast.LENGTH_SHORT)
                                .show()
                    }
                }.setNegativeButton("취소", null).show()
    }

    /**
     * 폴더를 생성합니다
     */
    private fun createFolder(folderName: String) {
        lifecycleScope.launch {
            try { // 같은 이름의 폴더가 있는지 확인
                val existingFolder = database.folderDao().getFolderByName(folderName)
                if (existingFolder != null) {
                    Toast.makeText(
                        this@MainActivity, "같은 이름의 폴더가 이미 존재합니다", Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val newFolder = FolderEntity(name = folderName)
                val folderId = database.folderDao().insertFolder(newFolder)

                // UI 업데이트
                val createdFolder = newFolder.copy(id = folderId.toInt())
                folderAdapter.addFolder(createdFolder)

                Toast.makeText(
                    this@MainActivity, "폴더가 생성되었습니다", Toast.LENGTH_SHORT
                ).show()

                android.util.Log.d("MainActivity", "Created folder: $folderName")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error creating folder", e)
                Toast.makeText(
                    this@MainActivity, "폴더 생성에 실패했습니다", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 이미지 추가 시 폴더 선택 다이얼로그를 표시합니다
     */
    private fun showFolderSelectionDialog(imageUri: Uri, folderPosition: Int?) {
        lifecycleScope.launch {
            // 현재 폴더에 자동 추가 (폴더 안에 있을 때)
            if (folderPosition != null && folderPosition >= 0 && folderPosition < folderList.size) {
                val currentFolder = folderList[folderPosition]
                saveImageToDatabase(imageUri, currentFolder.id)
                return@launch
            }
            
            // 폴더 목록에서 선택
            val folders = database.folderDao().getAllFolders()
            
            if (folders.isEmpty()) { // 폴더가 없으면 먼저 폴더를 생성하도록 안내
                AlertDialog.Builder(this@MainActivity).setTitle("폴더가 없습니다")
                        .setMessage("먼저 폴더를 생성해주세요").setPositiveButton("확인", null)
                        .show()
                return@launch
            }

            // 폴더 이름 배열 생성
            val folderNames = folders.map { it.name }.toTypedArray()

            AlertDialog.Builder(this@MainActivity).setTitle("폴더 선택")
                    .setItems(folderNames) { _, which ->
                        val selectedFolder = folders[which]
                        saveImageToDatabase(imageUri, selectedFolder.id)
                    }.setNegativeButton("취소", null).show()
        }
    }

    /**
     * 이미지를 데이터베이스에 저장합니다 (폴더 ID 포함)
     */
    private fun saveImageToDatabase(uri: Uri, folderId: Int) {
        lifecycleScope.launch {
            try {
                contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val copiedUri = copyImageToInternalStorage(uri)

                val newImage = ImageEntity(
                    title = "새 이미지",
                    imageUri = copiedUri?.toString() ?: uri.toString(),
                    description = "갤러리에서 추가된 사진",
                    folderId = folderId
                )
                database.imageDao().insertImage(newImage)

                // 현재 폴더를 보고 있다면 목록 다시 로드
                if (viewMode == ViewMode.IMAGES && currentFolderId == folderId) {
                    loadImagesFromFolder(folderId)
                }

                Toast.makeText(
                    this@MainActivity, "이미지가 추가되었습니다", Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainActivity", "Error saving image to database", e
                )
                Toast.makeText(
                    this@MainActivity, "이미지 저장에 실패했습니다", Toast.LENGTH_SHORT
                ).show()
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