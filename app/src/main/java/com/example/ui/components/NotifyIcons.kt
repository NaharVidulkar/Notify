package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object NotifyIcons {
    val CalendarMonth: ImageVector by lazy {
        ImageVector.Builder(
            name = "CalendarMonth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 4f)
            horizontalLineToRelative(-1f)
            verticalLineTo(2f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(2f)
            horizontalLineTo(8f)
            verticalLineTo(2f)
            horizontalLineTo(6f)
            verticalLineToRelative(2f)
            horizontalLineTo(5f)
            curveTo(3.89f, 4f, 3f, 4.9f, 3f, 6f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(6f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(19f, 20f)
            horizontalLineTo(5f)
            verticalLineTo(10f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(10f)
            close()
            moveTo(19f, 8f)
            horizontalLineTo(5f)
            verticalLineTo(6f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(2f)
            close()
            moveTo(9f, 14f)
            horizontalLineTo(7f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            close()
            moveTo(13f, 14f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            close()
            moveTo(17f, 14f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            close()
            moveTo(9f, 18f)
            horizontalLineTo(7f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            close()
            moveTo(13f, 18f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            close()
            moveTo(17f, 18f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            close()
        }.build()
    }

    val Tune: ImageVector by lazy {
        ImageVector.Builder(
            name = "Tune",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 17f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(-2f)
            horizontalLineTo(3f)
            close()
            moveTo(3f, 5f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(10f)
            verticalLineTo(5f)
            horizontalLineTo(3f)
            close()
            moveTo(13f, 21f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(-8f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(2f)
            close()
            moveTo(7f, 9f)
            verticalLineToRelative(2f)
            horizontalLineTo(3f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(2f)
            verticalLineTo(9f)
            horizontalLineTo(7f)
            close()
            moveTo(21f, 13f)
            verticalLineToRelative(-2f)
            horizontalLineTo(11f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(10f)
            close()
            moveTo(15f, 9f)
            horizontalLineToRelative(2f)
            verticalLineTo(7f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineToRelative(-4f)
            verticalLineTo(3f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(6f)
            close()
        }.build()
    }

    val Schedule: ImageVector by lazy {
        ImageVector.Builder(
            name = "Schedule",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(11.99f, 2f)
            curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.47f, 10f, 9.99f, 10f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
            close()
            moveTo(12f, 20f)
            curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
            reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
            reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
            reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
            close()
            moveTo(12.5f, 7f)
            horizontalLineTo(11f)
            verticalLineToRelative(6f)
            lineToRelative(5.25f, 3.15f)
            lineToRelative(0.75f, -1.23f)
            lineToRelative(-4.5f, -2.67f)
            verticalLineTo(7f)
            close()
        }.build()
    }

    val Alarm: ImageVector by lazy {
        ImageVector.Builder(
            name = "Alarm",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(22f, 5.72f)
            lineToRelative(-4.6f, -3.86f)
            lineToRelative(-1.29f, 1.53f)
            lineToRelative(4.6f, 3.86f)
            lineTo(22f, 5.72f)
            close()
            moveTo(7.88f, 3.39f)
            lineTo(6.6f, 1.86f)
            lineTo(2f, 5.71f)
            lineToRelative(1.29f, 1.53f)
            lineToRelative(4.59f, -3.85f)
            close()
            moveTo(12.5f, 8f)
            horizontalLineTo(11f)
            verticalLineToRelative(6f)
            lineToRelative(4.75f, 2.85f)
            lineToRelative(0.75f, -1.23f)
            lineToRelative(-4f, -2.37f)
            verticalLineTo(8f)
            close()
            moveTo(12f, 4f)
            curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
            curveToRelative(0f, 4.97f, 4.02f, 9f, 9f, 9f)
            reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
            curveToRelative(0f, -4.97f, -4.03f, -9f, -9f, -9f)
            close()
            moveTo(12f, 20f)
            curveToRelative(-3.87f, 0f, -7f, -3.13f, -7f, -7f)
            reflectiveCurveToRelative(3.13f, -7f, 7f, -7f)
            reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
            reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
            close()
        }.build()
    }

    val BatteryAlert: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryAlert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(15.67f, 4f)
            horizontalLineTo(14f)
            verticalLineTo(2f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            horizontalLineTo(8.33f)
            curveTo(7.6f, 4f, 7f, 4.6f, 7f, 5.33f)
            verticalLineToRelative(15.33f)
            curveTo(7f, 21.4f, 7.6f, 22f, 8.33f, 22f)
            horizontalLineToRelative(7.33f)
            curveToRelative(0.74f, 0f, 1.34f, -0.6f, 1.34f, -1.33f)
            verticalLineTo(5.33f)
            curveTo(17f, 4.6f, 16.4f, 4f, 15.67f, 4f)
            close()
            moveTo(13f, 18f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            close()
            moveTo(13f, 14f)
            horizontalLineToRelative(-2f)
            verticalLineTo(9f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(5f)
            close()
        }.build()
    }

    val BatteryChargingFull: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryChargingFull",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(15.67f, 4f)
            horizontalLineTo(14f)
            verticalLineTo(2f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            horizontalLineTo(8.33f)
            curveTo(7.6f, 4f, 7f, 4.6f, 7f, 5.33f)
            verticalLineToRelative(15.33f)
            curveTo(7f, 21.4f, 7.6f, 22f, 8.33f, 22f)
            horizontalLineToRelative(7.33f)
            curveToRelative(0.74f, 0f, 1.34f, -0.6f, 1.34f, -1.33f)
            verticalLineTo(5.33f)
            curveTo(17f, 4.6f, 16.4f, 4f, 15.67f, 4f)
            close()
            moveTo(11f, 20f)
            verticalLineToRelative(-5.5f)
            horizontalLineTo(9f)
            lineTo(13f, 7f)
            verticalLineToRelative(5.5f)
            horizontalLineToRelative(2f)
            lineTo(11f, 20f)
            close()
        }.build()
    }

    val DoNotDisturbOn: ImageVector by lazy {
        ImageVector.Builder(
            name = "DoNotDisturbOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
            reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
            reflectiveCurveTo(17.52f, 2f, 12f, 2f)
            close()
            moveTo(17f, 13f)
            horizontalLineTo(7f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(10f)
            verticalLineToRelative(2f)
            close()
        }.build()
    }

    val NotificationsActive: ImageVector by lazy {
        ImageVector.Builder(
            name = "NotificationsActive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 22f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            horizontalLineToRelative(-4f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            close()
            moveTo(18f, 16f)
            verticalLineToRelative(-5f)
            curveToRelative(0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
            verticalLineTo(4f)
            curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
            reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
            verticalLineToRelative(0.68f)
            curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
            verticalLineToRelative(5f)
            lineToRelative(-2f, 2f)
            verticalLineToRelative(1f)
            horizontalLineToRelative(16f)
            verticalLineToRelative(-1f)
            lineToRelative(-2f, -2f)
            close()
            moveTo(7.58f, 4.08f)
            lineTo(6.15f, 2.65f)
            curveTo(3.75f, 4.48f, 2.17f, 7.3f, 2.03f, 10.5f)
            horizontalLineToRelative(2f)
            curveToRelative(0.15f, -2.65f, 1.51f, -4.97f, 3.55f, -6.42f)
            close()
            moveTo(19.97f, 10.5f)
            horizontalLineToRelative(2f)
            curveToRelative(-0.15f, -3.2f, -1.73f, -6.02f, -4.12f, -7.85f)
            lineToRelative(-1.42f, 1.43f)
            curveToRelative(2.02f, 1.45f, 3.39f, 3.77f, 3.54f, 6.42f)
            close()
        }.build()
    }

    val NotificationsNone: ImageVector by lazy {
        ImageVector.Builder(
            name = "NotificationsNone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 22f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            horizontalLineToRelative(-4f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            close()
            moveTo(18f, 16f)
            verticalLineToRelative(-5f)
            curveToRelative(0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
            verticalLineTo(4f)
            curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
            reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
            verticalLineToRelative(0.68f)
            curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
            verticalLineToRelative(5f)
            lineToRelative(-2f, 2f)
            verticalLineToRelative(1f)
            horizontalLineToRelative(16f)
            verticalLineToRelative(-1f)
            lineToRelative(-2f, -2f)
            close()
            moveTo(16f, 17f)
            horizontalLineTo(8f)
            verticalLineToRelative(-6f)
            curveToRelative(0f, -2.48f, 1.51f, -4.5f, 4f, -4.5f)
            reflectiveCurveToRelative(4f, 2.02f, 4f, 4.5f)
            verticalLineToRelative(6f)
            close()
        }.build()
    }
}
