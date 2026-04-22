package com.example.aliveplease.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aliveplease.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarView(
    checkInDates: Set<String>,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember {
        mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) })
    }

    val year = currentMonth.get(Calendar.YEAR)
    val month = currentMonth.get(Calendar.MONTH)
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = currentMonth.get(Calendar.DAY_OF_WEEK)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayString = dateFormat.format(Date())

    // 當月打卡天數
    val checkedThisMonth = checkInDates.count { date ->
        date.startsWith("${year}-${(month + 1).toString().padStart(2, '0')}")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(AppColors.SurfaceMid.copy(0.9f), AppColors.SurfaceLight.copy(0.7f))
                )
            )
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── 月份標題列 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = currentMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        currentMonth = newCal
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceDark)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "上個月",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${year}年 ${month + 1}月",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "本月打卡 $checkedThisMonth 天",
                        fontSize = 11.sp,
                        color = AppColors.PrimaryGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = {
                        val newCal = currentMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        currentMonth = newCal
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceDark)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "下個月",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 星期標題 ──
            val daysOfWeek = listOf("日", "一", "二", "三", "四", "五", "六")
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEachIndexed { i, day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        color = if (i == 0) AppColors.Error.copy(0.8f) else AppColors.TextHint,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 日曆格 ──
            val emptyCells = firstDayOfWeek - 1
            val totalCells = emptyCells + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - emptyCells + 1

                        if (day in 1..daysInMonth) {
                            val calDay = currentMonth.clone() as Calendar
                            calDay.set(Calendar.DAY_OF_MONTH, day)
                            val dateString = dateFormat.format(calDay.time)
                            val isCheckedIn = checkInDates.contains(dateString)
                            val isToday = todayString == dateString
                            val isSunday = col == 0

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(3.dp)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .then(
                                        when {
                                            isCheckedIn -> Modifier.background(
                                                Brush.radialGradient(
                                                    listOf(AppColors.PrimaryGreen, AppColors.PrimaryGreenDim)
                                                )
                                            )
                                            isToday -> Modifier
                                                .background(AppColors.SurfaceDark)
                                                .border(1.5.dp, AppColors.PrimaryGreen, CircleShape)
                                            else -> Modifier.background(Color.Transparent)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = when {
                                        isCheckedIn -> Color.White
                                        isToday -> AppColors.PrimaryGreen
                                        isSunday -> AppColors.Error.copy(0.7f)
                                        else -> AppColors.TextSecondary
                                    },
                                    fontWeight = if (isCheckedIn || isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
