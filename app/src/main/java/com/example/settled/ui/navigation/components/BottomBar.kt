package com.example.settled.ui.navigation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settled.ui.theme.SettledTheme

@Composable
fun CustomBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val accentColor = Color(0xFF4A5BB6)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
            .background(Color.Transparent)
    ) {
        // 1. Main Bar Background
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {}

        // 2. Animated Indicator Layer (Weighted Row system for perfect centering)
        val weightLeading by animateFloatAsState(
            targetValue = selectedTab.toFloat(),
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
            label = "WeightLeading"
        )
        val weightTrailing by animateFloatAsState(
            targetValue = (2 - selectedTab).toFloat(),
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
            label = "WeightTrailing"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        ) {
            // This spacer takes up space for the items BEFORE the selected one
            if (weightLeading > 0f) Spacer(modifier = Modifier.weight(weightLeading))
            
            // This Box represents the 1/3 segment of the selected tab
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = accentColor),
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor
                ) {}
            }
            
            // This spacer takes up space for the items AFTER the selected one
            if (weightTrailing > 0f) Spacer(modifier = Modifier.weight(weightTrailing))
        }

        // 3. Tab Content Layer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(105.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            BottomBarItem(
                label = "DASHBOARD",
                icon = Icons.Default.Dashboard,
                isSelected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(0) }
            )
            BottomBarItem(
                label = "ADD CARD",
                icon = Icons.Default.Add,
                isSelected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(1) }
            )
            BottomBarItem(
                label = "SETTINGS",
                icon = Icons.Default.Settings,
                isSelected = selectedTab == 2,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
fun BottomBarItem(
    label: String, 
    icon: ImageVector, 
    isSelected: Boolean, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentColor = Color(0xFF4A5BB6)
    val interactionSource = remember { MutableInteractionSource() }

    // Floating Icon vertical offset
    val iconVerticalOffset by animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "IconFloat"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .offset(y = iconVerticalOffset)
                .size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Bold,
            color = if (isSelected) accentColor else Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp),
            letterSpacing = 0.5.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BottomBarPreview() {
    SettledTheme {
        var selectedTab by remember { mutableStateOf(0) }
        Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFF1F1F1))) {
            CustomBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}
