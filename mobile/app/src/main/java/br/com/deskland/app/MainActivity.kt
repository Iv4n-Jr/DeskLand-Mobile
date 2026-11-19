package br.com.deskland.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.deskland.app.ui.DesklandTema
import br.com.deskland.app.ui.LojaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DesklandTema {
                LojaApp()
            }
        }
    }
}
