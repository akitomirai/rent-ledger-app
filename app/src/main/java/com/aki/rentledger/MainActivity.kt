package com.aki.rentledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aki.rentledger.ui.theme.RentLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RentLedgerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RentLedgerApp()
                }
            }
        }
    }
}

