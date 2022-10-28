package com.mattmx.ktgui.scoreboards

import com.mattmx.ktgui.extensions.stripColor
import com.mattmx.ktgui.utils.Chat
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.lang.IndexOutOfBoundsException
import java.lang.Integer.min
import java.util.*


open class ScoreboardBuilder(
    private var title: String,
    private val displaySlot: DisplaySlot = DisplaySlot.SIDEBAR
) {
    // Holds the lines of text, we are able to remove them because of this
    private val modifies = arrayListOf<String>()

    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
    private val objective: Objective = scoreboard.registerNewObjective(title, "dummy")

    init {
        // If the title is too big then make sure to shorten it
        objective.displayName = title
        objective.displaySlot = displaySlot
    }

    /**
     * Call this if you want to dynamically change the title to something else.
     * It still must be provided in the constructor.
     *
     * @param title of the scoreboard
     */
    infix fun title(title: String): ScoreboardBuilder {
        this.title = title
        objective.displayName = this.title
        return this
    }

    /**
     * Clears the whole scoreboard.
     */
    fun clear(): ScoreboardBuilder {
        modifies.clear()
        scoreboard.resetScores(title)
        return this
    }

    /**
     * Set a specific line to something.
     *
     * @param index of the line we're replacing
     * @param line the new text
     * @throws IndexOutOfBoundsException if you specify index as more than MAX_LINES
     */
    fun set(index: Int, line: String): ScoreboardBuilder {
        if (index >= MAX_LINES) throw IndexOutOfBoundsException("Index too high. Maximum index is 15 (0-15).")
        if (index >= modifies.size) {
            repeat(index - modifies.size + 1) {
                whitespace()
            }
        }
        scoreboard.resetScores(modifies[index])
        modifies[index] = line
        objective.getScore(line).score = -index
        return this
    }

    fun setRGB(index: Int, line: String): ScoreboardBuilder {
        if (index >= MAX_LINES) throw IndexOutOfBoundsException("Index too high. Maximum index is 15 (0-15).")
        if (index >= modifies.size) {
            repeat(index - modifies.size + 1) {
                whitespace()
            }
        }
        val name = modifies[index]
        val team = scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
        team.suffix = line
        if (!team.hasEntry(name)) team.addEntry(name)
//        objective.getScore(name).score = -index
        return this
    }

    /**
     * Remove a line from the scoreboard
     *
     * @param index of the line to remove
     */
    fun remove(index: Int): ScoreboardBuilder {
        scoreboard.resetScores(modifies[index])
        return this
    }

    /**
     * Adds a line if we have space to.
     *
     * @param line contains the text value for the next line
     */
    fun add(line: String): ScoreboardBuilder {
        if (modifies.size > MAX_LINES) throw IndexOutOfBoundsException("You can't add more than 16 lines.")
        val modified = getLineCoded(line)
        objective.getScore(modified).score = -modifies.size
        modifies.add(modified)
        return this
    }

    fun addRGB(line: String) : ScoreboardBuilder {
        if (modifies.size > MAX_LINES) throw IndexOutOfBoundsException("You can't add more than 16 lines.")
        val name = Chat.color("&r").repeat(modifies.size)
        val team = scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
        team.suffix = line
        team.addEntry(name)
        objective.getScore(name).score = -modifies.size
        modifies.add(name)
        return this
    }

    /**
     * Create an empty line
     */
    fun whitespace(): ScoreboardBuilder {
        add(" ")
        return this
    }

    /**
     * Encode the text.
     *
     * @param line the we're formatting.
     */
    private fun getLineCoded(line: String): String {
        var result = line
        while (modifies.contains(result)) result += ChatColor.RESET
        return result.substring(0, min(40, result.length))
    }

    /**
     * @return the scoreboard instance
     */
    fun scoreboard(): Scoreboard {
        return scoreboard
    }

    companion object {
        const val MAX_LINES = 16
    }

}