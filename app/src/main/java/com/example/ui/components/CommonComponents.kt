package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GradeDanger
import com.example.ui.theme.GradeDangerBg
import com.example.ui.theme.GradeInfo
import com.example.ui.theme.GradeInfoBg
import com.example.ui.theme.GradeSuccess
import com.example.ui.theme.GradeSuccessBg
import com.example.ui.theme.GradeWarning
import com.example.ui.theme.GradeWarningBg
import com.example.ui.theme.ProfNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GradeBadge(
    letterGrade: String,
    percentage: Double,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when {
        letterGrade.startsWith("A") -> GradeSuccessBg to GradeSuccess
        letterGrade.startsWith("B") -> GradeInfoBg to GradeInfo
        letterGrade.startsWith("C") -> GradeWarningBg to GradeWarning
        letterGrade.startsWith("D") -> GradeWarningBg to GradeWarning
        else -> GradeDangerBg to GradeDanger
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = letterGrade,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${String.format("%.1f", percentage)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun SyncStatusChip(
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSyncing) GradeInfoBg else GradeSuccessBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                contentDescription = null,
                tint = if (isSyncing) GradeInfo else GradeSuccess,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isSyncing) "Syncing..." else "Realtime Sync",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSyncing) GradeInfo else GradeSuccess
            )
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = ProfNavy,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
