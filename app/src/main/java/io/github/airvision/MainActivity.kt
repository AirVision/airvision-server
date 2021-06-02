package io.github.airvision

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Content()
    }
  }
}

@Composable
fun Content() {
  Text("Hello World!")
}
