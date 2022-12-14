package com.mattmx.ktgui.commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

open class SimpleCommandBuilder(
    var name: String = "",
    var permission: String? = null,
    vararg alias: String
) {
    var aliases = arrayListOf(*alias)
    var description: String? = ""
    val subCommands = arrayListOf<SimpleCommandBuilder>()
    var suggestSubCommands = false
    var playerOnly = false
    private var suggests: (CommandInvocation.() -> List<String>?)? = null
    private var execute: (CommandInvocation.() -> Unit)? = null
    private var unknown: (CommandInvocation.() -> Unit)? = null
    var noPermissions: (CommandInvocation.() -> Unit)? = null
        private set

    infix fun permission(permission: String) : SimpleCommandBuilder {
        this.permission = permission
        return this
    }

    infix fun alias(alias: String) : SimpleCommandBuilder {
        this.aliases.add(alias)
        return this
    }

    fun noPermissions(cb: (CommandInvocation) -> Unit) : SimpleCommandBuilder {
        noPermissions = cb
        return this
    }

    infix fun subCommand(commandBuilder: SimpleCommandBuilder) : SimpleCommandBuilder {
        subCommands.add(commandBuilder)
        return this
    }

    infix fun hasPermission(executor: CommandSender) : Boolean {
        // todo check for subcommand permissions
        return permission == null || executor.hasPermission(permission!!)
    }

    fun executes(execute: (CommandInvocation) -> Unit) : SimpleCommandBuilder {
        this.execute = execute
        return this
    }

    fun unknownSubcommand(unknown: (CommandInvocation) -> Unit) : SimpleCommandBuilder {
        this.unknown = unknown
        return this
    }

    fun unknown(executor: CommandSender, args: List<String>, lastArg: String, alias: String) {
        unknown?.let { it(CommandInvocation(executor, args, lastArg, alias)) }
    }

    fun executeFor(executor: CommandSender, args: List<String>, lastArg: String, alias: String) {
        execute?.let { it(CommandInvocation(executor, args, lastArg, alias)) }
    }

    fun suggests(suggest: (CommandInvocation) -> List<String>?) {
        this.suggests = suggest
    }

    fun allAliases() : List<String> {
        return aliases.toMutableList() + name
    }

    fun getSuggetions(invocation: CommandInvocation) : List<String> {
        suggests?.also {
            return it(invocation) ?: listOf()
        } ?: run {
            if (suggestSubCommands) {
                return subCommands
                    .filter { it.hasPermission(invocation.source) }
                    .map { it.allAliases() }
                    .flatten()
                    .filter { it.startsWith(invocation.lastArg) }
            }
        }
        return listOf()
    }

    infix fun suggestSubCommands(value: Boolean) : SimpleCommandBuilder {
        this.suggestSubCommands = value
        return this
    }

    fun register(isInConfig: Boolean = false) {
        if (isInConfig) {
            Bukkit.getPluginCommand(name)?.setExecutor(DummyCommandExecutor(this))
        } else {
            val cmdMapField = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            cmdMapField.isAccessible = true
            val cmdMap = cmdMapField.get(Bukkit.getServer()) as CommandMap
            cmdMap.register(name, DummyCommand(this))
        }
    }

    fun couldBeCommand(arg: String) : Boolean {
        return name.startsWith(arg) || aliases.any { it.startsWith(arg) }
    }

    fun isCommand(arg: String) : Boolean {
        return name == arg || aliases.any { it == arg }
    }

    fun getCommand(args: List<String>) : SimpleCommandBuilder? {
        if (args.isEmpty()) return this
        subCommands.forEach { cmd ->
            if (cmd.isCommand(args[0])) {
                // if this is the last argument then this is the command
                return if (args.size > 1) cmd.getCommand(args.subList(1, args.size)) else cmd
            }
        }
        return this
    }

}

class CommandInvocation(
    val source: CommandSender,
    val args: List<String>,
    val lastArg: String,
    val alias: String
) {
    fun player() : Player {
        return source as Player
    }

    operator fun get(index: Int) : String? {
        if (index >= args.size) return null
        return args[index]
    }

    fun isNotEmpty() : Boolean = args.isNotEmpty()
    fun isEmpty() : Boolean = args.isEmpty()
}

inline fun simpleCommand(cmd: (SimpleCommandBuilder.() -> Unit)) : SimpleCommandBuilder {
    val cmdB = SimpleCommandBuilder()
    cmd(cmdB)
    return cmdB
}
