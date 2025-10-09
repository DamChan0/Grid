package com.example.grid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grid.databinding.ActivityMainBinding
import com.example.grid.ui.theme.GridTheme

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dummyData = listOf(
            Photo("멋진 풍경 1", "https://picsum.photos/id/10/200/200"),
            Photo("강아지 사진", "https://picsum.photos/id/237/200/200"),
            Photo("노트북 작업", "https://picsum.photos/id/1/200/200"),
            Photo("커피 한잔", "https://picsum.photos/id/30/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("푸른 하늘", "https://picsum.photos/id/54/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/28/200/200"),
            Photo("숲속 길", "https://picsum.photos/id/237/200/300"),

            )

        val photoAdapter = PhotoAdapter(dummyData)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // this allocating adpater is call onBindViewHolder onCreateViewHolder at the same time
        binding.recyclerView.adapter = photoAdapter

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