package com.toolbox.ddj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toolbox.ddj.ui.navigation.DingDongJiNavGraph
import com.toolbox.ddj.ui.theme.DingDongJiTheme

/**
 * 应用唯一 Activity，承载 Compose 导航图。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DingDongJiApp()
        }
    }
}

@Composable
private fun DingDongJiApp() {
    DingDongJiTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DingDongJiNavGraph()
        }
    }
}
