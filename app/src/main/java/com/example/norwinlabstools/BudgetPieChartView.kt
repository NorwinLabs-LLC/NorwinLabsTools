package com.example.norwinlabstools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BudgetPieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<BudgetCategory> = emptyList()
    private var totalIncome: Double = 0.0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rectF = RectF()

    fun setData(income: Double, categories: List<BudgetCategory>) {
        this.totalIncome = income
        this.data = categories
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (totalIncome <= 0) {
            // Draw empty gray circle
            paint.color = 0xFFEEEEEE.toInt()
            canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f, paint)
            return
        }

        val size = Math.min(width, height).toFloat()
        rectF.set((width - size) / 2, (height - size) / 2, (width + size) / 2, (height + size) / 2)

        var startAngle = -90f
        var totalAllocated = 0.0

        data.forEach { category ->
            val sweepAngle = (category.amount / totalIncome * 360).toFloat()
            paint.color = category.color
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
            totalAllocated += category.amount
        }

        // Draw extra/investment slice
        val extra = totalIncome - totalAllocated
        if (extra > 0) {
            val sweepAngle = (extra / totalIncome * 360).toFloat()
            paint.color = 0xFF4CAF50.toInt() // Primary Green for investment
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
        }
    }
}

data class BudgetCategory(
    val name: String,
    val amount: Double,
    val color: Int
)