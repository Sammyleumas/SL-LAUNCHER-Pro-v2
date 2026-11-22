package com.sllauncher.pro.catalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sllauncher.core.designsystem.catalogue.SLComponentCatalogue

/** Debug-only screen listing every SL component with sample data. */
class DesignCatalogueActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SLComponentCatalogue() }
    }
}
