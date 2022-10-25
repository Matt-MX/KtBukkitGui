package com.mattmx.ktgui.scoreboards

import org.bukkit.Bukkit
import org.bukkit.entity.Animals
import org.bukkit.plugin.java.JavaPlugin

open class AnimatedScoreboardBuilder(
    title: String,
    val updateEvery: Long = 2,
) : ScoreboardBuilder(title) {
    private var update: ((AnimatedScoreboardBuilder) -> Unit)? = null
    private var cancel = false
    private var started: Long = 0L
    private var ticks: Long = 0L

    fun begin(plugin: JavaPlugin) : AnimatedScoreboardBuilder {
        started = System.currentTimeMillis()
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, { it ->
            if (!cancel) update?.invoke(this)
            else {
                it.cancel()
            }
            ticks++
        } ,0, updateEvery)
        return this
    }

    fun getTicksPassed() : Long {
        return ticks
    }

    fun getTimeRunning() : Long {
        return System.currentTimeMillis() - started
    }

    fun cancel() : AnimatedScoreboardBuilder {
        cancel = true
        return this
    }

    fun update(scoreboard: AnimatedScoreboardBuilder.() -> Unit) : AnimatedScoreboardBuilder {
        this.update = scoreboard
        return this
    }

}