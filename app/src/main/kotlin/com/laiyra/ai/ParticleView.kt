package com.laiyra.ai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sqrt
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Particle(
        var x: Float,
        var y: Float,
        var dx: Float,
        var dy: Float,
        var size: Float
    )

    private val particles = mutableListOf<Particle>()

    private val particlePaint = Paint().apply {
    color = Color.RED
    alpha = 255
    isAntiAlias = true
}

    init {

        repeat(50) {

            particles.add(
                Particle(
                    Random.nextFloat() * 220f,
                    Random.nextFloat() * 220f,
                    (Random.nextFloat() - 0.5f) * 0.3f,
                    (Random.nextFloat() - 0.5f) * 0.3f,
                    Random.nextFloat() * 8f + 8f
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
       canvas.drawColor(Color.RED)
        val cx = width / 2f
        val cy = height / 2f
        val radius = width / 2f - 8f

        particles.forEach { p ->

            p.x += p.dx
            p.y += p.dy

            val dist = sqrt(
                ((p.x - cx) * (p.x - cx) +
                        (p.y - cy) * (p.y - cy)).toDouble()
            ).toFloat()

            if (dist > radius) {

                p.dx *= -1
                p.dy *= -1

                p.x += p.dx * 5
                p.y += p.dy * 5
            }

            
            canvas.drawCircle(
                 p.x,
                 p.y,
                 p.size,
                 particlePaint
)
        }

        postInvalidateOnAnimation()
    }
}