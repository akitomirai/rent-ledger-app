package com.aki.rentledger.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val icon: ImageVector
) {
    Rent(label = "公寓", icon = Icons.Rounded.Home),
    Building(label = "厂房", icon = Icons.Rounded.Apartment),
    System(label = "系统", icon = Icons.Rounded.Settings)
}
