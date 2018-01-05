package com.venantvr.RunInBackgroundPermissionSetter

import eu.chainfire.libsuperuser.Shell
import java.util.concurrent.CompletableFuture

/**
 * Created by Pavel Sikun on 16.07.17.
 */

typealias Callback = (isSuccess: Boolean) -> Unit

private val shell by lazy {
    Shell.Builder()
            .setShell("su")
            .open()
}

fun checkRunInBackgroundPermission(pkg: String): CompletableFuture<Boolean> {
    val future = CompletableFuture<Boolean>()
    shell.addCommand(

            // current : dumpsys activity activities | grep mFocusedActivity

            "pidof $pkg"
            // "cmd package query-receivers --brief -a android.intent.action.BOOT_COMPLETED"
            /* "cmd appops get $pkg android.permission.RECEIVE_BOOT_COMPLETED" */, 1) { _, _, output: MutableList<String> ->
        val outputString = output.joinToString()
        var numeric = false;

        if (output.size == 1)
        {
            try {
                val num = output[0].toInt()
                numeric = true
            } catch (e: NumberFormatException) {
                numeric = false
            }
        }

        val runInBackgroundEnabled = numeric // outputString.contains(pkg/*"allow"*/)
        future.complete(runInBackgroundEnabled)
    }

    return future
}

fun setRunInBackgroundPermission(pkg: String, setEnabled: Boolean, callback: Callback): CompletableFuture<Boolean> {
    val future = CompletableFuture<Boolean>()
    val cmd = if (setEnabled) "grant" else "revoke" // "allow" else "ignore"

    val rights = listOf(/* "RUN_IN_BACKGROUND"), */ /* "BOOT_COMPLETED" */ "android.permission.RECEIVE_BOOT_COMPLETED")

    for (right in rights) {
        // shell pm [grant|revoke] com.my.app android.permission.ACCESS_FINE_LOCATION
        shell.addCommand(
                "monkey -p $pkg -c android.intent.category.LAUNCHER 1 && am force-stop $pkg",
                // "monkey -p cm.aptoide.pt -c android.intent.category.LAUNCHER 1 && am force-stop cm.aptoide.pt",
                // "cmd pm $cmd $pkg $right",
                // "cmd appops set $pkg $right $cmd",
                1) { _, _, output: MutableList<String> ->
            val outputString = output.joinToString()
            val isSuccess = outputString.trim().isEmpty()
            callback(isSuccess)
            future.complete(isSuccess)
        }
    }

    /*
    shell.addCommand("cmd appops set $pkg RUN_IN_BACKGROUND $cmd", 1) { _, _, output: MutableList<String> ->
        val outputString = output.joinToString()
        val isSuccess = outputString.trim().isEmpty()
        callback(isSuccess)
        future.complete(isSuccess)
    }

    shell.addCommand("cmd appops set $pkg BOOT_COMPLETED $cmd", 1) { _, _, output: MutableList<String> ->
        val outputString = output.joinToString()
        val isSuccess = outputString.trim().isEmpty()
        callback(isSuccess)
        future.complete(isSuccess)
    } */

    return future
}